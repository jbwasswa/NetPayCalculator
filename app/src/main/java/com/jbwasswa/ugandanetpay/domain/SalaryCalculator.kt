package com.jbwasswa.ugandanetpay.domain

import kotlin.math.abs
import kotlin.math.max

data class SalaryInput(
    val grossPay: Double,
    val taxableAllowances: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val includeNssf: Boolean = true,
    val nssfEmployeeRate: Double = 0.05,
    val nssfEmployerRate: Double = 0.10,
    val taxYear: TaxYear = TaxYear.Fy2026_27,
    val residency: Residency = Residency.Resident
)

data class SalaryResult(
    val grossPay: Long,
    val taxableIncome: Long,
    val paye: Long,
    val employeeNssf: Long,
    val employerNssf: Long,
    val otherDeductions: Long,
    val netPay: Long,
    val effectiveTaxRate: Double,
    val takeHomeRate: Double,
    val taxYear: TaxYear,
    val residency: Residency
)

data class GrossFromNetResult(
    val targetNetPay: Long,
    val requiredGrossPay: Long,
    val salaryResult: SalaryResult,
    val difference: Long
)

class SalaryCalculator {
    fun calculateGrossToNet(input: SalaryInput): SalaryResult {
        val gross = max(0.0, input.grossPay)
        val taxableIncome = max(0.0, gross + input.taxableAllowances)
        val paye = calculatePaye(
            taxableIncome = taxableIncome,
            rules = PayeRuleBook.rulesFor(input.taxYear, input.residency)
        )
        val employeeNssf = if (input.includeNssf) gross * input.nssfEmployeeRate else 0.0
        val employerNssf = if (input.includeNssf) gross * input.nssfEmployerRate else 0.0
        val deductions = max(0.0, input.otherDeductions)
        val netPay = max(0.0, gross - paye - employeeNssf - deductions)

        return SalaryResult(
            grossPay = gross.toWholeShillings(),
            taxableIncome = taxableIncome.toWholeShillings(),
            paye = paye.toWholeShillings(),
            employeeNssf = employeeNssf.toWholeShillings(),
            employerNssf = employerNssf.toWholeShillings(),
            otherDeductions = deductions.toWholeShillings(),
            netPay = netPay.toWholeShillings(),
            effectiveTaxRate = if (gross > 0.0) paye / gross else 0.0,
            takeHomeRate = if (gross > 0.0) netPay / gross else 0.0,
            taxYear = input.taxYear,
            residency = input.residency
        )
    }

    fun calculateGrossFromNet(
        targetNetPay: Double,
        template: SalaryInput,
        tolerance: Double = 1.0
    ): GrossFromNetResult {
        val target = max(0.0, targetNetPay)
        var low = 0.0
        var high = max(1_000_000.0, target * 2.0)

        while (netForGross(high, template) < target) {
            high *= 2.0
        }

        repeat(80) {
            val mid = (low + high) / 2.0
            val net = netForGross(mid, template)
            if (abs(net - target) <= tolerance) {
                low = mid
                high = mid
                return@repeat
            }
            if (net < target) {
                low = mid
            } else {
                high = mid
            }
        }

        val requiredGross = high
        val result = calculateGrossToNet(template.copy(grossPay = requiredGross))
        return GrossFromNetResult(
            targetNetPay = target.toWholeShillings(),
            requiredGrossPay = requiredGross.toWholeShillings(),
            salaryResult = result,
            difference = result.netPay - target.toWholeShillings()
        )
    }

    private fun netForGross(gross: Double, template: SalaryInput): Double {
        return calculateGrossToNet(template.copy(grossPay = gross)).netPay.toDouble()
    }

    private fun calculatePaye(taxableIncome: Double, rules: PayeRules): Double {
        val band = rules.monthlyBands.firstOrNull { current ->
            val upper = current.upperBoundInclusive
            taxableIncome > current.lowerBoundExclusive && (upper == null || taxableIncome <= upper)
        } ?: rules.monthlyBands.last()

        val standardTax = band.baseTax + ((taxableIncome - band.lowerBoundExclusive) * band.rateOnExcess)
        val extraTax = if (taxableIncome > rules.highIncomeThreshold) {
            (taxableIncome - rules.highIncomeThreshold) * rules.highIncomeExtraRate
        } else {
            0.0
        }
        return max(0.0, standardTax + extraTax)
    }
}

