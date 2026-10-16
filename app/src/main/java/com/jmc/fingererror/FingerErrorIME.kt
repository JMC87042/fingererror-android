package com.jmc.fingererror

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 핑거에러 키보드.
 * - 틀린 글자를 ⌫로 지우고 다시 치면 그 실수를 배움 (엄지 쏠림 + 실수표)
 * - 자리별(단어 처음/중간/끝 × 첫자음/모음/받침)로 따로 배움
 * - 어미 교정 (먹었어료 → 먹었어요), 띄어쓰기 ↔ 마침표 교정
 * - 기록은 이 폰 안에만 저장
 */
class FingerErrorIME : InputMethodService(), KeyboardView.Listener {

    class Meta(var id: String, val fx: Float, val fy: Float, var ok: Boolean, val sp: Boolean, val raw: String = "")
    class WordEv(val i: Int, val wrong: String, val right: String, val fx: Float, val fy: Float)
    class AutoFix(val idx: Int, val orig: String, val key: String)
    class Pending(val idx: Int, val id: String, val fx: Float, val fy: Float, val raw: String)

    companion object {
        /** '요' 앞에 자주 오는 글자 (받침 없음). 자료·재료·나쵸·아뇨·아교 같은 진짜 단어는 피하도록 고름 */
        val END_PREV: Set<String> = "어아에세네해여워와예래게데줘봐돼써가거서져려대케레셔쳐펴혀까떠냐니지시".map { it.toString() }.toSet()
        val END_BASE = setOf("ㄹ", "ㅌ", "ㄷ", "ㅊ")
        val SP_LEARN = setOf("SPACE", ".", ",", "ENTER")
        /** 받침 없는 이름 뒤에 붙는 조사 (가료를 · 가료야 → 어미가 아니라 이름) */
        val PARTICLES = setOf("를", "는", "가", "야", "랑", "도", "한테", "한테서", "님", "씨", "의", "에", "에게", "에서", "와", "만", "께", "까지", "부터", "처럼", "보다", "하고", "라도")
    }

    private lateinit var lr: Learner
    private var kv: KeyboardView? = null

    private val tokens = ArrayList<String>()        // 조합 중인 단어의 자모
    private val history = ArrayList<Meta?>()        // 친 순서대로 (스택)
    private val delBuf = ArrayList<Meta?>()         // 지운 것들 (원래 순서)
    private var reIdx = 0
    private var delT = 0L
    private val wordEv = ArrayList<WordEv>()        // 이번 단어에서 지우고 고친 기록
    private var lastAuto: AutoFix? = null
    /** 어미 교정한 단어를 끝까지 지켜봄: 단어가 "요+조사"로 끝나면 이름이었던 것 → 되돌림 */
    private var endWatch: AutoFix? = null
    private var lastPunct: String? = null           // 단어 바로 뒤에 찍은 . 이나 ,
    private val pending = HashMap<Int, Pending>()
    private val handler = Handler(Looper.getMainLooper())
    private var repeatPid = -1
    private var lastEditAt = 0L                     // 내가 글자를 바꾼 시각 (그 직후 커서 알림은 무시)
    private val saveRunnable = Runnable { lr.save() }
    private val repeatTick = object : Runnable {
        override fun run() { backspace(); handler.postDelayed(this, 70) }
    }

    override fun onCreate() {
        super.onCreate()
        lr = Learner(this)
    }

