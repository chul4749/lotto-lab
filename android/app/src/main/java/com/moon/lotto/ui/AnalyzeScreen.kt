package com.moon.lotto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.Sub
import com.moon.lotto.core.Draw
import com.moon.lotto.core.Nums
import com.moon.lotto.core.comma
import com.moon.lotto.core.records
import com.moon.lotto.core.won

/** 분석 탭: 회차 · 통계 · 실험실 */
@Composable
fun AnalyzeScreen(vm: LottoViewModel) {
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = vm.sub.ordinal, containerColor = MaterialTheme.colorScheme.background) {
            Sub.entries.forEach { s ->
                Tab(
                    selected = vm.sub == s,
                    onClick = { vm.sub = s },
                    text = { Text(s.label, fontWeight = if (vm.sub == s) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp) },
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (vm.sub) {
                Sub.DRAW -> DrawScreen(vm)
                Sub.STAT -> StatScreen(vm)
                Sub.LAB -> SimScreen(vm)
            }
        }
    }
}

/** 회차 넘겨보기 + 역대 기록 */
@Composable
private fun DrawScreen(vm: LottoViewModel) = ScreenColumn {
    val s = vm.stats
    if (s.draws.isEmpty()) {
        Notice("당첨번호를 아직 못 받았어요. 인터넷 연결 뒤 다시 열어 주세요.")
        return@ScreenColumn
    }
    val no = if (vm.drawNo in 1..s.latestNo) vm.drawNo else s.latestNo
    val d = s.drawOf(no) ?: return@ScreenColumn
    val prev = s.drawOf(no - 1)

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { vm.drawNo = no - 1 }, enabled = no > 1) { Text("‹ 이전") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${no}회", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Hint(d.date + if (no == s.latestNo) " · 최신" else "")
            }
            OutlinedButton(onClick = { vm.drawNo = no + 1 }, enabled = no < s.latestNo) { Text("다음 ›") }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) { BallRow(d.nums, 34.dp, bonus = d.bonus) }
        val sum = d.nums.sum()
        val odd = Nums.oddCount(d.nums)
        val diff = prev?.let { sum - it.nums.sum() }
        val carried = prev?.let { p -> d.nums.filter { it in p.nums } } ?: emptyList()
        KvGrid(
            listOf(
                "$sum" to (diff?.let { "합계 (지난 회차보다 ${if (it >= 0) "+$it" else "$it"})" } ?: "합계"),
                "$odd:${6 - odd}" to "홀:짝",
                (if (carried.isEmpty()) "없음" else carried.joinToString(", ")) to "지난 회차 번호",
            ),
        )
        GoTo(s.latestNo) { vm.drawNo = it }
    }

    SectionCard("등수별 당첨") { PrizeTable(d) }

    Records(vm)
}

/** 회차 번호 넣고 바로 가기 */
@Composable
private fun GoTo(latest: Int, onGo: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            text, { text = it.filter(Char::isDigit).take(4) }, Modifier.weight(1f),
            placeholder = { Text("회차 번호 (1~$latest)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedButton(onClick = { text.toIntOrNull()?.let { onGo(it.coerceIn(1, latest)) } }) { Text("이동") }
    }
}

@Composable
private fun Records(vm: LottoViewModel) {
    val s = vm.stats
    val r = s.records()
    SectionCard("🏆 역대 기록", "1~${s.latestNo}회", explain = "1회(2002년 12월)부터 지금까지의 기록이에요. 회차가 적힌 줄을 누르면 그 회차로 가요.") {
        val item: @Composable (String, String, Draw?) -> Unit = { label, value, draw -> RecordRow(label, value, draw) { vm.drawNo = it } }
        item("지금까지 팔린 로또", won(r.totalSales), null)
        item("지금까지 나간 당첨금", won(r.totalPaid), null)
        item("1등 당첨자 수", "${r.firstWinners.comma()}명", null)
        item("1등 평균 당첨금", won(r.avgFirst), null)
        r.maxFirst?.let { item("가장 큰 1등 당첨금", won(it.prize), it) }
        r.minFirst?.let { item("가장 작은 1등 당첨금", won(it.prize), it) }
        r.mostFirst?.let { item("1등이 가장 많이 나온 회차", "${it.winners}명", it) }
        r.maxSales?.let { item("가장 많이 팔린 회차", won(it.sales), it) }
        item("1등이 없던 회차", "${r.noFirst}번", null)
    }
}

@Composable
private fun RecordRow(label: String, value: String, draw: Draw?, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().then(if (draw != null) Modifier.clickable { onPick(draw.no) } else Modifier).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        Column(horizontalAlignment = Alignment.End) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (draw != null) Hint("${draw.no}회 (${draw.date}) ›")
        }
    }
}
