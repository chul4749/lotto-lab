package com.moon.lotto.core

import java.util.Locale

fun Int.comma(): String = String.format(Locale.US, "%,d", this)
fun Long.comma(): String = String.format(Locale.US, "%,d", this)

/** 소수 첫째 자리까지, .0 은 뗀다 */
fun Double.short(): String =
    if (this == Math.floor(this)) toLong().comma() else String.format(Locale.US, "%,.1f", this)

fun pct(a: Long, b: Long): Double = if (b == 0L) 0.0 else Math.round(a * 1000.0 / b) / 10.0
fun pct(a: Int, b: Int): Double = pct(a.toLong(), b.toLong())

fun won(n: Long): String = when {
    n >= 100_000_000 -> (Math.round(n / 10_000_000.0) / 10.0).short() + "억 원"
    n >= 10_000 -> Math.round(n / 10_000.0).comma() + "만 원"
    else -> n.comma() + "원"
}

fun rangeLabel(r: Int) = if (r > 0) "최근 ${r}회" else "전체"

fun letter(i: Int) = ('A' + i).toString()
