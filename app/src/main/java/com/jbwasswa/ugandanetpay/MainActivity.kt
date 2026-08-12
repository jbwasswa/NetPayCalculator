package com.jbwasswa.ugandanetpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jbwasswa.ugandanetpay.domain.GrossFromNetResult
import com.jbwasswa.ugandanetpay.domain.Residency
import com.jbwasswa.ugandanetpay.domain.SalaryCalculator
import com.jbwasswa.ugandanetpay.domain.SalaryInput
import com.jbwasswa.ugandanetpay.domain.SalaryResult
import com.jbwasswa.ugandanetpay.domain.TaxYear
import com.jbwasswa.ugandanetpay.domain.formatUgx

private val Forest = Color(0xFF0F5B45)
private val ForestDark = Color(0xFF0A3E32)
private val Mint = Color(0xFFEAF6F0)
private val Ink = Color(0xFF13251E)
private val Muted = Color(0xFF5D6F67)
private val SoftBlue = Color(0xFFEAF3FB)

private enum class CalculatorMode {
    GrossToNet,
    NetToGross
}

private enum class MoneyFieldTarget {
    Amount,
    Allowances,
    Reimbursements,
    Deductions
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UgandaNetPayTheme {
                NetPayCalculatorScreen()
            }
        }
    }
}

@Composable
private fun UgandaNetPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest,
            secondary = Color(0xFF1F6FA8),
            background = Color(0xFFF5F8F6),
            surface = Color.White,
            onPrimary = Color.White,
            onSurface = Ink
        ),
        content = content
    )
}

