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
    val sprinkle: String? = null // 키 모서리 곳곳의 작은 잎 (그림 이름 앞부분)
) {
    val menuName get() = short ?: name
}

object Themes {
    private fun c(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    private val defaultLight = Theme(
        "default", "기본",
        bg = c(209, 213, 219), key = Color.WHITE, special = c(171, 178, 189),
        pressKey = c(171, 178, 189), pressSpecial = Color.WHITE,
        text = c(24, 32, 46), specialText = c(24, 32, 46), shadow = Color.argb(70, 0, 0, 0)
    )
    private val defaultDark = Theme(
        "default", "기본",
        bg = c(31, 31, 31), key = c(107, 107, 107), special = c(69, 69, 69),
        pressKey = c(69, 69, 69), pressSpecial = c(107, 107, 107),
        text = Color.WHITE, specialText = Color.WHITE, shadow = Color.argb(70, 0, 0, 0)
    )

    val all: List<Theme> = listOf(
        defaultLight,
        Theme(
            "oled", "OLED 매트 블랙",
            bg = Color.BLACK, key = c(26, 26, 28), special = c(14, 14, 16),
            pressKey = c(52, 52, 56), pressSpecial = c(40, 40, 44),
            text = c(214, 214, 210), specialText = c(150, 150, 146), shadow = Color.TRANSPARENT,
            rim = c(38, 38, 42), radiusDp = 7f,
            short = "OLED 블랙"
        ),
        Theme(
            "mint", "레트로 민트 & 크림",
            bg = c(38, 92, 88), key = c(244, 238, 222), special = c(120, 196, 184),
            pressKey = c(226, 218, 198), pressSpecial = c(104, 180, 168),
            text = c(40, 64, 60), specialText = c(24, 60, 56), shadow = Color.TRANSPARENT,
            side = c(190, 180, 156), sideSpecial = c(76, 150, 138), accent = c(238, 98, 74),
            depthDp = 5f, radiusDp = 5f,
            short = "민트 & 크림"
        ),
        Theme(
            "navy", "레트로 네이비 & 오렌지",
            bg = c(28, 38, 64), key = c(236, 232, 222), special = c(64, 82, 124),
            pressKey = c(216, 210, 196), pressSpecial = c(54, 70, 108),
            text = c(30, 40, 66), specialText = c(236, 232, 222), shadow = Color.TRANSPARENT,
            side = c(178, 172, 158), sideSpecial = c(40, 54, 90), accent = c(240, 128, 40),
            depthDp = 5f, radiusDp = 5f,
            short = "네이비 & 오렌지"
        ),
        Theme(
            "lime", "레트로 연두 & 그레이",
            bg = c(70, 76, 72), key = c(238, 240, 232), special = c(170, 214, 96),
            pressKey = c(220, 224, 212), pressSpecial = c(152, 196, 80),
            text = c(48, 58, 44), specialText = c(40, 70, 30), shadow = Color.TRANSPARENT,
            side = c(182, 186, 174), sideSpecial = c(120, 166, 60), accent = c(40, 70, 30),
            depthDp = 5f, radiusDp = 5f,
            short = "연두 & 그레이"
        ),
        Theme(
            "lavender", "레트로 파스텔 라벤더",
            bg = c(186, 176, 214), key = c(250, 246, 252), special = c(232, 196, 214),
            pressKey = c(232, 226, 238), pressSpecial = c(216, 178, 198),
            text = c(84, 72, 110), specialText = c(110, 60, 88), shadow = Color.TRANSPARENT,
            side = c(206, 196, 222), sideSpecial = c(206, 160, 184), accent = c(150, 120, 200),
            depthDp = 5f, radiusDp = 5f,
            short = "파스텔 라벤더"
        ),
        Theme(
            "coral", "가을 코랄",
            bg = c(250, 160, 130), key = c(255, 251, 247), special = c(238, 104, 80),
            pressKey = c(255, 234, 224), pressSpecial = c(214, 88, 66),
            text = c(196, 64, 52), specialText = Color.WHITE, shadow = Color.argb(100, 200, 80, 60),
            accent = c(218, 52, 52), radiusDp = 11f,
            bgRes = R.drawable.bg_coral, enterRes = R.drawable.enter_chestnut, stickerRes = R.drawable.sticker_chestnut, gloss = true,
            spaceRes = R.drawable.space_logo_chestnut, sprinkle = "sprinkle_maple_"
        ),
        Theme(
            "yellow", "가을 옐로우",
            bg = c(252, 204, 80), key = c(255, 253, 244), special = c(246, 166, 40),
            pressKey = c(255, 240, 200), pressSpecial = c(226, 146, 30),
            text = c(170, 100, 20), specialText = Color.WHITE, shadow = Color.argb(100, 200, 140, 30),
            accent = c(232, 92, 40), radiusDp = 11f,
            bgRes = R.drawable.bg_yellow, enterRes = R.drawable.enter_chestnut, stickerRes = R.drawable.sticker_chestnut, gloss = true,
            spaceRes = R.drawable.space_logo_chestnut, sprinkle = "sprinkle_ginkgo_"
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
