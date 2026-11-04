package com.moon.lotto.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/** 로또 판매 시간: 토요일 20시에 마감, 20시 35분 추첨, 일요일 6시에 다음 회차 판매 시작 */
object Schedule {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    /** open 이면 until 은 판매 마감 시각, 아니면 판매 다시 시작하는 시각 */
    data class Sale(val open: Boolean, val until: ZonedDateTime)

    fun sale(now: ZonedDateTime): Sale {
        val today = now.withZoneSameInstant(ZONE).toLocalDate()
        val lastClose = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)).atTime(20, 0).atZone(ZONE)
        val reopen = lastClose.plusHours(10)
        if (!now.isBefore(lastClose) && now.isBefore(reopen)) return Sale(false, reopen)
        var close = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).atTime(20, 0).atZone(ZONE)
        if (!now.isBefore(close)) close = close.plusWeeks(1)
        return Sale(true, close)
    }
}

/** 오늘의 추천번호: 같은 날·같은 당첨번호 자료면 누구에게나 같은 균형형 5게임 */
fun Stats.dailyPicks(date: LocalDate): List<List<Int>> =
    Generator(this, Random(Generator.seedOf("daily|$date|$latestNo")))
        .generate(Strategy.BALANCE, 50, 5, emptySet(), emptySet(), Filters()).picks

/** 역대 기록 */
data class Records(
    val draws: Int,
    val totalSales: Long,
    val totalPaid: Long,
    val firstWinners: Long,
    /** 1등 1명당 평균 당첨금 */
    val avgFirst: Long,
    val maxFirst: Draw?,
    val minFirst: Draw?,
    val mostFirst: Draw?,
    /** 1등이 없던 회차 수 */
    val noFirst: Int,
    val maxSales: Draw?,
)

fun Stats.records(): Records {
    val withFirst = draws.filter { it.winners > 0 && it.prize > 0 }
    val firstWinners = withFirst.sumOf { it.winners.toLong() }
    return Records(
        draws = draws.size,
        totalSales = draws.sumOf { it.sales },
        totalPaid = draws.sumOf { d -> (0 until 5).sumOf { i -> (d.prizes.getOrNull(i) ?: 0L) * (d.counts.getOrNull(i) ?: 0L) } },
        firstWinners = firstWinners,
        avgFirst = if (firstWinners == 0L) 0 else withFirst.sumOf { it.prize * it.winners } / firstWinners,
        maxFirst = withFirst.maxByOrNull { it.prize },
        minFirst = withFirst.minByOrNull { it.prize },
        mostFirst = draws.maxByOrNull { it.winners },
        noFirst = draws.count { it.winners == 0 },
        maxSales = draws.maxByOrNull { it.sales },
    )
}
