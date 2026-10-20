package com.moon.lotto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moon.lotto.LottoViewModel

private val STEPS = listOf(
    "🎲 뽑기" to "뽑는 방법을 고르고 '번호 뽑기'를 누르면 끝이에요. 마음에 드는 번호는 ☆을 눌러 저장해요.",
    "🎟️ 내 번호" to "저장한 번호가 모여요. 토요일 추첨이 끝나면 몇 등인지 자동으로 알려 줘요. 실제로 산 번호를 직접 넣어도 돼요.",
    "📊 통계" to "지난 당첨번호를 살펴봐요. 많이 나온 번호, 오래 안 나온 번호, 자주 같이 나온 번호 등.",
    "🧪 실험실" to "'만약에'를 실험해요. 내 번호를 옛날부터 샀다면? 평생 산다면? 1등까지 몇 년?",
    "✍️ 카페" to "뽑은 번호나 통계를 카페 글로 자동으로 써 줘요. 제목·본문을 따로 복사해서 카페에 붙여넣으면 돼요.",
)

private val WORDS = listOf(
    "핫 번호 🔥" to "최근 몇 회 동안 많이 나온 번호",
    "콜드 번호 🧊" to "마지막으로 나온 뒤 오래 안 나온 번호",
    "짝꿍 번호 🤝" to "같은 회차에 자주 함께 나온 두 번호",
    "이월수 🔁" to "지난주 당첨번호가 이번 주에 또 나온 것",
    "구간 🧩" to "1~10, 11~20, 21~30, 31~40, 41~45 다섯 묶음",
    "끝자리(끝수)" to "일의 자리 숫자. 7, 17, 27은 모두 끝자리 7",
    "합계" to "번호 6개를 더한 값. 당첨번호는 대부분 100~175",
    "보너스 번호" to "6개 말고 하나 더 뽑는 번호. 5개 + 보너스를 맞히면 2등",
    "꼭 넣을 번호 · 뺄 번호" to "고정수·제외수라고도 해요. 뽑을 때 꼭 넣거나 빼는 번호",
    "회수율" to "받은 돈 ÷ 쓴 돈. 100%보다 작으면 손해",
)

private val RANKS = listOf(
    "1등" to "6개 모두",
    "2등" to "5개 + 보너스 번호",
    "3등" to "5개",
    "4등" to "4개 (5만 원)",
    "5등" to "3개 (5천 원)",
)

@Composable
fun HelpScreen(vm: LottoViewModel) = ScreenColumn {
    SectionCard("이렇게 써요") {
        STEPS.forEachIndexed { i, (t, d) ->
            if (i > 0) Line()
            Text(t, fontWeight = FontWeight.Bold)
            Hint(d)
        }
    }
    SectionCard("등수는 이렇게 정해져요") {
        RANKS.forEach { (r, d) -> Term(r, d) }
    }
    SectionCard("용어 풀이") {
        WORDS.forEach { (w, d) -> Term(w, d) }
    }
    SectionCard("꼭 알아 두세요") {
        Text(
            "· 1등 확률은 어떤 번호든 똑같이 8,145,060분의 1이에요.\n" +
                "· 로또는 매주 새로 뽑기 때문에 지난 통계로 확률이 올라가지는 않아요. 이 앱의 모든 연구는 재미로만 즐겨 주세요.\n" +
                "· 로또는 만 19세 이상만 살 수 있어요.\n" +
                "· 복권 때문에 힘들다면 한국도박문제예방치유원 ☎ 1336 (24시간 무료 상담)",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    BigButton("닫기") { vm.showHelp = false }
}

@Composable
private fun Term(word: String, desc: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(word, fontWeight = FontWeight.SemiBold)
        Hint(desc)
    }
}
