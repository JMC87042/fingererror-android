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

    class Meta(var id: String, val fx: Float, val fy: Float, var ok: Boolean, val sp: Boolean)
    class WordEv(val i: Int, val wrong: String, val right: String, val fx: Float, val fy: Float)
    class AutoFix(val idx: Int, val orig: String, val key: String)
    class Pending(val idx: Int, val id: String, val fx: Float, val fy: Float, val raw: String)

    companion object {
        /** '요' 앞에 자주 오는 글자 (받침 없음). 자료·재료·나쵸·아뇨·아교 같은 진짜 단어는 피하도록 고름 */
        val END_PREV: Set<String> = "어아에세네해여워와예래게데줘봐돼써가거서져려대케레셔쳐펴혀까떠냐니지시".map { it.toString() }.toSet()
        val END_BASE = setOf("ㄹ", "ㅌ", "ㄷ", "ㅊ")
        val SP_LEARN = setOf("SPACE", ".", ",", "ENTER")
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
        resetWord()
        history.clear(); clearDel(); lastPunct = null
    }

    override fun onFinishInputView(finishingInput: Boolean) {
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

    private fun resetWord() {
        tokens.clear(); wordEv.clear(); lastAuto = null
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
            "BACK" -> { backspace(); startRepeat(pointerId) }
            "SHIFT" -> v.shift = !v.shift
            "MODE" -> { commitAll(); v.toggleMode() }
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
            p.id == "GLOBE" -> {
                commitAll()
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
            k.letter -> typeLetter(p.id, p.fx, p.fy, p.raw)
            else -> typeChar(chOf(p.id), p.id, p.fx, p.fy)
        }
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
        val o = lr.off[k.id] ?: return Pair(bx, by)
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
        val o = lr.off[id] ?: run { val e = effOff(k); doubleArrayOf(e.first, e.second, 0.0) }
        var rate = rate0
        if (rate < 0.1) rate = max(rate, 1.0 / (o[2] + 6))
        val r2 = if (rate > 0.1) 0.1 else 0.04
        lr.vx += r2 * (min((ox - o[0]) * (ox - o[0]), 0.5) - lr.vx)
        lr.vy += r2 * (min((oy - o[1]) * (oy - o[1]), 0.5) - lr.vy)
        lr.spreadN++
        o[0] = clamp(o[0] + rate * (ox - o[0]), 0.35)
        o[1] = clamp(o[1] + rate * (oy - o[1]), 0.35)
        o[2] += 1.0
        lr.off[id] = o
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
            for (c in cellsFor(b.id)) {
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
            lr.taps++
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

    private fun commitAll() {
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
        if ((p + c) in lr.endNo) return
        val learnedEnd = (lr.cell["end_cho"]?.get("$c>ㅇ") ?: 0) >= 2
        if (c !in END_BASE && !(learnedEnd && adjacent(c, "ㅇ"))) return
        tokens[n - 2] = "ㅇ"
        lastAuto = AutoFix(n - 2, c, p + c)
        lr.fixes++
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
        val v = kv ?: return

        // 단어 바로 뒤 . , 를 찍고 띄어쓰기 없이 다음 글자 → 스페이스였던 것 (그렇게 자주 틀리는 사람만)
        val lp = lastPunct
        if (lp != null && tokens.isEmpty() && lr.fixOn) {
            if ((lr.spConf["$lp>SPACE"] ?: 0) - (lr.spConf["SPACE>$lp"] ?: 0) >= 2) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(" ", 1)
                history.lastOrNull()?.id = "SPACE"
                lr.fixes++
            }
        }
        lastPunct = null

        // 지우고 다시 친 경우: 같은 자리끼리 비교해서 배움
        if (delBuf.isNotEmpty() && reIdx < delBuf.size && SystemClock.uptimeMillis() - delT < 10000) {
            val orig = delBuf[reIdx]
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
        pushHistory(Meta(id, fx, fy, false, false))
        if (raw != id) lr.fixes++
        if (lr.fixOn) endCheck()
        applyComposing()
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
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return

        // 자동 어미 교정 직후 ⌫ → 원래대로 되돌리고 이 조합은 다시 안 고침
        val la = lastAuto
        if (la != null && tokens.size == la.idx + 2) {
            tokens[la.idx] = la.orig
            lr.endNo.add(la.key)
            lr.fixes = max(0, lr.fixes - 1)
            lastAuto = null
            applyComposing()
            saveSoon()
            return
        }
        lastAuto = null
        lastPunct = null

        val m = if (history.isEmpty()) null else history.removeAt(history.size - 1)
        if (reIdx > 0) clearDel()          // 다시 치다가 또 지우면 새로 시작
        delBuf.add(0, m)                   // 원래 순서 유지
        if (delBuf.size > 12) delBuf.removeAt(delBuf.size - 1)
        delT = SystemClock.uptimeMillis()

        if (tokens.isNotEmpty()) {
            tokens.removeAt(tokens.size - 1)
            wordEv.removeAll { it.i >= tokens.size }
            applyComposing()
            return
        }

        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(1, 0)
        if (!before.isNullOrEmpty()) {
            // 앞 글자를 자모로 풀어서 한 자모만 지움 (값 → 갑)
            val jamo = Hangul.decompose(before[0])
            ic.deleteSurroundingText(1, 0)
            if (jamo != null && jamo.size > 1) {
                tokens.addAll(jamo.subList(0, jamo.size - 1))
                applyComposing()
            }
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
    }
}
