package com.jmc.fingererror

/** 글자 하나 안에서의 자리: 몇 번째 글자(syl)의 초성/중성/종성(part) */
data class Slot(val syl: Int, val part: String)

/** 두벌식 자모 → 완성형 한글 조합 */
object Hangul {
    val CHO = listOf("ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
    val JUNG = listOf("ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ")
    val JONG = listOf("", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
    val VCOMB = mapOf("ㅗㅏ" to "ㅘ", "ㅗㅐ" to "ㅙ", "ㅗㅣ" to "ㅚ", "ㅜㅓ" to "ㅝ", "ㅜㅔ" to "ㅞ", "ㅜㅣ" to "ㅟ", "ㅡㅣ" to "ㅢ")
    val JCOMB = mapOf(
        "ㄱㅅ" to "ㄳ", "ㄴㅈ" to "ㄵ", "ㄴㅎ" to "ㄶ", "ㄹㄱ" to "ㄺ", "ㄹㅁ" to "ㄻ", "ㄹㅂ" to "ㄼ",
        "ㄹㅅ" to "ㄽ", "ㄹㅌ" to "ㄾ", "ㄹㅍ" to "ㄿ", "ㄹㅎ" to "ㅀ", "ㅂㅅ" to "ㅄ"
    )
    val VSPLIT: Map<String, List<String>> = VCOMB.entries.associate { it.value to listOf(it.key.substring(0, 1), it.key.substring(1, 2)) }
    val JSPLIT: Map<String, List<String>> = JCOMB.entries.associate { it.value to listOf(it.key.substring(0, 1), it.key.substring(1, 2)) }
    private val SIMPLE_V = setOf("ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅛ", "ㅜ", "ㅠ", "ㅡ", "ㅣ")
    val SHIFT = mapOf("ㅂ" to "ㅃ", "ㅈ" to "ㅉ", "ㄷ" to "ㄸ", "ㄱ" to "ㄲ", "ㅅ" to "ㅆ", "ㅐ" to "ㅒ", "ㅔ" to "ㅖ")

    fun isC(s: String) = s in CHO
    fun isV(s: String) = s in SIMPLE_V
    fun isJamo(s: String) = isC(s) || isV(s)

    fun assemble(tokens: List<String>): String {
        val sb = StringBuilder()
        var cho: String? = null
        var jung: String? = null
        var jong: String? = null

        fun flush() {
            val c = cho
            val v = jung
            if (c != null && v != null) {
                val code = 0xAC00 + (CHO.indexOf(c) * 21 + JUNG.indexOf(v)) * 28 + JONG.indexOf(jong ?: "")
                sb.append(code.toChar())
            } else {
                if (c != null) sb.append(c)
                if (v != null) sb.append(v)
            }
            cho = null; jung = null; jong = null
        }

        for (t in tokens) {
            if (isC(t)) {
                val c = cho
                val v = jung
                val j = jong
                if (c == null && v == null) {
                    cho = t
                } else if (c != null && v == null) {
                    flush(); cho = t
                } else if (v != null && j == null) {
                    if (c != null && t in JONG) jong = t else { flush(); cho = t }
                } else {
                    val comb = JCOMB[(j ?: "") + t]
                    if (comb != null) jong = comb else { flush(); cho = t }
                }
            } else if (isV(t)) {
                val j = jong
                val v = jung
                if (j != null) {
                    val sp = JSPLIT[j]
                    val move: String
                    if (sp != null) { jong = sp[0]; move = sp[1] } else { jong = null; move = j }
                    flush(); cho = move; jung = t
                } else if (v != null) {
                    val comb = VCOMB[v + t]
                    if (comb != null) jung = comb else { flush(); jung = t }
                } else {
                    jung = t
                }
            } else {
                flush(); sb.append(t)
            }
        }
        flush()
        return sb.toString()
    }

    /** 각 자모가 몇 번째 글자의 어느 자리인지 (받침이 다음 글자로 넘어가는 것까지 반영) */
    fun trackSlots(tokens: List<String>): Pair<List<Slot?>, Int> {
        val res = MutableList<Slot?>(tokens.size) { null }
        var syl = -1
        var cho: String? = null
        var jung: String? = null
        var jong: String? = null
        val jongI = ArrayList<Int>()

        fun reset() { cho = null; jung = null; jong = null; jongI.clear() }
        fun newCho(t: String, i: Int) { reset(); syl++; cho = t; res[i] = Slot(syl, "cho") }

        for ((i, t) in tokens.withIndex()) {
            if (isC(t)) {
                val c = cho
                val v = jung
                val j = jong
                if (c == null && v == null) newCho(t, i)
                else if (c != null && v == null) newCho(t, i)
                else if (v != null && j == null) {
                    if (c != null && t in JONG) { jong = t; jongI.clear(); jongI.add(i); res[i] = Slot(syl, "jong") } else newCho(t, i)
                } else {
                    val comb = JCOMB[(j ?: "") + t]
                    if (comb != null) { jong = comb; jongI.add(i); res[i] = Slot(syl, "jong") } else newCho(t, i)
                }
            } else if (isV(t)) {
                val v = jung
                if (jong != null && jongI.isNotEmpty()) {
                    val mi = jongI[jongI.size - 1]
                    val mt = tokens[mi]
                    reset(); syl++; cho = mt; res[mi] = Slot(syl, "cho")
                    jung = t; res[i] = Slot(syl, "jung")
                } else if (v != null) {
                    val comb = VCOMB[v + t]
                    if (comb != null) { jung = comb; res[i] = Slot(syl, "jung") }
                    else { reset(); syl++; jung = t; res[i] = Slot(syl, "jung") }
                } else {
                    if (cho == null) syl++
                    jung = t; res[i] = Slot(syl, "jung")
                }
            }
        }
        return Pair(res, syl + 1)
    }

    /** 완성형 한 글자를 자모로 분해 (지우기를 자모 단위로 하기 위해) */
    fun decompose(ch: Char): List<String>? {
        val v = ch.code
        if (v in 0xAC00..0xD7A3) {
            val idx = v - 0xAC00
            val ci = idx / (21 * 28)
            val vi = (idx % (21 * 28)) / 28
            val ji = idx % 28
            val r = ArrayList<String>()
            r.add(CHO[ci])
            r.addAll(VSPLIT[JUNG[vi]] ?: listOf(JUNG[vi]))
            if (ji > 0) r.addAll(JSPLIT[JONG[ji]] ?: listOf(JONG[ji]))
            return r
        }
        val s = ch.toString()
        if (isJamo(s)) return listOf(s)
        return VSPLIT[s]
    }
}
