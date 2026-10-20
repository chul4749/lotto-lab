package com.moon.lotto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.Nums
import com.moon.lotto.core.Stats
import com.moon.lotto.core.pct
import com.moon.lotto.core.rangeLabel
import com.moon.lotto.core.short
import com.moon.lotto.core.won

@Composable
fun StatScreen(vm: LottoViewModel) = ScreenColumn {
    val s = vm.stats
    val r = vm.statRange
    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Select("통계 범위", RANGES, r, ::rangeLabel, Modifier.weight(1f)) { vm.statRange = it }
            Select("출현 횟수 정렬", listOf(false, true), vm.statSortByFreq, { if (it) "많이 나온 순" else "번호순" }, Modifier.weight(1f)) { vm.statSortByFreq = it }
        }
    }
    if (s.draws.isEmpty()) {
        Notice("당첨번호를 아직 못 받았어요. 인터넷 연결 뒤 다시 열어 주세요. 번호 뽑기(랜덤·균형형 등)는 지금도 돼요.")
        return@ScreenColumn
    }

    SectionCard("최근 당첨번호") {
        s.draws.takeLast(5).reversed().forEachIndexed { i, d ->
            if (i > 0) Line()
            Hint("${d.no}회 · ${d.date}" + if (d.prize > 0) " · 1등 ${d.winners}명 ${won(d.prize)}" else "")
            Row(Modifier.padding(top = 4.dp)) { BallRow(d.nums, 30.dp, bonus = d.bonus) }
        }
    }

    val sum = s.summary(r)
    SectionCard("한눈에 보기") {
        KvGrid(listOf("${sum.avgSum}" to "평균 합계", "${sum.consecPct.short()}%" to "연속번호 포함", "${sum.carryPct.short()}%" to "이월수 포함"))
        Hint("${rangeLabel(r)}(${sum.n}회) 기준. 이월수 = 직전 회차 번호가 다시 나온 것")
    }

    val f = s.freq(r)
    SectionCard("핫 번호 TOP 6", "많이 나온") {
        val hot = s.hotList(r).take(6)
        BallRow(hot)
        Hint(hot.joinToString(" · ") { "${it}번 ${f[it]}회" }, Modifier.padding(top = 4.dp))
        SubTitle("콜드 번호 TOP 6 · 오래 안 나온")
        val cold = s.coldList().take(6)
        BallRow(cold)
        Hint(cold.joinToString(" · ") { "${it}번 ${s.gaps[it]}회째" }, Modifier.padding(top = 4.dp))
    }

    SectionCard("짝꿍 번호 TOP 10", "같이 자주 나온 두 번호") {
        s.topPairs(r, 10).forEach { p ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) { BallRow(listOf(p.a, p.b), 26.dp) }
                Hint("${p.count}번 같이")
            }
        }
    }

    SectionCard("번호 돋보기") {
        Select("번호", Nums.ALL, vm.lensNum, { "${it}번" }) { vm.lensNum = it }
        val l = s.lens(vm.lensNum, r)
        Column(Modifier.padding(top = 10.dp)) {
            KvGrid(
                listOf(
                    "${l.all}" to "전체 출현", "${l.recent}" to rangeLabel(r), "${l.gap}" to "회째 안 나옴",
                    "${l.bonus}" to "보너스로", (l.avgGap?.short() ?: "-") to "평균 간격", "${l.ratePct.short()}%" to "나온 회차 비율",
                ),
            )
            SubTitle("같이 자주 나온 번호")
            BallRow(l.companions.map { it.first }, 26.dp)
            Hint(l.companions.joinToString(" · ") { "${it.first}번 ${it.second}회" }, Modifier.padding(top = 4.dp))
            Hint("마지막 출현: " + (l.lastSeen?.let { "${it.no}회 (${it.date})" } ?: "없음"), Modifier.padding(top = 4.dp))
        }
    }

    SectionCard("홀짝 비율", "홀:짝 개수") {
        val oe = s.oddEven(r)
        Bars(oe.indices.map { BarItem("$it:${6 - it}", oe[it], "${oe[it]}회") })
    }

    SectionCard("구간별 분포") {
        val zc = s.zones(r)
        val n = s.range(r).size
        val expect = listOf(10, 10, 10, 10, 5).map { Math.round(it / 45.0 * 6 * n) }
        Bars(zc.indices.map { BarItem(Nums.ZONE_LABEL[it], zc[it], "${zc[it]}개 (기대 ${expect[it]})", BallColors[it]) })
    }

    SectionCard("끝수 분포", "일의 자리") {
        val ec = s.ends(r)
        Bars(ec.indices.map { BarItem("끝 $it", ec[it], "${ec[it]}개") })
    }

    SectionCard("합계 분포") {
        val bins = s.sumBins(r)
        val n = s.range(r).size
        Bars(Stats.SUM_BINS.indices.map { i -> BarItem("${Stats.SUM_BINS[i].first}~${Stats.SUM_BINS[i].last}", bins[i], "${pct(bins[i], n).short()}%") }, labelWidth = 64.dp)
    }

    SectionCard("번호별 출현 횟수", "보너스 제외") {
        val order = if (vm.statSortByFreq) s.hotList(r) else Nums.ALL
        Bars(order.map { BarItem("$it", f[it], "${f[it]}", ballColor(it)) }, labelWidth = 28.dp)
    }
}