@Composable
private fun NetPayCalculatorScreen() {
    val calculator = remember { SalaryCalculator() }
    var mode by rememberSaveable { mutableStateOf(CalculatorMode.GrossToNet.name) }
    var amountText by rememberSaveable { mutableStateOf("2000000") }
    var allowancesText by rememberSaveable { mutableStateOf("0") }
    var reimbursementsText by rememberSaveable { mutableStateOf("0") }
    var deductionsText by rememberSaveable { mutableStateOf("0") }
    var includeNssf by rememberSaveable { mutableStateOf(true) }
    var taxYear by rememberSaveable { mutableStateOf(TaxYear.Fy2026_27.name) }
    var residency by rememberSaveable { mutableStateOf(Residency.Resident.name) }
    var showBreakdown by rememberSaveable { mutableStateOf(false) }
    var activeMoneyField by rememberSaveable { mutableStateOf(MoneyFieldTarget.Amount.name) }

    val selectedMode = CalculatorMode.valueOf(mode)
    val selectedTaxYear = TaxYear.valueOf(taxYear)
    val selectedResidency = Residency.valueOf(residency)
    val amount = amountText.moneyValue()
    val allowances = allowancesText.moneyValue()
    val reimbursements = reimbursementsText.moneyValue()
    val deductions = deductionsText.moneyValue()
    val template = SalaryInput(
        grossPay = if (selectedMode == CalculatorMode.GrossToNet) amount else 0.0,
        taxableAllowances = allowances,
        nonTaxableReimbursements = reimbursements,
        otherDeductions = deductions,
        includeNssf = includeNssf,
        taxYear = selectedTaxYear,
        residency = selectedResidency
    )
    val grossToNet = calculator.calculateGrossToNet(template)
    val netToGross = calculator.calculateGrossFromNet(amount, template)
    val result = if (selectedMode == CalculatorMode.GrossToNet) grossToNet else netToGross.salaryResult
    val selectedMoneyField = MoneyFieldTarget.valueOf(activeMoneyField)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Header(selectedTaxYear, selectedResidency)
        if (showBreakdown) {
            BreakdownScreen(result = result, onBack = { showBreakdown = false })
        } else {
            ModeSelector(selectedMode) { mode = it.name }
            IncomeCard(
                mode = selectedMode,
                activeField = selectedMoneyField,
                amountText = amountText,
                allowancesText = allowancesText,
                reimbursementsText = reimbursementsText,
                deductionsText = deductionsText,
                onFieldSelected = { activeMoneyField = it.name }
            )
            PinKeypad(
                activeField = selectedMoneyField,
                onKey = { key ->
                    when (selectedMoneyField) {
                        MoneyFieldTarget.Amount -> amountText = updateMoneyDigits(amountText, key)
                        MoneyFieldTarget.Allowances -> allowancesText = updateMoneyDigits(allowancesText, key)
                        MoneyFieldTarget.Reimbursements -> reimbursementsText = updateMoneyDigits(reimbursementsText, key)
                        MoneyFieldTarget.Deductions -> deductionsText = updateMoneyDigits(deductionsText, key)
                    }
                }
            )
            PayrollSettingsCard(
                includeNssf = includeNssf,
                onIncludeNssfChange = { includeNssf = it },
                taxYear = selectedTaxYear,
                onTaxYearChange = { taxYear = it.name },
                residency = selectedResidency,
                onResidencyChange = { residency = it.name }
            )
            ResultHero(selectedMode, result, netToGross)
            QuickStats(result)
            DetailsButton { showBreakdown = true }
            Text(
                text = "Estimate only. Confirm official payroll treatment with URA, your employer, or a tax adviser.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun Header(taxYear: TaxYear, residency: Residency) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Uganda Net Pay",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = ForestDark
        )
        Text(
            text = "Ugandan PAYE and NSSF salary calculator",
            color = Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ModeSelector(
    selectedMode: CalculatorMode,
    onSelected: (CalculatorMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SegmentButton(
                text = "Gross to Net",
                selected = selectedMode == CalculatorMode.GrossToNet,
                onClick = { onSelected(CalculatorMode.GrossToNet) },
                modifier = Modifier.weight(1f)
            )
            SegmentButton(
                text = "Net to Gross",
                selected = selectedMode == CalculatorMode.NetToGross,
                onClick = { onSelected(CalculatorMode.NetToGross) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = text, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = text, color = Muted, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ResultHero(
    mode: CalculatorMode,
    result: SalaryResult,
    reverseResult: GrossFromNetResult
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Forest),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (mode == CalculatorMode.GrossToNet) "Estimated Net Pay" else "Gross salary required",
                color = Color(0xFFCFEADF),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (mode == CalculatorMode.GrossToNet) {
                    result.netPay.formatUgx()
                } else {
                    reverseResult.requiredGrossPay.formatUgx()
                },
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            )
            Text(
                text = if (mode == CalculatorMode.GrossToNet) {
                    "From ${result.cashEarnings.formatUgx()} cash earnings"
                } else {
                    "To land at ${reverseResult.targetNetPay.formatUgx()} net pay"
                },
                color = Color(0xFFCFEADF),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun QuickStats(result: SalaryResult) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("PAYE", result.paye.formatUgx(), Modifier.weight(1f))
        StatCard("NSSF", result.employeeNssf.formatUgx(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SoftBlue),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IncomeCard(
    mode: CalculatorMode,
    activeField: MoneyFieldTarget,
    amountText: String,
    allowancesText: String,
    reimbursementsText: String,
    deductionsText: String,
    onFieldSelected: (MoneyFieldTarget) -> Unit
) {
    SectionCard {
        MoneyInputTile(
            label = if (mode == CalculatorMode.GrossToNet) "Basic / gross salary" else "Desired take-home pay",
            value = amountText,
            selected = activeField == MoneyFieldTarget.Amount,
            onClick = { onFieldSelected(MoneyFieldTarget.Amount) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoneyInputTile(
                label = "Taxable allowances",
                value = allowancesText,
                selected = activeField == MoneyFieldTarget.Allowances,
                onClick = { onFieldSelected(MoneyFieldTarget.Allowances) },
                modifier = Modifier.weight(1f)
            )
            MoneyInputTile(
                label = "Other deductions",
                value = deductionsText,
                selected = activeField == MoneyFieldTarget.Deductions,
                onClick = { onFieldSelected(MoneyFieldTarget.Deductions) },
                modifier = Modifier.weight(1f)
            )
        }
        MoneyInputTile(
            label = "Non-taxable reimbursements",
            value = reimbursementsText,
            selected = activeField == MoneyFieldTarget.Reimbursements,
            onClick = { onFieldSelected(MoneyFieldTarget.Reimbursements) }
        )
    }
}

@Composable
private fun PayrollSettingsCard(
    includeNssf: Boolean,
    onIncludeNssfChange: (Boolean) -> Unit,
    taxYear: TaxYear,
    onTaxYearChange: (TaxYear) -> Unit,
    residency: Residency,
    onResidencyChange: (Residency) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Deduct employee NSSF", fontWeight = FontWeight.SemiBold, color = Ink)
                Text("5% of taxable/contributable wage", color = Muted, fontSize = 12.sp)
            }
            Switch(checked = includeNssf, onCheckedChange = onIncludeNssfChange)
        }
        ChoiceRow(
            title = "PAYE rules",
            firstText = TaxYear.Fy2026_27.label,
            firstSelected = taxYear == TaxYear.Fy2026_27,
            onFirst = { onTaxYearChange(TaxYear.Fy2026_27) },
            secondText = TaxYear.Fy2025_26.label,
            secondSelected = taxYear == TaxYear.Fy2025_26,
            onSecond = { onTaxYearChange(TaxYear.Fy2025_26) }
        )
        ChoiceRow(
            title = "Tax residency",
            firstText = "Resident",
            firstSelected = residency == Residency.Resident,
            onFirst = { onResidencyChange(Residency.Resident) },
            secondText = "Non-resident",
            secondSelected = residency == Residency.NonResident,
            onSecond = { onResidencyChange(Residency.NonResident) }
        )
    }
}

@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun MoneyInputTile(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Mint else Color(0xFFF8FAF9)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = value.formatInputMoney(),
                color = if (selected) ForestDark else Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun PinKeypad(activeField: MoneyFieldTarget, onKey: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = activeField.keypadLabel(),
                color = Forest,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "Del")
            ).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        KeypadButton(
                            text = key,
                            onClick = { onKey(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = if (text == "Clear") Muted else ForestDark,
            fontWeight = FontWeight.Bold,
            fontSize = if (text.length > 1) 12.sp else 17.sp
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    firstText: String,
    firstSelected: Boolean,
    onFirst: () -> Unit,
    secondText: String,
    secondSelected: Boolean,
    onSecond: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, color = Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionButton(firstText, firstSelected, onFirst, Modifier.weight(1f))
            OptionButton(secondText, secondSelected, onSecond, Modifier.weight(1f))
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = text, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DetailsButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "View calculation details",
            color = Forest,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BreakdownScreen(result: SalaryResult, onBack: () -> Unit) {
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text("Back to calculator", color = Forest, fontWeight = FontWeight.Bold)
    }
    SectionCard {
        BreakdownGroup(
            title = "Income",
            rows = listOf(
                "Basic / gross salary" to result.grossPay.formatUgx(),
                "Taxable allowances" to result.taxableAllowances.formatUgx(),
                "Non-taxable reimbursements" to result.nonTaxableReimbursements.formatUgx(),
                "Cash earnings" to result.cashEarnings.formatUgx()
            )
        )
        BreakdownGroup(
            title = "Tax and deductions",
            rows = listOf(
                "Taxable income" to result.taxableIncome.formatUgx(),
                "NSSF contribution base" to result.nssfContributionBase.formatUgx(),
                "PAYE" to result.paye.formatUgx(),
                "Employee NSSF" to result.employeeNssf.formatUgx(),
                "Other deductions" to result.otherDeductions.formatUgx()
            )
        )
        BreakdownGroup(
            title = "Final",
            rows = listOf(
                "Net pay" to result.netPay.formatUgx(),
                "Employer NSSF" to result.employerNssf.formatUgx(),
                "Effective PAYE rate" to "${(result.effectiveTaxRate * 100).formatPercent()}%",
                "Take-home rate" to "${(result.takeHomeRate * 100).formatPercent()}%"
            ),
            strongLast = false
        )
    }
}

@Composable
private fun BreakdownGroup(
    title: String,
    rows: List<Pair<String, String>>,
    strongLast: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Forest, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        rows.forEachIndexed { index, row ->
            BreakdownRow(
                label = row.first,
                value = row.second,
                strong = strongLast && index == rows.lastIndex
            )
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(
            value,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold,
            color = if (strong) Forest else Ink,
            fontSize = 13.sp
        )
    }
}

private fun String.moneyValue(): Double {
    return replace(",", "").toDoubleOrNull() ?: 0.0
}

private fun String.formatInputMoney(): String {
    val number = digitsOnly().toLongOrNull() ?: 0L
    return number.formatUgx()
}

private fun String.digitsOnly(): String {
    return filter { it.isDigit() }.trimStart('0').ifBlank { "0" }
}

private fun updateMoneyDigits(current: String, key: String): String {
    val digits = current.digitsOnly()
    return when (key) {
        "Clear" -> "0"
        "Del" -> digits.dropLast(1).ifBlank { "0" }
        else -> (digits + key).trimStart('0').ifBlank { "0" }
    }
}

private fun MoneyFieldTarget.keypadLabel(): String {
    return when (this) {
        MoneyFieldTarget.Amount -> "Entering main amount"
        MoneyFieldTarget.Allowances -> "Entering taxable allowances"
        MoneyFieldTarget.Reimbursements -> "Entering non-taxable reimbursements"
        MoneyFieldTarget.Deductions -> "Entering other deductions"
    }
}

private fun Double.formatPercent(): String {
    return String.format("%.1f", this)
}
