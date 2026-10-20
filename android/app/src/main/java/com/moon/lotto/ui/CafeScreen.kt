package com.moon.lotto.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.PostKind

@Composable
fun CafeScreen(vm: LottoViewModel) = ScreenColumn {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    fun copyTitle() { clipboard.setText(AnnotatedString(vm.postTitle)); toast("제목을 복사했어요") }
    fun copyBody() { clipboard.setText(AnnotatedString(vm.postBody)); toast("본문을 복사했어요") }

    CafeSettings(vm)

    SectionCard {
        StepTitle(1, "어떤 글을 쓸까요?")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PostKind.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        ChoiceCard(k.emoji, k.label, k.desc, vm.postKind == k, Modifier.weight(1f)) { vm.postKind = k; vm.fillPost() }
                    }
                }
            }
        }
    }

    SectionCard {
        StepTitle(2, "내용 확인하고 고치기")
        Hint("자동으로 써 둔 글이에요. 마음대로 고쳐도 돼요.", Modifier.padding(bottom = 6.dp))
        OutlinedTextField(vm.postTitle, { vm.postTitle = it }, Modifier.fillMaxWidth(), label = { Text("제목") })
        TextButton(onClick = { copyTitle() }, Modifier.align(Alignment.End)) { Text("📋 제목 복사") }
        OutlinedTextField(vm.postBody, { vm.postBody = it }, Modifier.fillMaxWidth(), label = { Text("본문") }, minLines = 8)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = vm::fillPost) { Text("↺ 처음 글로 되돌리기") }
            TextButton(onClick = { copyBody() }) { Text("📋 본문 복사") }
        }
    }

    SectionCard {
        StepTitle(3, "카페에 올리기")
        Tinted("① [카페 열기] → 글쓰기를 눌러요\n② 앱으로 돌아와 [제목 복사] → 제목 칸에 붙여넣기\n③ 다시 와서 [본문 복사] → 본문 칸에 붙여넣기")
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { copyTitle() }, Modifier.weight(1f)) { Text("📋 제목 복사") }
            OutlinedButton(onClick = { copyBody() }, Modifier.weight(1f)) { Text("📋 본문 복사") }
        }
        Button(
            onClick = {
                var url = vm.cafeUrl.trim()
                if (url.isEmpty()) {
                    toast("위에서 카페 주소를 먼저 넣어 주세요")
                    return@Button
                }
                if (!url.startsWith("http")) url = "https://$url"
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    toast("카페를 열 수 없어요. 주소를 확인해 주세요")
                }
            },
            Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("☕ 카페 열기", fontWeight = FontWeight.Bold) }
        OutlinedButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, vm.postTitle)
                    .putExtra(Intent.EXTRA_TEXT, vm.postText)
                context.startActivity(Intent.createChooser(send, "보낼 앱 고르기"))
            },
            Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("다른 앱으로 공유 (카톡·메모 등)") }
    }
}

/** 카페 주소가 있으면 한 줄로 접고, 없으면 바로 입력칸 */
@Composable
private fun CafeSettings(vm: LottoViewModel) {
    var editing by rememberSaveable { mutableStateOf(vm.cafeUrl.isBlank()) }
    if (!editing) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("☕ 내 카페", fontWeight = FontWeight.Bold)
                    Hint(vm.cafeUrl + if (vm.nick.isNotBlank()) " · ${vm.nick}" else "")
                }
                TextButton(onClick = { editing = true }) { Text("바꾸기") }
            }
        }
        return
    }
    SectionCard("☕ 내 카페 등록", explain = "카페 주소를 넣어 두면 [카페 열기]로 바로 갈 수 있어요. 닉네임은 글 끝에 서명으로 붙어요. 한 번만 넣으면 기억해요.") {
        OutlinedTextField(
            vm.cafeUrl, { vm.cafeUrl = it; vm.saveCafe() }, Modifier.fillMaxWidth(),
            label = { Text("카페 주소") }, placeholder = { Text("예: cafe.naver.com/내카페주소") }, singleLine = true,
        )
        OutlinedTextField(
            vm.nick, { vm.nick = it; vm.saveCafe() }, Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("닉네임 (선택)") }, placeholder = { Text("예: 연구소장") }, singleLine = true,
        )
        if (vm.cafeUrl.isNotBlank()) {
            Button(onClick = { editing = false; vm.fillPost() }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("저장") }
        }
    }
}
