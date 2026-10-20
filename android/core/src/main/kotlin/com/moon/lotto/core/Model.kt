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

enum class Strategy(val label: String, val group: String, val desc: String = "") {
    RANDOM("완전 랜덤", "기본"),
    BALANCE("균형형", "기본", "홀짝 2~4 · 합계 100~175 · 3연번 없음"),
    ZONE("구간 분산형", "기본", "한 구간 최대 2개, 4구간 이상"),
    HOT("핫 번호 위주", "통계", "많이 나온 번호"),
    COLD("콜드 번호 위주", "통계", "오래 안 나온 번호"),
    MIX("핫+콜드 반반", "통계"),
    PAIR("짝꿍 번호", "통계", "같이 자주 나온 번호끼리"),
    CARRY("이월수 포함", "통계", "직전 회차에서 1개"),
    WARM("미지근 번호", "통계", "중간쯤 나온 번호만"),
    SEED("행운 번호", "재미", "이름·생일·문장으로"),
    MIRROR("거울 번호", "재미", "직전 회차 46-n 뒤집기"),
    MANUAL("직접 입력", "");

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
