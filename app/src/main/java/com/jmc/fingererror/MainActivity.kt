package com.jmc.fingererror

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 키보드 켜는 법 안내 + 연습 칸 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        fun text(s: String, size: Float, bold: Boolean = false) = TextView(this).apply {
            this.text = s
            textSize = size
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
        }

        col.addView(text("핑거에러", 28f, true))
        col.addView(text("쓸수록 내 엄지에 맞춰지는 키보드", 15f).apply { setTextColor(Color.GRAY) })
        col.addView(text("1. 아래 '키보드 설정 열기'에서 핑거에러를 켜세요.\n2. '키보드 바꾸기'로 핑거에러를 고르세요.\n3. 틀린 글자를 ⌫로 지우고 바로 다시 치면 그 실수를 배워요. 같은 실수가 쌓이면 자동으로 고쳐줘요.\n\n기록은 이 폰 안에만 저장되고 인터넷을 쓰지 않아요.", 16f))

        col.addView(Button(this).apply {
            text = "키보드 설정 열기"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        col.addView(Button(this).apply {
            text = "키보드 바꾸기"
            setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
        })
        col.addView(text("여기서 연습해 보세요", 16f, true))
        col.addView(EditText(this).apply { hint = "핑거에러로 쳐보세요"; minLines = 3 })
        col.addView(Button(this).apply {
            text = "학습 기록 초기화"
            setOnClickListener { Learner(this@MainActivity).reset(); text = "초기화했어요 (키보드를 다시 열면 적용)" }
        })

        setContentView(ScrollView(this).apply { addView(col) })
    }
}