    override fun onCreateInputView(): View {
        val v = KeyboardView(this)
        v.listener = this
        kv = v
        return v
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        kv?.reloadTheme()
        kv?.let { v ->
            v.hand = lr.mode.removePrefix("W")
            lr.switchMode(v.profileKey())
        }
        resetWord()
        history.clear(); clearDel(); lastPunct = null
        autoCap()
        // 앱에서 '데이터 초기화'를 눌렀으면 여기서 엄지 기록을 처음부터
        if (lr.applyResetIfAsked()) {
            handler.postDelayed({ kv?.showToast("기록을 비웠어요", "팻핑이가 처음부터 다시 배울게요") }, 600)
        }
        // 예전부터 쌓인 기록이 이미 100개를 넘었으면 키보드를 열 때 바로 리포트를 올림
        lr.publishReportIfDue()?.let { r ->
            lr.save()
            if (!toastOff()) {
                val t = Anniversary.reportText(r)
                handler.postDelayed({ kv?.showToast(t.first, t.second) }, 600)
            }
        }
        updateMenuStatus()
        // 날짜 기념일 (만난 지 22·100일 …): 그날 처음 열 때 한 번
        lr.newDayMilestone()?.let { d ->
            saveSoon()
            if (!toastOff()) {
                val t = Anniversary.dayText(d)
                handler.postDelayed({ kv?.showToast(t.first, t.second) }, 600)
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        kv?.closeMenu()
        finalizeWord()
        stopRepeat()
        currentInputConnection?.finishComposingText()
        tokens.clear()
        lr.save()
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // 사용자가 커서를 옮기면 조합 중이던 글자를 확정
        if (tokens.isEmpty()) return
        val recent = SystemClock.uptimeMillis() - lastEditAt < 300
        val moved = candidatesEnd >= 0 && (newSelEnd != candidatesEnd || newSelStart != newSelEnd)
        val dropped = candidatesEnd < 0 && !recent
        if (moved || dropped) {
            finalizeWord()
            resetWord()
            currentInputConnection?.finishComposingText()
            lastPunct = null
        }
    }

    private fun toastOff() = getSharedPreferences("fingererror_theme", MODE_PRIVATE).getBoolean("toastOff", false)

    /** 고친 오타 1개 기록 + 기념일이면 말풍선 */
    /** 고친 오타 1개 기록 → 기념일이거나 100개 단위면 팻핑이가 키보드에서 알려줌 */
    private fun updateMenuStatus() {
        val n = lr.totalFixes
        kv?.menuStatus = "고친 오타 $n · 다음 리포트 ${(n / 100 + 1) * 100}"
    }

    private fun countFix(english: Boolean) {
        lr.countFix(english)
        updateMenuStatus()
        val m = lr.newCountMilestone()
        val r = lr.publishReportIfDue()
        if (m == null && r == null) return
        lr.save()
        if (toastOff()) return
        val t = when {
            m != null && r != null -> Anniversary.countText(Themes.currentId(this), m).first to ("📊 " + Anniversary.REPORT_SUB)
            m != null -> Anniversary.countText(Themes.currentId(this), m)
            else -> Anniversary.reportText(r!!)
        }
        kv?.showToast(t.first, t.second)
    }

    /** 영어: 문장 첫 글자는 자동 대문자 */
    private fun autoCap() {
        val v = kv ?: return
        if (v.page != KeyboardView.Page.ENGLISH || v.capsLock) return
        val before = currentInputConnection?.getTextBeforeCursor(3, 0)?.toString() ?: ""
        val t = before.trimEnd(' ')
        val start = before.isEmpty() || before.endsWith("\n") || (before.endsWith(" ") && (t.endsWith(".") || t.endsWith("!") || t.endsWith("?")))
        if (start && !v.shift) v.shift = true
    }

    private var lastShiftAt = 0L

    /** 들고 있는 단어가 화면 글자와 다르면(앱이 입력창을 비움 등) 놓아줌 */
    private fun syncWithScreen() {
        if (tokens.isEmpty() || SystemClock.uptimeMillis() - lastEditAt < 300) return
        // 앱이 앞 글자를 못 알려주면(null) 모르는 것 → 쓰던 글자를 그대로 둠 (놓아주면 자모로 못 지우고 통째로 지워짐)
        val before = currentInputConnection?.getTextBeforeCursor(tokens.size + 2, 0)?.toString() ?: return
        if (!before.endsWith(Hangul.assemble(tokens))) {
            resetWord()
            currentInputConnection?.finishComposingText()
        }
    }

    private fun resetWord() {
        tokens.clear(); wordEv.clear(); lastAuto = null; endWatch = null; wholeSyllable = false
    }

    // ---------------- 터치 ----------------

    override fun onPress(pointerId: Int, x: Float, y: Float): Int? {
        val v = kv ?: return null
        val r = pick(x, y) ?: return null
        val k = v.keys[r.first]
        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val fx = x / max(v.width, 1)
        val fy = y / max(v.height, 1)
        when (k.id) {
            "BACK" -> { backspace(); autoCap(); startRepeat(pointerId) }
            "SHIFT" -> {
                if (v.page == KeyboardView.Page.ENGLISH) {
                    // 한 번: 다음 글자만 대문자 / 빠르게 두 번: 계속 대문자
                    val now = SystemClock.uptimeMillis()
                    if (v.capsLock) { v.capsLock = false; v.shift = false }
                    else if (v.shift && now - lastShiftAt < 350) v.capsLock = true
                    else v.shift = !v.shift
                    lastShiftAt = now
                } else v.shift = !v.shift
            }
            "MODE" -> { commitAll(); v.toggleMode(); autoCap() }
            "SYMPG" -> v.toggleSym()
            else -> pending[pointerId] = Pending(r.first, k.id, fx, fy, r.second)
        }
        return r.first
    }

    override fun onRelease(pointerId: Int) {
        if (pointerId == repeatPid) stopRepeat()
        val p = pending.remove(pointerId) ?: return
        val v = kv ?: return
        val k = v.keys.getOrNull(p.idx) ?: return
        when {
            p.id == "LANG" -> { commitAll(); v.toggleLang(); autoCap() }
            k.letter && v.page == KeyboardView.Page.ENGLISH -> typeEnglish(p.id, p.fx, p.fy, p.raw)
            k.letter -> typeLetter(p.id, p.fx, p.fy, p.raw)
            else -> typeChar(chOf(p.id), p.id, p.fx, p.fy)
        }
    }

    override fun onCancel(pointerId: Int) {
        pending.remove(pointerId)
    }

    override fun onHandChange(mode: String) {
        // 손 모드마다 엄지 기록을 따로 (처음이면 양손/반대쪽 기록에서 출발)
        commitAll()
        history.clear(); clearDel()
        kv?.hand = mode
        lr.switchMode(kv?.profileKey() ?: mode)
    }

    override fun onWideChange() {
        // 폴드를 펼치면 펼침용 기록, 접으면 원래 기록 (처음 펼치면 접었을 때 기록에서 출발)
        commitAll()
        history.clear(); clearDel()
        kv?.let { lr.switchMode(it.profileKey()) }
    }

    override fun onLangHold() {
        // 한/A 꾹: 다른 키보드(삼성 키보드 등) 고르기
        commitAll()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun chOf(id: String) = when (id) { "SPACE" -> " "; "ENTER" -> "\n"; else -> id }

    private fun startRepeat(pid: Int) {
        stopRepeat()
        repeatPid = pid
        handler.postDelayed(repeatTick, 420)
    }

    private fun stopRepeat() {
        handler.removeCallbacks(repeatTick)
        repeatPid = -1
    }

    private fun saveSoon() {
        handler.removeCallbacks(saveRunnable)
        handler.postDelayed(saveRunnable, 1000)
    }

    // ---------------- 키 판정 ----------------

    private fun keyOf(id: String) = kv?.keys?.firstOrNull { it.id == id }

    private fun rowOf(id: String) = keyOf(id)?.row ?: -1

    private fun adjacent(a: String, b: String): Boolean {
        val ka = keyOf(a) ?: return false
        val kb = keyOf(b) ?: return false
        if (ka.w <= 0f || ka.h <= 0f) return false
        return hypot(((ka.cx - kb.cx) / ka.w).toDouble(), ((ka.cy - kb.cy) / ka.h).toDouble()) < 1.7
    }

    private fun sideOf(k: KeyboardView.Key): String {
        val w = kv?.width ?: 1
        return if (k.cx < w / 2f) "L" else "R"
    }

    /** 키의 실제 판정 중심: 그 키 기록이 적으면 같은 쪽 엄지의 전체 쏠림을 빌려 씀 */
    private fun effOff(k: KeyboardView.Key): Pair<Double, Double> {
        val sd = lr.side[sideOf(k)] ?: DoubleArray(3)
        val sw = min(1.0, sd[2] / 10.0)
        val bx = sd[0] * sw
        val by = sd[1] * sw
        val o = lr.off[KeyboardView.posId(k.id)] ?: return Pair(bx, by)
        val w = min(1.0, o[2] / 8.0)
        return Pair(o[0] * w + bx * (1 - w), o[1] * w + by * (1 - w))
    }

    private fun clamp(v: Double, a: Double) = max(-a, min(a, v))

    private fun learn(id: String, fx: Float, fy: Float, rate0: Double) {
        val v = kv ?: return
        val k = keyOf(id) ?: return
        if (k.w <= 0f || k.h <= 0f) return
        val x = fx * v.width
        val y = fy * v.height
        val ox = clamp(((x - k.cx) / k.w).toDouble(), 0.9)
        val oy = clamp(((y - k.cy) / k.h).toDouble(), 0.9)
        val sd = lr.side.getOrPut(sideOf(k)) { DoubleArray(3) }
        val sr = max(0.03, 1.0 / (sd[2] + 5))
        sd[0] = clamp(sd[0] + sr * (ox - sd[0]), 0.35)
        sd[1] = clamp(sd[1] + sr * (oy - sd[1]), 0.35)
        sd[2] += 1.0
        val o = lr.off[KeyboardView.posId(id)] ?: run { val e = effOff(k); doubleArrayOf(e.first, e.second, 0.0) }
        var rate = rate0
        if (rate < 0.1) rate = max(rate, 1.0 / (o[2] + 6))
        val r2 = if (rate > 0.1) 0.1 else 0.04
        lr.vx += r2 * (min((ox - o[0]) * (ox - o[0]), 0.5) - lr.vx)
        lr.vy += r2 * (min((oy - o[1]) * (oy - o[1]), 0.5) - lr.vy)
        lr.spreadN++
        o[0] = clamp(o[0] + rate * (ox - o[0]), 0.35)
        o[1] = clamp(o[1] + rate * (oy - o[1]), 0.35)
        o[2] += 1.0
        lr.off[KeyboardView.posId(id)] = o
    }

    /** 엄지 타입: "v" 위아래형 / "h" 양옆형 / "even" / null(분석 중), 판정 영역 비율 k */
    private fun thumbType(): Pair<String?, Double> {
        val total = lr.dirH + lr.dirV
        if (lr.spreadN < 30 && total < 20) return Pair(null, 1.0)
        var score = 0.0
        var w = 0.0
        val vx = max(lr.vx, 1e-4)
        val vy = max(lr.vy, 1e-4)
        if (lr.spreadN >= 30) { score += ln(vy / vx); w += 1 }
        if (total >= 20) { score += ln((lr.dirV + 1.0) / (lr.dirH + 1.0)); w += 1 }
        score /= w
        val k = if (lr.spreadN >= 30) clamp(sqrt(vy / vx) - 1, 0.4) + 1 else 1.0
        val type = if (score > 0.25) "v" else if (score < -0.25) "h" else "even"
        return Pair(type, k)
    }

    /** 지금 누르려는 키가 들어갈 '가능한 칸들' */
    private fun cellsFor(keyId: String): List<String> {
        if (tokens.isEmpty()) return if (Hangul.isV(keyId)) listOf("head_jung") else listOf("head_cho")
        val res = Hangul.trackSlots(tokens).first
        val last = res[tokens.size - 1] ?: Slot(0, "cho")
        val syl: Int
        val parts: List<String>
        if (Hangul.isV(keyId)) {
            syl = if (last.part == "cho") last.syl else last.syl + 1
            parts = listOf("jung")
        } else if (last.part == "jung") {
            syl = last.syl
            parts = listOf("jong", "cho")
        } else {
            syl = last.syl + 1
            parts = listOf("cho")
        }
        val poss = if (syl == 0) listOf("head") else listOf("mid", "end")
        val out = ArrayList<String>()
        for (p in poss) for (q in parts) out.add("${p}_$q")
        return out
    }

    private fun spNeighbor(k: KeyboardView.Key, x: Float): Pair<KeyboardView.Key, Float>? {
        val v = kv ?: return null
        val row = v.keys.filter { it.row == k.row }.sortedBy { it.rect.left }
        val i = row.indexOf(k)
        val fromLeft = (x - k.rect.left) / k.w
        val nb = if (fromLeft < 0.5f) row.getOrNull(i - 1) else row.getOrNull(i + 1)
        val edge = if (fromLeft < 0.5f) fromLeft else 1 - fromLeft
        val dot = keyOf(".")?.w ?: k.w
        if (nb == null || nb.id !in SP_LEARN) return null
        return Pair(nb, edge * k.w / min(k.w, dot))
    }

    /** 터치 위치 → (입력할 키 번호, 손가락 바로 아래 키) */
    private fun pick(x: Float, y: Float): Pair<Int, String>? {
        val v = kv ?: return null
        val keys = v.keys
        if (keys.isEmpty()) return null
        for ((i, k) in keys.withIndex()) {
            if (k.letter) continue
            if (x >= k.rect.left - v.gap / 2 && x <= k.rect.right + v.gap / 2 &&
                y >= k.rect.top - v.vgap / 2 && y <= k.rect.bottom + v.vgap / 2) {
                // 스페이스·마침표 경계: 내가 자주 헷갈리는 쪽이면 이웃 키로
                if (lr.fixOn && k.id in SP_LEARN) {
                    val nb = spNeighbor(k, x)
                    if (nb != null && nb.second < 0.45f) {
                        val lean = (lr.spConf["${k.id}>${nb.first.id}"] ?: 0) - (lr.spConf["${nb.first.id}>${k.id}"] ?: 0)
                        if (lean >= 2) return Pair(keys.indexOf(nb.first), k.id)
                    }
                }
                return Pair(i, k.id)
            }
        }
        val letterIdx = keys.indices.filter { keys[it].letter }
        val pool = if (letterIdx.isEmpty()) keys.indices.toList() else letterIdx
        fun dist(i: Int, dx: Double, dy: Double, sq: Double): Double {
            val k = keys[i]
            if (k.w <= 0f || k.h <= 0f) return Double.MAX_VALUE
            val ex = (x - (k.cx + dx * k.w)) / k.w
            val ey = (y - (k.cy + dy * k.h)) / k.h
            return hypot(ex * sq, ey / sq)
        }
        val rawI = pool.minByOrNull { dist(it, 0.0, 0.0, 1.0) } ?: return null
        val raw = keys[rawI].id
        if (!lr.fixOn || letterIdx.isEmpty()) return Pair(rawI, raw)

        // 내 엄지 쏠림 + 흔들림 방향으로 판정
        val t = thumbType()
        val sq = sqrt(t.second)
        val scored = letterIdx.map { i -> val o = effOff(keys[i]); Pair(i, dist(i, o.first, o.second, sq)) }.sortedBy { it.second }
        var chosen = scored[0].first
        if (scored.size > 1 && scored[1].second / max(scored[0].second, 0.01) < 1.25) {
            val a = keys[scored[0].first]
            val b = keys[scored[1].first]
            // 지금 자리 기록이 충분하면 그걸, 아니면 전체 기록을
            var cellLean = 0
            var cellN = 0
            if (v.page == KeyboardView.Page.HANGUL) for (c in cellsFor(b.id)) {
                val tb = lr.cell[c] ?: continue
                cellLean += (tb["${a.id}>${b.id}"] ?: 0) - (tb["${b.id}>${a.id}"] ?: 0)
                cellN += tb.values.sum()
            }
            val lean = if (cellN >= 4) cellLean else (lr.conf["${a.id}>${b.id}"] ?: 0) - (lr.conf["${b.id}>${a.id}"] ?: 0)
            val vertical = a.row != b.row
            val need = if ((vertical && t.first == "v") || (!vertical && t.first == "h")) 2 else 3
            if (lean >= need) chosen = scored[1].first
        }
        return Pair(chosen, raw)
    }

    // ---------------- 입력 ----------------

    private fun clearDel() { delBuf.clear(); reIdx = 0 }

    private fun pushHistory(m: Meta?) {
        history.add(m)
        if (history.size > 80) history.removeAt(0)
    }

    private fun confirmPrev() {
        val m = history.lastOrNull() ?: return
        if (!m.ok && !m.sp) {
            learn(m.id, m.fx, m.fy, 0.04)
            m.ok = true
            lr.countTap(KeyboardView.EN2KO.containsKey(m.id))
        }
    }

    private fun applyComposing() {
        val ic = currentInputConnection ?: return
        lastEditAt = SystemClock.uptimeMillis()
        if (tokens.isEmpty()) {
            ic.setComposingText("", 1)
            ic.finishComposingText()
        } else {
            ic.setComposingText(Hangul.assemble(tokens), 1)
        }
    }

    /** 단어가 끝날 때: "요" 뒤에 조사만 붙었으면 어미가 아니라 이름 → 원래대로 (가요를 → 가료를) */
    private fun endWordCheck() {
        val w = endWatch ?: return
        endWatch = null
        if (tokens.size <= w.idx + 2 || tokens[w.idx] != "ㅇ") return
        val after = Hangul.assemble(tokens.subList(w.idx + 2, tokens.size))
        if (after !in PARTICLES) return
        tokens[w.idx] = w.orig
        applyComposing()
        lr.endNo.add(w.key)
        lr.uncountFix(false)
        updateMenuStatus()
        saveSoon()
    }

    private fun commitAll() {
        endWordCheck()   // !#1 · 한/A 로 단어를 끝낼 때도 '가료를' 확인
        finalizeWord()
        if (tokens.isNotEmpty()) {
            currentInputConnection?.finishComposingText()
            resetWord()
        }
    }

    private fun prevSyllable(arr: List<String>): String? {
        if (arr.isEmpty() || !Hangul.isV(arr[arr.size - 1])) return null
        val w = Hangul.assemble(arr)
        return if (w.isEmpty()) null else w.substring(w.length - 1)
    }

    /** 어미 교정: 받침 없는 말끝 글자 뒤에서 ㅛ를 칠 때, ㅇ 옆 키가 눌렸으면 ㅇ으로 */
    private fun endCheck() {
        val n = tokens.size
        if (n < 3 || tokens[n - 1] != "ㅛ") return
        val c = tokens[n - 2]
        if (!Hangul.isC(c) || c == "ㅇ") return
        val p = prevSyllable(tokens.subList(0, n - 2)) ?: return
        if (p !in END_PREV) return
        // 예외는 단어 통째로 기억 (가료만 빼고, 먹었어료→먹었어요 같은 교정은 그대로)
        val key = "W:" + Hangul.assemble(tokens.subList(0, n - 2)) + c
        if (key in lr.endNo) return
        val learnedEnd = (lr.cell["end_cho"]?.get("$c>ㅇ") ?: 0) >= 2
        if (c !in END_BASE && !(learnedEnd && adjacent(c, "ㅇ"))) return
        tokens[n - 2] = "ㅇ"
        lastAuto = AutoFix(n - 2, c, key)
        endWatch = lastAuto
        countFix(false)
    }

    /** 단어가 끝나면, 지우고 고친 기록을 정확한 자리 칸에 저장 */
    private fun finalizeWord() {
        if (wordEv.isEmpty()) return
        val (res, count) = Hangul.trackSlots(tokens)
        for (ev in wordEv) {
            val r = res.getOrNull(ev.i) ?: continue
            val pos = if (r.syl == 0) "head" else if (r.syl == count - 1) "end" else "mid"
            val cellName = "${pos}_${r.part}"
            val t = lr.cell.getOrPut(cellName) { HashMap() }
            val key = "${ev.wrong}>${ev.right}"
            t[key] = (t[key] ?: 0) + 1
            if (pos == "end") {
                // 말끝 실수는 그 칸에만 (단어 처음의 같은 키는 영향 안 받게)
                lr.conf[key] = max(0, (lr.conf[key] ?: 0) - 1)
            } else {
                learn(ev.right, ev.fx, ev.fy, 0.12)
            }
        }
        wordEv.clear()
    }

    private fun typeLetter(id: String, fx: Float, fy: Float, raw: String) {
        val ic = currentInputConnection ?: return
        syncWithScreen()
        wholeSyllable = false
        val v = kv ?: return

        // 단어 바로 뒤 . , 를 찍고 띄어쓰기 없이 다음 글자 → 스페이스였던 것 (그렇게 자주 틀리는 사람만)
        val lp = lastPunct
        if (lp != null && tokens.isEmpty() && lr.fixOn) {
            if ((lr.spConf["$lp>SPACE"] ?: 0) - (lr.spConf["SPACE>$lp"] ?: 0) >= 2) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(" ", 1)
                history.lastOrNull()?.id = "SPACE"
                countFix(false)
            }
        }
        lastPunct = null

        // 지우고 다시 친 경우: 같은 자리끼리 비교해서 배움
        if (delBuf.isNotEmpty() && reIdx < delBuf.size && SystemClock.uptimeMillis() - delT < 10000) {
            val orig = delBuf[reIdx]
            // 키보드가 바꿔 준 글자를 지우고 원래 눌린 키를 다시 침 → 잘못 고친 것, 숫자에서 뺌
            if (orig != null && orig.raw.isNotEmpty() && orig.raw != orig.id && id == orig.raw) { lr.uncountFix(KeyboardView.EN2KO.containsKey(orig.id)); updateMenuStatus() }
            // 예외로 기억했던 단어인데 사용자가 직접 '요'로 고침 → 예외 취소 (가료 예외 때문에 '가요'를 못 고치는 일 방지)
            if (orig != null && id == "ㅇ" && orig.id in END_BASE && (prevSyllable(tokens) ?: "") in END_PREV) {
                if (lr.endNo.remove("W:" + Hangul.assemble(tokens) + orig.id)) saveSoon()
            }
            if (orig != null && !orig.sp && orig.id != id && adjacent(orig.id, id)) {
                wordEv.add(WordEv(tokens.size, orig.id, id, orig.fx, orig.fy))
                val key = "${orig.id}>$id"
                lr.conf[key] = (lr.conf[key] ?: 0) + 1
                lr.learned++
                if (rowOf(orig.id) == rowOf(id)) lr.dirH++ else lr.dirV++
            }
            reIdx++
            if (reIdx >= delBuf.size) clearDel()
        } else if (delBuf.isNotEmpty()) {
            clearDel()
        }
        confirmPrev()

        var ch = id
        if (v.shift) {
            ch = Hangul.SHIFT[id] ?: id
            v.shift = false
        }
        lastAuto = null
        tokens.add(ch)
        pushHistory(Meta(id, fx, fy, false, false, raw))
        if (raw != id) countFix(false)
        if (lr.fixOn) endCheck()
        applyComposing()
        saveSoon()
    }

    /** 영어 한 글자 (조합 없이 바로 입력, 엄지 학습은 한글과 같은 자리로 공유) */
    private fun typeEnglish(id: String, fx: Float, fy: Float, raw: String) {
        val ic = currentInputConnection ?: return
        val v = kv ?: return
        lastPunct = null
        if (delBuf.isNotEmpty() && reIdx < delBuf.size && SystemClock.uptimeMillis() - delT < 10000) {
            val orig = delBuf[reIdx]
            // 키보드가 바꿔 준 글자를 지우고 원래 눌린 키를 다시 침 → 잘못 고친 것, 숫자에서 뺌
            if (orig != null && orig.raw.isNotEmpty() && orig.raw != orig.id && id == orig.raw) { lr.uncountFix(KeyboardView.EN2KO.containsKey(orig.id)); updateMenuStatus() }
            if (orig != null && !orig.sp && orig.id != id && adjacent(orig.id, id)) {
                learn(id, orig.fx, orig.fy, 0.12)
                val key = "${orig.id}>$id"
                lr.conf[key] = (lr.conf[key] ?: 0) + 1
                lr.learned++
                if (rowOf(orig.id) == rowOf(id)) lr.dirH++ else lr.dirV++
            }
            reIdx++
            if (reIdx >= delBuf.size) clearDel()
        } else if (delBuf.isNotEmpty()) {
            clearDel()
        }
        confirmPrev()
        commitAll()
        var ch = id
        if (v.shift || v.capsLock) {
            ch = id.uppercase()
            if (!v.capsLock) v.shift = false
        }
        lastAuto = null
        ic.commitText(ch, 1)
        lastEditAt = SystemClock.uptimeMillis()
        pushHistory(Meta(id, fx, fy, false, false, raw))
        if (raw != id) countFix(true)
        saveSoon()
    }

    private fun typeChar(ch: String, keyId: String, fx: Float, fy: Float) {
        val ic = currentInputConnection ?: return

        // 지우고 다시 친 경우: 띄어쓰기·문장부호 실수 배우기
        if (delBuf.isNotEmpty() && reIdx < delBuf.size && SystemClock.uptimeMillis() - delT < 10000) {
            val orig = delBuf[reIdx]
            if (orig != null && orig.sp && orig.id != keyId && keyId in SP_LEARN) {
                val key = "${orig.id}>$keyId"
                lr.spConf[key] = (lr.spConf[key] ?: 0) + 1
                lr.learned++
            }
        }
        confirmPrev()
        lastAuto = null
        if (delBuf.isNotEmpty()) {
            reIdx++
            if (reIdx >= delBuf.size) clearDel()
        }
        endWordCheck()
        val hadWord = tokens.isNotEmpty()
        commitAll()

        if (keyId == "ENTER") {
            val ei = currentInputEditorInfo
            val action = (ei?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
            val noEnterAction = ((ei?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            if (ei != null && !noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action)
            } else {
                ic.commitText("\n", 1)
            }
        } else {
            ic.commitText(ch, 1)
        }
        pushHistory(Meta(keyId, fx, fy, true, true))
        lastPunct = if ((ch == "." || ch == ",") && hadWord) ch else null
        saveSoon()
        autoCap()
    }

    /** ⌫가 앞 글자로 넘어갔는지 (지금 쓰던 글자만 자모 단위, 그 앞 글자는 통째로) */
    private var wholeSyllable = false

    /** 지운 만큼 친 기록을 '지운 것들'로 옮김 (다시 치면 같은 자리끼리 비교해서 배움) */
    private fun moveToDelBuf(n: Int) {
        if (reIdx > 0) clearDel()
        repeat(maxOf(n, 1)) {
            val m = if (history.isEmpty()) null else history.removeAt(history.size - 1)
            delBuf.add(0, m)
        }
        while (delBuf.size > 12) delBuf.removeAt(delBuf.size - 1)
        delT = SystemClock.uptimeMillis()
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return

        // 자동 어미 교정 직후 ⌫ → 원래대로 되돌리고 이 조합은 다시 안 고침
        val la = lastAuto
        if (la != null && tokens.size == la.idx + 2) {
            tokens[la.idx] = la.orig
            lr.endNo.add(la.key)
            lr.uncountFix(false)
            updateMenuStatus()
            lastAuto = null
            endWatch = null
            applyComposing()
            saveSoon()
            return
        }
        lastAuto = null
        lastPunct = null
        syncWithScreen()
        endWatch?.let { if (tokens.size <= it.idx + 2) endWatch = null }   // 교정한 글자까지 지우면 지켜보기 끝

        if (tokens.isNotEmpty()) {
            val before = Hangul.assemble(tokens)
            if (wholeSyllable) {
                // 앞 글자는 통째로 (아이폰·삼성 기본 키보드와 같게)
                val target = before.dropLast(1)
                var k = 1
                while (k < tokens.size && Hangul.assemble(tokens.subList(0, tokens.size - k)) != target) k++
                moveToDelBuf(k)
                repeat(k) { tokens.removeAt(tokens.size - 1) }
            } else {
                // 띄어쓰기 전 단어는 끝까지 자모 하나씩 (습니다 → 습니ㄷ → 습니 → 습ㄴ → 습 → 스 → ㅅ)
                moveToDelBuf(1)
                tokens.removeAt(tokens.size - 1)
            }
            wordEv.removeAll { it.i >= tokens.size }
            applyComposing()
            return
        }

        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        // 이미 확정된 글자(띄어쓰기 뒤 등)는 한 글자씩 통째로 지움
        val prev = ic.getTextBeforeCursor(1, 0)
        if (!prev.isNullOrEmpty()) {
            moveToDelBuf(Hangul.decompose(prev[0])?.size ?: 1)
            ic.deleteSurroundingText(1, 0)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
    }
}
