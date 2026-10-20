package com.moon.lotto.core

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTest {
    /** 저장소의 draws.json (JSON 라이브러리 없이 필요한 칸만 읽는다) */
    private val draws: List<Draw> by lazy {
        val text = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "draws.json") }.first { it.exists() }.readText()
        Regex("\"no\":(\\d+),\"date\":\"([^\"]+)\",\"nums\":\\[([\\d,]+)],\"bonus\":(\\d+)").findAll(text).map { m ->
            Draw(m.groupValues[1].toInt(), m.groupValues[2], m.groupValues[3].split(',').map { it.toInt() }, m.groupValues[4].toInt())
        }.toList()
    }
    private val stats by lazy { Stats(draws) }

    @Test fun `당첨번호를 빠짐없이 읽는다`() {
        assertTrue(draws.size > 1200)
        assertEquals((1..draws.size).toList(), draws.map { it.no })
        assertTrue(draws.all { it.nums.size == 6 && it.nums == it.nums.sorted() && it.bonus !in it.nums })
    }

    @Test fun `등수 계산`() {
        val d = Draw(1, "", listOf(1, 2, 3, 4, 5, 6), 7)
        assertEquals(1, Rank.of(listOf(1, 2, 3, 4, 5, 6), d))
        assertEquals(2, Rank.of(listOf(1, 2, 3, 4, 5, 7), d))
        assertEquals(3, Rank.of(listOf(1, 2, 3, 4, 5, 8), d))
        assertEquals(4, Rank.of(listOf(1, 2, 3, 4, 8, 9), d))
        assertEquals(5, Rank.of(listOf(1, 2, 3, 7, 8, 9), d))
        assertEquals(0, Rank.of(listOf(1, 2, 7, 8, 9, 10), d))
        assertEquals(5000, Rank.prize(5, d))
    }

    @Test fun `역대 1등 번호는 역대 성적에서 1등이 나온다`() {
        val d = draws[500]
        val h = stats.history(d.nums)
        assertTrue(h.cnt[1] >= 1)
        assertEquals(1, h.hits.first().first)
        assertEquals(draws.size * 1000L, h.spent)
        assertEquals(1, stats.bestEver(d.nums)?.first)
    }

    @Test fun `모든 뽑는 방식이 규칙에 맞는 6개를 낸다`() {
        val g = Generator(stats, Random(1))
        for (s in Strategy.choosable) {
            val r = g.generate(s, 50, 10, emptySet(), emptySet(), Filters(), seedText = "홍길동", today = "2026-10-20")
            assertNull(r.error, "$s")
            assertEquals(10, r.picks.size, "$s")
            for (p in r.picks) {
                assertEquals(6, p.toSet().size, "$s $p")
                assertTrue(p.all { it in 1..45 } && p == p.sorted(), "$s $p")
                assertFalse(p in stats.pastSet, "$s 역대 1등 조합 $p")
            }
            assertEquals(10, r.picks.toSet().size, "$s 중복")
        }
    }

    @Test fun `균형형과 구간 분산형 규칙`() {
        val g = Generator(stats, Random(2))
        g.generate(Strategy.BALANCE, 50, 10, emptySet(), emptySet(), Filters()).picks.forEach {
            assertTrue(Nums.oddCount(it) in 2..4 && it.sum() in 100..175 && Nums.maxRun(it) <= 2, "$it")
        }
        g.generate(Strategy.ZONE, 50, 10, emptySet(), emptySet(), Filters()).picks.forEach {
            val z = Nums.zonePattern(it)
            assertTrue(z.max() <= 2 && z.count { c -> c > 0 } >= 4, "$it")
        }
    }

    @Test fun `세부 필터와 고정·제외수를 지킨다`() {
        val f = Filters(odd = 3, consec = Consec.NONE, sumMin = 120, sumMax = 150, endMax = 1, last = LastRule.NONE, hiLo = HiLo.EVEN)
        val r = Generator(stats, Random(3)).generate(Strategy.HOT, 50, 10, setOf(7), setOf(9, 10, 11), f)
        assertNull(r.error)
        val last = stats.last!!.nums
        for (p in r.picks) {
            assertTrue(7 in p && p.none { it in setOf(9, 10, 11) }, "$p")
            assertEquals(3, Nums.oddCount(p))
            assertFalse(Nums.hasConsec(p))
            assertTrue(p.sum() in 120..150)
            assertEquals(6, p.map { it % 10 }.toSet().size)
            assertTrue(p.none { it in last } || 7 in last)
            assertEquals(3, p.count { it <= 22 })
        }
    }

    @Test fun `이월수는 직전 회차 번호를 정확히 1개 넣는다`() {
        val last = stats.last!!.nums
        Generator(stats, Random(4)).generate(Strategy.CARRY, 50, 10, emptySet(), emptySet(), Filters()).picks.forEach {
            assertEquals(1, it.count { n -> n in last }, "$it")
        }
    }

    @Test fun `행운 번호는 같은 말·같은 날이면 같다`() {
        fun gen(text: String, day: String) = Generator(stats).generate(Strategy.SEED, 50, 3, emptySet(), emptySet(), Filters(), text, day).picks
        assertEquals(gen("홍길동 0315", "2026-10-20"), gen("홍길동 0315", "2026-10-20"))
        assertTrue(gen("홍길동 0315", "2026-10-20") != gen("홍길동 0315", "2026-10-21"))
        assertEquals("이름이나 생일을 넣어 주세요", Generator(stats).generate(Strategy.SEED, 50, 1, emptySet(), emptySet(), Filters()).error)
    }

    @Test fun `못 맞추는 조건은 알려 준다`() {
        val r = Generator(stats, Random(5)).generate(Strategy.RANDOM, 50, 1, emptySet(), emptySet(), Filters(odd = 6, sumMax = 30))
        assertEquals("조건이 너무 빡빡해요. 필터나 고정·제외수를 줄여 보세요", r.error)
        assertEquals("고정수는 5개까지만 돼요", Generator(stats).generate(Strategy.RANDOM, 50, 1, (1..6).toSet(), emptySet(), Filters()).error)
    }

    @Test fun `통계`() {
        val f = stats.freq(0)
        assertEquals(draws.size * 6, f.sum())
        assertEquals(draws.size, stats.oddEven(0).sum())
        assertEquals(draws.size * 6, stats.zones(0).sum())
        assertEquals(draws.size, stats.sumBins(0).sum())
        assertEquals(f[stats.hotList(0)[0]], f.max())
        assertEquals(0, stats.gaps.min())
        val p = stats.topPairs(0, 3)
        assertTrue(p[0].count >= p[1].count)
        val lens = stats.lens(7, 50)
        assertEquals(f[7], lens.all)
        assertEquals(5, lens.companions.size)
    }

    @Test fun `가상 추첨`() {
        val y = Sim.years(5, 10, Random(6))
        assertEquals(2600L, y.cnt.sum())
        assertEquals(2_600_000L, y.spent)
        val u = Sim.Until(listOf(1, 2, 3, 4, 5, 6), Random(7))
        var guard = 0
        while (!u.step(1_000_000) && guard++ < 200) Unit
        assertTrue(u.won)
        assertEquals(u.weeks, u.cnt.sum())
        assertEquals(2, Sim.rank(Sim.mask(listOf(1, 2, 3, 4, 5, 7)), Sim.mask(listOf(1, 2, 3, 4, 5, 6)), 7))
    }

    @Test fun `글 만들기`() {
        val b = PostBuilder(stats, "연구소장")
        val pick = Pick(listOf(3, 11, 17, 25, 33, 41), stats.nextNo, Strategy.HOT)
        val post = b.pick(listOf(pick), listOf(11), listOf(9), 50)
        assertTrue(post.title.contains("핫 번호 위주"))
        assertTrue(post.body.contains("A)  3, 11, 17, 25, 33, 41") && post.body.contains("고정수: 11") && post.body.endsWith("— 연구소장 (로또 1등 연구소)"))
        assertTrue(b.report(50).body.contains("${stats.latestNo}회"))
        assertTrue(b.pairs(50).body.contains("짝꿍 번호 TOP 10"))
        val last = stats.last!!
        val check = b.check(listOf(Pick(last.nums, last.no, Strategy.MANUAL)))
        assertTrue(check.title.contains("1등 당첨!"))
        assertEquals("18.8억 원", won(1_881_025_430))
        assertEquals("7,838만 원", won(78_376_060))
        assertEquals("5,000원", won(5000))
    }

    @Test fun `번호 읽기`() {
        assertEquals(listOf(3, 11, 17, 25, 33, 41), Nums.parse("41, 3 11/17 25 33"))
        assertNull(Nums.parse("1 2 3 4 5"))
        assertNull(Nums.parse("1 2 3 4 5 46"))
        assertNull(Nums.parse("99999999999999 1 2 3 4 5"))
    }
}
