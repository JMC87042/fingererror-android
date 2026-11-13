package com.jmc.fingererror

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/** 키보드 위 예측 단어 줄: 왼쪽 작은 팻핑이 + 후보 3칸 (테마 색을 따름) */
class SuggestBar(ctx: Context) : View(ctx) {
    var onPick: ((String) -> Unit)? = null
    var onForget: ((String) -> Unit)? = null
    private val dp = resources.displayMetrics.density
    private var words: List<String> = emptyList()
    private var theme: Theme? = null
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 16f * resources.displayMetrics.density }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val slots = Array(3) { RectF() }
    private var hot = -1
    private val pat = try { android.graphics.BitmapFactory.decodeResource(resources, R.drawable.space_logo) } catch (_: Exception) { null }
    private val patRect = RectF()
    private val longPress = Runnable { if (hot in words.indices) { onForget?.invoke(words[hot]); hot = -1; invalidate() } }
    private var longFired = false

    fun setTheme(t: Theme) { theme = t; invalidate() }
    fun setWords(w: List<String>) { if (w != words) { words = w; invalidate() } }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (38 * dp).toInt())
    }

    private fun dark(c: Int) = Color.red(c) * 0.3 + Color.green(c) * 0.59 + Color.blue(c) * 0.11 < 128

    override fun onDraw(c: Canvas) {
        val t = theme ?: return
        c.drawColor(t.bg)
        val fg = if (dark(t.bg)) Color.rgb(240, 240, 244) else Color.rgb(40, 40, 46)
        val ps = 26 * dp
        patRect.set(8 * dp, (height - ps) / 2, 8 * dp + ps, (height + ps) / 2)
        pat?.let { c.drawBitmap(it, null, patRect, null) }
        val left = patRect.right + 6 * dp
        val w = (width - left - 6 * dp) / 3
        for (i in 0 until 3) {
            slots[i].set(left + w * i, 3 * dp, left + w * (i + 1), height - 3 * dp)
            if (i == hot && i < words.size) {
                fill.color = Color.argb(60, Color.red(fg), Color.green(fg), Color.blue(fg))
                c.drawRoundRect(slots[i], 9 * dp, 9 * dp, fill)
            }
            if (i > 0) {
                line.color = Color.argb(70, Color.red(fg), Color.green(fg), Color.blue(fg))
                c.drawRect(slots[i].left, height * 0.28f, slots[i].left + 1 * dp, height * 0.72f, line)
            }
            val word = words.getOrNull(i) ?: continue
            text.color = fg
            text.isFakeBoldText = i == 0
            var s = word
            while (text.measureText(s) > slots[i].width() - 10 * dp && s.length > 1) s = s.dropLast(1)
            if (s != word) s = s.dropLast(1) + "…"
            val fm = text.fontMetrics
            c.drawText(s, slots[i].centerX(), slots[i].centerY() - (fm.ascent + fm.descent) / 2, text)
        }
    }

    private val lp = Runnable { longFired = true; longPress.run() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val i = slots.indexOfFirst { it.contains(e.x, e.y) }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                hot = i; longFired = false
                if (i in words.indices) postDelayed(lp, 650)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> if (i != hot) { removeCallbacks(lp); hot = -1; invalidate() }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(lp)
                if (!longFired && i == hot && i in words.indices) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    onPick?.invoke(words[i])
                }
                hot = -1; invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { removeCallbacks(lp); hot = -1; invalidate() }
        }
        return true
    }
}
