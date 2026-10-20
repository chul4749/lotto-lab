package com.moon.lotto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    SectionCard("내 연구 번호") {
        Hint("저장한 번호는 이 기기에만 남아요. 추첨이 끝나면 자동으로 등수를 확인해요.")
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                vm.manualNums, { vm.manualNums = it }, Modifier.weight(1f),
                label = { Text("직접 넣기") },
                placeholder = { Text("예: 1 7 13 22 34 45") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedButton(onClick = vm::addManual) { Text("추가") }
        }

        if (vm.saved.isEmpty()) {
            Column(Modifier.padding(top = 12.dp)) { Notice("아직 저장한 번호가 없어요. 뽑기에서 저장하거나 직접 넣어 보세요.") }
        }
        var spent = 0L
        var prize = 0L
        var done = 0
        vm.saved.forEachIndexed { i, p ->
            Line()
            val d = vm.stats.drawOf(p.round)
            val rk = d?.let { Rank.of(p.nums, it) }
            if (d != null && rk != null) {
                done++
                spent += 1000
                prize += Rank.prize(rk, d)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row {
                        Hint("${p.round}회 · ${p.strategy.label} · ")
                        when {
                            rk == null -> Hint("추첨 전")
                            rk > 0 -> Text("${rk}등!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            else -> Hint("낙첨")
                        }
                    }
                    BallRow(p.nums, 28.dp) { n -> d != null && n !in d.nums && n != d.bonus }
                }
                Column {
                    TextButton(onClick = { vm.tab = Tab.SIM; vm.runHist(p.nums) }) { Text("역대") }
                    TextButton(onClick = { vm.deleteSaved(i) }) { Text("삭제") }
                }
            }
        }
        if (done > 0) {
            Column(Modifier.padding(top = 10.dp)) {
                KvGrid(listOf("$done" to "추첨 끝난 게임", won(prize) to "당첨금", "${pct(prize, spent).short()}%" to "회수율"))
            }
        }
        Button(onClick = { vm.openPost(PostKind.CHECK) }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("당첨 확인 글 만들기") }
    }
}
