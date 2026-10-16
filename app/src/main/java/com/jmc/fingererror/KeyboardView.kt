package com.jmc.fingererror

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View

/** 두벌식 자판을 그리고, 터치 위치를 서비스에 넘겨주는 화면 */
class KeyboardView(ctx: Context) : View(ctx) {

    interface Listener {
        /** 손가락이 닿음. 눌린 키의 번호를 돌려주면 그 키를 눌린 모양으로 그림 */
        fun onPress(pointerId: Int, x: Float, y: Float): Int?
        fun onRelease(pointerId: Int)
        /** 꾹 눌러 테마 메뉴를 연 손가락: 입력하지 않고 버림 */
        fun onCancel(pointerId: Int)
        /** 한/A 꾹 누르기 → 다른 키보드 고르기 */
        fun onLangHold()
        /** 손 모드 바꾸기: "B" 양손 / "R" 한손 오른쪽 / "L" 한손 왼쪽 */
        fun onHandChange(mode: String)
        /** 폴드를 펼치거나 접어서 넓은 화면 여부가 바뀜 → 학습 기록 바꾸기 */
        fun onWideChange()
    }

    enum class Page { HANGUL, ENGLISH, NUMBER }

    class Key(val id: String, val letter: Boolean, val row: Int, val weight: Float, val fill: Boolean, val special: Boolean) {
        val rect = RectF()
        val cx get() = rect.centerX()
        val cy get() = rect.centerY()
        val w get() = rect.width()
        val h get() = rect.height()
    }

    var listener: Listener? = null
    val keys = ArrayList<Key>()
    var page = Page.HANGUL
        private set
    var shift = false
        set(v) { field = v; invalidate() }
    var capsLock = false
        set(v) { field = v; invalidate() }
    /** 숫자 화면에서 돌아갈 글자 화면 */
    var letterPage = Page.HANGUL
        private set

    private val dp = resources.displayMetrics.density
    val gap = 6f * dp
    val vgap = 11f * dp
    private val side = 3f * dp
    private val topPad = 8f * dp
    private val bottomPad = 6f * dp
    private val down = HashMap<Int, Int>()

    private val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private var theme: Theme = Themes.current(ctx, night)

