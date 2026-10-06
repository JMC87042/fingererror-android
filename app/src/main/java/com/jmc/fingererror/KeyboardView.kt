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
    private val bgColor = if (night) Color.rgb(31, 31, 31) else Color.rgb(209, 213, 219)
    private val keyColor = if (night) Color.rgb(107, 107, 107) else Color.WHITE
    private val specialColor = if (night) Color.rgb(69, 69, 69) else Color.rgb(171, 178, 189)
    private val textColor = if (night) Color.WHITE else Color.rgb(24, 32, 46)
    private val accent = Color.rgb(230, 62, 122)

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 0, 0, 0) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val spaceLogo = BitmapFactory.decodeResource(resources, R.drawable.space_logo)
    private val logoRect = RectF()

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
        "MODE" -> if (page == Page.HANGUL) "123" else "가"
        else -> if (shift) (Hangul.SHIFT[k.id] ?: k.id) else k.id
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bgColor)
        val r = 6f * dp
        val pressed = down.values.toSet()
        for ((i, k) in keys.withIndex()) {
            val isDown = i in pressed
            val base = if (k.special) specialColor else keyColor
            val shiftOn = k.id == "SHIFT" && shift
            keyPaint.color = when {
                shiftOn -> textColor
                isDown -> if (k.special) keyColor else specialColor
                else -> base
            }
            canvas.drawRoundRect(k.rect.left, k.rect.top + 1.2f * dp, k.rect.right, k.rect.bottom + 1.2f * dp, r, r, shadowPaint)
            canvas.drawRoundRect(k.rect, r, r, keyPaint)

            val text = label(k)
            textPaint.color = if (shiftOn) bgColor else textColor
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = when {
                k.id == "SPACE" -> 15f * dp
                k.id == "MODE" -> 16f * dp
                else -> 21f * dp
            }
            if (k.id == "SPACE" && spaceLogo != null) {
                // 스페이스바에 엄지 로고
                val s = k.h * 0.72f
                logoRect.set(k.cx - s / 2, k.cy - s / 2, k.cx + s / 2, k.cy + s / 2)
                canvas.drawBitmap(spaceLogo, null, logoRect, bmpPaint)
                continue
            }
            val fm = textPaint.fontMetrics
            val ty = k.cy - (fm.ascent + fm.descent) / 2
            canvas.drawText(text, k.cx, ty, textPaint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val pid = e.getPointerId(i)
                val idx = listener?.onPress(pid, e.getX(i), e.getY(i))
                if (idx != null) down[pid] = idx
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pid = e.getPointerId(e.actionIndex)
                listener?.onRelease(pid)
                down.remove(pid)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                for (pid in down.keys.toList()) listener?.onRelease(pid)
                down.clear()
                invalidate()
            }
        }
        return true
    }
}
