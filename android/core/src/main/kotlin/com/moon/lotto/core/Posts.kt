package com.moon.lotto.core

data class Post(val title: String, val body: String)

enum class PostKind(val label: String, val emoji: String, val desc: String) {
    PICK("이번 주 연구 번호", "🎱", "뽑기에서 뽑은 번호를 소개해요"),
    REPORT("주간 당첨 리포트", "📢", "지난주 당첨번호와 핫·콜드 번호 정리"),
    PAIRS("짝꿍·구간 분석", "🤝", "자주 같이 나온 번호, 구간·끝수 분석"),
    HIST("내 번호 역대 성적", "🕰", "실험실 '옛날부터 샀다면' 결과"),
    SIM("가상 추첨 결과", "🎰", "실험실 '매주 산다면'·'1등까지' 결과"),
    CHECK("당첨 확인", "🎟", "저장한 번호의 이번 주 결과"),
}

/** 시뮬 탭에서 마지막으로 돌린 결과 */
sealed interface SimSummary {
    data class Years(val r: Sim.Years) : SimSummary
    data class Until(val nums: List<Int>, val weeks: Long, val cnt: LongArray, val won: Boolean) : SimSummary
}

/** 카페에 올릴 글을 만든다. */
class PostBuilder(private val stats: Stats, nick: String) {
    private val sign = if (nick.isBlank()) "" else "\n\n— ${nick.trim()} (로또 1등 연구소)"
    private val footer = "\n\n※ 재미로 하는 연구예요. 로또는 매 회차 독립 추첨입니다."
    private fun line(nums: List<Int>) = nums.joinToString(", ")
    private fun end(body: String) = body + footer + sign

    fun pick(picks: List<Pick>, fixed: Collection<Int>, excluded: Collection<Int>, range: Int): Post {
        val strategy = picks.firstOrNull()?.strategy?.label ?: ""
        val rl = if (range > 0) "최근 ${range}회" else "전체 회차"
        val b = StringBuilder("🎱 ${stats.nextNo}회 연구 번호\n\n방식: ${strategy.ifEmpty { "-" }}")
        if (stats.draws.isNotEmpty() && picks.firstOrNull()?.strategy?.usesStats == true) b.append(" ($rl 분석)")
        b.append('\n')
        if (fixed.isNotEmpty()) b.append("고정수: ${fixed.sorted().joinToString(", ")}\n")
        if (excluded.isNotEmpty()) b.append("제외수: ${excluded.sorted().joinToString(", ")}\n")
        b.append('\n')
        if (picks.isEmpty()) b.append("(뽑기에서 먼저 번호를 뽑아 주세요)")
        else b.append(picks.mapIndexed { i, p -> "${letter(i)})  ${line(p.nums)}\n     ${stats.pickInfo(p.nums)}" }.joinToString("\n"))
        if (stats.draws.isNotEmpty()) {
            b.append("\n\n📊 참고 — $rl 핫 번호: ${stats.hotList(range).take(6).joinToString(", ")}")
            b.append("\n콜드 번호: ${stats.coldList().take(6).joinToString(", ")}")
        }
        b.append("\n\n여러분 번호도 댓글로 남겨 주세요!")
        return Post("[연구번호] ${stats.nextNo}회 ${strategy.ifEmpty { "연구" }} 조합 공유합니다", end(b.toString()))
    }

    fun report(r: Int): Post {
        val d = stats.last ?: return noData()
        val f = stats.freq(r)
        val g = stats.gaps
        val odd = Nums.oddCount(d.nums)
        val prev = stats.draws.getOrNull(stats.draws.size - 2)
        val carried = prev?.let { p -> d.nums.filter { it in p.nums } } ?: emptyList()
        val prize = if (d.prize > 0) "\n1등 ${d.winners}명 · 1인당 ${won(d.prize)}" else ""
        val body = "📢 ${d.no}회 (${d.date}) 당첨번호\n\n${line(d.nums)}  + 보너스 ${d.bonus}$prize\n\n" +
            "· 합계 ${d.nums.sum()} · 홀짝 $odd:${6 - odd} · 구간 ${Nums.zonePattern(d.nums).joinToString("-")}\n" +
            "· 연속번호 ${if (Nums.hasConsec(d.nums)) "있음" else "없음"} · 이월수 ${if (carried.isEmpty()) "없음" else carried.joinToString(", ")}\n\n" +
            "🔥 ${rangeLabel(r)} 핫 번호\n${stats.hotList(r).take(6).joinToString(", ") { "${it}번(${f[it]}회)" }}\n\n" +
            "🧊 오래 안 나온 번호\n${stats.coldList().take(6).joinToString(", ") { "${it}번(${g[it]}회째)" }}\n\n" +
            "다음 ${stats.nextNo}회도 함께 연구해 봐요!"
        return Post("[주간리포트] ${d.no}회 당첨번호와 통계", end(body))
    }

