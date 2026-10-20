package com.moon.lotto.core

import kotlin.random.Random

/** 뽑는 방식과 필터로 번호 조합을 만든다. */
class Generator(private val stats: Stats, private val random: Random = Random.Default) {

    data class Result(val picks: List<List<Int>>, val error: String? = null)

    fun generate(
        strategy: Strategy,
        range: Int,
        games: Int,
        fixed: Set<Int>,
        excluded: Set<Int>,
        filters: Filters,
        seedText: String = "",
        today: String = "",
    ): Result {
        if (fixed.size > 5) return Result(emptyList(), "고정수는 5개까지만 돼요")
        if (strategy.needsDraws && stats.draws.isEmpty()) return Result(emptyList(), "당첨번호를 받은 뒤에 쓸 수 있어요")
        val rnd = if (strategy == Strategy.SEED) {
            if (seedText.isBlank()) return Result(emptyList(), "이름이나 생일을 넣어 주세요")
            Random(seedOf("${seedText.trim()}|$today"))
        } else random
        val ctx = Ctx(range, rulesFor(strategy, filters), rnd)
        val out = ArrayList<List<Int>>()
        val seen = HashSet<List<Int>>()
        repeat(games) {
            var nums: List<Int>? = null
            for (k in 0 until 20) {
                nums = makeOne(strategy, fixed, excluded, ctx)
                if (nums == null || nums !in seen) break
            }
            if (nums == null) return Result(out, "조건이 너무 빡빡해요. 필터나 고정·제외수를 줄여 보세요")
            seen += nums
            out += nums
        }
        return Result(out)
    }

    /** 한 번 뽑을 때 쓰는 통계를 미리 계산해 둔다 */
    private inner class Ctx(val range: Int, val rules: Rules, val rnd: Random) {
        val freq by lazy { stats.freq(range) }
        val hot by lazy { stats.hotList(range) }
        val cold by lazy { stats.coldList() }
        val pairs by lazy { stats.pairMatrix(range) }
    }

    private class Rules(
        val f: Filters,
        val oddRange: IntRange?,
        val sumMin: Int,
        val sumMax: Int,
        val consec: Consec,
        val zoneMax: Int?,
        val minZones: Int,
    )

    private fun rulesFor(s: Strategy, f: Filters): Rules = when (s) {
        Strategy.BALANCE -> Rules(
            f, 2..4, maxOf(f.sumMin ?: 0, 100), minOf(f.sumMax ?: 999, 175),
            if (f.consec == Consec.ANY) Consec.MAX2 else f.consec, f.zoneMax, 0,
        )
        Strategy.ZONE -> Rules(f, null, f.sumMin ?: 0, f.sumMax ?: 999, f.consec, f.zoneMax ?: 2, 4)
        else -> Rules(f, null, f.sumMin ?: 0, f.sumMax ?: 999, f.consec, f.zoneMax, 0)
    }

    private fun makeOne(s: Strategy, fixed: Set<Int>, excluded: Set<Int>, ctx: Ctx): List<Int>? {
        val pool = Nums.ALL.filter { it !in fixed && it !in excluded }
        val need = 6 - fixed.size
        repeat(5000) {
            val nums = (fixed + candidate(s, pool, need, fixed, ctx)).sorted()
            if (nums.size < 6) return null
            if (passes(nums, ctx.rules)) return nums
        }
        return null
    }

