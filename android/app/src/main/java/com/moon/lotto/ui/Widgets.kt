package com.moon.lotto.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.Stats
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** 공이 하나씩 통통 튀어나온다. key 가 바뀌면 다시 */
@Composable
fun PopBallRow(nums: List<Int>, key: Any, size: Dp = 32.dp, startDelay: Int = 0) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        nums.forEachIndexed { i, n ->
            val scale = remember(key, n) { Animatable(0f) }
            LaunchedEffect(key, n) {
                delay((startDelay + i * 60).toLong())
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
            Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) { Ball(n, size) }
        }
    }
}

/** "이게 뭐예요?" 를 누르면 펼쳐지는 설명 */
@Composable
fun Explain(text: String, label: String = "ⓘ 이게 뭐예요?") {
    var open by rememberSaveable(text) { mutableStateOf(false) }
    Column {
        Text(
            if (open) "ⓘ 설명 접기" else label,
            Modifier.clickable { open = !open }.padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (open) Tinted(text)
    }
}

/** 옅은 강조색 바탕의 안내 글 */
@Composable
fun Tinted(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Text(
        text,
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

/** ① ② 단계 제목 */
@Composable
fun StepTitle(n: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text("$n", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/** 하나 고르는 칩 묶음 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipChoice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) }) }
    }
}

/** 작은 꼬리표 */
@Composable
fun InfoChip(text: String, accent: Boolean = false) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 고를 수 있는 카드 (뽑는 방식, 글 종류) */
@Composable
fun ChoiceCard(emoji: String, title: String, sub: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) c.primaryContainer else c.surfaceVariant)
            .border(BorderStroke(if (selected) 2.dp else 0.dp, if (selected) c.primary else c.surfaceVariant), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 22.sp)
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) c.onPrimaryContainer else c.onSurface)
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant, maxLines = 2)
        }
    }
}

/** 큰 주 버튼 */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = RoundedCornerShape(14.dp)) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** "다음 추첨 1247회 · 10월 24일(토) · 4일 남음" */
@Composable
fun NextDrawBanner(stats: Stats) {
    val date = stats.nextDrawDate ?: return
    val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    val days = ChronoUnit.DAYS.between(today, date)
    val left = when {
        days > 0 -> "${days}일 남음"
        days == 0L -> "오늘 저녁 8시 35분 추첨!"
        else -> "결과 반영 중"
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("다음 추첨 · ${stats.nextNo}회", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("${date.monthValue}월 ${date.dayOfMonth}일(토) 저녁 8시 35분", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        Text(
            left,
            Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        )
    }
}

/** 처음 켰을 때 한 번 보여 주는 안내 */
@Composable
fun GuideCard(vm: LottoViewModel) {
    SectionCard("👋 처음 오셨네요!") {
        Text("① 아래에서 뽑는 방법을 고르고\n② '번호 뽑기'를 누르면 끝이에요.\n③ 마음에 드는 번호는 ☆을 눌러 저장하면, 토요일 추첨 뒤 '내 번호'에서 몇 등인지 알려 줘요.", style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = { vm.showHelp = true }, Modifier.weight(1f)) { Text("도움말 보기") }
            Button(onClick = vm::dismissGuide, Modifier.weight(1f)) { Text("알겠어요") }
        }
    }
}
