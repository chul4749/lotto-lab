package com.moon.lotto.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.Consec
import com.moon.lotto.core.HiLo
import com.moon.lotto.core.LastRule
import com.moon.lotto.core.PostKind
import com.moon.lotto.core.Strategy
import com.moon.lotto.core.letter
import com.moon.lotto.core.rangeLabel

val RANGES = listOf(10, 20, 50, 100, 0)

@Composable
fun GenScreen(vm: LottoViewModel) = ScreenColumn {
    SectionCard("연구 방법") {
        Select("뽑는 방식", Strategy.choosable, vm.strategy, { s -> "${s.group} · ${s.label}" + if (s.desc.isEmpty()) "" else " (${s.desc})" }) { vm.strategy = it }
        if (vm.strategy == Strategy.SEED) {
            OutlinedTextField(
                vm.seedText, { vm.seedText = it },
                Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("이름·생일·좋아하는 말") },
                supportingText = { Text("같은 날 같은 말이면 같은 번호가 나와요") },
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Select("분석 범위", RANGES, vm.range, ::rangeLabel, Modifier.weight(1f)) { vm.range = it }
            Select("게임 수", listOf(1, 2, 3, 4, 5, 10), vm.games, { "${it}게임" }, Modifier.weight(1f)) { vm.games = it }
        }
        FilterPanel(vm)
        Hint("고정수·제외수 (한 번 누르면 고정, 두 번 누르면 제외)", Modifier.padding(top = 12.dp, bottom = 6.dp))
        NumberGrid(vm.grid) { vm.tapCell(it) }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::generate, Modifier.weight(1f)) { Text("번호 뽑기") }
            OutlinedButton(onClick = vm::clearGrid, Modifier.weight(1f)) { Text("고정·제외 지우기") }
        }
    }

    if (vm.picks.isNotEmpty()) {
        SectionCard("이번 연구 결과", "${vm.stats.nextNo}회 대비") {
            vm.picks.forEachIndexed { i, row ->
                if (i > 0) Line()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BallRow(row.pick.nums, 30.dp)
                        Hint("${letter(i)} · ${row.info}", Modifier.padding(top = 4.dp))
                    }
                    val done = i in vm.savedRows
                    OutlinedButton(onClick = { vm.saveRow(i) }, enabled = !done, modifier = Modifier.padding(start = 6.dp)) {
                        Text(if (done) "저장됨" else "저장")
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::saveAll, Modifier.weight(1f)) { Text("모두 저장") }
                Button(onClick = { vm.openPost(PostKind.PICK) }, Modifier.weight(1f)) { Text("카페 글로 만들기") }
            }
        }
    }
}

@Composable
private fun FilterPanel(vm: LottoViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    val f = vm.filters
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("세부 필터", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (f.activeCount > 0) Text("${f.activeCount}개 켜짐  ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            Text(if (open) "▴" else "▾")
        }
        if (!open) return@Column
        val opt: (Int?) -> String = { it?.toString() ?: "상관없음" }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Select("홀수 개수", listOf(null) + (0..6).toList(), f.odd, { it?.let { n -> "${n}개" } ?: "상관없음" }, Modifier.weight(1f)) { vm.filters = f.copy(odd = it) }
            Select("연속번호", Consec.entries, f.consec, { it.label }, Modifier.weight(1f)) { vm.filters = f.copy(consec = it) }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("합계 최소", f.sumMin, "21", Modifier.weight(1f)) { vm.filters = f.copy(sumMin = it) }
            NumberField("합계 최대", f.sumMax, "255", Modifier.weight(1f)) { vm.filters = f.copy(sumMax = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Select("같은 끝수", listOf(null, 1, 2), f.endMax, { when (it) { 1 -> "모두 다르게"; 2 -> "2개까지"; else -> opt(it) } }, Modifier.weight(1f)) { vm.filters = f.copy(endMax = it) }
            Select("한 구간(1~10…)", listOf(null, 2, 3), f.zoneMax, { it?.let { n -> "최대 ${n}개" } ?: "상관없음" }, Modifier.weight(1f)) { vm.filters = f.copy(zoneMax = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Select("직전 회차 번호", LastRule.entries, f.last, { it.label }, Modifier.weight(1f)) { vm.filters = f.copy(last = it) }
            Select("저(1~22):고(23~45)", HiLo.entries, f.hiLo, { it.label }, Modifier.weight(1f)) { vm.filters = f.copy(hiLo = it) }
        }
        CheckRow("역대 1등 번호와 똑같은 조합은 빼기", f.excludePast) { vm.filters = f.copy(excludePast = it) }
        CheckRow("한 줄 패턴(1,2,3,4,5,6 · 5의 배수만 등) 빼기", f.excludePattern) { vm.filters = f.copy(excludePattern = it) }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun NumberField(label: String, value: Int?, hint: String, modifier: Modifier, onChange: (Int?) -> Unit) {
    OutlinedTextField(
        value?.toString() ?: "",
        { t -> onChange(t.filter(Char::isDigit).take(3).toIntOrNull()) },
        modifier,
        label = { Text(label) },
        placeholder = { Text(hint) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onChange)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
