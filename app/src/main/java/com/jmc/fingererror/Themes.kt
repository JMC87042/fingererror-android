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
    val cap: String = "jelly"    // 키캡 모양: jelly 젤리 · round 동글 타자기 · clear 투명 젤리 · clay 클레이
) {
    val menuName get() = short ?: name
}

object Themes {
    private fun c(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    // 1. 크림 젤리 (기본) — 말랑한 크림색 키캡, 복숭아 포인트
    private val defaultLight = Theme(
        "default", "크림 젤리",
        bg = c(246, 232, 218), key = c(255, 250, 242), special = c(255, 204, 182),
        pressKey = c(244, 234, 220), pressSpecial = c(246, 184, 160),
        text = c(112, 78, 60), specialText = c(156, 72, 50), shadow = Color.TRANSPARENT,
        side = c(226, 202, 180), sideSpecial = c(228, 152, 124), accent = c(255, 138, 108),
        radiusDp = 10f
    )
    // 폰이 다크모드일 때의 기본 (그레이 젤리)
    private val defaultDark = Theme(
        "default", "크림 젤리",
        bg = c(26, 26, 30), key = c(92, 92, 100), special = c(62, 62, 70),
        pressKey = c(72, 72, 80), pressSpecial = c(84, 84, 92),
        text = Color.WHITE, specialText = Color.WHITE, shadow = Color.TRANSPARENT,
        side = c(52, 52, 58), sideSpecial = c(36, 36, 42), accent = c(255, 138, 108),
        radiusDp = 10f
    )

    val all: List<Theme> = listOf(
        defaultLight,
        Theme(
            "oled", "블랙 젤리",
            bg = Color.BLACK, key = c(40, 40, 46), special = c(26, 26, 30),
            pressKey = c(62, 62, 68), pressSpecial = c(46, 46, 52),
            text = c(226, 226, 222), specialText = c(160, 160, 156), shadow = Color.TRANSPARENT,
            side = c(18, 18, 22), sideSpecial = c(10, 10, 12), radiusDp = 10f
        ),
        Theme(
            "mint", "민트 소다",
            bg = c(132, 222, 206), key = c(236, 252, 248), special = c(90, 200, 186),
            pressKey = c(212, 240, 234), pressSpecial = c(74, 184, 170),
            text = c(30, 96, 92), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(44, 170, 190), radiusDp = 10f, cap = "round", short = "동글 민트"
        ),
        Theme(
            "peach", "투명 피치 젤리",
            bg = c(255, 214, 204), key = c(255, 176, 160), special = c(255, 140, 130),
            pressKey = c(250, 150, 134), pressSpecial = c(236, 116, 106),
            text = c(150, 50, 46), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 110, 110), radiusDp = 12f, cap = "clear", short = "투명 피치"
        ),
        Theme(
            "lime", "라임 젤리",
            bg = c(214, 236, 190), key = c(249, 252, 241), special = c(176, 220, 110),
            pressKey = c(232, 242, 218), pressSpecial = c(156, 204, 90),
            text = c(60, 90, 40), specialText = c(40, 70, 24), shadow = Color.TRANSPARENT,
            side = c(196, 214, 168), sideSpecial = c(116, 166, 56), accent = c(110, 172, 52),
            radiusDp = 10f
        ),
        Theme(
            "lavender", "라벤더 클레이",
            bg = c(232, 224, 248), key = c(242, 235, 253), special = c(198, 180, 240),
            pressKey = c(226, 216, 246), pressSpecial = c(180, 160, 230),
            text = c(100, 80, 150), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(160, 130, 232), radiusDp = 12f, cap = "clay", short = "라벤더 클레이"
        ),
        Theme(
            "coral", "가을 코랄",
            bg = c(250, 160, 130), key = c(255, 251, 247), special = c(238, 104, 80),
            pressKey = c(255, 234, 224), pressSpecial = c(214, 88, 66),
            text = c(196, 64, 52), specialText = Color.WHITE, shadow = Color.argb(100, 200, 80, 60),
            accent = c(218, 52, 52), radiusDp = 11f,
            bgRes = R.drawable.bg_coral, enterRes = R.drawable.enter_chestnut, stickerRes = R.drawable.sticker_chestnut, gloss = true,
            sprinkle = "sprinkle_maple_"
        ),
        Theme(
            "yellow", "가을 옐로우",
            bg = c(252, 204, 80), key = c(255, 253, 244), special = c(246, 166, 40),
            pressKey = c(255, 240, 200), pressSpecial = c(226, 146, 30),
            text = c(170, 100, 20), specialText = Color.WHITE, shadow = Color.argb(100, 200, 140, 30),
            accent = c(232, 92, 40), radiusDp = 11f,
            bgRes = R.drawable.bg_yellow, enterRes = R.drawable.enter_chestnut, stickerRes = R.drawable.sticker_chestnut, gloss = true,
            sprinkle = "sprinkle_ginkgo_"
        ),
        Theme(
            "winter", "겨울 눈사람",
            bg = c(170, 200, 240), key = c(252, 254, 255), special = c(196, 218, 248),
            pressKey = c(226, 238, 252), pressSpecial = c(176, 202, 240),
            text = c(70, 110, 180), specialText = c(50, 90, 160), shadow = Color.argb(110, 100, 140, 200),
            accent = c(120, 160, 230), radiusDp = 12f,
            bgRes = R.drawable.bg_winter, enterRes = R.drawable.enter_snowman, stickerRes = R.drawable.sticker_snow, gloss = true
        ),
        Theme(
            "red", "겨울 레드",
            bg = c(118, 14, 24), key = c(255, 250, 242), special = c(214, 52, 58),
            pressKey = c(240, 226, 214), pressSpecial = c(190, 40, 48),
            text = c(30, 112, 64), specialText = c(255, 246, 236), shadow = Color.argb(140, 40, 0, 0),
            accent = c(34, 112, 66), radiusDp = 12f,
            bgRes = R.drawable.bg_red, enterRes = R.drawable.enter_snowman_red, stickerRes = R.drawable.sticker_holly, gloss = true
        )
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

    /** 기본 테마는 폰의 다크모드를 따라감 */
    fun current(ctx: Context, night: Boolean): Theme {
        val id = currentId(ctx)
        if (id == "default") return if (night) defaultDark else defaultLight
        return all.firstOrNull { it.id == id } ?: defaultLight
    }
}
