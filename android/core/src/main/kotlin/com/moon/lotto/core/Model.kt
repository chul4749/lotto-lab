package com.moon.lotto.core

/** 한 회차 당첨 결과. nums 는 오름차순 6개, prizes 는 1~5등 1게임당 당첨금(모르면 비어 있음). */
data class Draw(
    val no: Int,
    val date: String,
    val nums: List<Int>,
    val bonus: Int,
    val prize: Long = 0,
    val winners: Int = 0,
    val prizes: List<Long> = emptyList(),
)

/**
 * 뽑는 방식. short 는 카드에 쓰는 한 줄, explain 은 골랐을 때 보여 주는 쉬운 설명.
 * featured 는 처음 화면에 바로 보이는 추천 방식.
 */
enum class Strategy(
    val label: String,
    val emoji: String,
    val short: String,
    val explain: String,
    val featured: Boolean = false,
    val usesStats: Boolean = false,
) {
    RANDOM(
        "완전 랜덤", "🎲", "규칙 없이 무작위",
        "아무 규칙 없이 컴퓨터가 무작위로 골라요. 사실 확률로는 이게 가장 정직한 방법이에요.",
        featured = true,
    ),
    BALANCE(
        "균형형", "⚖️", "당첨번호 흔한 모양",
        "홀수·짝수가 한쪽에 몰리지 않고(홀수 2~4개), 6개 합이 흔한 범위(100~175)이고, 3개 연속 번호가 없게 골라요. 실제 당첨번호에서 자주 보이는 모양이에요.",
        featured = true,
    ),
    SEED(
        "행운 번호", "🍀", "이름·생일로 만들기",
        "이름·생일·좋아하는 말을 넣으면 오늘의 번호를 만들어 줘요. 같은 날 같은 말을 넣으면 같은 번호가 나와요.",
        featured = true,
    ),
    HOT(
        "핫 번호", "🔥", "요즘 자주 나온 번호",
        "최근 몇 회 동안 많이 나온 번호일수록 더 잘 뽑히게 해요. '요즘 잘 나오는 번호가 또 나온다'고 믿는 분께.",
        featured = true, usesStats = true,
    ),
    COLD(
        "콜드 번호", "🧊", "오래 쉬고 있는 번호",
        "마지막으로 나온 뒤 오래 안 나온 번호일수록 더 잘 뽑히게 해요. '이제 나올 때가 됐다'고 믿는 분께.",
        usesStats = true,
    ),
    MIX(
        "핫+콜드", "🌗", "반은 핫, 반은 콜드",
        "요즘 자주 나온 번호에서 절반, 오래 쉬고 있는 번호에서 절반을 골라요.",
        usesStats = true,
    ),
    ZONE(
        "구간 분산", "🧩", "1~45를 고르게",
        "1~10, 11~20, 21~30, 31~40, 41~45 다섯 묶음(구간)에 번호가 고르게 흩어지게 골라요. 한 구간에 최대 2개까지만 넣어요.",
    ),
    PAIR(
        "짝꿍 번호", "🤝", "같이 자주 나온 번호끼리",
        "역대 당첨번호에서 같은 회차에 자주 함께 나온 번호끼리 묶어서 골라요.",
        usesStats = true,
    ),
    CARRY(
        "이월수", "🔁", "지난주 번호 1개 넣기",
        "지난주 당첨번호 중 1개를 넣고 나머지는 새로 골라요. 지난주 번호가 이번 주에 또 나오는 일(이월수)이 생각보다 자주 있거든요.",
    ),
    WARM(
        "미지근 번호", "🌤️", "너무 많지도 적지도 않게",
        "너무 많이 나오지도, 너무 적게 나오지도 않은 중간쯤 번호들 중에서 골라요.",
        usesStats = true,
    ),
    MIRROR(
        "거울 번호", "🪞", "지난주 번호 뒤집기",
        "지난주 당첨번호를 거울처럼 뒤집어서(n → 46-n) 섞어요. 예를 들어 3은 43, 10은 36이 돼요.",
    ),
    MANUAL("직접 입력", "✍️", "", "");

    val needsDraws get() = this == CARRY || this == MIRROR

    companion object {
        val choosable = entries.filter { it != MANUAL }
    }
}

/** 내가 고른 번호. round 는 이 번호로 노리는 회차. */
data class Pick(val nums: List<Int>, val round: Int, val strategy: Strategy, val at: Long = 0)

enum class Consec(val label: String) { ANY("상관없음"), NONE("연속 없음"), MAX2("2연속까지"), NEED("2연속 꼭 포함") }
enum class LastRule(val label: String) { ANY("상관없음"), NONE("하나도 없게"), MAX1("1개까지"), NEED("1개 이상 꼭") }
enum class HiLo(val label: String) { ANY("상관없음"), BALANCED("2:4 ~ 4:2"), EVEN("3:3만") }

/** 세부 필터. null·ANY 는 꺼짐. */
data class Filters(
    val odd: Int? = null,
    val consec: Consec = Consec.ANY,
    val sumMin: Int? = null,
    val sumMax: Int? = null,
    val endMax: Int? = null,
    val zoneMax: Int? = null,
    val last: LastRule = LastRule.ANY,
    val hiLo: HiLo = HiLo.ANY,
    val excludePast: Boolean = true,
    val excludePattern: Boolean = true,
) {
    val activeCount
        get() = listOf(
            odd != null, consec != Consec.ANY, sumMin != null, sumMax != null,
            endMax != null, zoneMax != null, last != LastRule.ANY, hiLo != HiLo.ANY,
        ).count { it }
}

object Nums {
    val ALL = (1..45).toList()
    val ZONE_LABEL = listOf("1~10", "11~20", "21~30", "31~40", "41~45")

    fun zoneOf(n: Int) = minOf(4, (n - 1) / 10)
    fun zonePattern(a: List<Int>) = IntArray(5).also { z -> a.forEach { z[zoneOf(it)]++ } }
    fun oddCount(a: List<Int>) = a.count { it % 2 == 1 }

    /** a 는 오름차순 */
    fun hasConsec(a: List<Int>) = a.zipWithNext().any { (x, y) -> y - x == 1 }

    fun maxRun(a: List<Int>): Int {
        var best = 1
        var cur = 1
        for (i in 1 until a.size) {
            cur = if (a[i] - a[i - 1] == 1) cur + 1 else 1
            best = maxOf(best, cur)
        }
        return best
    }

    /** "3 11, 17 …" 에서 1~45 서로 다른 6개를 읽는다. 아니면 null. */
    fun parse(s: String): List<Int>? {
        val a = Regex("\\d+").findAll(s).mapNotNull { it.value.toIntOrNull() }.filter { it in 1..45 }.distinct().toList()
        return if (a.size == 6) a.sorted() else null
    }
}
