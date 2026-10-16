package com.jmc.fingererror

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.ImageView
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
        col.addView(text("1. 아래 '키보드 설정 열기'에서 핑거에러를 켜세요.\n2. '키보드 바꾸기'로 핑거에러를 고르세요.\n   (한/A 키: 한글↔영어, 꾹 누르면 다른 키보드)\n3. 틀린 글자를 ⌫로 지우고 바로 다시 치면 그 실수를 배워요. 같은 실수가 쌓이면 자동으로 고쳐줘요.\n\n기록은 이 폰 안에만 저장되고 인터넷을 쓰지 않아요.", 16f))

        addReport(col, dp)

        col.addView(Button(this).apply {
            text = "키보드 설정 열기"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        col.addView(Button(this).apply {
            text = "키보드 바꾸기"
            setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
        })
        // 테마 고르기 (색만 바뀌고 학습 기록은 그대로)
        col.addView(text("키보드 테마", 21f).apply { typeface = Themes.font(this@MainActivity) })
        val cards = ArrayList<Pair<String, LinearLayout>>()
        fun refreshCards() {
            val cur = Themes.currentId(this)
            for ((id, card) in cards) {
                (card.background as GradientDrawable).setStroke(
                    (if (id == cur) 3 else 1) * dp.toInt(),
                    if (id == cur) Color.rgb(120, 166, 60) else Color.rgb(220, 220, 220)
                )
            }
        }
        for (t in Themes.all) {
            val resId = resources.getIdentifier("theme_${t.id}", "drawable", packageName)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val p = (8 * dp).toInt()
                setPadding(p, p, p, p)
                background = GradientDrawable().apply { cornerRadius = 14 * dp; setColor(Color.WHITE) }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (12 * dp).toInt()
                }
                addView(TextView(this@MainActivity).apply {
                    this.text = t.name
                    textSize = 17f
                    typeface = Themes.font(this@MainActivity)
                    setPadding(0, 0, 0, (6 * dp).toInt())
                })
                if (resId != 0) addView(ImageView(this@MainActivity).apply {
                    setImageResource(resId)
                    adjustViewBounds = true
                })
                setOnClickListener {
                    Themes.setCurrent(this@MainActivity, t.id)
                    refreshCards()
                }
            }
            cards.add(Pair(t.id, card))
            col.addView(card)
        }
        refreshCards()
        col.addView(text("기본 테마는 폰의 다크모드를 따라가요. 고른 테마는 키보드를 다시 열면 적용돼요. 키보드를 쓰다가 스페이스바의 팻핑이를 꾹 누르면 바로 바꿀 수도 있어요.", 13f).apply { setTextColor(Color.GRAY) })

        col.addView(android.widget.Switch(this).apply {
            val prefs = getSharedPreferences("fingererror_theme", MODE_PRIVATE)
            text = "맨 위 숫자 줄 보이기 (삼성 키보드처럼)"
            textSize = 15f
            isChecked = prefs.getBoolean("numRow", true)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("numRow", on).apply() }
            setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
        })
        col.addView(text("여기서 연습해 보세요", 16f, true))
        col.addView(EditText(this).apply { hint = "핑거에러로 쳐보세요"; minLines = 3 })

        col.addView(text("버전 2026.10.17 (기호 1/2 · 2/2) · 새 리포트 화면 · 100개마다 리포트 · 한/A 영어 · 숫자 줄 · 한손 모드 · 폴드8", 12f).apply { setTextColor(Color.GRAY) })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    // ---------------- 내 엄지 리포트 (누르면 리포트 화면으로) ----------------

    private var refreshReport: (() -> Unit)? = null

    /** 키보드로 치고 돌아오면 요약을 새로 읽음 */
    override fun onResume() {
        super.onResume()
        refreshReport?.invoke()
    }

    private fun addReport(col: LinearLayout, dp: Float) {
        val jua = Themes.font(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * dp).toInt()
            setPadding(p, p, p, p)
            background = GradientDrawable().apply { cornerRadius = 16 * dp; setColor(Color.rgb(244, 248, 236)) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (8 * dp).toInt(); bottomMargin = (16 * dp).toInt()
            }
            setOnClickListener { startActivity(Intent(this@MainActivity, ReportActivity::class.java)) }
        }
        box.addView(TextView(this).apply { text = "📊 내 엄지 리포트  ›"; textSize = 21f; typeface = jua; setTextColor(Color.rgb(28, 28, 30)) })
        val sub = TextView(this).apply { textSize = 14f; setTextColor(Color.rgb(90, 110, 80)); setPadding(0, (6 * dp).toInt(), 0, 0) }
        box.addView(sub)
        refreshReport = {
            val r = Learner(this).lastReport()
            sub.text = if (r == null) "첫 리포트를 기다리는 중이에요 · 오타 100개마다 올라와요"
                       else "오타 ${r.optInt("at")}개 리포트 도착 · ${r.optString("date")}\n자주 헷갈리는 키, 엄지 지도, 기념일을 볼 수 있어요"
        }
        refreshReport?.invoke()
        col.addView(box)
    }
}
