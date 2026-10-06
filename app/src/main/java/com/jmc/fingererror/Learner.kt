package com.jmc.fingererror

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
        prefs.edit().putString("data", o.toString()).apply()
    }

    fun reset() {
        off.clear(); conf.clear(); cell.clear(); spConf.clear(); endNo.clear()
        side["L"] = DoubleArray(3); side["R"] = DoubleArray(3)
        vx = 0.06; vy = 0.06; spreadN = 0; dirH = 0; dirV = 0
        taps = 0; learned = 0; fixes = 0
        save()
    }
}
