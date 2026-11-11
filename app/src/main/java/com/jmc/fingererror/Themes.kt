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
    val group: String = "color"  // color: 컬러 · retro: 레트로 (둘 다 키캡 모양을 고를 수 있음) · winter: 겨울 (레트로 기계식 고정)
) {
    val menuName get() = short ?: name
}

object Themes {
    private fun c(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    // 1. 크림 젤리 (기본) — 말랑한 크림색 키캡, 복숭아 포인트
    private val defaultLight = Theme(
        "default", "크림",
        bg = c(246, 232, 218), key = c(255, 250, 242), special = c(255, 204, 182),
        pressKey = c(244, 234, 220), pressSpecial = c(246, 184, 160),
        text = c(112, 78, 60), specialText = c(156, 72, 50), shadow = Color.TRANSPARENT,
        side = c(226, 202, 180), sideSpecial = c(228, 152, 124), accent = c(255, 138, 108),
        radiusDp = 10f
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
            "oled", "블랙",
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
            accent = c(44, 170, 190), radiusDp = 10f
        ),
        Theme(
            "peach", "피치",
            bg = c(255, 214, 204), key = c(255, 176, 160), special = c(255, 140, 130),
            pressKey = c(250, 150, 134), pressSpecial = c(236, 116, 106),
            text = c(150, 50, 46), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 110, 110), radiusDp = 12f
        ),
        Theme(
            "lime", "라임",
            bg = c(214, 236, 190), key = c(249, 252, 241), special = c(176, 220, 110),
            pressKey = c(232, 242, 218), pressSpecial = c(156, 204, 90),
            text = c(60, 90, 40), specialText = c(40, 70, 24), shadow = Color.TRANSPARENT,
            side = c(196, 214, 168), sideSpecial = c(116, 166, 56), accent = c(110, 172, 52),
            radiusDp = 10f
        ),
        Theme(
            "lavender", "라벤더",
            bg = c(232, 224, 248), key = c(242, 235, 253), special = c(198, 180, 240),
            pressKey = c(226, 216, 246), pressSpecial = c(180, 160, 230),
            text = c(100, 80, 150), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(160, 130, 232), radiusDp = 12f
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
            "sky", "하늘",
            bg = c(200, 226, 252), key = c(255, 255, 255), special = c(120, 180, 244),
            pressKey = c(232, 242, 252), pressSpecial = c(100, 164, 232),
            text = c(60, 110, 170), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 128, 170), radiusDp = 10f
        ),
        Theme(
            "cocoa", "코코아",
            bg = c(232, 214, 196), key = c(255, 249, 240), special = c(176, 128, 96),
            pressKey = c(244, 234, 220), pressSpecial = c(156, 110, 80),
            text = c(110, 72, 50), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(150, 96, 64), radiusDp = 10f
        ),

        // ---- 레트로 10색 (키캡 모양 고르기) ----
        Theme(
            "rt_rose", "블랙 & 로즈 골드",
            bg = c(14, 12, 14), key = c(42, 38, 40), special = c(28, 25, 27),
            pressKey = c(60, 55, 57), pressSpecial = c(46, 42, 44),
            text = c(226, 176, 156), specialText = c(226, 176, 156), shadow = Color.TRANSPARENT,
            radiusDp = 10f, group = "retro", short = "로즈 골드"
        ),
        Theme(
            "rt_navy", "네이비 & 오렌지",
            bg = c(36, 52, 92), key = c(246, 242, 232), special = c(72, 98, 156),
            pressKey = c(231, 227, 218), pressSpecial = c(66, 90, 143),
            text = c(34, 46, 76), specialText = c(255, 248, 236), shadow = Color.TRANSPARENT,
            accent = c(250, 152, 40), radiusDp = 10f, group = "retro", short = "네이비"
        ),
        Theme(
            "rt_royal", "로열 블루 & 골드",
            bg = c(46, 84, 176), key = c(80, 124, 220), special = c(255, 204, 64),
            pressKey = c(75, 116, 206), pressSpecial = c(234, 187, 58),
            text = c(255, 255, 255), specialText = c(90, 60, 10), shadow = Color.TRANSPARENT,
            accent = c(255, 204, 64), radiusDp = 10f, group = "retro", short = "로열 블루"
        ),
        Theme(
            "rt_mint", "민트 & 크림",
            bg = c(120, 220, 200), key = c(255, 253, 242), special = c(70, 206, 184),
            pressKey = c(239, 237, 227), pressSpecial = c(64, 189, 169),
            text = c(40, 110, 100), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 112, 90), radiusDp = 10f, group = "retro", short = "민트 크림"
        ),
        Theme(
            "rt_cherry", "체리 & 크림",
            bg = c(214, 44, 72), key = c(255, 242, 228), special = c(255, 90, 110),
            pressKey = c(239, 227, 214), pressSpecial = c(234, 82, 101),
            text = c(170, 40, 60), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 196, 90), radiusDp = 10f, group = "retro", short = "체리"
        ),
        Theme(
            "rt_mono", "블랙",
            bg = c(30, 30, 34), key = c(70, 70, 76), special = c(50, 50, 55),
            pressKey = c(92, 92, 98), pressSpecial = c(70, 70, 76),
            text = c(246, 246, 242), specialText = c(246, 246, 242), shadow = Color.TRANSPARENT,
            radiusDp = 10f, group = "retro", short = "블랙"
        ),
        Theme(
            "rt_milk", "딸기 우유",
            bg = c(255, 206, 220), key = c(255, 255, 255), special = c(255, 116, 156),
            pressKey = c(239, 239, 239), pressSpecial = c(234, 106, 143),
            text = c(200, 70, 110), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 72, 124), radiusDp = 10f, group = "retro", short = "딸기 우유"
        ),
        Theme(
            "rt_matcha", "말차 라떼",
            bg = c(168, 214, 130), key = c(252, 252, 238), special = c(104, 176, 80),
            pressKey = c(236, 236, 223), pressSpecial = c(95, 161, 73),
            text = c(70, 110, 50), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(226, 146, 74), radiusDp = 10f, group = "retro", short = "말차"
        ),
        Theme(
            "rt_sky", "하늘 & 핑크",
            bg = c(204, 228, 252), key = c(255, 255, 255), special = c(118, 180, 244),
            pressKey = c(239, 239, 239), pressSpecial = c(108, 165, 224),
            text = c(60, 110, 170), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 128, 170), radiusDp = 10f, group = "retro", short = "하늘 핑크"
        ),
        Theme(
            "rt_lavender", "라벤더 드림",
            bg = c(178, 148, 244), key = c(250, 246, 255), special = c(196, 156, 255),
            pressKey = c(235, 231, 239), pressSpecial = c(180, 143, 234),
            text = c(110, 70, 190), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(136, 86, 250), radiusDp = 10f, group = "retro", short = "라벤더"
        ),

        // ---- 겨울 (레트로 기계식 키캡: 오목한 윗면 + 도톰한 몸통) ----
        Theme(
            "cherry", "체리 & 크림",
            bg = c(214, 44, 72), key = c(255, 242, 228), special = c(255, 90, 110),
            pressKey = c(240, 224, 208), pressSpecial = c(236, 72, 94),
            text = c(170, 40, 60), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 196, 90), cap = "sa", group = "winter", short = "체리"
        ),
        Theme(
            "milk", "딸기 우유",
            bg = c(255, 206, 220), key = c(255, 255, 255), special = c(255, 116, 156),
            pressKey = c(246, 236, 240), pressSpecial = c(240, 96, 138),
            text = c(200, 70, 110), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(255, 72, 124), cap = "sa", group = "winter"
        ),
        Theme(
            "royal", "로열 블루 & 골드",
            bg = c(46, 84, 176), key = c(80, 124, 220), special = c(255, 204, 64),
            pressKey = c(66, 106, 196), pressSpecial = c(236, 184, 44),
            text = Color.WHITE, specialText = c(90, 60, 10), shadow = Color.TRANSPARENT,
            accent = c(255, 204, 64), cap = "sa", group = "winter", short = "로열 블루"
        ),
        Theme(
            "winter", "눈사람",
            bg = c(170, 200, 240), key = c(252, 254, 255), special = c(150, 190, 240),
            pressKey = c(226, 238, 252), pressSpecial = c(130, 172, 228),
            text = c(60, 100, 170), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(110, 160, 232), cap = "sa", group = "winter",
            bgRes = R.drawable.bg_winter, enterRes = R.drawable.enter_snowman, stickerRes = R.drawable.sticker_snow
        ),
        Theme(
            "red", "크리스마스",
            bg = c(150, 20, 32), key = c(255, 248, 238), special = c(214, 52, 58),
            pressKey = c(240, 226, 214), pressSpecial = c(190, 40, 48),
            text = c(40, 120, 70), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(34, 130, 70), cap = "sa", group = "winter",
            bgRes = R.drawable.bg_red, enterRes = R.drawable.enter_snowman_red, stickerRes = R.drawable.sticker_holly
        ),
        Theme(
            "holly", "홀리 그린",
            bg = c(30, 110, 70), key = c(255, 250, 240), special = c(220, 50, 56),
            pressKey = c(240, 232, 218), pressSpecial = c(196, 40, 48),
            text = c(30, 100, 60), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(220, 50, 56), cap = "sa", group = "winter", short = "홀리"
        ),
        Theme(
            "choco", "핫초코",
            bg = c(110, 70, 50), key = c(255, 248, 236), special = c(170, 110, 80),
            pressKey = c(240, 228, 212), pressSpecial = c(150, 94, 66),
            text = c(110, 70, 50), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(240, 150, 170), cap = "sa", group = "winter"
        ),
        Theme(
            "aurora", "오로라",
            bg = c(40, 44, 100), key = c(214, 240, 250), special = c(140, 110, 230),
            pressKey = c(190, 222, 238), pressSpecial = c(120, 92, 210),
            text = c(50, 70, 130), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(70, 206, 170), cap = "sa", group = "winter"
        ),
        Theme(
            "ginger", "진저브레드",
            bg = c(196, 120, 70), key = c(255, 250, 244), special = c(160, 90, 50),
            pressKey = c(240, 230, 218), pressSpecial = c(140, 76, 40),
            text = c(150, 85, 45), specialText = Color.WHITE, shadow = Color.TRANSPARENT,
            accent = c(220, 60, 60), cap = "sa", group = "winter", short = "진저"
        ),
        Theme(
            "mono", "기본 블랙",
            bg = c(30, 30, 34), key = c(70, 70, 76), special = c(48, 48, 52),
            pressKey = c(96, 96, 102), pressSpecial = c(70, 70, 76),
            text = c(246, 246, 242), specialText = c(246, 246, 242), shadow = Color.TRANSPARENT,
            cap = "sa", group = "winter"
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

    val colors get() = all.filter { it.group == "color" }
    val retro get() = all.filter { it.group == "retro" }
    val winter get() = all.filter { it.group == "winter" }
    fun group(g: String) = all.filter { it.group == g }
    /** 키캡 모양을 고를 수 있는 테마 */
    fun capped(t: Theme) = t.group == "color" || t.group == "retro"

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
        val t = if (id == "default") (if (night) defaultDark else defaultLight) else (all.firstOrNull { it.id == id } ?: defaultLight)
        return if (capped(t)) t.copy(cap = capId(ctx)) else t
    }
}
