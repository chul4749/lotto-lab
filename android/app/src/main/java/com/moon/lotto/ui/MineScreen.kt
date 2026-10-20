package com.moon.lotto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.Tab
import com.moon.lotto.core.PostKind
import com.moon.lotto.core.Rank
import com.moon.lotto.core.pct
import com.moon.lotto.core.short
import com.moon.lotto.core.won

@Composable
fun MineScreen(vm: LottoViewModel) = ScreenColumn {
    NextDrawBanner(vm.stats)

    // 추첨 끝난 번호 합계
    var spent = 0L
    var prize = 0L
    var done = 0
    vm.saved.forEach { p ->
        vm.stats.drawOf(p.round)?.let { d ->
            done++
            spent += 1000
            prize += Rank.prize(Rank.of(p.nums, d), d)
        }
    }
    if (done > 0) {
        SectionCard("지금까지 성적", explain = "저장한 번호 중 추첨이 끝난 것만 계산해요. 실제로 산 게 아니어도 '샀다면'으로 쳐요. 회수율 = 받은 돈 ÷ 쓴 돈.") {
            KvGrid(listOf("${done}게임" to "추첨 끝남", won(prize) to "받았을 돈", "${pct(prize, spent).short()}%" to "회수율"))
        }
    }

    SectionCard("✍️ 번호 직접 넣기", explain = "실제로 산 번호나 생각해 둔 번호를 넣어 두면, 추첨 뒤 몇 등인지 알려 줘요. 다음 회차(${vm.stats.nextNo}회) 번호로 저장돼요.") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                vm.manualNums, { vm.manualNums = it }, Modifier.weight(1f),
                placeholder = { Text("예: 1 7 13 22 34 45") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedButton(onClick = vm::addManual) { Text("저장") }
        }
    }

    if (vm.saved.isEmpty()) {
        SectionCard("🎟 저장한 번호") {
            Notice("아직 저장한 번호가 없어요.\n뽑기에서 마음에 드는 번호의 ☆을 누르면 여기에 모여요.")
            BigButton("🎲 번호 뽑으러 가기", Modifier.padding(top = 10.dp)) { vm.tab = Tab.GEN }
        }
        return@ScreenColumn
    }

    // 회차별로 묶어서 최근 회차부터
    vm.saved.withIndex().groupBy { it.value.round }.toSortedMap(compareByDescending { it }).forEach { (round, items) ->
        val d = vm.stats.drawOf(round)
        val ranks = items.map { d?.let { dd -> Rank.of(it.value.nums, dd) } }
        val title = if (d == null) "${round}회 · 추첨 전" else "${round}회 결과"
        val sub = if (d == null) "${items.size}게임 기다리는 중" else {
            val wins = ranks.filterNotNull().filter { it > 0 }
            if (wins.isEmpty()) "${items.size}게임 모두 꽝" else wins.groupingBy { it }.eachCount().toSortedMap().entries.joinToString(" · ") { "${it.key}등 ${it.value}개" }
        }
        SectionCard(title, sub) {
            if (d != null) {
                Hint("당첨번호", Modifier.padding(bottom = 4.dp))
                BallRow(d.nums, 28.dp, bonus = d.bonus)
                Line()
            }
            items.forEachIndexed { k, (index, p) ->
                if (k > 0) Line()
                val rk = ranks[k]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when {
                                rk == null -> InfoChip("추첨 전")
                                rk > 0 -> InfoChip("🎉 ${rk}등", accent = true)
                                else -> InfoChip("꽝 · ${p.nums.count { it in d!!.nums }}개 맞음")
                            }
                            Hint("  ${p.strategy.emoji} ${p.strategy.label}")
                        }
                        Row(Modifier.padding(top = 4.dp)) {
                            BallRow(p.nums, 28.dp) { n -> d != null && n !in d.nums && n != d.bonus }
                        }
                    }
                    Column {
                        TextButton(onClick = { vm.tab = Tab.SIM; vm.runHist(p.nums) }) { Text("옛날엔?") }
                        TextButton(onClick = { vm.deleteSaved(index) }) { Text("삭제", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            if (d != null) Hint("흐린 공은 못 맞힌 번호예요.", Modifier.padding(top = 6.dp))
        }
    }

    if (done > 0) {
        BigButton("✍️ 당첨 확인 글 쓰기") { vm.openPost(PostKind.CHECK) }
    }
}
