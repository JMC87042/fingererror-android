package com.jmc.fingererror

import android.content.Context
import java.io.File

/**
 * 예측 단어 (키보드 위 후보 3칸).
 * 인터넷 없이 폰 안에서만: 기본 단어장 + 내가 쓴 말을 배워서 자주 쓰는 순으로 추천.
 * - 쓰는 중인 단어: 자모 단위로 앞부분이 맞는 단어 ("안녕핫" → 안녕하세요)
 * - 아무것도 안 쓴 때: 앞 단어 다음에 자주 쓴 단어 ("감사" 다음 → 합니다)
 */
class Predictor(private val ctx: Context) {
    private val uni = HashMap<String, Int>()            // 단어 → 쓴 횟수
    private val bi = HashMap<String, HashMap<String, Int>>()   // 앞 단어 → (다음 단어 → 횟수)
    private val seed = LinkedHashSet<String>()
    private val file get() = File(ctx.filesDir, "predict.txt")
    private var dirty = false

    init {
        try {
            ctx.assets.open("words_ko.txt").bufferedReader().readText().split(Regex("\\s+"))
                .filter { it.isNotBlank() }.forEach { seed.add(it) }
        } catch (_: Exception) {}
        load()
    }

    private fun load() {
        try {
            if (!file.exists()) return
            file.forEachLine { line ->
                val p = line.split('\t')
                when {
                    p.size == 2 -> uni[p[0]] = p[1].toIntOrNull() ?: 1
                    p.size == 3 -> bi.getOrPut(p[0]) { HashMap() }[p[1]] = p[2].toIntOrNull() ?: 1
                }
            }
        } catch (_: Exception) {}
    }

    fun save() {
        if (!dirty) return
        try {
            // 너무 커지지 않게: 자주 쓴 단어 3000개, 이어지는 말 4000개까지만
            val sb = StringBuilder()
            uni.entries.sortedByDescending { it.value }.take(3000).forEach { sb.append(it.key).append('\t').append(it.value).append('\n') }
            bi.flatMap { (a, m) -> m.map { Triple(a, it.key, it.value) } }.sortedByDescending { it.third }.take(4000)
                .forEach { sb.append(it.first).append('\t').append(it.second).append('\t').append(it.third).append('\n') }
            file.writeText(sb.toString())
            dirty = false
        } catch (_: Exception) {}
    }

    fun clear() { uni.clear(); bi.clear(); dirty = true; save() }

    /** 단어 하나를 다 쓰고 띄었을 때 배우기 */
    fun learn(prev: String?, word: String) {
        val w = clean(word) ?: return
        uni[w] = (uni[w] ?: 0) + 1
        clean(prev)?.let { p -> bi.getOrPut(p) { HashMap() }.let { it[w] = (it[w] ?: 0) + 1 } }
        dirty = true
    }

    /** 후보 단어를 지우기 (후보를 꾹 누르면) */
    fun forget(word: String) {
        uni.remove(word); bi.values.forEach { it.remove(word) }; seed.remove(word); dirty = true
    }

    /** 후보 3개 */
    fun suggest(prev: String?, cur: String): List<String> {
        val out = ArrayList<String>()
        if (cur.isEmpty()) {
            val p = clean(prev) ?: return out
            bi[p]?.entries?.sortedByDescending { it.value }?.forEach { if (out.size < 3) out.add(it.key) }
            return out
        }
        val cj = jamo(cur)
        val scored = HashMap<String, Int>()
        for ((w, n) in uni) if (w != cur && w.length >= cur.length && jamo(w).startsWith(cj)) scored[w] = n * 10
        clean(prev)?.let { p -> bi[p]?.forEach { (w, n) -> if (w != cur && jamo(w).startsWith(cj)) scored[w] = (scored[w] ?: 0) + n * 30 } }
        for (w in seed) if (w != cur && w.length >= cur.length && jamo(w).startsWith(cj)) scored[w] = (scored[w] ?: 0) + 1
        scored.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.length })
            .forEach { if (out.size < 3) out.add(it.key) }
        return out
    }

    private fun clean(w: String?): String? {
        val t = w?.trim()?.trimEnd('.', ',', '!', '?', '~', ')', '"', '\'') ?: return null
        if (t.isEmpty() || t.length > 20 || t.any { it.isDigit() } || t.contains("http")) return null
        return t
    }

    companion object {
        private val CHO = arrayOf("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
        private val JUNG = arrayOf("ㅏ","ㅐ","ㅑ","ㅒ","ㅓ","ㅔ","ㅕ","ㅖ","ㅗ","ㅗㅏ","ㅗㅐ","ㅗㅣ","ㅛ","ㅜ","ㅜㅓ","ㅜㅔ","ㅜㅣ","ㅠ","ㅡ","ㅡㅣ","ㅣ")
        private val JONG = arrayOf("","ㄱ","ㄲ","ㄱㅅ","ㄴ","ㄴㅈ","ㄴㅎ","ㄷ","ㄹ","ㄹㄱ","ㄹㅁ","ㄹㅂ","ㄹㅅ","ㄹㅌ","ㄹㅍ","ㄹㅎ","ㅁ","ㅂ","ㅂㅅ","ㅅ","ㅆ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
        private val SPLIT = mapOf("ㄳ" to "ㄱㅅ","ㄵ" to "ㄴㅈ","ㄶ" to "ㄴㅎ","ㄺ" to "ㄹㄱ","ㄻ" to "ㄹㅁ","ㄼ" to "ㄹㅂ","ㄽ" to "ㄹㅅ","ㄾ" to "ㄹㅌ","ㄿ" to "ㄹㅍ","ㅀ" to "ㄹㅎ","ㅄ" to "ㅂㅅ",
            "ㅘ" to "ㅗㅏ","ㅙ" to "ㅗㅐ","ㅚ" to "ㅗㅣ","ㅝ" to "ㅜㅓ","ㅞ" to "ㅜㅔ","ㅟ" to "ㅜㅣ","ㅢ" to "ㅡㅣ")

        /** 한글을 자모로 풀기 (겹받침·겹모음도 낱자로) — 쓰는 중인 글자도 앞부분 비교가 되게 */
        fun jamo(s: String): String {
            val sb = StringBuilder()
            for (ch in s) {
                val c = ch.code
                if (c in 0xAC00..0xD7A3) {
                    val i = c - 0xAC00
                    sb.append(CHO[i / 588]).append(JUNG[(i % 588) / 28]).append(JONG[i % 28])
                } else {
                    val t = ch.toString()
                    sb.append(SPLIT[t] ?: t.lowercase())
                }
            }
            return sb.toString()
        }
    }
}
