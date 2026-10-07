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
    }

    enum class Page { HANGUL, NUMBER }

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

    init {
        isHapticFeedbackEnabled = true
        build()
    }

    fun toggleMode() {
        page = if (page == Page.HANGUL) Page.NUMBER else Page.HANGUL
        shift = false
        build()
    }

    private fun build() {
        keys.clear()
        fun letters(s: String, row: Int, isLetter: Boolean) = s.map { Key(it.toString(), isLetter, row, 1f, false, false) }
        val rows: List<List<Key>> = if (page == Page.HANGUL) listOf(
            letters("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", 0, true),
            letters("ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", 1, true),
            listOf(Key("SHIFT", false, 2, 1f, true, true)) + letters("ㅋㅌㅊㅍㅠㅜㅡ", 2, true) + listOf(Key("BACK", false, 2, 1f, true, true)),
            bottomRow()
        ) else listOf(
            letters("1234567890", 0, false),
            letters("-/:;()₩&@\"", 1, false),
            letters("=?!'~%*", 2, false) + listOf(Key("BACK", false, 2, 1f, true, true)),
            bottomRow()
        )
        rows.forEach { keys.addAll(it) }
        layoutKeys()
        invalidate()
    }

    private fun bottomRow() = listOf(
        Key("MODE", false, 3, 1.3f, false, true),
        Key("GLOBE", false, 3, 1.1f, false, true),
        Key(",", false, 3, 1f, false, false),
        Key("SPACE", false, 3, 1f, true, false),
        Key(".", false, 3, 1f, false, false),
        Key("ENTER", false, 3, 1.9f, false, true)
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (236 * dp).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys()
    }

    private fun layoutKeys() {
        if (width == 0 || height == 0 || keys.isEmpty()) return
        val wAll = width - side * 2
        val u = (wAll - 9 * gap) / 10
        val nRows = 4
        val rowH = (height - topPad - bottomPad - vgap * (nRows - 1)) / nRows
        for (r in 0 until nRows) {
            val row = keys.filter { it.row == r }
            val fixed = row.filter { !it.fill }.sumOf { (it.weight * u).toDouble() }.toFloat()
            val fills = row.count { it.fill }
            val gaps = gap * (row.size - 1)
            val fillW = if (fills > 0) maxOf(u, (wAll - fixed - gaps) / fills) else 0f
            val total = fixed + fills * fillW + gaps
            var x = side + maxOf(0f, (wAll - total) / 2)
            val y = topPad + r * (rowH + vgap)
            for (k in row) {
                val kw = if (k.fill) fillW else k.weight * u
                k.rect.set(x, y, x + kw, y + rowH)
                x += kw + gap
            }
        }
    }

    private fun label(k: Key): String = when (k.id) {
        "SHIFT" -> "⇧"
        "BACK" -> "⌫"
        "SPACE" -> ""
        "ENTER" -> "⏎"
        "GLOBE" -> "🌐"
        "MODE" -> if (page == Page.HANGUL) "!#1" else "가"
        else -> if (shift) (Hangul.SHIFT[k.id] ?: k.id) else k.id
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
            val logo = bmp(t.spaceRes) ?: spaceLogo
            if (k.id == "SPACE" && logo != null) {
                // 스페이스바에 팻핑이 (테마에 따라 밤 모자 등)
                val s = (faceBottom - top) * (if (t.spaceRes != 0) 0.86f else 0.72f)
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
            textPaint.textSize = if (k.id == "MODE") 16f * dp else 21f * dp
            val fm = textPaint.fontMetrics
            canvas.drawText(label(k), k.cx, cy - (fm.ascent + fm.descent) / 2, textPaint)
        }
        if (menuOpen) drawMenu(canvas)
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
        Themes.setCurrent(context, id)
        theme = Themes.current(context, night)
        closeMenu()
    }

    private fun layoutMenu() {
        val space = keys.firstOrNull { it.id == "SPACE" }?.rect ?: RectF(0f, height - 50 * dp, width.toFloat(), height.toFloat())
        panel.set(8 * dp, 6 * dp, width - 8 * dp, maxOf(126 * dp, space.top - 6 * dp))
        val pad = 10 * dp; val g = 6 * dp
        val cw = (panel.width() - pad * 2 - g * 4) / 5
        val rowsTop = panel.top + 32 * dp
        val rowH = (panel.bottom - rowsTop - 6 * dp) / 2
        val ih = minOf(cw * 0.6f, rowH - 18 * dp)
        chipRects.clear(); nameRects.clear()
        for ((i, t) in Themes.all.withIndex()) {
            val x = panel.left + pad + (i % 5) * (cw + g)
            val y = rowsTop + (i / 5) * rowH
            chipRects[t.id] = RectF(x, y, x + cw, y + ih)
            nameRects[t.id] = RectF(x - 2 * dp, y + ih + 2 * dp, x + cw + 2 * dp, y + ih + 16 * dp)
        }
    }

    private fun itemAt(x: Float, y: Float): String? {
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
                val idx = listener?.onPress(pid, x, y)
                if (idx != null) {
                    down[pid] = idx
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
                listener?.onRelease(pid)
                down.remove(pid)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelHold()
                menuPid = -1
                for (pid in down.keys.toList()) listener?.onRelease(pid)
                down.clear()
                invalidate()
            }
        }
        return true
    }
}
