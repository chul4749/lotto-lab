package com.moon.lotto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
    Tinted("📊 지난 당첨번호를 여러 방향으로 살펴봐요.\n로또는 매주 새로 뽑기 때문에 지난 기록이 다음 결과를 바꾸지는 않아요. 재미로 봐 주세요!")
    Spacer12()
    if (s.draws.isEmpty()) {
        Notice("당첨번호를 아직 못 받았어요. 인터넷 연결 뒤 다시 열어 주세요.")
        return@ScreenColumn
    }

    SectionCard("살펴볼 기간") {
        ChipChoice(RANGES, r, ::rangeLabel) { vm.statRange = it }
        Hint("아래 통계는 모두 이 기간(${s.range(r).size}회) 기준이에요.", Modifier.padding(top = 4.dp))
    }

    SectionCard("최근 당첨번호", explain = "+ 뒤의 번호는 보너스 번호예요. 5개를 맞히고 보너스까지 맞히면 2등이에요.") {
        s.draws.takeLast(5).reversed().forEachIndexed { i, d ->
            if (i > 0) Line()
            Hint("${d.no}회 · ${d.date}" + if (d.prize > 0) " · 1등 ${d.winners}명, 1인당 ${won(d.prize)}" else "")
            Row(Modifier.padding(top = 4.dp)) { BallRow(d.nums, 30.dp, bonus = d.bonus) }
        }
    }

    val sum = s.summary(r)
    SectionCard(
        "한눈에 보기",
        explain = "· 평균 합계: 당첨번호 6개를 더한 값의 평균이에요.\n· 이어진 번호: 12·13처럼 이어진 번호가 들어 있던 회차의 비율이에요.\n· 지난주 번호 또 나옴: 바로 전 회차 번호가 다시 나온(이월수) 회차의 비율이에요.",
    ) {
        KvGrid(listOf("${sum.avgSum}" to "평균 합계", "${sum.consecPct.short()}%" to "이어진 번호", "${sum.carryPct.short()}%" to "지난주 번호 또 나옴"))
    }

    val f = s.freq(r)
    SectionCard("🔥 요즘 자주 나온 번호", "핫 번호", explain = "고른 기간 동안 가장 많이 나온 번호 6개예요. 아래 숫자는 나온 횟수예요.") {
        val hot = s.hotList(r).take(6)
        BallRow(hot)
        Hint(hot.joinToString(" · ") { "${it}번 ${f[it]}회" }, Modifier.padding(top = 6.dp))
    }

    SectionCard("🧊 오래 쉬고 있는 번호", "콜드 번호", explain = "마지막으로 나온 뒤 가장 오래 안 나온 번호 6개예요. 'N회째'는 그동안 지나간 회차 수예요. (기간 선택과 상관없이 전체 기준)") {
        val cold = s.coldList().take(6)
        BallRow(cold)
        Hint(cold.joinToString(" · ") { "${it}번 ${s.gaps[it]}회째" }, Modifier.padding(top = 6.dp))
    }

    SectionCard("🤝 자주 같이 나온 번호", "짝꿍", explain = "같은 회차에 함께 당첨된 횟수가 많은 두 번호예요.") {
        s.topPairs(r, 10).forEachIndexed { i, p ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Hint("${i + 1}", Modifier.padding(end = 10.dp))
                Row(Modifier.weight(1f)) { BallRow(listOf(p.a, p.b), 26.dp) }
                Hint("${p.count}번 같이 나옴")
            }
        }
    }

    SectionCard("🔍 번호 하나 자세히 보기", explain = "궁금한 번호를 고르면 그 번호의 기록을 보여 줘요. '평균 간격'은 보통 몇 회마다 한 번 나오는지예요.") {
        Hint("번호를 눌러 고르세요", Modifier.padding(bottom = 6.dp))
        BallPicker(vm.lensNum) { vm.lensNum = it }
        val l = s.lens(vm.lensNum, r)
        Column(Modifier.padding(top = 10.dp)) {
            Row { Ball(l.n, 40.dp) }
            Spacer12()
            KvGrid(
                listOf(
                    "${l.all}번" to "역대 나온 횟수", "${l.recent}번" to "${rangeLabel(r)} 동안", "${l.gap}회째" to "안 나오는 중",
                    "${l.bonus}번" to "보너스로 나옴", (l.avgGap?.let { "${it.short()}회" } ?: "-") to "평균 간격", "${l.ratePct.short()}%" to "나온 회차 비율",
                ),
            )
            SubTitle("이 번호와 자주 같이 나온 번호")
            BallRow(l.companions.map { it.first }, 26.dp)
            Hint(l.companions.joinToString(" · ") { "${it.first}번 ${it.second}회" }, Modifier.padding(top = 4.dp))
            Hint("마지막으로 나온 날: " + (l.lastSeen?.let { "${it.no}회 (${it.date})" } ?: "없음"), Modifier.padding(top = 6.dp))
        }
    }

    SectionCard("홀수·짝수 비율", explain = "6개 중 홀수:짝수 개수별로 몇 번 있었는지예요. 보통 3:3, 2:4, 4:2가 많아요.") {
        val oe = s.oddEven(r)
        Bars(oe.indices.map { BarItem("홀$it:짝${6 - it}", oe[it], "${oe[it]}회") }, labelWidth = 64.dp)
    }

    SectionCard("번호 묶음(구간)별", explain = "번호를 10개씩 묶어서 몇 개가 나왔는지예요. '기대'는 완전히 고르게 나왔다면 나왔을 개수예요. 41~45는 5개뿐이라 적어요.") {
        val zc = s.zones(r)
        val n = s.range(r).size
        val expect = listOf(10, 10, 10, 10, 5).map { Math.round(it / 45.0 * 6 * n) }
        Bars(zc.indices.map { BarItem(Nums.ZONE_LABEL[it], zc[it], "${zc[it]}개 (기대 ${expect[it]})", BallColors[it]) })
    }

    SectionCard("끝자리별", explain = "일의 자리 숫자별로 몇 개 나왔는지예요. 예를 들어 7, 17, 27, 37은 모두 끝자리 7이에요.") {
        val ec = s.ends(r)
        Bars(ec.indices.map { BarItem("끝자리 $it", ec[it], "${ec[it]}개") }, labelWidth = 64.dp)
    }

    SectionCard("6개 합계", explain = "당첨번호 6개를 더한 값이 어느 범위에 몇 % 있었는지예요.") {
        val bins = s.sumBins(r)
        val n = s.range(r).size
        Bars(Stats.SUM_BINS.indices.map { i -> BarItem("${Stats.SUM_BINS[i].first}~${Stats.SUM_BINS[i].last}", bins[i], "${pct(bins[i], n).short()}%") }, labelWidth = 64.dp)
    }

    SectionCard("번호별 나온 횟수", explain = "1~45번이 각각 몇 번 나왔는지예요(보너스 번호 제외).") {
        ChipChoice(listOf(false, true), vm.statSortByFreq, { if (it) "많이 나온 순" else "번호순" }) { vm.statSortByFreq = it }
        Spacer12()
        val order = if (vm.statSortByFreq) s.hotList(r) else Nums.ALL
        Bars(order.map { BarItem("$it", f[it], "${f[it]}", ballColor(it)) }, labelWidth = 28.dp)
    }
}

@Composable
fun Spacer12() = androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
