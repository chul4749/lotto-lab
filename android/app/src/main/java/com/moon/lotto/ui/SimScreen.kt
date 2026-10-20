package com.moon.lotto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.comma
import com.moon.lotto.core.letter
import com.moon.lotto.core.pct
import com.moon.lotto.core.short
import com.moon.lotto.core.won

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimScreen(vm: LottoViewModel) = ScreenColumn {
    SectionCard("내 조합을 역대 전 회차에 넣어보기") {
        OutlinedTextField(
            vm.simNums, { vm.simNums = it }, Modifier.fillMaxWidth(),
            label = { Text("번호 6개 (띄어쓰기나 쉼표로)") },
            placeholder = { Text("예: 3 11 17 25 33 41") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        val quick = vm.picks.mapIndexed { i, p -> "뽑기 ${letter(i)}" to p.pick.nums } +
            vm.saved.take(5).mapIndexed { i, p -> "저장 ${i + 1}" to p.nums }
        if (quick.isNotEmpty()) {
            FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quick.forEach { (label, nums) -> AssistChip(onClick = { vm.simNums = nums.joinToString(" ") }, label = { Text(label) }) }
            }
        }
        Button(onClick = { vm.runHist() }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("역대 성적 보기") }
        vm.hist?.let { h ->
            Hint("1회부터 ${vm.stats.latestNo}회까지 ${vm.stats.draws.size}주 동안 매주 이 번호 1게임을 샀다면", Modifier.padding(vertical = 10.dp))
            RankTable((1..5).map { h.cnt[it].toLong() } + h.cnt[0].toLong())
            KvGrid(listOf(won(h.spent) to "쓴 돈", won(h.prize) to "당첨금", "${pct(h.prize, h.spent).short()}%" to "회수율"))
            if (h.hits.isNotEmpty()) {
                SubTitle("4등 이상 맞은 회차")
                h.hits.take(8).forEach { (rk, d) ->
                    Hint("${d.no}회 · ${rk}등", Modifier.padding(top = 6.dp))
                    BallRow(d.nums, 26.dp) { it !in h.nums }
                }
            }
        }
    }

    SectionCard("매주 꾸준히 산다면?") {
        Hint("가상 추첨으로 돌려봐요. 매주 랜덤 번호를 사는 경우예요.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Select("매주 게임 수", listOf(1, 5, 10, 20), vm.simGames, { "${it}게임" }, Modifier.weight(1f)) { vm.simGames = it }
            Select("기간", listOf(1, 10, 30, 50, 100), vm.simYears, { "${it}년" }, Modifier.weight(1f)) { vm.simYears = it }
        }
        Button(onClick = vm::runYears, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("돌려보기") }
        vm.years?.let { y ->
            val games = y.years * 52L * y.games
            Hint("${y.years}년(${(y.years * 52).comma()}주) × 매주 ${y.games}게임 = ${games.comma()}게임", Modifier.padding(vertical = 10.dp))
            RankTable((1..5).map { y.cnt[it] })
            KvGrid(listOf(won(y.spent) to "쓴 돈", won(y.prize) to "당첨금(평균 기준)", "${pct(y.prize, y.spent).short()}%" to "회수율"))
            Hint("돌릴 때마다 결과가 달라요. 1~3등 상금은 평균값으로 계산했어요.")
        }
    }

    SectionCard("1등 나올 때까지 사보기") {
        Hint("위에 넣은 번호(없으면 랜덤)로 매주 1게임씩, 1등이 나올 때까지 가상 추첨을 돌려요.")
        val u = vm.until
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::startUntil, Modifier.weight(1f), enabled = u?.running != true) { Text("시작") }
            OutlinedButton(onClick = vm::stopUntil, Modifier.weight(1f), enabled = u?.running == true) { Text("멈추기") }
        }
        if (u != null) {
            LinearProgressIndicator(
                progress = { if (u.won) 1f else minOf(0.95f, u.weeks / 8_145_060f * 0.5f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            Hint("번호 ${u.nums.joinToString(", ")}", Modifier.padding(bottom = 6.dp))
            KvGrid(listOf(u.weeks.comma() to "주", "${(u.weeks / 52).comma()}년" to "매주 1게임이면", won(u.weeks * 1000) to "쓴 돈"))
            Hint("그동안 2등 ${u.cnt[2].comma()} · 3등 ${u.cnt[3].comma()} · 4등 ${u.cnt[4].comma()} · 5등 ${u.cnt[5].comma()}번")
            if (u.won) {
                Text(
                    "🎉 ${u.weeks.comma()}주 만에 1등! 약 ${(u.weeks / 52).comma()}년 걸렸어요.",
                    Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                )
            } else if (!u.running) Hint("멈췄어요.", Modifier.padding(top = 8.dp))
        }
    }
}

/** 1~5등(+낙첨) 횟수 표 */
@Composable
private fun RankTable(counts: List<Long>) {
    counts.forEachIndexed { i, c ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(if (i < 5) "${i + 1}등" else "낙첨", Modifier.weight(1f))
            Text("${c.comma()}번")
        }
    }
    Line()
}
