package com.moon.lotto.core

import kotlin.random.Random

/** 가상 추첨. 번호 6개를 Long 비트(1~45번 비트)로 바꿔 빠르게 맞춰 본다. */
object Sim {
    fun mask(nums: List<Int>): Long = nums.fold(0L) { m, n -> m or (1L shl n) }

    private fun maskOf(a: IntArray): Long {
        var m = 0L
        for (i in 0 until 6) m = m or (1L shl a[i])
        return m
    }

    /** out 에 7개: 앞 6개 본번호, 7번째 보너스 */
    fun randDraw(rnd: Random, pool: IntArray, out: IntArray) {
        for (i in 0 until 45) pool[i] = i + 1
        for (i in 0 until 7) {
            val j = i + rnd.nextInt(45 - i)
            val t = pool[i]; pool[i] = pool[j]; pool[j] = t
            out[i] = pool[i]
        }
    }

    fun rank(ticket: Long, draw: Long, bonus: Int): Int = when (java.lang.Long.bitCount(ticket and draw)) {
        6 -> 1
        5 -> if (ticket and (1L shl bonus) != 0L) 2 else 3
        4 -> 4
        3 -> 5
        else -> 0
    }

    data class Years(val games: Int, val years: Int, val cnt: LongArray, val spent: Long, val prize: Long)

    /** years 년 동안 매주 games 게임씩 랜덤 번호로 샀다면 */
    fun years(games: Int, years: Int, rnd: Random = Random.Default): Years {
        val weeks = years * 52
        val pool = IntArray(45)
        val d = IntArray(7)
        val t = IntArray(7)
        val cnt = LongArray(6)
        repeat(weeks) {
            randDraw(rnd, pool, d)
            val dm = maskOf(d)
            repeat(games) {
                randDraw(rnd, pool, t)
                cnt[rank(maskOf(t), dm, d[6])]++
            }
        }
        val prize = (1..5).sumOf { cnt[it] * Rank.AVG_PRIZE[it] }
        return Years(games, years, cnt, weeks.toLong() * games * 1000, prize)
    }

    /** 1등 나올 때까지 같은 번호로 매주 1게임. step 을 여러 번 불러 조금씩 돌린다. */
    class Until(val nums: List<Int>, private val rnd: Random = Random.Default) {
        private val ticket = mask(nums)
        private val pool = IntArray(45)
        private val d = IntArray(7)
        var weeks = 0L; private set
        val cnt = LongArray(6)
        var won = false; private set

        /** 최대 n 주 돌린다. 1등이 나오면 true */
        fun step(n: Int): Boolean {
            repeat(n) {
                randDraw(rnd, pool, d)
                weeks++
                val rk = rank(ticket, maskOf(d), d[6])
                cnt[rk]++
                if (rk == 1) {
                    won = true
                    return true
                }
            }
            return false
        }

        companion object {
            const val LIMIT = 100_000_000L

            fun randomNums(rnd: Random = Random.Default): List<Int> {
                val t = IntArray(7)
                randDraw(rnd, IntArray(45), t)
                return t.take(6).sorted()
            }
        }
    }
}