    fun pairs(r: Int): Post {
        if (stats.draws.isEmpty()) return noData()
        val zc = stats.zones(r)
        val ec = stats.ends(r)
        val endTop = (0..9).sortedByDescending { ec[it] }.take(3)
        val list = stats.range(r)
        val body = "🤝 ${rangeLabel(r)} 짝꿍 번호 TOP 10\n" +
            stats.topPairs(r, 10).mapIndexed { i, p -> "${i + 1}. ${p.a} - ${p.b}  (${p.count}번)" }.joinToString("\n") +
            "\n\n📦 구간별 출현\n" + zc.indices.joinToString("\n") { "${Nums.ZONE_LABEL[it]}: ${zc[it]}개" } +
            "\n\n🔢 많이 나온 끝수: ${endTop.joinToString(", ") { "${it}끝(${ec[it]}개)" }}\n" +
            "연속번호 포함 회차: ${pct(list.count { Nums.hasConsec(it.nums) }, list.size).short()}%"
        return Post("[분석] ${rangeLabel(r)} 짝꿍 번호·구간 분석", end(body))
    }

    fun hist(h: History?): Post {
        if (h == null) return Post("[역대성적]", "시뮬 탭에서 \"역대 성적 보기\"를 먼저 눌러 주세요.")
        val body = "🕰 번호 ${line(h.nums)} 를 1회부터 ${stats.latestNo}회까지 매주 샀다면?\n\n" +
            (1..5).joinToString("\n") { "${it}등: ${h.cnt[it]}번" } +
            "\n\n쓴 돈 ${won(h.spent)} · 당첨금 ${won(h.prize)} · 회수율 ${pct(h.prize, h.spent).short()}%" +
            (h.hits.firstOrNull()?.let { "\n\n가장 아까웠던 회차: ${it.second.no}회 (${it.first}등)" } ?: "")
        return Post("[역대성적] ${line(h.nums)} 로 ${stats.draws.size}주 샀다면", end(body))
    }

    fun sim(s: SimSummary?): Post = when (s) {
        null -> Post("[시뮬]", "시뮬 탭에서 먼저 돌려 주세요.")
        is SimSummary.Years -> {
            val r = s.r
            Post(
                "[시뮬] ${r.years}년 동안 매주 ${r.games}게임 샀더니",
                end(
                    "🎲 가상 추첨 ${r.years}년 × 매주 ${r.games}게임\n\n" +
                        (1..5).joinToString("\n") { "${it}등: ${r.cnt[it].comma()}번" } +
                        "\n\n쓴 돈 ${won(r.spent)} · 당첨금 ${won(r.prize)} · 회수율 ${pct(r.prize, r.spent).short()}%\n\n여러분도 돌려보고 결과 공유해 주세요!",
                ),
            )
        }
        is SimSummary.Until -> Post(
            "[시뮬] 1등까지 ${s.weeks.comma()}주${if (s.won) " 걸렸습니다" else " 돌려봤습니다"}",
            end(
                "🎰 번호 ${line(s.nums)} 로 1등 나올 때까지 매주 1게임\n\n" +
                    (if (s.won) "${s.weeks.comma()}주 만에 1등!" else "${s.weeks.comma()}주 동안 1등 못 함") +
                    " (약 ${(s.weeks / 52).comma()}년)\n쓴 돈 ${won(s.weeks * 1000)}\n\n" +
                    "그동안 2등 ${s.cnt[2].comma()} · 3등 ${s.cnt[3].comma()} · 4등 ${s.cnt[4].comma()} · 5등 ${s.cnt[5].comma()}번",
            ),
        )
    }

    fun check(saved: List<Pick>): Post {
        val checked = saved.filter { stats.drawOf(it.round) != null }
        if (checked.isEmpty()) return Post("[당첨확인]", "아직 추첨이 끝난 저장 번호가 없어요.")
        val round = checked.maxOf { it.round }
        val d = stats.drawOf(round)!!
        val mine = checked.filter { it.round == round }
        val best = mine.minOf { Rank.of(it.nums, d).takeIf { r -> r > 0 } ?: 9 }
        val body = "${round}회 당첨번호: ${line(d.nums)} + ${d.bonus}\n\n내 연구 번호\n" +
            mine.joinToString("\n") { p ->
                val rk = Rank.of(p.nums, d)
                val hit = p.nums.filter { it in d.nums }
                "${line(p.nums)}  → ${if (rk > 0) "${rk}등" else "낙첨"} (${hit.size}개 맞음${if (hit.isEmpty()) "" else ": " + hit.joinToString(", ")})"
            } +
            "\n\n${if (best < 9) "🎉 당첨!" else "다음엔 꼭!"} 다들 결과 어떠셨나요?"
        return Post("[당첨확인] ${round}회 ${if (best < 9) "${best}등 당첨!" else "결과 공유"}", end(body))
    }

    private fun noData() = Post("", "당첨번호를 아직 못 받았어요.")
}
