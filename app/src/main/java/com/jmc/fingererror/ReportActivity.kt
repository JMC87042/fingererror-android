package com.jmc.fingererror

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import org.json.JSONObject

/** 내 엄지 리포트 (아이폰 앱과 같은 모양 · 횟수와 키 위치만, 친 글은 없음) */
class ReportActivity : Activity() {
    private val BG = Color.rgb(242, 242, 247)
    private val INK = Color.rgb(28, 28, 30)
    private val GRAY = Color.rgb(142, 142, 147)
    private val LINE = Color.rgb(230, 230, 235)
    private val ACCENT = Color.rgb(255, 110, 80)
    private val GREEN = Color.rgb(46, 90, 40)

    private var english = false
    private var dp = 1f
    private lateinit var jua: Typeface
    private lateinit var col: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        dp = resources.displayMetrics.density
        jua = Themes.font(this)
        window.statusBarColor = BG
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(20), px(16), px(28))
        }
        setContentView(ScrollView(this).apply { setBackgroundColor(BG); addView(col) })
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun px(v: Int) = (v * dp).toInt()
    private fun px(v: Float) = (v * dp).toInt()

    private fun round(color: Int, r: Float) = GradientDrawable().apply { cornerRadius = r * dp; setColor(color) }

    private fun text(s: String, size: Float, color: Int = INK, face: Typeface? = null, bold: Boolean = false) =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            if (face != null) typeface = face else if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }

    private fun lp(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(top); bottomMargin = px(bottom)
        }

    private fun header(s: String) = col.addView(text(s, 15f, GRAY, jua).apply { setPadding(px(12), 0, 0, px(6)) }, lp(top = 4))

    private fun card(): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = round(Color.WHITE, 12f)
            setPadding(px(16), px(10), px(16), px(10))
        }
        col.addView(c, lp(bottom = 18))
        return c
    }

    private fun divider(c: LinearLayout) = c.addView(View(this).apply { setBackgroundColor(LINE) },
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, px(0.7f))))

    /** 왼쪽 글자 + 오른쪽 회색 글자 한 줄 */
    private fun row(c: LinearLayout, left: String, right: String?, leftBold: Boolean, bullet: Boolean = false) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, px(11), 0, px(11)) }
        if (bullet) r.addView(text("•", 15f, Color.rgb(255, 140, 165), bold = true).apply { setPadding(0, 0, px(10), 0) })
        r.addView(text(left, 16f, INK, bold = leftBold), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (right != null) r.addView(text(right, 15f, GRAY))
        c.addView(r)
    }

    private fun render() {
        col.removeAllViews()
        col.addView(text("내 엄지 리포트", 30f, INK, jua), lp(bottom = 8))
        val r = Learner(this).lastReport()

        if (r == null) {
            val c = card()
            c.setPadding(px(16), px(16), px(16), px(16))
            c.addView(text("첫 리포트를 기다리는 중이에요", 18f, INK, jua))
            c.addView(text("핑거에러가 오타를 100개 고칠 때마다 리포트가 여기에 올라와요. 올라오면 키보드에서 팻핑이가 알려줄게요.", 13f, GRAY).apply { setPadding(0, px(6), 0, 0) })
            settings()
            return
        }

        val at = r.optInt("at")
        val date = r.optString("date")
        col.addView(text("오타 ${at}개 리포트 · $date", 16f, GREEN, bold = true))
        col.addView(text("다음 리포트는 오타 ${at + 100}개 때 올라와요", 12.5f, GRAY), lp(top = 2))
        val mode = r.optString("mode", "B")
        val handName = when (mode.removePrefix("W")) { "R" -> "한손 (오른쪽)"; "L" -> "한손 (왼쪽)"; else -> "양손" }
        col.addView(text("엄지 기록: " + (if (mode.startsWith("W")) "폴드 펼침 · " else "") + handName + " 모드", 12.5f, GRAY), lp(top = 2, bottom = 12))

        segmented()

        // 리포트 직전 7일 / 그 전 7일
        val idx = if (english) 1 else 0
        val slot = if (english) 3 else 1
        val daily = r.optJSONObject("daily")
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        try { fmt.parse(date)?.let { cal.time = it } } catch (e: Exception) {}
        var thisW = 0; var lastW = 0
        for (i in 0 until 14) {
            val v = daily?.optJSONArray(fmt.format(cal.time))?.optInt(slot, 0) ?: 0
            if (i < 7) thisW += v else lastW += v
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        header("리포트 직전 7일 동안 고친 오타")
        val w = card()
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(text("${thisW}개", 38f, INK, jua), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(text("그 전 7일 ${lastW}개", 15f, GRAY))
        w.addView(top)
        val fixes = r.optJSONArray("fixes")?.optInt(idx, 0) ?: 0
        val taps = r.optJSONArray("taps")?.optInt(idx, 0) ?: 0
        w.addView(text("전체 고친 오타 ${fixes}개 · 전체 타수 ${String.format("%,d", taps)}", 12f, GRAY).apply { setPadding(0, px(4), 0, px(6)) })

        // 자주 헷갈리는 키
        header("자주 헷갈리는 키")
        val conf = r.optJSONObject("conf")
        val list = ArrayList<Triple<String, String, Int>>()
        if (conf != null) {
            val it = conf.keys()
            while (it.hasNext()) {
                val k = it.next()
                val p = k.split(">"); val n = conf.optInt(k)
                if (p.size == 2 && n > 0 && p[0].all { ch -> ch.code < 128 } == english) list.add(Triple(p[1], p[0], n))
            }
        }
        list.sortByDescending { it.third }
        val cc = card()
        if (list.isEmpty()) row(cc, "조금 더 써보면 보여요", null, false)
        else list.take(5).forEachIndexed { i, (want, hit, n) ->
            if (i > 0) divider(cc)
            row(cc, "$want 치려다 $hit", "${n}번", true)
        }

        // 엄지 지도 (키보드 모양)
        header("내 엄지 지도")
        thumbMap(card(), r.optJSONObject("off"))

        // 받은 기념일
        header("받은 기념일")
        val rc = card()
        val reached = r.optJSONArray("reached")
        if (reached == null || reached.length() == 0) row(rc, "첫 기념일은 오타 22개 (투투)!", null, false)
        else for (i in reached.length() - 1 downTo 0) {
            if (i < reached.length() - 1) divider(rc)
            val s = reached.optString(i)
            val n = s.drop(1).toIntOrNull() ?: 0
            val t = if (s.startsWith("d")) (if (n == 365) "팻핑이와 만난 지 1년" else "팻핑이와 만난 지 ${n}일")
                    else when (n) { 22 -> "오타 22개 (투투)"; 365 -> "오타 365개 (1주년)"; 1000 -> "오타 1000개 (천일)"; else -> "오타 ${n}개" }
            row(rc, t, null, false, bullet = true)
        }
        settings()
    }

    /** 한글 | 영어 둥근 전환 */
    private fun segmented() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = round(Color.rgb(232, 232, 237), 9f)
            setPadding(px(2), px(2), px(2), px(2))
        }
        for ((i, lab) in listOf("한글", "영어").withIndex()) {
            val on = (i == 1) == english
            outer.addView(text(lab, 14f, INK, bold = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, px(7), 0, px(7))
                background = if (on) round(Color.WHITE, 7f) else null
                setOnClickListener { english = i == 1; render() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val c = card()
        c.setPadding(px(8), px(8), px(8), px(8))
        c.addView(outer)
    }

    private fun thumbMap(c: LinearLayout, off: JSONObject?) {
        c.setPadding(px(10), px(12), px(10), px(12))
        val ko = listOf("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ")
        val en = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val arrows = listOf("→", "↗", "↑", "↖", "←", "↙", "↓", "↘")
        val indent = listOf(0, 14, 40)
        for (r in 0 until 3) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(px(indent[r]), 0, px(indent[r]), 0) }
            val labels = if (english) en[r] else ko[r]
            for (i in labels.indices) {
                val o = off?.optJSONArray(ko[r][i].toString())
                val a = if (o == null || Math.hypot(o.optDouble(0), o.optDouble(1)) < 0.06) "·" else {
                    val deg = Math.toDegrees(Math.atan2(-o.optDouble(1), o.optDouble(0)))
                    arrows[(((deg + 360 + 22.5) % 360) / 45).toInt() % 8]
                }
                val key = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = round(BG, 6f)
                    setPadding(0, px(3), 0, px(4))
                    addView(text(labels[i].toString(), 11f, GRAY).apply { gravity = Gravity.CENTER })
                    addView(text(a, 16f, if (a == "·") GRAY else ACCENT, bold = true).apply { gravity = Gravity.CENTER })
                }
                line.addView(key, LinearLayout.LayoutParams(0, px(44), 1f).apply { setMargins(px(2), 0, px(2), 0) })
            }
            c.addView(line, lp(bottom = 6))
        }
        c.addView(text("화살표는 그 키를 누를 때 엄지가 대략 쏠리는 쪽이에요.", 11.5f, GRAY).apply { setPadding(px(4), px(4), 0, 0) })
    }

    /** 기념일 알림 · 안내 · 데이터 초기화 */
    private fun settings() {
        val prefs = getSharedPreferences("fingererror_theme", MODE_PRIVATE)
        val sc = card()
        sc.setPadding(px(16), px(6), px(16), px(6))
        sc.addView(Switch(this).apply {
            text = "기념일 알림"; textSize = 16f; setTextColor(INK)
            isChecked = !prefs.getBoolean("toastOff", false)
            setOnCheckedChangeListener { _, on -> prefs.edit().putBoolean("toastOff", !on).apply() }
            setPadding(0, px(8), 0, px(8))
        })
        col.addView(text("이 기록은 폰 안에만 있고, 친 글은 저장하지 않아요.", 11.5f, GRAY).apply { setPadding(px(12), 0, 0, 0) },
            lp(top = -10, bottom = 18))

        val rc = card()
        rc.addView(text("데이터 초기화", 16f, Color.rgb(220, 50, 50)).apply {
            setPadding(0, px(8), 0, px(8))
            setOnClickListener {
                AlertDialog.Builder(this@ReportActivity)
                    .setTitle("데이터를 초기화할까요?")
                    .setMessage("엄지 습관, 고친 오타 수, 리포트, 기념일이 모두 지워지고 처음부터 다시 배워요. 테마와 설정은 그대로예요. 되돌릴 수 없어요.")
                    .setPositiveButton("초기화") { _, _ ->
                        Learner(this@ReportActivity).requestReset()
                        render()
                        col.addView(text("초기화했어요 · 키보드를 열면 적용돼요", 12.5f, GRAY).apply { setPadding(px(12), 0, 0, 0) })
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        })
        col.addView(text("엄지 습관, 고친 오타 수, 리포트, 기념일이 지워져요. 테마와 설정은 그대로예요.", 11.5f, GRAY).apply { setPadding(px(12), 0, px(12), 0) },
            lp(top = -10, bottom = 8))
    }
}
