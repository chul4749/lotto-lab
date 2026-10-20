package com.moon.lotto.core

/** 당첨번호 통계. draws 는 오래된 회차부터. */
class Stats(val draws: List<Draw>) {
    val last: Draw? get() = draws.lastOrNull()
    val latestNo get() = last?.no ?: 0
    val nextNo get() = latestNo + 1

    val pastSet: Set<List<Int>> by lazy { draws.mapTo(HashSet()) { it.nums } }

    /** 번호별로 마지막으로 나온 뒤 지난 회차 수 */
    val gaps: IntArray by lazy {
        IntArray(46) { draws.size }.also { g -> draws.forEachIndexed { i, d -> d.nums.forEach { g[it] = draws.size - 1 - i } } }
    }

    private val byNo by lazy { draws.associateBy { it.no } }
    fun drawOf(no: Int) = byNo[no]

    fun range(r: Int): List<Draw> = if (r > 0) draws.takeLast(r) else draws
    fun freq(r: Int) = freqOf(range(r))
    fun pairMatrix(r: Int) = pairMatrixOf(range(r))

    fun hotList(r: Int): List<Int> {
        val f = freq(r)
        return Nums.ALL.sortedWith(compareByDescending<Int> { f[it] }.thenBy { it })
    }

    fun coldList(): List<Int> = Nums.ALL.sortedWith(compareByDescending<Int> { gaps[it] }.thenBy { it })

    data class PairCount(val a: Int, val b: Int, val count: Int)

    fun topPairs(r: Int, k: Int): List<PairCount> {
        val m = pairMatrix(r)
        val out = ArrayList<PairCount>()
        for (a in 1..45) for (b in a + 1..45) out += PairCount(a, b, m[a][b])
        return out.sortedWith(compareByDescending<PairCount> { it.count }.thenBy { it.a }.thenBy { it.b }).take(k)
    }

    data class Summary(val n: Int, val avgSum: Int, val consecPct: Double, val carryPct: Double)

    fun summary(r: Int): Summary {
        val list = range(r)
        if (list.isEmpty()) return Summary(0, 0, 0.0, 0.0)
        val start = draws.size - list.size
        val consec = list.count { Nums.hasConsec(it.nums) }
        val carry = list.withIndex().count { (i, d) -> start + i > 0 && d.nums.any { it in draws[start + i - 1].nums } }
        val avg = Math.round(list.sumOf { it.nums.sum() }.toDouble() / list.size).toInt()
        return Summary(list.size, avg, pct(consec, list.size), pct(carry, list.size))
    }

    fun oddEven(r: Int) = IntArray(7).also { c -> range(r).forEach { c[Nums.oddCount(it.nums)]++ } }
    fun zones(r: Int) = IntArray(5).also { c -> range(r).forEach { d -> d.nums.forEach { c[Nums.zoneOf(it)]++ } } }
    fun ends(r: Int) = IntArray(10).also { c -> range(r).forEach { d -> d.nums.forEach { c[it % 10]++ } } }
    fun sumBins(r: Int): List<Int> = SUM_BINS.map { b -> range(r).count { it.nums.sum() in b } }

    data class Lens(
        val n: Int, val all: Int, val recent: Int, val gap: Int, val bonus: Int,
        val avgGap: Double?, val ratePct: Double, val companions: List<Pair<Int, Int>>, val lastSeen: Draw?,
    )

    fun lens(n: Int, r: Int): Lens {
        val seen = draws.filter { n in it.nums }
        val gapsList = seen.zipWithNext { a, b -> b.no - a.no }
        val m = pairMatrixOf(draws)[n]
        val comp = Nums.ALL.filter { it != n }.sortedWith(compareByDescending<Int> { m[it] }.thenBy { it }).take(5).map { it to m[it] }
        return Lens(
            n = n,
            all = seen.size,
            recent = freq(r)[n],
            gap = gaps[n],
            bonus = draws.count { it.bonus == n },
            avgGap = if (gapsList.isEmpty()) null else Math.round(gapsList.average() * 10) / 10.0,
            ratePct = pct(seen.size, draws.size),
            companions = comp,
            lastSeen = seen.lastOrNull(),
        )
    }

    companion object {
        val SUM_BINS = listOf(21..79, 80..99, 100..119, 120..139, 140..159, 160..179, 180..199, 200..255)

        fun freqOf(list: List<Draw>) = IntArray(46).also { f -> list.forEach { d -> d.nums.forEach { f[it]++ } } }

        fun pairMatrixOf(list: List<Draw>): Array<IntArray> {
            val m = Array(46) { IntArray(46) }
            for (d in list) {
                val a = d.nums
                for (i in a.indices) for (j in i + 1 until a.size) {
                    m[a[i]][a[j]]++
                    m[a[j]][a[i]]++
                }
            }
            return m
        }
    }
}
