package com.jmc.fingererror

import android.content.Context

/**
 * 14일 무료 체험판.
 * 정식판(TRIAL_DAYS = 0)은 아무 제한 없음.
 * 체험이 끝나면 자동 교정만 꺼지고, 타자·학습·리포트는 그대로 (정식판을 깔면 바로 교정 다시 켜짐).
 */
object Trial {
    val isTrial get() = BuildConfig.TRIAL_DAYS > 0
    private const val DAY = 24L * 60 * 60 * 1000

    /** 처음 설치한 날 (앱 데이터를 지워도 안 바뀜) */
    private fun start(c: Context): Long = try {
        c.packageManager.getPackageInfo(c.packageName, 0).firstInstallTime
    } catch (e: Exception) { System.currentTimeMillis() }

    /** 남은 날 (오늘 포함, 0이면 끝) */
    fun daysLeft(c: Context): Int {
        if (!isTrial) return Int.MAX_VALUE
        val used = ((System.currentTimeMillis() - start(c)) / DAY).toInt()
        return (BuildConfig.TRIAL_DAYS - used).coerceAtLeast(0)
    }

    fun over(c: Context) = isTrial && daysLeft(c) <= 0

    /** 키보드 알림: 남은 3일부터·끝난 뒤엔 하루에 한 번 */
    fun noticeToday(c: Context): Pair<String, String>? {
        if (!isTrial) return null
        val left = daysLeft(c)
        if (left > 3) return null
        val sp = c.getSharedPreferences("fingererror_trial", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (sp.getString("noticed", "") == today) return null
        sp.edit().putString("noticed", today).apply()
        return if (left <= 0) Pair("무료 체험이 끝났어요", "자동 교정은 쉬고 있어요 · 팻핑이는 계속 배우는 중")
        else Pair("무료 체험 ${left}일 남았어요", "정식판으로 바꿔도 배운 기록은 그대로예요")
    }

    /** 앱 첫 화면 안내 문구 */
    fun banner(c: Context): String? {
        if (!isTrial) return null
        val left = daysLeft(c)
        return if (left <= 0) "무료 체험 14일이 끝났어요\n자동 교정은 꺼졌지만 키보드·학습·리포트는 계속 돼요.\n정식판을 설치하면 지금까지 배운 그대로 바로 교정이 다시 켜져요."
        else "무료 체험판 · ${left}일 남았어요\n체험이 끝나도 배운 기록은 사라지지 않아요."
    }
}
