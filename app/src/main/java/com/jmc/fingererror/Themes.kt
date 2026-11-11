package com.jmc.fingererror

import android.content.Context
import android.graphics.Color

/** 키보드 테마. 색만 바뀌고 키 위치·크기는 모든 테마가 같음 (학습 기록 유지) */
data class Theme(
    val id: String,
    val name: String,
    val bg: Int,
    val key: Int,
    val special: Int,
    val pressKey: Int,
    val pressSpecial: Int,
    val text: Int,
    val specialText: Int,
    val shadow: Int,
    val side: Int = 0,          // 레트로: 키 아랫단 색
    val sideSpecial: Int = 0,
    val rim: Int = 0,           // OLED: 키 테두리
    val accent: Int = 0,        // 줄바꿈 키 포인트 색
    val depthDp: Float = 0f,    // 레트로 입체 두께
    val radiusDp: Float = 6f,
    val bgRes: Int = 0,         // 계절: 배경 그림
    val enterRes: Int = 0,      // 계절: 줄바꿈 키 그림 (단풍·눈사람)
    val stickerRes: Int = 0,    // 계절: 키 모서리 스티커
    val gloss: Boolean = false, // 계절: 키 윗부분 반짝임
    val short: String? = null,  // 테마 메뉴에 보일 짧은 이름
    val spaceRes: Int = 0,      // 스페이스바 팻핑이 (밤 모자 등)
    val sprinkle: String? = null, // 키 모서리 곳곳의 작은 잎 (그림 이름 앞부분)
    val jelly: Boolean = true,   // 3D 키캡 (아래 cap 모양으로 그림)
    val cap: String = "jelly",   // 키캡 모양: jelly 젤리 · round 동글 타자기 · clear 투명 젤리 · clay 클레이 · sa 레트로 기계식
    val stitch: Int = 0,         // 가죽 박음질 색 (글자 키 안쪽 점선)
    val group: String = "color"  // color: 컬러 · retro: 레트로 (둘 다 키캡 모양을 고를 수 있음) · winter: 겨울 (레트로 기계식 고정)
) {
    val menuName get() = short ?: name
}

