package com.moon.lotto.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moon.lotto.Cell

val BallColors = listOf(Color(0xFFF2B600), Color(0xFF3FA8DE), Color(0xFFE8504C), Color(0xFF8A8A8A), Color(0xFF7FB51F))
fun ballColor(n: Int) = BallColors[minOf(4, (n - 1) / 10)]

@Composable
fun Ball(n: Int, size: Dp = 32.dp, dim: Boolean = false) {
    Box(
        Modifier.size(size).alpha(if (dim) 0.3f else 1f).clip(CircleShape).background(ballColor(n)),
        contentAlignment = Alignment.Center,
    ) {
        Text("$n", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
}

@Composable
fun BallRow(nums: List<Int>, size: Dp = 32.dp, bonus: Int? = null, dim: (Int) -> Boolean = { false }) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        nums.forEach { Ball(it, size, dim(it)) }
        if (bonus != null) {
            Text("+", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Ball(bonus, size)
        }
    }
}

/** 탭 화면 공통: 세로 스크롤 */
@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), content = content)
}

@Composable
fun SectionCard(title: String? = null, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (subtitle != null) Text("  $subtitle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun SubTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** 라벨 + 눌러서 고르는 목록 */
@Composable
fun <T> Select(label: String, options: List<T>, selected: T, text: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(modifier.padding(top = 6.dp)) {
        Hint(label, Modifier.padding(bottom = 4.dp))
        Box {
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            ) {
                Text(text(selected), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("▾")
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
                }
            }
        }
    }
}

data class BarItem(val label: String, val value: Int, val text: String, val color: Color? = null)

@Composable
fun Bars(items: List<BarItem>, labelWidth: Dp = 56.dp) {
    val max = items.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { b ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.label, Modifier.width(labelWidth), style = MaterialTheme.typography.bodySmall)
                Box(Modifier.weight(1f).height(12.dp)) {
                    Box(
                        Modifier.fillMaxWidth(b.value.toFloat() / max).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                            .background(b.color ?: MaterialTheme.colorScheme.primary),
                    )
                }
                Text(b.text, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** 큰 숫자 + 작은 설명 상자, 한 줄에 3개 */
@Composable
fun KvGrid(items: List<Pair<String, String>>) {
    Column {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (v, l) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                        Text(v, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Hint(l)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** 1~45 판: 한 번 누르면 고정, 두 번 누르면 제외 */
@Composable
fun NumberGrid(state: Map<Int, Cell>, onTap: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        (1..45).chunked(9).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { n ->
                    val c = state[n]
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                            .background(if (c == Cell.FIX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onTap(n) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$n",
                            Modifier.alpha(if (c == Cell.EX) 0.35f else 1f),
                            color = if (c == Cell.FIX) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = if (c == Cell.FIX) FontWeight.Bold else FontWeight.Normal,
                            textDecoration = if (c == Cell.EX) TextDecoration.LineThrough else TextDecoration.None,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Line() = HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
fun Notice(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
