package com.jbwasswa.ugandanetpay.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

fun Double.toWholeShillings(): Long = roundToLong()

fun Long.formatUgx(): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "UGX ${formatter.format(this)}"
}

