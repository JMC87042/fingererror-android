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
    /** 3D 젤리 카드·버튼 배경: 진한 아랫단 + 위가 밝은 윗면. 윗면(테두리 표시용)도 같이 돌려줌 */
    private fun jelly(face: Int, side: Int, dp: Float, r: Float = 18f): Pair<android.graphics.drawable.LayerDrawable, GradientDrawable> {
        fun mix(a: Int, b: Int, f: Float) = Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt())
        val bottom = GradientDrawable().apply { cornerRadius = r * dp; setColor(side) }
        val top = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(mix(face, Color.WHITE, 0.4f), face)).apply { cornerRadius = r * dp }
        val ld = android.graphics.drawable.LayerDrawable(arrayOf(bottom, top))
        ld.setLayerInset(1, 0, 0, 0, (4 * dp).toInt())
        return Pair(ld, top)
    }
    private fun jellyButton(b: Button, dp: Float) {
        b.background = jelly(Color.rgb(255, 160, 132), Color.rgb(226, 120, 96), dp).first
        b.setTextColor(Color.WHITE)
        b.isAllCaps = false
        b.textSize = 17f
        b.typeface = Themes.font(this)
        b.stateListAnimator = null
        b.setPadding(0, (14 * dp).toInt(), 0, (18 * dp).toInt())
        b.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = (10 * dp).toInt() }
    }

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

        Trial.banner(this)?.let { b ->
            col.addView(text(b, 15f, true).apply {
                val over = Trial.over(this@MainActivity)
                setTextColor(if (over) Color.rgb(190, 70, 40) else Color.rgb(70, 120, 30))
                setPadding(pad / 2, pad / 2, pad / 2, pad / 2)
                background = GradientDrawable().apply {
                    cornerRadius = 14 * dp
                    setColor(if (over) Color.rgb(253, 236, 230) else Color.rgb(228, 242, 222))
                }
            })
        }

        addReport(col, dp)

        col.addView(Button(this).apply {
            text = "키보드 설정 열기"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
            jellyButton(this, dp)
        })
        col.addView(Button(this).apply {
            text = "키보드 바꾸기"
            setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
            jellyButton(this, dp)
        })
        // 테마 고르기 (색만 바뀌고 학습 기록은 그대로)
        col.addView(text("키보드 테마", 21f).apply { typeface = Themes.font(this@MainActivity) })
        val cards = ArrayList<Triple<Theme, GradientDrawable, ImageView>>()
        fun refreshCards() {
            val cur = Themes.currentId(this)
            val cap = Themes.capId(this)
            for ((t, top, img) in cards) {
                top.setStroke(
                    (if (t.id == cur) 3 else 1) * dp.toInt(),
                    if (t.id == cur) Color.rgb(120, 166, 60) else Color.rgb(220, 220, 220)
                )
                val rid = resources.getIdentifier(Themes.previewName(t, cap), "drawable", packageName)
                if (rid != 0) img.setImageResource(rid)
            }
        }
        // 키캡 모양 드롭다운 (컬러·레트로 테마에 적용)
        col.addView(text("키캡 모양", 15f, true))
        col.addView(android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, Themes.CAPS.map { it.second })
            setSelection(Themes.CAPS.indexOfFirst { it.first == Themes.capId(this@MainActivity) }.coerceAtLeast(0))
            background = jelly(Color.WHITE, Color.rgb(232, 220, 206), dp, 14f).first
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = (12 * dp).toInt() }
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    Themes.setCap(this@MainActivity, Themes.CAPS[pos].first)
                    refreshCards()
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        })
        for ((group, title) in listOf("color" to "테마 ${Themes.all.count { it.group == "color" }}가지 · 키캡 모양 적용", "special" to "버건디 스페셜")) {
            col.addView(text(title, 17f).apply { typeface = Themes.font(this@MainActivity) })
            for (t in Themes.all.filter { it.group == group }) {
                val jb = jelly(Color.WHITE, Color.rgb(232, 220, 206), dp, 16f)
                val img = ImageView(this@MainActivity).apply { adjustViewBounds = true }
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    val p = (10 * dp).toInt()
                    setPadding(p, p, p, p + (4 * dp).toInt())
                    background = jb.first
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = (12 * dp).toInt()
                    }
                    addView(TextView(this@MainActivity).apply {
                        this.text = t.name
                        textSize = 17f
                        typeface = Themes.font(this@MainActivity)
                        setPadding(0, 0, 0, (6 * dp).toInt())
                    })
                    addView(img)
                    setOnClickListener {
                        Themes.setCurrent(this@MainActivity, t.id)
                        refreshCards()
                    }
                }
                cards.add(Triple(t, jb.second, img))
                col.addView(card)
            }
        }
        refreshCards()
        col.addView(text("고른 테마는 키보드를 다시 열면 적용돼요. 키보드를 쓰다가 스페이스바 오른쪽 팻핑이를 꾹 누르면 바로 바꿀 수도 있어요.", 13f).apply { setTextColor(Color.GRAY) })

        col.addView(android.widget.Switch(this).apply {
            val prefs = getSharedPreferences("fingererror_theme", MODE_PRIVATE)
            text = "맨 위 숫자 줄 보이기 (삼성 키보드처럼)"
            textSize = 15f
            isChecked = prefs.getBoolean("numRow", true)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("numRow", on).apply() }
            setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
        })
        for ((key, label) in listOf("haptic" to "키 누를 때 진동", "sound" to "키 누를 때 소리 (무음 모드에선 안 나요)")) {
            col.addView(android.widget.Switch(this).apply {
                val prefs = getSharedPreferences("fingererror_theme", MODE_PRIVATE)
                text = label
                textSize = 15f
                isChecked = prefs.getBoolean(key, true)
                setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean(key, on).apply() }
                setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
            })
        }
        col.addView(text("여기서 연습해 보세요", 16f, true))
        col.addView(EditText(this).apply { hint = "핑거에러로 쳐보세요"; minLines = 3 })

        col.addView(text("버전 2026.11.12-11" + (if (Trial.isTrial) " 무료 체험판" else "") + " (버건디 스페셜) · 새 리포트 화면 · 100개마다 리포트 · 한/A 영어 · 숫자 줄 · 한손 모드 · 폴드8", 12f).apply { setTextColor(Color.GRAY) })
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.rgb(250, 242, 234)); addView(col) })
        window.statusBarColor = Color.rgb(250, 242, 234)
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
            setPadding(p, p, p, p + (4 * dp).toInt())
            background = jelly(Color.rgb(236, 246, 226), Color.rgb(196, 220, 176), dp).first
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
