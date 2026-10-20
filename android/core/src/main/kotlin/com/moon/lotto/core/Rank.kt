package com.moon.lotto.core

object Rank {
    /** 당첨금 정보가 없을 때 쓰는 1~5등 평균 당첨금 */
    val AVG_PRIZE = longArrayOf(0, 2_000_000_000, 60_000_000, 1_500_000, 50_000, 5_000)

    /** 1~5등, 낙첨은 0 */
    fun of(nums: List<Int>, d: Draw): Int {
        val hit = nums.count { it in d.nums }
        return when {
            hit == 6 -> 1
            hit == 5 && d.bonus in nums -> 2
            hit == 5 -> 3
            hit == 4 -> 4
            hit == 3 -> 5
            else -> 0
        }
    }

    fun prize(rank: Int, d: Draw?): Long {
        if (rank == 0) return 0
        val p = d?.prizes?.getOrNull(rank - 1) ?: 0
        if (p > 0) return p
        if (rank == 1 && d != null && d.prize > 0) return d.prize
        return AVG_PRIZE[rank]
    }
}

/** 한 조합을 1회부터 매주 1게임씩 샀다면 */
data class History(
    val nums: List<Int>,
    val cnt: IntArray,
    val prize: Long,
    val spent: Long,
    /** 4등 이상, 등수 높은 순 → 최근 회차 순 */
    val hits: List<Pair<Int, Draw>>,
)

fun Stats.history(nums: List<Int>): History {
    val cnt = IntArray(6)
    var prize = 0L
    val hits = ArrayList<Pair<Int, Draw>>()
    for (d in draws) {
        val rk = Rank.of(nums, d)
        cnt[rk]++
        if (rk > 0) {
            prize += Rank.prize(rk, d)
            if (rk <= 4) hits += rk to d
        }
    }
    hits.sortWith(compareBy<Pair<Int, Draw>> { it.first }.thenByDescending { it.second.no })
    return History(nums, cnt, prize, draws.size * 1000L, hits)
}

/** 역대 최고 등수와 그 회차. 3개 이상 맞은 적 없으면 null */
fun Stats.bestEver(nums: List<Int>): Pair<Int, Int>? {
    var best: Pair<Int, Int>? = null
    for (d in draws) {
        val rk = Rank.of(nums, d)
        if (rk > 0 && (best == null || rk < best.first)) {
            best = rk to d.no
            if (rk == 1) break
        }
    }
    return best
}

/** "합 120 · 홀짝 3:3 · 구간 1-2-1-1-1 · 역대 최고 4등(512회)" */
fun Stats.pickInfo(nums: List<Int>): String {
    val odd = Nums.oddCount(nums)
    val s = "합 ${nums.sum()} · 홀짝 $odd:${6 - odd} · 구간 ${Nums.zonePattern(nums).joinToString("-")}"
    if (draws.isEmpty()) return s
    val b = bestEver(nums)
    return s + if (b != null) " · 역대 최고 ${b.first}등(${b.second}회)" else " · 역대 3개 이상 맞은 적 없음"
}
