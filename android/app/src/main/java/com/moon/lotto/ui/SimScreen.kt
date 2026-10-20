package com.moon.lotto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.PostKind
import com.moon.lotto.core.comma
import com.moon.lotto.core.letter
import com.moon.lotto.core.pct
import com.moon.lotto.core.short
import com.moon.lotto.core.won

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimScreen(vm: LottoViewModel) = ScreenColumn {
    Tinted("🧪 '만약에'를 실험해 보는 곳이에요.\n내 번호로 옛날부터 샀다면? 평생 산다면? 1등까지 얼마나 걸릴까?")
    Spacer12()

    SectionCard(
        "🕰 내 번호, 옛날부터 샀다면?",
        explain = "번호 6개를 1회(2002년 12월)부터 지난주까지 매주 1게임씩 샀다고 치고, 몇 등이 몇 번 됐을지 실제 당첨번호로 계산해요.",
    ) {
        OutlinedTextField(
            vm.simNums, { vm.simNums = it }, Modifier.fillMaxWidth(),
            label = { Text("번호 6개 (띄어쓰기나 쉼표로)") },
            placeholder = { Text("예: 3 11 17 25 33 41") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        val quick = vm.picks.mapIndexed { i, p -> "뽑은 ${letter(i)}" to p.pick.nums } +
            vm.saved.take(5).mapIndexed { i, p -> "저장 ${i + 1}" to p.nums }
        if (quick.isNotEmpty()) {
            Hint("눌러서 넣기", Modifier.padding(top = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quick.forEach { (label, nums) -> AssistChip(onClick = { vm.simNums = nums.joinToString(" ") }, label = { Text(label) }) }
            }
        }
        BigButton("계산해 보기", Modifier.padding(top = 10.dp)) { vm.runHist() }
        vm.hist?.let { h ->
            val weeks = vm.stats.draws.size
            Tinted(
                "${weeks.comma()}주 동안 ${won(h.spent)} 쓰고 ${won(h.prize)} 받았어요.\n" +
                    (h.hits.firstOrNull()?.let { "가장 잘 된 건 ${it.second.no}회 ${it.first}등!" } ?: "4등 이상은 한 번도 없었어요."),
                Modifier.padding(vertical = 12.dp), bold = true,
            )
            RankTable((1..5).map { h.cnt[it].toLong() } + h.cnt[0].toLong())
            KvGrid(listOf(won(h.spent) to "쓴 돈", won(h.prize) to "받은 돈", "${pct(h.prize, h.spent).short()}%" to "회수율"))
            Hint("회수율 = 받은 돈 ÷ 쓴 돈. 100%보다 작으면 손해예요.")
            if (h.hits.isNotEmpty()) {
                SubTitle("4등 이상 된 회차 (흐린 공은 못 맞힌 번호)")
                h.hits.take(8).forEach { (rk, d) ->
                    Hint("${d.no}회 · ${rk}등", Modifier.padding(top = 6.dp))
                    BallRow(d.nums, 26.dp) { it !in h.nums }
                }
            }
            OutlinedButton(onClick = { vm.openPost(PostKind.HIST) }, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("✍️ 이 결과로 카페 글 쓰기") }
        }
    }

    SectionCard(
        "📅 매주 꾸준히 산다면?",
        explain = "컴퓨터로 가상 추첨을 돌려서, 매주 정한 게임 수만큼 랜덤 번호를 사면 어떻게 될지 보여 줘요. 1~3등 상금은 평균값으로 계산해요. 누를 때마다 결과가 달라요.",
    ) {
        Hint("매주 몇 게임?")
        ChipChoice(listOf(1, 5, 10, 20), vm.simGames, { "${it}게임 (${(it * 1000).comma()}원)" }) { vm.simGames = it }
        Hint("몇 년 동안?", Modifier.padding(top = 6.dp))
        ChipChoice(listOf(1, 10, 30, 50, 100), vm.simYears, { "${it}년" }) { vm.simYears = it }
        BigButton("돌려 보기", Modifier.padding(top = 10.dp), onClick = vm::runYears)
        vm.years?.let { y ->
            val games = y.years * 52L * y.games
            Tinted(
                "${y.years}년 동안 ${games.comma()}게임 사서 ${won(y.spent)} 쓰고 ${won(y.prize)} 받았어요." +
                    if (y.cnt[1] > 0) "\n🎉 1등이 ${y.cnt[1]}번 나왔어요!" else "\n1등은 한 번도 안 나왔어요.",
                Modifier.padding(vertical = 12.dp), bold = true,
            )
            RankTable((1..5).map { y.cnt[it] })
            KvGrid(listOf(won(y.spent) to "쓴 돈", won(y.prize) to "받은 돈", "${pct(y.prize, y.spent).short()}%" to "회수율"))
            OutlinedButton(onClick = { vm.openPost(PostKind.SIM) }, Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("✍️ 이 결과로 카페 글 쓰기") }
        }
    }

    SectionCard(
        "🎰 1등 나올 때까지 사보기",
        explain = "위에 넣은 번호(없으면 랜덤)로 매주 1게임씩, 1등이 나올 때까지 가상 추첨을 돌려요. 1등 확률은 8,145,060분의 1이라 평균 15만 년쯤 걸려요. 운에 따라 몇백 년일 수도, 40만 년일 수도 있어요.",
    ) {
        val u = vm.until
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton(if (u?.running == true) "돌리는 중…" else "시작", Modifier.weight(1f), enabled = u?.running != true, onClick = vm::startUntil)
            OutlinedButton(onClick = vm::stopUntil, Modifier.weight(1f).padding(top = 4.dp), enabled = u?.running == true) { Text("멈추기") }
        }
        if (u != null) {
            LinearProgressIndicator(
                progress = { if (u.won) 1f else minOf(0.95f, u.weeks / 8_145_060f * 0.5f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            Hint("번호 ${u.nums.joinToString(", ")}", Modifier.padding(bottom = 6.dp))
            when {
                u.won -> Tinted("🎉 ${u.weeks.comma()}주 만에 1등!\n매주 1게임이면 약 ${(u.weeks / 52).comma()}년, ${won(u.weeks * 1000)}이 들었어요.", bold = true)
                !u.running -> Tinted("${u.weeks.comma()}주(약 ${(u.weeks / 52).comma()}년) 동안 1등이 안 나와서 멈췄어요.")
                else -> {}
            }
            Spacer12()
            KvGrid(listOf(u.weeks.comma() to "주", "${(u.weeks / 52).comma()}년" to "매주 1게임이면", won(u.weeks * 1000) to "쓴 돈"))
            Hint("그동안 2등 ${u.cnt[2].comma()}번 · 3등 ${u.cnt[3].comma()}번 · 4등 ${u.cnt[4].comma()}번 · 5등 ${u.cnt[5].comma()}번")
            if (!u.running) {
                OutlinedButton(onClick = { vm.openPost(PostKind.SIM) }, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("✍️ 이 결과로 카페 글 쓰기") }
            }
        }
    }
}

/** 1~5등(+낙첨) 횟수 표 */
@Composable
private fun RankTable(counts: List<Long>) {
    counts.forEachIndexed { i, c ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(if (i < 5) "${i + 1}등" else "꽝", Modifier.weight(1f))
            Text("${c.comma()}번")
        }
    }
    Line()
}
