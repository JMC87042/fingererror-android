package com.jmc.fingererror

import kotlin.math.max
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 한 사람의 엄지 습관 (이 폰 안에만 저장, 인터넷 사용 안 함) */
class Learner(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("fingererror", Context.MODE_PRIVATE)

    /** 키별 엄지 쏠림: [dx, dy, n] (키 크기 대비 비율) */
    val off = HashMap<String, DoubleArray>()
    /** 왼손/오른손 전체 쏠림: [dx, dy, n] */
    val side = hashMapOf("L" to DoubleArray(3), "R" to DoubleArray(3))
    /** "실제로 눌린 키>치려던 키" 횟수 */
    val conf = HashMap<String, Int>()
    /** 자리별 실수표: "head|mid|end_cho|jung|jong" → { "눌린>의도": 횟수 } */
    val cell = HashMap<String, HashMap<String, Int>>()
    /** 띄어쓰기·문장부호 실수 { ".>SPACE": 3 } */
    val spConf = HashMap<String, Int>()
    /** 되돌린 어미 교정 조합 */
    val endNo = HashSet<String>()

    var vx = 0.06
    var vy = 0.06
    var spreadN = 0
    var dirH = 0
    var dirV = 0
    var taps = 0
    var learned = 0
    var fixes = 0
    var fixOn = true
    // 리포트·기념일용 기록 (친 글은 저장하지 않고 횟수만)
    var tapsEn = 0
    var fixesEn = 0
    val daily = java.util.TreeMap<String, IntArray>()   // 날짜 → [한글 타수, 한글 고침, 영어 타수, 영어 고침]
    var installDay: String? = null
    var countDone = 0
    var dayDone = 0
    val reached = ArrayList<String>()
    /** 마지막으로 앱에 올린 리포트 (고친 오타 100개 단위) */
    var reportAt = 0
    /** 지금 쓰는 손 모드: "B" 양손 / "R" 한손 오른쪽 / "L" 한손 왼쪽 (모드마다 엄지 기록을 따로) */
    var mode = "B"
        private set
    private val profiles = HashMap<String, JSONObject>()

    init { load() }

    private fun load() {
        try {
            val s = prefs.getString("data", null) ?: return
            val o = JSONObject(s)
            o.optJSONObject("off")?.let { m -> m.keys().forEach { k -> off[k] = arr3(m.getJSONArray(k)) } }
            o.optJSONObject("side")?.let { m -> m.keys().forEach { k -> side[k] = arr3(m.getJSONArray(k)) } }
            o.optJSONObject("conf")?.let { m -> m.keys().forEach { k -> conf[k] = m.getInt(k) } }
            o.optJSONObject("spConf")?.let { m -> m.keys().forEach { k -> spConf[k] = m.getInt(k) } }
            o.optJSONObject("cell")?.let { m ->
                m.keys().forEach { c ->
                    val inner = m.getJSONObject(c)
                    val t = HashMap<String, Int>()
                    inner.keys().forEach { k -> t[k] = inner.getInt(k) }
                    cell[c] = t
                }
            }
            o.optJSONArray("endNo")?.let { a -> for (i in 0 until a.length()) endNo.add(a.getString(i)) }
            vx = o.optDouble("vx", 0.06)
            vy = o.optDouble("vy", 0.06)
            spreadN = o.optInt("spreadN", 0)
            dirH = o.optInt("dirH", 0)
            dirV = o.optInt("dirV", 0)
            taps = o.optInt("taps", 0)
            learned = o.optInt("learned", 0)
            fixes = o.optInt("fixes", 0)
            fixOn = o.optBoolean("fixOn", true)
            tapsEn = o.optInt("tapsEn", 0)
            fixesEn = o.optInt("fixesEn", 0)
            o.optJSONObject("daily")?.let { m -> m.keys().forEach { k ->
                val a = m.getJSONArray(k); daily[k] = IntArray(4) { i -> a.optInt(i, 0) } } }
            installDay = o.optString("installDay", "").ifEmpty { null }
            countDone = o.optInt("countDone", 0)
            dayDone = o.optInt("dayDone", 0)
            o.optJSONArray("reached")?.let { a -> for (i in 0 until a.length()) reached.add(a.getString(i)) }
            reportAt = o.optInt("reportAt", 0)
            mode = o.optString("mode", "B")
            o.optJSONObject("profiles")?.let { m -> m.keys().forEach { k -> profiles[k] = m.getJSONObject(k) } }
        } catch (e: Exception) {
            // 기록이 깨졌으면 새로 시작
        }
    }

    private fun arr3(a: JSONArray) = doubleArrayOf(a.optDouble(0, 0.0), a.optDouble(1, 0.0), a.optDouble(2, 0.0))
    private fun jarr(d: DoubleArray) = JSONArray().apply { d.forEach { put(it) } }

    fun save() {
        val o = JSONObject()
        o.put("off", JSONObject().apply { off.forEach { (k, v) -> put(k, jarr(v)) } })
        o.put("side", JSONObject().apply { side.forEach { (k, v) -> put(k, jarr(v)) } })
        o.put("conf", JSONObject().apply { conf.forEach { (k, v) -> put(k, v) } })
        o.put("spConf", JSONObject().apply { spConf.forEach { (k, v) -> put(k, v) } })
        o.put("cell", JSONObject().apply {
            cell.forEach { (c, t) -> put(c, JSONObject().apply { t.forEach { (k, v) -> put(k, v) } }) }
        })
        o.put("endNo", JSONArray().apply { endNo.forEach { put(it) } })
        o.put("vx", vx); o.put("vy", vy); o.put("spreadN", spreadN)
        o.put("dirH", dirH); o.put("dirV", dirV)
        o.put("taps", taps); o.put("learned", learned); o.put("fixes", fixes)
        o.put("fixOn", fixOn)
        o.put("tapsEn", tapsEn); o.put("fixesEn", fixesEn)
        o.put("daily", JSONObject().apply { daily.forEach { (k, v) -> put(k, JSONArray().apply { v.forEach { put(it) } }) } })
        o.put("installDay", installDay ?: "")
        o.put("countDone", countDone); o.put("dayDone", dayDone)
        o.put("reached", JSONArray().apply { reached.forEach { put(it) } })
        o.put("reportAt", reportAt)
        o.put("mode", mode)
        o.put("profiles", JSONObject().apply { profiles.forEach { (k, v) -> put(k, v) } })
        prefs.edit().putString("data", o.toString()).apply()
    }

    /** 앱에서 초기화를 누름 → 키보드가 다음에 열릴 때 메모리 속 기록까지 지움 */
    fun requestReset() {
        prefs.edit().putLong("resetAt", System.currentTimeMillis()).remove("report").remove("data").apply()
    }

    /** 앱에서 초기화를 눌렀으면 지우고 true */
    fun applyResetIfAsked(): Boolean {
        val at = prefs.getLong("resetAt", 0)
        if (at <= prefs.getLong("resetDone", 0)) return false
        reset()
        prefs.edit().putLong("resetDone", at).apply()
        return true
    }

    fun reset() {
        off.clear(); conf.clear(); cell.clear(); spConf.clear(); endNo.clear()
        side["L"] = DoubleArray(3); side["R"] = DoubleArray(3)
        vx = 0.06; vy = 0.06; spreadN = 0; dirH = 0; dirV = 0
        taps = 0; learned = 0; fixes = 0
        tapsEn = 0; fixesEn = 0; daily.clear(); countDone = 0; reached.clear()
        dayDone = 0; installDay = null; reportAt = 0
        profiles.clear()
        prefs.edit().remove("report").apply()
        save()
    }

    // ---------------- 리포트·기념일 ----------------

    private fun today(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    private fun bumpDay(slot: Int, by: Int = 1) {
        val row = daily.getOrPut(today()) { IntArray(4) }
        row[slot] = max(0, row[slot] + by)
        while (daily.size > 70) daily.remove(daily.firstKey())
    }

    fun countTap(english: Boolean) { if (english) tapsEn++ else taps++; bumpDay(if (english) 2 else 0) }
    fun countFix(english: Boolean) { if (english) fixesEn++ else fixes++; bumpDay(if (english) 3 else 1) }
    /** 키보드가 고쳤는데 사용자가 바로 되돌림 → 잘못 고친 것이라 숫자에서 뺌 */
    fun uncountFix(english: Boolean) { if (english) fixesEn = max(0, fixesEn - 1) else fixes = max(0, fixes - 1); bumpDay(if (english) 3 else 1, -1) }
    val totalFixes get() = fixes + fixesEn

    /** 오타 기념일: 22 · 100 · 200 · 300 · 365 · 500 · 1000 → 이후 1000개마다 */
    fun newCountMilestone(): Int? {
        val n = totalFixes
        val list = arrayListOf(22, 100, 200, 300, 365, 500, 1000)
        var k = 2000
        while (k <= n) { list.add(k); k += 1000 }
        val m = list.filter { it <= n }.lastOrNull() ?: return null
        if (m <= countDone) return null
        countDone = m
        reached.add("c$m")
        return m
    }

    /** 고친 오타가 100개 단위(100·200·300…)에 닿으면 리포트를 앱에 올림. 올렸으면 그 개수를 돌려줌 */
    fun publishReportIfDue(): Int? {
        val h = totalFixes / 100 * 100
        if (h < 100 || h <= reportAt) return null
        reportAt = h
        val r = JSONObject()
        r.put("at", h); r.put("date", today()); r.put("mode", mode)
        r.put("taps", JSONArray().apply { put(taps); put(tapsEn) })
        r.put("fixes", JSONArray().apply { put(fixes); put(fixesEn) })
        r.put("daily", JSONObject().apply { daily.forEach { (k, v) -> put(k, JSONArray().apply { v.forEach { put(it) } }) } })
        r.put("conf", JSONObject().apply { conf.forEach { (k, v) -> put(k, v) } })
        r.put("off", JSONObject().apply { off.forEach { (k, v) -> if (v[2] >= 5) put(k, jarr(v)) } })
        r.put("reached", JSONArray().apply { reached.forEach { put(it) } })
        prefs.edit().putString("report", r.toString()).apply()
        return h
    }

    /** 앱에 올라간 리포트 (없으면 null) */
    fun lastReport(): JSONObject? = prefs.getString("report", null)?.let { try { JSONObject(it) } catch (e: Exception) { null } }

    /** 날짜 기념일 (만난 지 22·100·200·300·365·500·1000일). 3일 넘게 지났으면 조용히 넘김 */
    fun newDayMilestone(): Int? {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val inst = installDay ?: run { installDay = today(); return null }
        val start = try { fmt.parse(inst) } catch (e: Exception) { null } ?: return null
        val now = fmt.parse(today()) ?: return null
        val days = ((now.time - start.time) / 86_400_000L).toInt() + 1
        val m = listOf(22, 100, 200, 300, 365, 500, 1000).filter { it <= days }.lastOrNull() ?: return null
        if (m <= dayDone) return null
        dayDone = m
        if (days - m > 3) return null
        reached.add("d$m")
        return m
    }

    // ---------------- 손 모드별 엄지 기록 ----------------

    /** 엄지 습관 부분만 (쏠림·흔들림·실수표) */
    private fun physToJson(): JSONObject = JSONObject().apply {
        put("off", JSONObject().apply { off.forEach { (k, v) -> put(k, jarr(v)) } })
        put("side", JSONObject().apply { side.forEach { (k, v) -> put(k, jarr(v)) } })
        put("conf", JSONObject().apply { conf.forEach { (k, v) -> put(k, v) } })
        put("cell", JSONObject().apply { cell.forEach { (c, t) -> put(c, JSONObject().apply { t.forEach { (k, v) -> put(k, v) } }) } })
        put("vx", vx); put("vy", vy); put("spreadN", spreadN); put("dirH", dirH); put("dirV", dirV)
    }

    private fun physFromJson(o: JSONObject) {
        off.clear(); conf.clear(); cell.clear()
        side["L"] = DoubleArray(3); side["R"] = DoubleArray(3)
        o.optJSONObject("off")?.let { m -> m.keys().forEach { k -> off[k] = arr3(m.getJSONArray(k)) } }
        o.optJSONObject("side")?.let { m -> m.keys().forEach { k -> side[k] = arr3(m.getJSONArray(k)) } }
        o.optJSONObject("conf")?.let { m -> m.keys().forEach { k -> conf[k] = m.getInt(k) } }
        o.optJSONObject("cell")?.let { m -> m.keys().forEach { c ->
            val inner = m.getJSONObject(c); val t = HashMap<String, Int>()
            inner.keys().forEach { k -> t[k] = inner.getInt(k) }; cell[c] = t } }
        vx = o.optDouble("vx", 0.06); vy = o.optDouble("vy", 0.06)
        spreadN = o.optInt("spreadN", 0); dirH = o.optInt("dirH", 0); dirV = o.optInt("dirV", 0)
    }

    /** 좌우로 뒤집은 기록 (오른손 기록 → 왼손 출발점) */
    private fun mirrored(src: JSONObject): JSONObject {
        val rows = listOf("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅋㅌㅊㅍㅠㅜㅡ")
        val mir = HashMap<String, String>()
        rows.forEach { r -> r.forEachIndexed { i, c -> mir[c.toString()] = r[r.length - 1 - i].toString() } }
        mir["SHIFT"] = "BACK"; mir["BACK"] = "SHIFT"
        fun m(k: String) = mir[k] ?: k
        val out = JSONObject()
        val off = JSONObject()
        src.optJSONObject("off")?.let { o -> o.keys().forEach { k ->
            val a = o.getJSONArray(k); off.put(m(k), JSONArray().apply { put(-a.optDouble(0)); put(a.optDouble(1)); put(a.optDouble(2)) }) } }
        out.put("off", off)
        val sd = JSONObject()
        src.optJSONObject("side")?.let { o -> o.keys().forEach { k ->
            val a = o.getJSONArray(k); sd.put(if (k == "L") "R" else "L", JSONArray().apply { put(-a.optDouble(0)); put(a.optDouble(1)); put(a.optDouble(2)) }) } }
        out.put("side", sd)
        val cf = JSONObject()
        src.optJSONObject("conf")?.let { o -> o.keys().forEach { k ->
            val p = k.split(">"); if (p.size == 2) cf.put("${m(p[0])}>${m(p[1])}", o.getInt(k)) } }
        out.put("conf", cf)
        for (k in listOf("vx", "vy", "spreadN", "dirH", "dirV")) if (src.has(k)) out.put(k, src.get(k))
        return out
    }

    /** 손 모드 바꾸기: 지금 기록은 보관하고, 새 모드 기록을 꺼냄 (처음이면 출발점을 만들어 줌) */
    fun switchMode(newMode: String) {
        if (newMode == mode) return
        profiles[mode] = physToJson()
        val next = profiles[newMode] ?: run {
            val other = when {
                newMode.endsWith("L") -> newMode.dropLast(1) + "R"
                newMode.endsWith("R") -> newMode.dropLast(1) + "L"
                else -> null
            }
            when {
                newMode == "B" -> JSONObject()
                other != null && profiles[other] != null -> mirrored(profiles[other]!!)
                other != null && mode == other -> mirrored(profiles[mode]!!)
                else -> JSONObject((profiles[if (newMode.startsWith("W")) "WB" else "B"] ?: profiles["B"])?.toString() ?: physToJson().toString()).apply {
                    // 양손 기록에서 출발 (자리별 실수표는 새로)
                    remove("cell")
                }
            }
        }
        physFromJson(next)
        mode = newMode
        save()
    }
}