    /** 앱에서 고른 테마를 다시 읽음 (키보드가 열릴 때마다) */
    fun reloadTheme() {
        readNumRow()
        val t = Themes.current(context, night)
        if (t != theme) { theme = t; invalidate() }
    }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val spaceLogo = BitmapFactory.decodeResource(resources, R.drawable.space_logo)
    private val logoRect = RectF()
    private val tmpRect = RectF()
    private val bmpCache = HashMap<Int, android.graphics.Bitmap?>()
    private fun bmp(res: Int) = if (res == 0) null else bmpCache.getOrPut(res) { BitmapFactory.decodeResource(resources, res) }
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 255, 255, 255) }
    /** 계절 스티커 자리 (키 안에서의 비율) */
    private val stickerSpots = mapOf("ㅂ" to (0.08f to 0.12f), "ㅔ" to (0.92f to 0.12f),
        "SHIFT" to (0.10f to 0.15f), "BACK" to (0.90f to 0.15f), "SPACE" to (0.96f to 0.20f))
    /** 작은 잎이 앉는 자리: 키 → (가로 비율, 세로 비율, 그림 번호). 글자는 안 가림 */
    private val sprinkleSpots = mapOf(
        "ㅈ" to Triple(0.86f, 0.84f, 0), "ㄱ" to Triple(0.14f, 0.16f, 1), "ㅛ" to Triple(0.86f, 0.16f, 2), "ㅑ" to Triple(0.14f, 0.84f, 0),
        "ㅐ" to Triple(0.86f, 0.84f, 1), "ㄴ" to Triple(0.86f, 0.16f, 2), "ㄹ" to Triple(0.14f, 0.84f, 0), "ㅗ" to Triple(0.86f, 0.84f, 1),
        "ㅏ" to Triple(0.14f, 0.16f, 2), "ㅋ" to Triple(0.14f, 0.84f, 1), "ㅊ" to Triple(0.86f, 0.16f, 0), "ㅠ" to Triple(0.14f, 0.16f, 2),
        "ㅡ" to Triple(0.86f, 0.84f, 0), "ㅇ" to Triple(0.14f, 0.84f, 2))
    private fun resId(name: String) = resources.getIdentifier(name, "drawable", context.packageName)

    /** 숫자 줄 보이기 (갤럭시 기본값: 켬). 켜면 글자 키 크기는 그대로 두고 키보드만 한 줄 높아짐 */
    var numRow = ctx.getSharedPreferences("fingererror_theme", Context.MODE_PRIVATE).getBoolean("numRow", true)
        private set

    init {
        isHapticFeedbackEnabled = true
        build()
    }

    /** 기호 화면 쪽수 (삼성 키보드처럼 1/2 · 2/2) */
    var symPage = 0
        private set

    fun toggleMode() {
        if (page == Page.NUMBER) page = letterPage else { letterPage = page; page = Page.NUMBER; symPage = 0 }
        shift = false
        build()
        requestLayout()   // 숫자 줄을 끈 사람도 기호 화면은 5줄 (높이가 바뀔 수 있음)
    }

    /** 1/2 ↔ 2/2 */
    fun toggleSym() {
        symPage = 1 - symPage
        build()
    }

    /** 한/A: 한글 ↔ 영어 */
    fun toggleLang() {
        letterPage = if (letterPage == Page.ENGLISH || page == Page.ENGLISH) Page.HANGUL else Page.ENGLISH
        page = letterPage
        shift = false; capsLock = false
        build()
        requestLayout()
    }

    companion object {
        /** 영어 키 → 같은 자리의 한글 키 (엄지 쏠림 학습을 한글·영어가 함께 씀) */
        val EN2KO: Map<String, String> = run {
            val m = HashMap<String, String>()
            listOf("qwertyuiop" to "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "asdfghjkl" to "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "zxcvbnm" to "ㅋㅌㅊㅍㅠㅜㅡ")
                .forEach { (e, k) -> e.forEachIndexed { i, c -> m[c.toString()] = k[i].toString() } }
            m
        }
        fun posId(id: String) = EN2KO[id] ?: id
    }

    private fun build() {
        keys.clear()
        fun letters(s: String, row: Int, isLetter: Boolean) = s.map { Key(it.toString(), isLetter, row, 1f, false, false) }
        val rows: List<List<Key>> = if (page == Page.HANGUL) listOf(
            letters("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", 0, true),
            letters("ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", 1, true),
            listOf(Key("SHIFT", false, 2, 1f, true, true)) + letters("ㅋㅌㅊㅍㅠㅜㅡ", 2, true) + listOf(Key("BACK", false, 2, 1f, true, true)),
            bottomRow()
        ) else if (page == Page.ENGLISH) listOf(
            // 한글 자판과 키 위치·크기가 완전히 같음
            letters("qwertyuiop", 0, true),
            letters("asdfghjkl", 1, true),
            listOf(Key("SHIFT", false, 2, 1f, true, true)) + letters("zxcvbnm", 2, true) + listOf(Key("BACK", false, 2, 1f, true, true)),
            bottomRow()
        ) else if (symPage == 0) listOf(
            // 삼성 키보드 !#1 화면 1/2
            letters("1234567890", 0, false),
            letters("+×÷=/_<>[]", 1, false),
            letters("!@#₩%^&*()", 2, false),
            listOf(Key("SYMPG", false, 3, 1.5f, false, true)) + letters("-'\":;,?", 3, false) + listOf(Key("BACK", false, 3, 1.5f, false, true)),
            bottomRow()
        ) else listOf(
            // 삼성 키보드 !#1 화면 2/2
            letters("1234567890", 0, false),
            letters("`~\\|{}€£¥\$", 1, false),
            letters("°•○●□■♤♡◇♧", 2, false),
            listOf(Key("SYMPG", false, 3, 1.5f, false, true)) + letters("☆▪¤《》¡¿", 3, false) + listOf(Key("BACK", false, 3, 1.5f, false, true)),
            bottomRow()
        )
        // 삼성 기본 키보드처럼 글자 화면 맨 위에 숫자 줄 (앱에서 끌 수 있음)
        val all = if (numRow && page != Page.NUMBER) listOf(letters("1234567890", 0, false)) + rows else rows
        all.forEachIndexed { r, row -> row.forEach { k -> keys.add(Key(k.id, k.letter, r, k.weight, k.fill, k.special)) } }
        layoutKeys()
        invalidate()
    }


    private fun readNumRow() {
        val v = context.getSharedPreferences("fingererror_theme", Context.MODE_PRIVATE).getBoolean("numRow", true)
        if (v != numRow) { numRow = v; build(); requestLayout() }
    }

    private fun bottomRow() = listOf(
        Key("MODE", false, 3, 1.3f, false, true),
        Key("LANG", false, 3, 1.1f, false, true),
        Key(",", false, 3, 1f, false, false),
        Key("SPACE", false, 3, 1f, true, false)
    ) + (if (split && page != Page.NUMBER) listOf(Key("SPACE", false, 3, 1f, true, false)) else emptyList()) + listOf(
        Key(".", false, 3, 1f, false, false),
        Key("ENTER", false, 3, 1.9f, false, true)
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 글자 키 높이가 숫자 줄 유무와 상관없이 같도록 (학습 기록 유지)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), ((if (numRow || page == Page.NUMBER) 294.25f else 236f) * dp).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 폴드를 펼친 화면: 폴드8 울트라처럼 세로로 길면 일반 자판 그대로,
        // 폴드8(와이드)처럼 가로가 더 넓으면 삼성처럼 좌우로 나뉜 자판
        val cfg = resources.configuration
        val dm = resources.displayMetrics
        val nowWide = cfg.smallestScreenWidthDp >= 600
        val nowSplit = nowWide && dm.widthPixels > dm.heightPixels
        val changed = nowWide != wide || nowSplit != split
        wide = nowWide
        if (nowSplit != split) { split = nowSplit; build() } else layoutKeys()
        if (changed) listener?.onWideChange()
    }

    /** 넓은 화면(폴드 펼침) 여부 */
    var wide = false
        private set
    /** 좌우로 나뉜 자판 (폴드8 와이드를 펼쳤을 때) */
    var split = false
        private set
    private val splitL = RectF()
    private val splitR = RectF()
    private var baseX = 0f
    private var baseW = 0f

    /** 학습 기록 이름: 펼침이면 앞에 W (예: "B", "R", "WB") */
    fun profileKey() = (if (wide) "W" else "") + hand

    // ---------------- 한손 모드 ----------------

    /** "B" 양손 / "R" 오른쪽에 붙임 / "L" 왼쪽에 붙임 (키 배열은 같고 폭만 80%) */
    var hand = "B"
        set(v) { if (field != v) { field = v; if (v != "B") lastSide = v; layoutKeys(); invalidate() } }
    private var lastSide = "R"
    private val handPanel = RectF()
    private val moveBtn = RectF()
    private val bothBtn = RectF()
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun layoutHandPanel(xStart: Float, wAll: Float) {
        if (hand == "B") { handPanel.setEmpty(); return }
        if (hand == "R") handPanel.set(baseX - side, 0f, xStart - gap / 2, height.toFloat())
        else handPanel.set(xStart + wAll + gap / 2, 0f, baseX + baseW + side, height.toFloat())
        val r = minOf(handPanel.width() * 0.36f, 18 * dp)
        val cx = handPanel.centerX()
        moveBtn.set(cx - r, height * 0.36f - r, cx + r, height * 0.36f + r)
        bothBtn.set(cx - r, height * 0.66f - r, cx + r, height * 0.66f + r)
    }

    private fun drawHandPanel(canvas: Canvas) {
        if (hand == "B") return
        val t = theme
        for ((rc, lab) in listOf(moveBtn to (if (hand == "R") "◀" else "▶"), bothBtn to "양손")) {
            handPaint.style = Paint.Style.FILL
            handPaint.color = t.special
            canvas.drawOval(rc, handPaint)
            handPaint.color = t.specialText
            handPaint.textAlign = Paint.Align.CENTER
            handPaint.typeface = if (lab == "양손") Themes.font(context) else Typeface.DEFAULT
            handPaint.textSize = if (lab == "양손") 13 * dp else 17 * dp
            val fm = handPaint.fontMetrics
            canvas.drawText(lab, rc.centerX(), rc.centerY() - (fm.ascent + fm.descent) / 2, handPaint)
        }
    }

    /**
     * 폴드8(와이드) 펼친 화면의 삼성 분리 자판과 같은 배치
     * 왼쪽: 1~5 / ㅂㅈㄷㄱㅅ / ㅁㄴㅇㄹㅎ / ⇧ㅋㅌㅊㅍ / !#1 한/A , 스페이스
     * 오른쪽: 6~0 / ㅛㅕㅑㅐㅔ / ㅗㅓㅏㅣ / ㅠㅜㅡ⌫ / 스페이스 . 줄바꿈
     */
    private fun layoutSplit() {
        handPanel.setEmpty()
        val bw = width * 0.305f
        val m = width * 0.11f
        val lx = m
        val rx = width - m - bw
        splitL.set(lx, 0f, lx + bw, height.toFloat())
        splitR.set(rx, 0f, rx + bw, height.toFloat())
        baseX = lx; baseW = rx + bw - lx
        val g = gap
        val u = (bw - 4 * g) / 5
        val nRows = (keys.maxOfOrNull { it.row } ?: 3) + 1
        val rowH = (height - topPad - bottomPad - vgap * (nRows - 1)) / nRows
        val leftCount = if (nRows == 5) intArrayOf(5, 5, 5, 5, 4) else intArrayOf(5, 5, 5, 4)
        for (r in 0 until nRows) {
            val row = keys.filter { it.row == r }
            val y = topPad + r * (rowH + vgap)
            val left = row.take(leftCount[r]); val right = row.drop(leftCount[r])
            fun put(k: Key, x: Float, w: Float) = k.rect.set(x, y, x + w, y + rowH)
            val bottom = r == nRows - 1
            val shiftRow = r == nRows - 2
            val indentRow = r == nRows - 3
            // 왼쪽
            var x = lx + if (indentRow) u * 0.5f else 0f
            for (k in left) {
                val w = when {
                    bottom && k.id == "SPACE" -> lx + bw - x
                    k.id == "MODE" || k.id == "SHIFT" -> u * 1.25f
                    else -> u
                }
                put(k, x, w); x += w + g
            }
            // 오른쪽
            if (bottom) {
                var xr = rx + bw
                for (k in right.reversed()) {
                    val w = if (k.id == "SPACE") xr - rx else u
                    xr -= w; put(k, xr, w); xr -= g
                }
            } else {
                var xr = rx + if (indentRow) u * 0.5f else 0f
                for (k in right) {
                    val w = if (shiftRow && k.id == "BACK") rx + bw - xr else u
                    put(k, xr, w); xr += w + g
                }
            }
        }
    }

    private fun layoutKeys() {
        if (width == 0 || height == 0 || keys.isEmpty()) return
        if (split && hand == "B" && page != Page.NUMBER) { layoutSplit(); return }
        splitL.setEmpty(); splitR.setEmpty()
        // 나뉜 자판의 숫자·기호 화면만 가운데 85%로, 그 밖에는 화면 폭 그대로 (삼성과 같음)
        val region = if (split) width * 0.85f else width.toFloat()
        baseX = (width - region) / 2 + side
        baseW = region - side * 2
        val wAll = if (hand == "B") baseW else baseW * 0.80f
        val xStart = if (hand == "R") baseX + baseW - wAll else baseX
        layoutHandPanel(xStart, wAll)
        val u = (wAll - 9 * gap) / 10
        val nRows = (keys.maxOfOrNull { it.row } ?: 3) + 1
        val rowH = (height - topPad - bottomPad - vgap * (nRows - 1)) / nRows
        for (r in 0 until nRows) {
            val row = keys.filter { it.row == r }
            val fixed = row.filter { !it.fill }.sumOf { (it.weight * u).toDouble() }.toFloat()
            val fills = row.count { it.fill }
            val gaps = gap * (row.size - 1)
            val fillW = if (fills > 0) maxOf(u, (wAll - fixed - gaps) / fills) else 0f
            val total = fixed + fills * fillW + gaps
            var x = xStart + maxOf(0f, (wAll - total) / 2)
            val y = topPad + r * (rowH + vgap)
            for (k in row) {
                val kw = if (k.fill) fillW else k.weight * u
                k.rect.set(x, y, x + kw, y + rowH)
                x += kw + gap
            }
        }
    }

    private fun label(k: Key): String = when (k.id) {
        "SHIFT" -> if (capsLock) "⇪" else "⇧"
        "LANG" -> if (page == Page.ENGLISH || (page == Page.NUMBER && letterPage == Page.ENGLISH)) "한" else "A"
        "BACK" -> "⌫"
        "SPACE" -> ""
        "ENTER" -> "⏎"
        "GLOBE" -> "🌐"
        "MODE" -> if (page != Page.NUMBER) "!#1" else if (letterPage == Page.ENGLISH) "ABC" else "가"
        "SYMPG" -> if (symPage == 0) "1/2" else "2/2"
        else -> if (page == Page.ENGLISH) (if (shift || capsLock) k.id.uppercase() else k.id)
                else if (shift) (Hangul.SHIFT[k.id] ?: k.id) else k.id
    }

    override fun onDraw(canvas: Canvas) {
        val t = theme
        canvas.drawColor(t.bg)
        bmp(t.bgRes)?.let { b ->
            // 배경 그림을 화면에 꽉 차게 (가운데 기준 자르기)
            val sc = maxOf(width / b.width.toFloat(), height / b.height.toFloat())
            val bw = b.width * sc; val bh = b.height * sc
            tmpRect.set((width - bw) / 2, (height - bh) / 2, (width + bw) / 2, (height + bh) / 2)
            canvas.drawBitmap(b, null, tmpRect, bmpPaint)
        }
        val r = t.radiusDp * dp
        val depth = t.depthDp * dp
        val pressed = down.values.toSet()
        shadowPaint.color = t.shadow
        rimPaint.color = t.rim
        rimPaint.strokeWidth = 1f * dp
        for ((i, k) in keys.withIndex()) {
            val isDown = i in pressed
            val shiftOn = k.id == "SHIFT" && shift
            val isAccent = k.id == "ENTER" && t.accent != 0
            val face = when {
                shiftOn -> t.text
                isAccent -> t.accent
                isDown -> if (k.special) t.pressSpecial else t.pressKey
                k.special -> t.special
                else -> t.key
            }
            // 그리는 모양만 바뀌고, 누르는 판정 영역(k.rect)은 모든 테마가 같음
            var top = k.rect.top
            val faceBottom: Float
            if (depth > 0f) {
                keyPaint.color = if (k.special || isAccent) t.sideSpecial else t.side
                canvas.drawRoundRect(k.rect.left, k.rect.top + depth, k.rect.right, k.rect.bottom, r, r, keyPaint)
                if (isDown) top += depth * 0.6f
                faceBottom = top + k.h - depth
            } else {
                if (Color.alpha(t.shadow) > 0) {
                    val off = if (t.gloss) 2.5f * dp else 1.2f * dp
                    canvas.drawRoundRect(k.rect.left, k.rect.top + off, k.rect.right, k.rect.bottom + off, r, r, shadowPaint)
                }
                faceBottom = k.rect.bottom
            }
            keyPaint.color = face
            canvas.drawRoundRect(k.rect.left, top, k.rect.right, faceBottom, r, r, keyPaint)
            if (t.rim != 0) canvas.drawRoundRect(k.rect.left, top, k.rect.right, faceBottom, r, r, rimPaint)
            if (t.gloss && !isDown) {
                val gh = (faceBottom - top)
                canvas.drawRoundRect(k.rect.left + k.w * 0.12f, top + gh * 0.08f, k.rect.right - k.w * 0.12f, top + gh * 0.30f, r * 0.7f, r * 0.7f, glossPaint)
            }
            // 계절 스티커 (그림만, 누르는 영역과 무관)
            if (t.stickerRes != 0) stickerSpots[k.id]?.let { (fx, fy) ->
                bmp(t.stickerRes)?.let { b ->
                    val ss = 15f * dp
                    val sx = k.rect.left + k.w * fx; val sy = top + k.h * fy
                    tmpRect.set(sx - ss / 2, sy - ss / 2, sx + ss / 2, sy + ss / 2)
                    canvas.drawBitmap(b, null, tmpRect, bmpPaint)
                }
            }

            t.sprinkle?.let { pre -> sprinkleSpots[k.id]?.let { (fx, fy, n) ->
                bmp(resId(pre + n))?.let { b ->
                    val ss = 11f * dp
                    val sx = k.rect.left + k.w * fx; val sy = top + k.h * fy
                    tmpRect.set(sx - ss / 2, sy - ss / 2, sx + ss / 2, sy + ss / 2)
                    canvas.drawBitmap(b, null, tmpRect, bmpPaint)
                }
            } }

            val cy = (top + faceBottom) / 2
            val logo = bmp(resId("pat_" + t.id)) ?: spaceLogo
            if (k.id == "SPACE" && i != keys.indexOfFirst { it.id == "SPACE" }) continue   // 나뉜 자판의 두 번째 스페이스는 빈 키
            if (k.id == "SPACE" && logo != null) {
                // 스페이스바에 팻핑이 (테마에 따라 밤 모자 등)
                val s = (faceBottom - top) * 0.84f
                logoRect.set(k.cx - s / 2, cy - s / 2, k.cx + s / 2, cy + s / 2)
                canvas.drawBitmap(logo, null, logoRect, bmpPaint)
                continue
            }
            val enterBmp = if (k.id == "ENTER") bmp(t.enterRes) else null
            if (enterBmp != null) {
                val es = (faceBottom - top) * 0.72f
                tmpRect.set(k.cx - es / 2, cy - es / 2, k.cx + es / 2, cy + es / 2)
                canvas.drawBitmap(enterBmp, null, tmpRect, bmpPaint)
                continue
            }
            textPaint.color = when {
                shiftOn -> t.bg
                isAccent -> Color.rgb(255, 250, 240)
                k.special -> t.specialText
                else -> t.text
            }
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = if (k.id == "MODE" || k.id == "SYMPG") 16f * dp else 21f * dp
            val fm = textPaint.fontMetrics
            canvas.drawText(label(k), k.cx, cy - (fm.ascent + fm.descent) / 2, textPaint)
            if (k.id == "LANG") {
                // 작은 '한'/'A' + 길게 누르면 다른 키보드라는 작은 🌐
                val c = textPaint.color
                textPaint.textSize = 10f * dp
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.alpha = 150
                canvas.drawText(if (label(k) == "A") "한" else "A", k.rect.left + 5 * dp, top + 13 * dp, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("🌐", k.rect.right - 4 * dp, top + 13 * dp, textPaint)
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = c
            }
        }
        drawHandPanel(canvas)
        if (menuOpen) drawMenu(canvas)
        drawToast(canvas)
    }

    // ---------------- 기념일 말풍선 ----------------

    private var toastTitle = ""
    private var toastSub = ""
    private var toastStart = 0L
    private val toastPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val TOAST_MS = 2800L

    fun showToast(title: String, sub: String) {
        toastTitle = title; toastSub = sub
        toastStart = android.os.SystemClock.uptimeMillis()
        postInvalidateOnAnimation()
    }

    private fun drawToast(canvas: Canvas) {
        if (toastStart == 0L) return
        val el = android.os.SystemClock.uptimeMillis() - toastStart
        if (el > TOAST_MS) { toastStart = 0L; return }
        val a = when { el < 250 -> el / 250f; el > TOAST_MS - 350 -> (TOAST_MS - el) / 350f; else -> 1f }
        val t = theme
        val accent = if (t.accent != 0) t.accent else t.special
        val w = minOf(width - 30 * dp, 340 * dp); val h = 50 * dp
        val l = (width - w) / 2; val tp = 3 * dp
        val al = (a * 255).toInt()
        toastPaint.style = Paint.Style.FILL
        toastPaint.color = Color.WHITE; toastPaint.alpha = (a * 250).toInt()
        toastPaint.setShadowLayer(6 * dp, 0f, 3 * dp, Color.argb((a * 60).toInt(), 0, 0, 0))
        canvas.drawRoundRect(l, tp, l + w, tp + h, h / 2, h / 2, toastPaint)
        toastPaint.clearShadowLayer()
        toastPaint.style = Paint.Style.STROKE; toastPaint.strokeWidth = 1.5f * dp
        toastPaint.color = accent; toastPaint.alpha = al
        canvas.drawRoundRect(l, tp, l + w, tp + h, h / 2, h / 2, toastPaint)
        toastPaint.style = Paint.Style.FILL
        val r = h * 0.4f; val fcx = l + h * 0.52f; val fcy = tp + h / 2
        toastPaint.color = accent; toastPaint.alpha = (a * 56).toInt()
        canvas.drawCircle(fcx, fcy, r, toastPaint)
        bmpPaint.alpha = al
        bmp(resId("pat_" + t.id))?.let { b -> val fs = r * 1.6f; tmpRect.set(fcx - fs / 2, fcy - fs / 2, fcx + fs / 2, fcy + fs / 2); canvas.drawBitmap(b, null, tmpRect, bmpPaint) }
        bmp(resId("deco_" + t.id))?.let { b -> val ds = 36 * dp; val dx = l + w - h * 0.95f; tmpRect.set(dx, fcy - ds / 2, dx + ds, fcy + ds / 2); canvas.drawBitmap(b, null, tmpRect, bmpPaint) }
        bmpPaint.alpha = 255
        toastPaint.typeface = Themes.font(context)
        toastPaint.textAlign = Paint.Align.LEFT
        toastPaint.color = Color.rgb(54, 40, 44); toastPaint.alpha = al
        toastPaint.textSize = 16 * dp
        canvas.drawText(toastTitle, l + h * 1.05f, tp + 23 * dp, toastPaint)
        toastPaint.color = Color.rgb(130, 118, 122); toastPaint.alpha = al
        toastPaint.textSize = 12 * dp
        canvas.drawText(toastSub, l + h * 1.05f, tp + 41 * dp, toastPaint)
        postInvalidateOnAnimation()
    }

    // ---------------- 팻핑이 꾹 누르기 → 테마 메뉴 ----------------

    var menuOpen = false
        private set
    private var holdPid = -1
    private var holdX = 0f
    private var holdY = 0f
    private var menuPid = -1
    private var hot: String? = null
    private val panel = RectF()
    private val chipRects = LinkedHashMap<String, RectF>()
    private val nameRects = HashMap<String, RectF>()
    private val menuPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val menuText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val openMenu = Runnable { openMenuFromHold() }

    private fun cancelHold() { removeCallbacks(openMenu); holdPid = -1 }

    // 한/A 꾹 누르기
    private var langPid = -1
    private val langHold = Runnable {
        val pid = langPid
        langPid = -1
        if (pid >= 0 && down.containsKey(pid)) {
            listener?.onCancel(pid)
            down.remove(pid)
            invalidate()
            listener?.onLangHold()
        }
    }
    private fun cancelLang() { removeCallbacks(langHold); langPid = -1 }

    private fun openMenuFromHold() {
        val pid = holdPid
        if (pid < 0 || !down.containsKey(pid)) { holdPid = -1; return }
        // 꾹 누른 스페이스는 띄어쓰기로 넣지 않음 (학습에도 안 들어감)
        listener?.onCancel(pid)
        down.remove(pid)
        holdPid = -1
        menuPid = pid
        hot = null
        menuOpen = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        layoutMenu()
        invalidate()
    }

    fun closeMenu() {
        if (!menuOpen) return
        menuOpen = false; menuPid = -1; hot = null
        invalidate()
    }

    private fun choose(id: String) {
        if (id == "#hand") {
            closeMenu()
            listener?.onHandChange(if (hand == "B") lastSide else "B")
            return
        }
        Themes.setCurrent(context, id)
        theme = Themes.current(context, night)
        closeMenu()
    }

    /** 팻핑이 메뉴에 보일 진행 상황 ("고친 오타 412개 · 다음 리포트 500개") */
    var menuStatus = ""

    private val handBtn = RectF()

    private fun layoutMenu() {
        val space = keys.firstOrNull { it.id == "SPACE" }?.rect ?: RectF(0f, height - 50 * dp, width.toFloat(), height.toFloat())
        panel.set(baseX + 5 * dp, 6 * dp, baseX + baseW - 5 * dp, maxOf(126 * dp, space.top - 6 * dp))
        handBtn.set(panel.right - 118 * dp, panel.top + 7 * dp, panel.right - 10 * dp, panel.top + 29 * dp)
        val pad = 10 * dp; val g = 6 * dp
        val cw = (panel.width() - pad * 2 - g * 4) / 5
        val rowsTop = panel.top + 32 * dp
        val rowH = (panel.bottom - rowsTop - 6 * dp) / 2
        val ih = minOf(cw * 0.75f, rowH - 18 * dp)
        chipRects.clear(); nameRects.clear()
        for ((i, t) in Themes.all.withIndex()) {
            val x = panel.left + pad + (i % 5) * (cw + g)
            val y = rowsTop + (i / 5) * rowH
            chipRects[t.id] = RectF(x, y, x + cw, y + ih)
            nameRects[t.id] = RectF(x - 2 * dp, y + ih + 2 * dp, x + cw + 2 * dp, y + ih + 16 * dp)
        }
    }

    private fun itemAt(x: Float, y: Float): String? {
        if (handBtn.contains(x, y)) return "#hand"
        for ((id, r) in chipRects) {
            val n = nameRects[id] ?: continue
            if (x >= r.left - 3 * dp && x <= r.right + 3 * dp && y >= r.top - 3 * dp && y <= n.bottom + 3 * dp) return id
        }
        return null
    }

    private fun drawMenu(canvas: Canvas) {
        menuPaint.style = Paint.Style.FILL
        menuPaint.color = Color.argb(90, 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), menuPaint)
        menuPaint.color = Color.rgb(250, 250, 252)
        menuPaint.setShadowLayer(8 * dp, 0f, 3 * dp, Color.argb(70, 0, 0, 0))
        canvas.drawRoundRect(panel, 16 * dp, 16 * dp, menuPaint)
        menuPaint.clearShadowLayer()
        // 팻핑이를 가리키는 꼬리
        keys.firstOrNull { it.id == "SPACE" }?.let { sp ->
            val path = android.graphics.Path()
            path.moveTo(sp.cx - 8 * dp, panel.bottom - 1); path.lineTo(sp.cx + 8 * dp, panel.bottom - 1); path.lineTo(sp.cx, panel.bottom + 6 * dp); path.close()
            canvas.drawPath(path, menuPaint)
        }
        val jua = Themes.font(context)
        menuText.typeface = jua
        menuText.color = Color.rgb(30, 32, 40)
        menuText.textAlign = Paint.Align.LEFT
        menuText.textSize = 16f * dp
        canvas.drawText("테마 바꾸기", panel.left + 12 * dp, panel.top + 24 * dp, menuText)
        if (menuStatus.isNotEmpty()) {
            // 진행 상황: 고친 오타 수 · 다음 리포트
            val tw = menuText.measureText("테마 바꾸기")   // 제목 폭 (16dp)
            menuText.textSize = 10f * dp
            menuText.color = Color.rgb(120, 120, 128)
            canvas.drawText(menuStatus, panel.left + 12 * dp + tw + 8 * dp, panel.top + 23 * dp, menuText)
            menuText.textSize = 16f * dp
            menuText.color = Color.rgb(30, 32, 40)
        }
        // 한손/양손 모드 버튼 (메뉴 오른쪽 위)
        menuPaint.style = Paint.Style.FILL
        menuPaint.color = if (hot == "#hand") Color.rgb(255, 214, 200) else Color.rgb(234, 238, 228)
        canvas.drawRoundRect(handBtn, handBtn.height() / 2, handBtn.height() / 2, menuPaint)
        menuText.textAlign = Paint.Align.CENTER
        menuText.textSize = 13f * dp
        menuText.color = Color.rgb(50, 70, 40)
        canvas.drawText(if (hand == "B") "✋ 한손 모드" else "🙌 양손 모드", handBtn.centerX(), handBtn.centerY() + 4.5f * dp, menuText)
        menuText.color = Color.rgb(30, 32, 40)
        val cur = Themes.currentId(context)
        menuText.textAlign = Paint.Align.CENTER
        menuText.textSize = 11f * dp
        menuText.color = Color.rgb(60, 62, 70)
        for (t in Themes.all) {
            val r = chipRects[t.id] ?: continue
            bmp(resId("theme_" + t.id))?.let { b -> canvas.drawBitmap(b, null, r, bmpPaint) }
            val ring = when (t.id) { hot -> Color.rgb(255, 120, 90); cur -> Color.rgb(52, 199, 89); else -> 0 }
            if (ring != 0) {
                menuPaint.style = Paint.Style.STROKE
                menuPaint.strokeWidth = 2.5f * dp
                menuPaint.color = ring
                canvas.drawRoundRect(r.left - 2 * dp, r.top - 2 * dp, r.right + 2 * dp, r.bottom + 2 * dp, 7 * dp, 7 * dp, menuPaint)
                menuPaint.style = Paint.Style.FILL
            }
            val n = nameRects[t.id] ?: continue
            canvas.drawText(t.menuName, n.centerX(), n.bottom - 3 * dp, menuText)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val pid = e.getPointerId(i)
                val x = e.getX(i); val y = e.getY(i)
                if (menuOpen) {
                    // 메뉴가 떠 있는 동안의 터치는 글자 입력·학습과 무관
                    if (panel.contains(x, y)) { menuPid = pid; hot = itemAt(x, y) } else closeMenu()
                    invalidate()
                    return true
                }
                if (!splitL.isEmpty && !(x >= splitL.left - side && x <= splitL.right + gap * 3) && !(x >= splitR.left - gap && x <= splitR.right + side)) return true   // 나뉜 자판 사이·바깥 빈 곳은 무시
                if (splitL.isEmpty && split && (x < baseX - side || x > baseX + baseW + side)) return true
                if (hand != "B" && handPanel.contains(x, y)) {
                    // 빈 쪽: 버튼 두 개 말고는 아무 반응 없음 (키로 잘못 들어가지 않게)
                    val r = moveBtn.width() * 0.7f
                    if (Math.hypot((x - moveBtn.centerX()).toDouble(), (y - moveBtn.centerY()).toDouble()) < r)
                        listener?.onHandChange(if (hand == "R") "L" else "R")
                    else if (Math.hypot((x - bothBtn.centerX()).toDouble(), (y - bothBtn.centerY()).toDouble()) < r)
                        listener?.onHandChange("B")
                    return true
                }
                val idx = listener?.onPress(pid, x, y)
                if (idx != null) {
                    down[pid] = idx
                    if (keys.getOrNull(idx)?.id == "LANG") {
                        cancelLang(); langPid = pid
                        postDelayed(langHold, 500)
                    }
                    if (keys.getOrNull(idx)?.id == "SPACE") {
                        cancelHold()
                        holdPid = pid; holdX = x; holdY = y
                        postDelayed(openMenu, 600)
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val pid = e.getPointerId(i)
                    val x = e.getX(i); val y = e.getY(i)
                    if (pid == menuPid) {
                        val h = itemAt(x, y)
                        if (h != hot) { hot = h; invalidate() }
                    } else if (pid == holdPid && Math.hypot((x - holdX).toDouble(), (y - holdY).toDouble()) > 12 * dp) {
                        // 밀면 메뉴 대신 (나중에) 커서 이동용으로 남겨둠
                        cancelHold()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                val pid = e.getPointerId(i)
                if (pid == menuPid) {
                    menuPid = -1
                    val id = itemAt(e.getX(i), e.getY(i))
                    if (id != null) choose(id) else { hot = null; invalidate() }
                    return true
                }
                if (pid == holdPid) cancelHold()
                if (pid == langPid) cancelLang()
                listener?.onRelease(pid)
                down.remove(pid)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelHold()
                cancelLang()
                menuPid = -1
                for (pid in down.keys.toList()) listener?.onRelease(pid)
                down.clear()
                invalidate()
            }
        }
        return true
    }
}
