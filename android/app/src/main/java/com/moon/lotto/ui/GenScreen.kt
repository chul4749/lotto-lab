package com.moon.lotto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.Consec
import com.moon.lotto.core.HiLo
import com.moon.lotto.core.LastRule
import com.moon.lotto.core.Nums
import com.moon.lotto.core.PostKind
import com.moon.lotto.core.Strategy
import com.moon.lotto.core.letter
import com.moon.lotto.core.rangeLabel

val RANGES = listOf(10, 20, 50, 100, 0)

@Composable
fun GenScreen(vm: LottoViewModel) = ScreenColumn {
    NextDrawBanner(vm.stats)
    if (vm.showGuide) GuideCard(vm)

    SectionCard {
        StepTitle(1, "뽑는 방법 고르기")
        StrategyPicker(vm)
        StepTitle(2, "몇 게임 뽑을까요?", Modifier.padding(top = 12.dp))
        ChipChoice(listOf(1, 3, 5, 10), vm.games, { "${it}게임" }) { vm.games = it }
        Spacer(Modifier.height(14.dp))
        BigButton("🎲  번호 뽑기", onClick = vm::generate)
    }

    if (vm.picks.isNotEmpty()) ResultCard(vm)

    AdvancedCard(vm)
}

@Composable
private fun StrategyPicker(vm: LottoViewModel) {
    var more by rememberSaveable { mutableStateOf(!vm.strategy.featured) }
    val shown = Strategy.choosable.filter { more || it.featured }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    ChoiceCard(s.emoji, s.label, s.short, vm.strategy == s, Modifier.weight(1f)) { vm.strategy = s }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    TextButton(onClick = { more = !more }) {
        Text(if (more) "추천 방법만 보기 ▴" else "다른 방법 ${Strategy.choosable.count { !it.featured }}개 더 보기 ▾")
    }
    val s = vm.strategy
    Tinted("${s.emoji} ${s.label}\n${s.explain}")
    if (s == Strategy.SEED) {
        OutlinedTextField(
            vm.seedText, { vm.seedText = it },
            Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("이름·생일·좋아하는 말") },
            placeholder = { Text("예: 홍길동 0315") },
            singleLine = true,
        )
    }
    if (s.usesStats) {
        Hint("통계를 볼 기간", Modifier.padding(top = 10.dp, bottom = 2.dp))
        ChipChoice(RANGES, vm.range, ::rangeLabel) { vm.range = it }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(vm: LottoViewModel) {
    SectionCard("🎉 뽑은 번호", "${vm.stats.nextNo}회용 · ${vm.picks.first().pick.strategy.label}") {
        vm.picks.forEachIndexed { i, row ->
            if (i > 0) Line()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Text(letter(i), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    PopBallRow(row.pick.nums, vm.genCount, 31.dp, startDelay = i * 120)
                    val nums = row.pick.nums
                    val odd = Nums.oddCount(nums)
                    FlowRow(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoChip("합 ${nums.sum()}")
                        InfoChip("홀 $odd · 짝 ${6 - odd}")
                        val b = row.best
                        if (b != null) InfoChip("예전에 ${b.first}등 했던 조합(${b.second}회)", accent = b.first <= 4)
                    }
                }
                val done = i in vm.savedRows
                TextButton(onClick = { vm.toggleRow(i) }) {
                    Text(if (done) "★" else "☆", fontSize = 24.sp, color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Hint("☆을 누르면 '내 번호'에 저장돼요. 추첨이 끝나면 몇 등인지 알려 드려요.", Modifier.padding(top = 10.dp))
        Explain("· 합: 6개 번호를 더한 값이에요. 실제 당첨번호는 대부분 100~175 사이예요.\n· 홀·짝: 홀수와 짝수가 몇 개인지예요. 3:3이 가장 흔해요.\n· 예전에 N등: 이 조합을 지난 회차들에 맞춰 봤을 때 가장 잘 나왔던 등수예요.", "ⓘ 아래 꼬리표는 뭐예요?")
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::saveAll, Modifier.weight(1f)) { Text("★ 모두 저장") }
            Button(onClick = { vm.openPost(PostKind.PICK) }, Modifier.weight(1f)) { Text("✍️ 카페 글로") }
        }
    }
}

@Composable
private fun AdvancedCard(vm: LottoViewModel) {
    var open by rememberSaveable { mutableStateOf(false) }
    val on = vm.filters.activeCount + vm.grid.size
    SectionCard {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("⚙️ 고급 설정", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Hint(if (on > 0) "${on}개 조건이 켜져 있어요" else "몰라도 돼요. 번호를 원하는 대로 조절하고 싶을 때만 쓰세요.")
            }
            Text(if (open) "▴" else "▾", fontSize = 18.sp)
        }
        if (!open) return@SectionCard

        SubTitle("꼭 넣을 번호 · 뺄 번호")
        Hint("번호를 한 번 누르면 빨간색 = 꼭 넣기(최대 5개), 두 번 누르면 줄 그어짐 = 빼기, 세 번 누르면 원래대로.", Modifier.padding(bottom = 8.dp))
        NumberGrid(vm.grid) { vm.tapCell(it) }
        if (vm.grid.isNotEmpty()) {
            TextButton(onClick = vm::clearGrid) { Text("넣기·빼기 모두 지우기") }
        }

        SubTitle("번호 모양 조건")
        val f = vm.filters
        FilterRow("홀수 몇 개?", "6개 중 홀수(1, 3, 5…)가 몇 개일지 정해요.") {
            ChipChoice(listOf(null) + (1..5).toList(), f.odd, { it?.let { n -> "${n}개" } ?: "상관없음" }) { vm.filters = f.copy(odd = it) }
        }
        FilterRow("이어지는 번호", "12·13처럼 바로 이어지는 번호를 넣을지 정해요.") {
            ChipChoice(Consec.entries, f.consec, { it.label }) { vm.filters = f.copy(consec = it) }
        }
        FilterRow("6개 합계", "6개를 더한 값의 범위예요. 당첨번호는 대부분 100~175 사이예요.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("최소", f.sumMin, "21", Modifier.weight(1f)) { vm.filters = f.copy(sumMin = it) }
                NumberField("최대", f.sumMax, "255", Modifier.weight(1f)) { vm.filters = f.copy(sumMax = it) }
            }
        }
        FilterRow("끝자리 겹침", "7·17·27처럼 일의 자리가 같은 번호를 몇 개까지 허용할지예요.") {
            ChipChoice(listOf(null, 1, 2), f.endMax, { when (it) { 1 -> "모두 다르게"; 2 -> "2개까지"; else -> "상관없음" } }) { vm.filters = f.copy(endMax = it) }
        }
        FilterRow("한 묶음에 몰림", "1~10, 11~20 … 같은 묶음(구간)에 몇 개까지 넣을지예요.") {
            ChipChoice(listOf(null, 2, 3), f.zoneMax, { it?.let { n -> "최대 ${n}개" } ?: "상관없음" }) { vm.filters = f.copy(zoneMax = it) }
        }
        FilterRow("지난주 번호", "지난주 당첨번호를 넣을지 정해요.") {
            ChipChoice(LastRule.entries, f.last, { it.label }) { vm.filters = f.copy(last = it) }
        }
        FilterRow("작은 수 : 큰 수", "1~22는 작은 수, 23~45는 큰 수예요. 한쪽에 몰리지 않게 할 수 있어요.") {
            ChipChoice(HiLo.entries, f.hiLo, { it.label }) { vm.filters = f.copy(hiLo = it) }
        }
        CheckRow("예전 1등 번호와 똑같은 조합은 빼기", f.excludePast) { vm.filters = f.copy(excludePast = it) }
        CheckRow("1·2·3·4·5·6 같은 한 줄 패턴은 빼기", f.excludePattern) { vm.filters = f.copy(excludePattern = it) }
        if (f.activeCount > 0) TextButton(onClick = vm::resetFilters) { Text("조건 모두 끄기") }
    }
}

@Composable
private fun FilterRow(title: String, desc: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Hint(desc, Modifier.padding(bottom = 4.dp))
        content()
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