object Themes {
    private fun c(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    // 1. 블랙 퍼플 (기본) — 위는 블랙, 아래로 갈수록 보라 그라데이션 + 골드 포인트
    private val defaultLight = Theme(
        "default", "블랙 퍼플",
        bg = c(40, 30, 80), key = c(36, 32, 56), special = c(112, 82, 204),
        pressKey = c(56, 50, 84), pressSpecial = c(96, 68, 184),
        text = c(226, 218, 255), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
        accent = c(196, 152, 64), radiusDp = 10f, bgRes = R.drawable.bg_kit
    )
    // 폰이 다크모드일 때의 기본 (그레이 젤리)
    private val defaultDark = Theme(
        "default", "크림",
        bg = c(26, 26, 30), key = c(92, 92, 100), special = c(62, 62, 70),
        pressKey = c(72, 72, 80), pressSpecial = c(84, 84, 92),
        text = Color.WHITE, specialText = Color.WHITE, shadow = Color.TRANSPARENT,
        side = c(52, 52, 58), sideSpecial = c(36, 36, 42), accent = c(255, 138, 108),
        radiusDp = 10f
    )

    val all: List<Theme> = listOf(
        defaultLight,
        Theme(
            "redblue", "레드 블루",
            bg = c(30, 40, 110), key = c(250, 248, 244), special = c(170, 20, 72),
            pressKey = c(232, 228, 222), pressSpecial = c(150, 14, 60),
            text = c(0, 62, 146), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(200, 156, 60), radiusDp = 10f, bgRes = R.drawable.bg_stripe
        ),
        Theme(
            "race", "레이싱 레드",
            bg = c(16, 18, 24), key = c(30, 30, 33), special = c(190, 18, 28),
            pressKey = c(46, 46, 50), pressSpecial = c(160, 12, 22),
            text = c(236, 236, 238), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(214, 22, 32), radiusDp = 10f
        ),
        Theme(
            "rt_rose", "블랙 & 로즈 골드",
            bg = c(14, 12, 14), key = c(42, 38, 40), special = c(28, 25, 27),
            pressKey = c(60, 55, 57), pressSpecial = c(46, 42, 44),
            text = c(246, 172, 142), specialText = c(246, 172, 142), shadow = Color.TRANSPARENT,
            radiusDp = 10f, short = "로즈 골드"
        ),
        Theme(
            "rosegold", "블랙 & 화이트 레드",
            bg = c(12, 11, 13), key = c(255, 255, 255), special = c(252, 226, 226),
            pressKey = c(240, 234, 234), pressSpecial = c(240, 206, 206),
            text = c(214, 26, 42), specialText = c(204, 22, 38), shadow = Color.TRANSPARENT,
            accent = c(220, 30, 44), radiusDp = 10f, short = "화이트 레드"
        ),
        Theme(
            "rt_mono", "블랙",
            bg = c(30, 30, 34), key = c(70, 70, 76), special = c(50, 50, 55),
            pressKey = c(92, 92, 98), pressSpecial = c(70, 70, 76),
            text = c(246, 246, 242), specialText = c(246, 246, 242), shadow = Color.TRANSPARENT,
            radiusDp = 10f, short = "블랙"
        ),
        Theme(
            "pin", "핀스트라이프",
            bg = c(250, 250, 251), key = c(255, 255, 255), special = c(30, 30, 34),
            pressKey = c(236, 236, 238), pressSpecial = c(54, 54, 58),
            text = c(24, 24, 28), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(204, 24, 44), radiusDp = 10f, bgRes = R.drawable.bg_pin, short = "핀스트라이프"
        ),
        Theme(
            "rt_navy", "네이비 & 오렌지",
            bg = c(36, 52, 92), key = c(246, 242, 232), special = c(72, 98, 156),
            pressKey = c(231, 227, 218), pressSpecial = c(66, 90, 143),
            text = c(34, 46, 76), specialText = c(255, 248, 236), shadow = Color.TRANSPARENT,
            accent = c(250, 152, 40), radiusDp = 10f, short = "네이비"
        ),
        Theme(
            "rt_royal", "로열 블루 & 골드",
            bg = c(46, 84, 176), key = c(80, 124, 220), special = c(255, 204, 64),
            pressKey = c(75, 116, 206), pressSpecial = c(234, 187, 58),
            text = c(255, 255, 255), specialText = c(90, 60, 10), shadow = Color.TRANSPARENT,
            accent = c(255, 204, 64), radiusDp = 10f, short = "로열 블루"
        ),
        Theme(
            "aurora", "오로라",
            bg = c(40, 44, 100), key = c(214, 240, 250), special = c(140, 110, 230),
            pressKey = c(190, 222, 238), pressSpecial = c(120, 92, 210),
            text = c(50, 70, 130), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(70, 206, 170)
        ),
        Theme(
            "rt_sky", "하늘 & 핑크",
            bg = c(204, 228, 252), key = c(255, 255, 255), special = c(118, 180, 244),
            pressKey = c(239, 239, 239), pressSpecial = c(108, 165, 224),
            text = c(60, 110, 170), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 128, 170), radiusDp = 10f, short = "하늘 핑크"
        ),
        Theme(
            "winter", "눈사람",
            bg = c(170, 200, 240), key = c(252, 254, 255), special = c(150, 190, 240),
            pressKey = c(226, 238, 252), pressSpecial = c(130, 172, 228),
            text = c(60, 100, 170), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(110, 160, 232),
            bgRes = R.drawable.bg_winter, enterRes = R.drawable.enter_snowman, stickerRes = R.drawable.sticker_snow
        ),
        Theme(
            "mint", "민트 소다",
            bg = c(132, 222, 206), key = c(236, 252, 248), special = c(90, 200, 186),
            pressKey = c(212, 240, 234), pressSpecial = c(74, 184, 170),
            text = c(30, 96, 92), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(44, 170, 190), radiusDp = 10f
        ),
        Theme(
            "holly", "홀리 그린",
            bg = c(30, 110, 70), key = c(255, 250, 240), special = c(220, 50, 56),
            pressKey = c(240, 232, 218), pressSpecial = c(196, 40, 48),
            text = c(30, 100, 60), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(220, 50, 56), short = "홀리"
        ),
        Theme(
            "peach", "피치",
            bg = c(255, 214, 204), key = c(255, 251, 249), special = c(255, 140, 130),
            pressKey = c(246, 234, 230), pressSpecial = c(236, 116, 106),
            text = c(150, 50, 46), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 110, 110), radiusDp = 12f
        ),
        Theme(
            "rt_cherry", "체리 & 크림",
            bg = c(214, 44, 72), key = c(255, 242, 228), special = c(255, 90, 110),
            pressKey = c(239, 227, 214), pressSpecial = c(234, 82, 101),
            text = c(170, 40, 60), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 196, 90), radiusDp = 10f, short = "체리"
        ),
        Theme(
            "rt_milk", "딸기 우유",
            bg = c(255, 206, 220), key = c(255, 255, 255), special = c(255, 116, 156),
            pressKey = c(239, 239, 239), pressSpecial = c(234, 106, 143),
            text = c(200, 70, 110), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 72, 124), radiusDp = 10f, short = "딸기 우유"
        ),
        Theme(
            "red", "크리스마스",
            bg = c(150, 20, 32), key = c(255, 248, 238), special = c(214, 52, 58),
            pressKey = c(240, 226, 214), pressSpecial = c(190, 40, 48),
            text = c(40, 120, 70), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(34, 130, 70),
            bgRes = R.drawable.bg_red, enterRes = R.drawable.enter_snowman_red, stickerRes = R.drawable.sticker_holly
        ),
        Theme(
            "lavender", "라벤더",
            bg = c(232, 224, 248), key = c(242, 235, 253), special = c(198, 180, 240),
            pressKey = c(226, 216, 246), pressSpecial = c(180, 160, 230),
            text = c(100, 80, 150), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(160, 130, 232), radiusDp = 12f
        ),
        Theme(
            "rt_lavender", "라벤더 드림",
            bg = c(178, 148, 244), key = c(250, 246, 255), special = c(196, 156, 255),
            pressKey = c(235, 231, 239), pressSpecial = c(180, 143, 234),
            text = c(110, 70, 190), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(136, 86, 250), radiusDp = 10f, short = "라벤더"
        ),

    )

    private const val PREFS = "fingererror_theme"

    /** 테마 이름에 쓰는 귀여운 글씨체 (주아, 무료 OFL 폰트) */
    private var jua: android.graphics.Typeface? = null
    fun font(ctx: Context): android.graphics.Typeface {
        jua?.let { return it }
        val t = try { android.graphics.Typeface.createFromAsset(ctx.assets, "Jua-Theme.ttf") } catch (e: Exception) { android.graphics.Typeface.DEFAULT_BOLD }
        jua = t
        return t
    }

    fun currentId(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("theme", "default") ?: "default"

    fun setCurrent(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("theme", id).apply()
    }

    val colors get() = all.filter { it.group == "color" }
    val retro get() = all.filter { it.group == "retro" }
    val winter get() = all.filter { it.group == "winter" }
    fun group(g: String) = all.filter { it.group == g }
    /** 키캡 모양을 고를 수 있는 테마 (이제 전부) */
    fun capped(t: Theme) = true
    /** 메뉴 한 장에 10개씩 */
    val pages get() = all.chunked(10)
    fun page(i: Int) = pages.getOrElse(i) { pages.first() }
    fun pageOf(id: String) = maxOf(0, pages.indexOfFirst { p -> p.any { it.id == id } })

    /** 컬러 테마의 키캡 모양 (드롭다운) */
    val CAPS = listOf("jelly" to "젤리", "round" to "동글 타자기", "clear" to "투명 젤리", "clay" to "클레이", "sa" to "레트로 기계식")
    fun capName(id: String) = CAPS.firstOrNull { it.first == id }?.second ?: "젤리"
    fun capId(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("cap", "jelly") ?: "jelly"
    fun setCap(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("cap", id).apply()
    }
    /** 미리보기 그림 이름 (컬러 테마는 키캡 모양마다 따로) */
    fun previewName(t: Theme, cap: String) = if (capped(t)) "theme_${t.id}_$cap" else "theme_${t.id}"

    /** 기본 테마는 폰의 다크모드를 따라감. 컬러 테마는 고른 키캡 모양으로 */
    fun current(ctx: Context, night: Boolean): Theme {
        val id = currentId(ctx)
        val t = if (id == "default") defaultLight else (all.firstOrNull { it.id == id } ?: defaultLight)
        return if (capped(t)) t.copy(cap = capId(ctx)) else t
    }
}
