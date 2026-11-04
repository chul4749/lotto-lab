package com.moon.lotto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.Sub
import com.moon.lotto.Tab
import com.moon.lotto.core.Draw
import com.moon.lotto.core.Nums
import com.moon.lotto.core.PostKind
import com.moon.lotto.core.Rank
import com.moon.lotto.core.Schedule
import com.moon.lotto.core.afterTax
import com.moon.lotto.core.comma
import com.moon.lotto.core.letter
import com.moon.lotto.core.won
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

@Composable
fun HomeScreen(vm: LottoViewModel) = ScreenColumn {
    Countdown(vm)
    val d = vm.stats.last
    if (d == null) {
        Notice("당첨번호를 불러오는 중이에요. 인터넷 연결을 확인해 주세요.")
        return@ScreenColumn
    }
    SectionCard("${d.no}회 당첨 결과", d.date) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BallRow(d.nums, 34.dp, bonus = d.bonus) }
        PrizeTable(d)
        TextButton(onClick = { vm.drawNo = 0; vm.sub = Sub.DRAW; vm.tab = Tab.ANALYZE }, Modifier.align(Alignment.End)) { Text("지난 회차들 보기 ›") }
    }
    DailyCard(vm)
    MineSummary(vm)
    BigButton("🎲  내 번호 뽑으러 가기") { vm.tab = Tab.GEN }
}

/** 판매 마감까지 일·시·분·초 */
@Composable
private fun Countdown(vm: LottoViewModel) {
    var now by remember { mutableStateOf(ZonedDateTime.now(Schedule.ZONE)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = ZonedDateTime.now(Schedule.ZONE)
        }
    }
    val sale = Schedule.sale(now)
    val left = maxOf(0L, sale.until.toEpochSecond() - now.toEpochSecond())
    val parts = listOf(left / 86400 to "일", left % 86400 / 3600 to "시간", left % 3600 / 60 to "분", left % 60 to "초")
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(c.primary).padding(16.dp),
    ) {
        Text(
            if (sale.open) "${vm.stats.nextNo}회 판매 마감까지" else "판매 쉬는 시간 · 다음 회차 판매 시작까지",
            color = c.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp,
        )
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEach { (v, unit) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.onPrimary.copy(alpha = 0.16f)).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (unit == "일") "$v" else "%02d".format(v), color = c.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                    Text(unit, color = c.onPrimary.copy(alpha = 0.85f), fontSize = 12.sp)
                }
            }
        }
        val u = sale.until
        Text(
            if (sale.open) "${u.monthValue}월 ${u.dayOfMonth}일(토) 저녁 8시 마감 · 8시 35분 추첨" else "${u.monthValue}월 ${u.dayOfMonth}일(일) 오전 6시부터 다시 살 수 있어요",
            color = c.onPrimary.copy(alpha = 0.9f), fontSize = 13.sp,
        )
    }
}

/** 등수별 당첨자 수와 1게임 당첨금(세전·세후) */
@Composable
fun PrizeTable(d: Draw) {
    if (d.counts.size < 5 || d.prizes.size < 5) return
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Hint("등수", Modifier.width(44.dp))
            Hint("당첨자", Modifier.weight(1f))
            Text("1게임 당첨금", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        }
        for (i in 0 until 5) {
            Line()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}등", Modifier.width(44.dp), fontWeight = FontWeight.Bold, color = if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text("${d.counts[i].comma()}명", Modifier.weight(1f), fontSize = 14.sp)
                Column(horizontalAlignment = Alignment.End) {
                    Text("${d.prizes[i].comma()}원", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (afterTax(d.prizes[i]) != d.prizes[i]) Hint("세후 ${afterTax(d.prizes[i]).comma()}원")
                }
            }
        }
        Line()
        val how = if (d.auto + d.semi + d.manual > 0) " · 1등 자동 ${d.auto} · 반자동 ${d.semi} · 수동 ${d.manual}" else ""
        if (d.sales > 0) Hint("총 판매액 ${won(d.sales)}$how")
        Explain("세후는 세금을 뗀 금액이에요. 200만 원 이하는 세금이 없고, 넘으면 22%(3억 원 넘는 부분은 33%)를 떼요.", "ⓘ 세후가 뭐예요?")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyCard(vm: LottoViewModel) {
    if (vm.daily.isEmpty()) return
    val t = vm.today
    SectionCard(
        "🍀 오늘의 추천번호", "${t.monthValue}/${t.dayOfMonth}",
        explain = "매일 바뀌는 균형형 추천 5게임이에요. 같은 날엔 이 앱을 쓰는 누구에게나 같은 번호가 나와서, 카페에서 같이 이야기하기 좋아요. (당첨을 보장하진 않아요)",
    ) {
        vm.daily.forEachIndexed { i, nums ->
            if (i > 0) Line()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(letter(i), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 8.dp)) {
                    BallRow(nums, 30.dp)
                    val odd = Nums.oddCount(nums)
                    FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoChip("합 ${nums.sum()}")
                        InfoChip("홀 $odd · 짝 ${6 - odd}")
                    }
                }
            }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = vm::saveDaily, Modifier.weight(1f)) { Text("★ 내 번호에 저장") }
            Button(onClick = { vm.openPost(PostKind.DAILY) }, Modifier.weight(1f)) { Text("✍️ 카페 글로") }
        }
    }
}

@Composable
private fun MineSummary(vm: LottoViewModel) {
    val s = vm.stats
    val waiting = vm.saved.count { it.round == s.nextNo }
    val last = s.last ?: return
    val lastMine = vm.saved.filter { it.round == last.no }.map { Rank.of(it.nums, last) }
    SectionCard("🎟 내 번호") {
        Text(if (waiting > 0) "${s.nextNo}회 번호 ${waiting}게임이 추첨을 기다리고 있어요." else "${s.nextNo}회 번호를 아직 안 골랐어요.", fontSize = 15.sp)
        if (lastMine.isNotEmpty()) {
            val wins = lastMine.filter { it > 0 }
            Text(
                "지난 ${last.no}회: " + if (wins.isEmpty()) "${lastMine.size}게임 모두 꽝 😢" else wins.groupingBy { it }.eachCount().toSortedMap().entries.joinToString(" · ") { "${it.key}등 ${it.value}개 🎉" },
                Modifier.padding(top = 4.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        TextButton(onClick = { vm.tab = Tab.MINE }, Modifier.align(Alignment.End)) { Text("내 번호 보기 ›") }
    }
}