    private fun candidate(s: Strategy, pool: List<Int>, need: Int, fixed: Set<Int>, ctx: Ctx): List<Int> {
        val rnd = ctx.rnd
        val w = DoubleArray(46) { 1.0 }
        val last = stats.last
        if (last == null) return weightedPick(pool, w, need, rnd)
        return when (s) {
            Strategy.HOT -> {
                for (n in 1..45) w[n] = 1.0 + ctx.freq[n] * 2
                weightedPick(pool, w, need, rnd)
            }
            Strategy.COLD -> {
                for (n in 1..45) w[n] = 1.0 + stats.gaps[n]
                weightedPick(pool, w, need, rnd)
            }
            Strategy.WARM -> {
                val mid = ctx.hot.subList(12, 33).filter { it in pool }
                weightedPick(if (mid.size >= need) mid else pool, w, need, rnd)
            }
            Strategy.MIX -> {
                val hot = ctx.hot.filter { it in pool }.take(12)
                val cold = ctx.cold.filter { it in pool && it !in hot }.take(12)
                val h = (need + 1) / 2
                weightedPick(hot, w, h, rnd) + weightedPick(cold, w, need - h, rnd)
            }
            Strategy.CARRY -> {
                val ln = last.nums
                val from = ln.filter { it in pool }
                val one = if (from.isNotEmpty() && fixed.none { it in ln }) weightedPick(from, w, 1, rnd) else emptyList()
                one + weightedPick(pool.filter { it !in one && it !in ln }, w, need - one.size, rnd)
            }
            Strategy.MIRROR -> {
                val mir = last.nums.map { 46 - it }.filter { it in pool }
                val a = weightedPick(mir, w, minOf(need, 3 + rnd.nextInt(2)), rnd)
                a + weightedPick(pool.filter { it !in a }, w, need - a.size, rnd)
            }
            Strategy.PAIR -> {
                val m = ctx.pairs
                val chosen = fixed.toMutableList()
                val out = mutableListOf<Int>()
                val p = pool.toMutableList()
                if (chosen.isEmpty()) {
                    val first = weightedPick(ctx.hot.take(10).filter { it in p }, w, 1, rnd)
                    out += first
                    chosen += first
                    p.removeAll(first)
                }
                while (out.size < need && p.isNotEmpty()) {
                    val pw = DoubleArray(46)
                    for (n in p) {
                        val together = chosen.sumOf { c -> m[c][n] }.toDouble()
                        pw[n] = 0.5 + together * together
                    }
                    val n = weightedPick(p, pw, 1, rnd)[0]
                    out += n
                    chosen += n
                    p.remove(n)
                }
                out
            }
            else -> weightedPick(pool, w, need, rnd)
        }
    }

    private fun passes(a: List<Int>, r: Rules): Boolean {
        val f = r.f
        val odd = Nums.oddCount(a)
        val sum = a.sum()
        if (f.odd != null && odd != f.odd) return false
        if (r.oddRange != null && odd !in r.oddRange) return false
        if (sum < r.sumMin || sum > r.sumMax) return false
        when (r.consec) {
            Consec.NONE -> if (Nums.hasConsec(a)) return false
            Consec.MAX2 -> if (Nums.maxRun(a) > 2) return false
            Consec.NEED -> if (!Nums.hasConsec(a)) return false
            Consec.ANY -> {}
        }
        if (f.endMax != null) {
            val e = IntArray(10).also { e -> a.forEach { e[it % 10]++ } }
            if (e.max() > f.endMax) return false
        }
        val z = Nums.zonePattern(a)
        if (r.zoneMax != null && z.max() > r.zoneMax) return false
        if (z.count { it > 0 } < r.minZones) return false
        val last = stats.last
        if (last != null) {
            val c = a.count { it in last.nums }
            when (f.last) {
                LastRule.NONE -> if (c > 0) return false
                LastRule.MAX1 -> if (c > 1) return false
                LastRule.NEED -> if (c < 1) return false
                LastRule.ANY -> {}
            }
        }
        val lo = a.count { it <= 22 }
        when (f.hiLo) {
            HiLo.EVEN -> if (lo != 3) return false
            HiLo.BALANCED -> if (lo !in 2..4) return false
            HiLo.ANY -> {}
        }
        if (f.excludePast && a in stats.pastSet) return false
        if (f.excludePattern) {
            val d = a.zipWithNext { x, y -> y - x }
            if (d.all { it == d[0] }) return false
            if (a.all { it % 5 == 0 } || a.all { it % 7 == 0 }) return false
        }
        return true
    }

    companion object {
        fun weightedPick(pool: List<Int>, w: DoubleArray, k: Int, rnd: Random): List<Int> {
            val p = pool.toMutableList()
            val out = ArrayList<Int>(maxOf(k, 0))
            while (out.size < k && p.isNotEmpty()) {
                var x = rnd.nextDouble() * p.sumOf { w[it] }
                var i = 0
                while (i < p.size - 1) {
                    x -= w[p[i]]
                    if (x <= 0) break
                    i++
                }
                out += p.removeAt(i)
            }
            return out
        }

        /** 같은 글자면 같은 값 (FNV-1a) */
        fun seedOf(s: String): Long {
            var h = -0x340d631b7bdddcdbL
            for (c in s) {
                h = h xor c.code.toLong()
                h *= 0x100000001b3L
            }
            return h
        }
    }
}
