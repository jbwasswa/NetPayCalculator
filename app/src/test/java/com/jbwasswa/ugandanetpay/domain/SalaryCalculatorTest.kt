package com.jbwasswa.ugandanetpay.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SalaryCalculatorTest {
    private val calculator = SalaryCalculator()

    @Test
    fun residentFy2026ThresholdHasNoPaye() {
        val result = calculator.calculateGrossToNet(
            SalaryInput(grossPay = 335_000.0, includeNssf = false)
        )

        assertEquals(0, result.paye)
        assertEquals(335_000, result.netPay)
    }

    @Test
    fun residentFy2026CalculatesMiddleBand() {
        val result = calculator.calculateGrossToNet(
            SalaryInput(grossPay = 410_000.0, includeNssf = false)
        )

        assertEquals(15_000, result.paye)
        assertEquals(395_000, result.netPay)
    }

    @Test
    fun grossFromNetFindsRequiredGross() {
        val target = 2_000_000.0
        val result = calculator.calculateGrossFromNet(
            targetNetPay = target,
            template = SalaryInput(grossPay = 0.0)
        )

        assertEquals(target, result.salaryResult.netPay.toDouble(), 2.0)
    }
}
