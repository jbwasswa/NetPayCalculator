package com.jbwasswa.ugandanetpay.domain

enum class Residency {
    Resident,
    NonResident
}

enum class TaxYear(val label: String) {
    Fy2025_26("FY 2025/26"),
    Fy2026_27("FY 2026/27")
}

data class PayeBand(
    val lowerBoundExclusive: Double,
    val upperBoundInclusive: Double?,
    val baseTax: Double,
    val rateOnExcess: Double
)

data class PayeRules(
    val taxYear: TaxYear,
    val residency: Residency,
    val monthlyBands: List<PayeBand>,
    val highIncomeThreshold: Double = 10_000_000.0,
    val highIncomeExtraRate: Double = 0.10
)

object PayeRuleBook {
    fun rulesFor(taxYear: TaxYear, residency: Residency): PayeRules {
        return when (residency) {
            Residency.Resident -> residentRules(taxYear)
            Residency.NonResident -> nonResidentRules(taxYear)
        }
    }

    private fun residentRules(taxYear: TaxYear): PayeRules {
        val bands = when (taxYear) {
            TaxYear.Fy2026_27 -> listOf(
                PayeBand(0.0, 335_000.0, 0.0, 0.0),
                PayeBand(335_000.0, 410_000.0, 0.0, 0.20),
                PayeBand(410_000.0, 485_000.0, 15_000.0, 0.25),
                PayeBand(485_000.0, null, 33_750.0, 0.30)
            )

            TaxYear.Fy2025_26 -> listOf(
                PayeBand(0.0, 235_000.0, 0.0, 0.0),
                PayeBand(235_000.0, 335_000.0, 0.0, 0.10),
                PayeBand(335_000.0, 410_000.0, 10_000.0, 0.20),
                PayeBand(410_000.0, null, 25_000.0, 0.30)
            )
        }
        return PayeRules(taxYear, Residency.Resident, bands)
    }

    private fun nonResidentRules(taxYear: TaxYear): PayeRules {
        val bands = when (taxYear) {
            TaxYear.Fy2026_27 -> listOf(
                PayeBand(0.0, 335_000.0, 0.0, 0.10),
                PayeBand(335_000.0, 410_000.0, 33_500.0, 0.20),
                PayeBand(410_000.0, 485_000.0, 48_500.0, 0.25),
                PayeBand(485_000.0, null, 67_250.0, 0.30)
            )

            TaxYear.Fy2025_26 -> listOf(
                PayeBand(0.0, 335_000.0, 0.0, 0.10),
                PayeBand(335_000.0, 410_000.0, 33_500.0, 0.20),
                PayeBand(410_000.0, null, 48_500.0, 0.30)
            )
        }
        return PayeRules(taxYear, Residency.NonResident, bands)
    }
}

