package com.moon.lotto.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.moon.lotto.LottoViewModel
import com.moon.lotto.core.PostKind

@Composable
fun CafeScreen(vm: LottoViewModel) = ScreenColumn {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    fun copy() = clipboard.setText(AnnotatedString(vm.postText))

    SectionCard("카페 설정") {
        OutlinedTextField(
            vm.cafeUrl, { vm.cafeUrl = it; vm.saveCafe() }, Modifier.fillMaxWidth(),
            label = { Text("카페 주소") }, placeholder = { Text("예: https://cafe.naver.com/내카페주소") }, singleLine = true,
        )
        OutlinedTextField(
            vm.nick, { vm.nick = it; vm.saveCafe() }, Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("글에 넣을 닉네임 (선택)") }, placeholder = { Text("예: 연구소장") }, singleLine = true,
        )
    }

    SectionCard("글 만들기") {
        Select("글 종류", PostKind.entries, vm.postKind, { it.label }) { vm.postKind = it; vm.fillPost() }
        OutlinedTextField(vm.postTitle, { vm.postTitle = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("제목") })
        OutlinedTextField(
            vm.postBody, { vm.postBody = it }, Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("본문 (자유롭게 고쳐도 돼요)") }, minLines = 10,
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.fillPost() }, Modifier.weight(1f)) { Text("다시 만들기") }
            OutlinedButton(onClick = { copy(); Toast.makeText(context, "복사했어요", Toast.LENGTH_SHORT).show() }, Modifier.weight(1f)) { Text("복사") }
        }
        Button(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, vm.postTitle)
                    .putExtra(Intent.EXTRA_TEXT, vm.postText)
                context.startActivity(Intent.createChooser(send, "카페 앱으로 보내기"))
            },
            Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("공유하기 (네이버 카페 앱 등)") }
        Button(
            onClick = {
                var url = vm.cafeUrl.trim()
                if (url.isEmpty()) {
                    Toast.makeText(context, "카페 주소를 먼저 넣어 주세요", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!url.startsWith("http")) url = "https://$url"
                copy()
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    Toast.makeText(context, "복사했어요. 카페에서 글쓰기 후 붙여넣기!", Toast.LENGTH_LONG).show()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "카페를 열 수 없어요. 주소를 확인해 주세요", Toast.LENGTH_SHORT).show()
                }
            },
            Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("복사하고 카페 열기") }
        Hint("카페가 열리면 글쓰기 → 게시판 고르고 → 본문에 붙여넣기 하면 돼요. 제목은 첫 줄에 있어요.", Modifier.padding(top = 8.dp))
    }
}
