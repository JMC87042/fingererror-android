package com.jmc.fingererror

/** 기념일 말풍선 문구 (테마마다 팻핑이 말투가 달라짐) */
object Anniversary {
    fun tag(n: Int): String = when (n) {
        22 -> "투투"
        365 -> "1주년"
        1000 -> "천일"
        else -> if (n < 1000) "${n}일" else "${n}개"
    }

    private val countCopy = mapOf(
        "default" to ("{t}! 오타 {n}개 기념일" to "팻핑이랑 같이 고쳤어요"),
        "oled" to ("{t}. 오타 {n}개" to "조용히, 확실하게 고쳤어요"),
        "mint" to ("{t}~ 오타 {n}개" to "레트로하게 고쳐드렸어요"),
        "navy" to ("{t}! 오타 {n}개 홈런" to "팻핑이가 다 받아냈어요"),
        "lime" to ("{t}! 오타 {n}개" to "새싹처럼 쑥쑥 맞춰지는 중"),
        "lavender" to ("{t}♡ 오타 {n}개" to "향기롭게 고쳐줬어요"),
        "coral" to ("{t}! 밤 {n}톨 모았다" to "오타 {n}개 고쳤어요"),
        "yellow" to ("{t}! 은행잎 {n}장" to "오타 {n}개 고쳤어요"),
        "winter" to ("{t}! 눈송이 {n}개" to "오타 {n}개 고쳤어요"),
        "red" to ("{t}! 메리 오타마스" to "오타 {n}개 고쳤어요")
    )

    fun countText(theme: String, n: Int): Pair<String, String> {
        val c = countCopy[theme] ?: countCopy.getValue("default")
        fun fill(s: String) = s.replace("{t}", tag(n)).replace("{n}", n.toString())
        return fill(c.first) to fill(c.second)
    }

    const val REPORT_SUB = "리포트가 앱에 올라갔어요"

    /** 100개마다 리포트 도착 알림 */
    fun reportText(n: Int): Pair<String, String> = "📊 오타 ${n}개 리포트 도착!" to "핑거에러 앱에서 확인해 보세요"

    fun dayText(d: Int): Pair<String, String> =
        (if (d == 365) "팻핑이와 만난 지 1년" else "팻핑이와 만난 지 ${d}일") to "오늘도 네 엄지 편이야"
}
