package com.moon.lotto

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moon.lotto.core.Filters
import com.moon.lotto.core.Generator
import com.moon.lotto.core.History
import com.moon.lotto.core.Nums
import com.moon.lotto.core.Pick
import com.moon.lotto.core.Post
import com.moon.lotto.core.PostBuilder
import com.moon.lotto.core.PostKind
import com.moon.lotto.core.Sim
import com.moon.lotto.core.SimSummary
import com.moon.lotto.core.Stats
import com.moon.lotto.core.Strategy
import com.moon.lotto.core.history
import com.moon.lotto.core.bestEver
import com.moon.lotto.data.DrawRepository
import com.moon.lotto.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class Tab(val label: String, val emoji: String) {
    GEN("뽑기", "🎲"), STAT("통계", "📊"), SIM("실험실", "🧪"), MINE("내 번호", "🎟️"), CAFE("카페", "✍️"),
}
enum class Cell { FIX, EX }

/** 방금 뽑은 번호 한 줄. best = 역대 최고 등수와 회차 */
data class PickRow(val pick: Pick, val best: Pair<Int, Int>?)

class UntilView(val nums: List<Int>, val weeks: Long, val cnt: LongArray, val won: Boolean, val running: Boolean)

class LottoViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DrawRepository(app)
    private val prefs = Prefs(app)

    var tab by mutableStateOf(Tab.GEN)
    var message by mutableStateOf<String?>(null)
    var showHelp by mutableStateOf(false)
    var showGuide by mutableStateOf(!prefs.seenGuide)
        private set

    var stats by mutableStateOf(Stats(emptyList()))

        private set
    var dataInfo by mutableStateOf("당첨번호 불러오는 중…")
        private set

    // 뽑기
    var strategy by mutableStateOf(Strategy.RANDOM)
    var range by mutableIntStateOf(50)
    var games by mutableIntStateOf(5)
    var seedText by mutableStateOf("")
    var filters by mutableStateOf(Filters())
    val grid = mutableStateMapOf<Int, Cell>()
    var picks by mutableStateOf(listOf<PickRow>())
        private set
    /** 뽑을 때마다 늘어서 공 애니메이션을 다시 돌린다 */
    var genCount by mutableIntStateOf(0)
        private set
    val savedRows = mutableStateListOf<Int>()

    // 통계
    var statRange by mutableIntStateOf(50)
    var statSortByFreq by mutableStateOf(false)
    var lensNum by mutableIntStateOf(7)

    // 시뮬
    var simNums by mutableStateOf("")
    var hist by mutableStateOf<History?>(null)
        private set
    var years by mutableStateOf<Sim.Years?>(null)
        private set
    var simGames by mutableIntStateOf(5)
    var simYears by mutableIntStateOf(10)
    var until by mutableStateOf<UntilView?>(null)
        private set
    private var untilJob: Job? = null
    private var lastSim: SimSummary? = null

    // 내 번호
    val saved = mutableStateListOf<Pick>()
    var manualNums by mutableStateOf("")

    // 카페
    var cafeUrl by mutableStateOf(prefs.cafeUrl)
    var nick by mutableStateOf(prefs.nick)
    var postKind by mutableStateOf(PostKind.PICK)
    var postTitle by mutableStateOf("")
    var postBody by mutableStateOf("")

    init {
        saved.addAll(prefs.loadSaved())
        viewModelScope.launch {
            setDraws(repo.loadLocal())
            val remote = repo.fetchRemote()
            if (remote != null && remote.size >= stats.draws.size) setDraws(remote)
            else if (remote == null && stats.draws.isEmpty()) dataInfo = "당첨번호를 못 받았어요. 인터넷 연결 뒤 다시 열어 주세요"
        }
    }

    private fun setDraws(draws: List<com.moon.lotto.core.Draw>) {
        if (draws.isEmpty()) return
        stats = Stats(draws)
        val d = stats.last!!
        dataInfo = "${d.no}회(${d.date})까지 ${draws.size}회로 연구 중 · 다음은 ${stats.nextNo}회"
        picks = picks.map { it.copy(best = stats.bestEver(it.pick.nums)) }
    }

    // ---------- 뽑기 ----------
    fun tapCell(n: Int) {
        when (grid[n]) {
            null -> grid[n] = Cell.FIX
            Cell.FIX -> grid[n] = Cell.EX
            Cell.EX -> grid.remove(n)
        }
    }

    fun clearGrid() = grid.clear()
    val fixed get() = grid.filterValues { it == Cell.FIX }.keys
    val excluded get() = grid.filterValues { it == Cell.EX }.keys

    fun generate() {
        val r = Generator(stats).generate(strategy, range, games, fixed, excluded, filters, seedText, LocalDate.now().toString())
        r.error?.let { message = it }
        if (r.picks.isEmpty()) return
        picks = r.picks.map { PickRow(Pick(it, stats.nextNo, strategy), stats.bestEver(it)) }
        savedRows.clear()
        genCount++
    }

    /** ☆ 누르면 저장, ★ 다시 누르면 저장 취소 */
    fun toggleRow(i: Int) {
        val p = picks[i].pick
        if (i in savedRows) {
            savedRows.remove(i)
            val k = saved.indexOfFirst { it.nums == p.nums && it.round == p.round }
            if (k >= 0) deleteSaved(k)
        } else {
            addSaved(p)
            savedRows += i
        }
    }

    fun dismissGuide() {
        showGuide = false
        prefs.seenGuide = true
    }

    fun saveAll() {
        picks.indices.filter { it !in savedRows }.forEach { toggleRow(it) }
        message = "${picks.size}게임을 내 번호에 저장했어요"
    }

    private fun addSaved(p: Pick) {
        saved.add(0, p.copy(at = System.currentTimeMillis()))
        prefs.saveSaved(saved)
    }

    // ---------- 내 번호 ----------
    fun addManual() {
        val nums = Nums.parse(manualNums) ?: run { message = "1~45 사이 서로 다른 번호 6개를 넣어 주세요"; return }
        addSaved(Pick(nums, stats.nextNo, Strategy.MANUAL))
        manualNums = ""
        message = "${stats.nextNo}회 번호로 저장했어요"
    }

    fun deleteSaved(i: Int) {
        saved.removeAt(i)
        prefs.saveSaved(saved)
    }

    // ---------- 시뮬 ----------
    fun runHist(nums: List<Int>? = Nums.parse(simNums)) {
        if (nums == null) { message = "1~45 사이 서로 다른 번호 6개를 넣어 주세요"; return }
        if (stats.draws.isEmpty()) { message = "당첨번호를 받은 뒤에 쓸 수 있어요"; return }
        simNums = nums.joinToString(" ")
        hist = stats.history(nums)
    }

    fun runYears() {
        viewModelScope.launch {
            val y = withContext(Dispatchers.Default) { Sim.years(simGames, simYears) }
            years = y
            lastSim = SimSummary.Years(y)
        }
    }

    fun startUntil() {
        if (untilJob?.isActive == true) return
        val u = Sim.Until(Nums.parse(simNums) ?: Sim.Until.randomNums())
        untilJob = viewModelScope.launch {
            while (isActive) {
                val won = withContext(Dispatchers.Default) { u.step(300_000) }
                val done = won || u.weeks >= Sim.Until.LIMIT
                until = UntilView(u.nums, u.weeks, u.cnt.copyOf(), u.won, running = !done)
                if (done) break
            }
            lastSim = SimSummary.Until(u.nums, u.weeks, u.cnt.copyOf(), u.won)
        }
    }

    fun stopUntil() {
        untilJob?.cancel()
        until = until?.let { UntilView(it.nums, it.weeks, it.cnt, it.won, running = false) }
        until?.let { lastSim = SimSummary.Until(it.nums, it.weeks, it.cnt, it.won) }
    }

    // ---------- 카페 ----------
    fun openPost(kind: PostKind) {
        postKind = kind
        fillPost()
        tab = Tab.CAFE
    }

    fun fillPost() {
        val b = PostBuilder(stats, nick)
        val p: Post = when (postKind) {
            PostKind.PICK -> b.pick(picks.map { it.pick }.ifEmpty { saved.filter { it.round == stats.nextNo }.take(10) }, fixed, excluded, range)
            PostKind.REPORT -> b.report(statRange)
            PostKind.PAIRS -> b.pairs(statRange)
            PostKind.HIST -> b.hist(hist)
            PostKind.SIM -> b.sim(lastSim)
            PostKind.CHECK -> b.check(saved)
        }
        postTitle = p.title
        postBody = p.body
    }

    fun saveCafe() {
        prefs.cafeUrl = cafeUrl.trim()
        prefs.nick = nick.trim()
    }

    val postText get() = "$postTitle\n\n$postBody"

    fun resetFilters() {
        filters = Filters()
    }
}
