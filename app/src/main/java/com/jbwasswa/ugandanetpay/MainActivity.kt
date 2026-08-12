package com.jbwasswa.ugandanetpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jbwasswa.ugandanetpay.domain.GrossFromNetResult
import com.jbwasswa.ugandanetpay.domain.Residency
import com.jbwasswa.ugandanetpay.domain.SalaryCalculator
import com.jbwasswa.ugandanetpay.domain.SalaryInput
import com.jbwasswa.ugandanetpay.domain.SalaryResult
import com.jbwasswa.ugandanetpay.domain.TaxYear
import com.jbwasswa.ugandanetpay.domain.formatUgx

private enum class CalculatorMode {
    GrossToNet,
    NetToGross
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
            primary = Color(0xFF0F5B45),
            secondary = Color(0xFF1F6FA8),
            background = Color(0xFFF3F7F4),
            surface = Color.White,
            onPrimary = Color.White,
            onSurface = Color(0xFF13251E)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Header()
        ModeSelector(
            selectedMode = selectedMode,
            onSelected = { mode = it.name }
        )
        InputCard(
            mode = selectedMode,
            amountText = amountText,
            onAmountChange = { amountText = it },
            allowancesText = allowancesText,
            onAllowancesChange = { allowancesText = it },
            reimbursementsText = reimbursementsText,
            onReimbursementsChange = { reimbursementsText = it },
            deductionsText = deductionsText,
            onDeductionsChange = { deductionsText = it },
            includeNssf = includeNssf,
            onIncludeNssfChange = { includeNssf = it },
            taxYear = selectedTaxYear,
            onTaxYearChange = { taxYear = it.name },
            residency = selectedResidency,
            onResidencyChange = { residency = it.name }
        )
        ResultHero(
            mode = selectedMode,
            result = result,
            reverseResult = netToGross
        )
        Breakdown(result = result)
        Text(
            text = "Estimate only. Confirm official payroll treatment with URA, your employer, or a tax adviser.",
            color = Color(0xFF61736B),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Uganda Net Pay",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F5B45)
        )
        Text(
            text = "PAYE calculator for gross-to-net and net-to-gross salary planning.",
            color = Color(0xFF52645D),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ModeSelector(
    selectedMode: CalculatorMode,
    onSelected: (CalculatorMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeButton(
            text = "I know Gross",
            selected = selectedMode == CalculatorMode.GrossToNet,
            onClick = { onSelected(CalculatorMode.GrossToNet) },
            modifier = Modifier.weight(1f)
        )
        ModeButton(
            text = "I know Net",
            selected = selectedMode == CalculatorMode.NetToGross,
            onClick = { onSelected(CalculatorMode.NetToGross) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp)) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(8.dp)) {
            Text(text)
        }
    }
}

@Composable
private fun InputCard(
    mode: CalculatorMode,
    amountText: String,
    onAmountChange: (String) -> Unit,
    allowancesText: String,
    onAllowancesChange: (String) -> Unit,
    reimbursementsText: String,
    onReimbursementsChange: (String) -> Unit,
    deductionsText: String,
    onDeductionsChange: (String) -> Unit,
    includeNssf: Boolean,
    onIncludeNssfChange: (Boolean) -> Unit,
    taxYear: TaxYear,
    onTaxYearChange: (TaxYear) -> Unit,
    residency: Residency,
    onResidencyChange: (Residency) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoneyField(
                label = if (mode == CalculatorMode.GrossToNet) "Gross monthly salary" else "Desired monthly net pay",
                value = amountText,
                onValueChange = onAmountChange
            )
            MoneyField("Taxable allowances", allowancesText, onAllowancesChange)
            MoneyField("Non-taxable reimbursements", reimbursementsText, onReimbursementsChange)
            MoneyField("Other deductions", deductionsText, onDeductionsChange)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Deduct employee NSSF", modifier = Modifier.weight(1f))
                Switch(checked = includeNssf, onCheckedChange = onIncludeNssfChange)
            }

            OptionGroup(
                title = "PAYE rules",
                options = TaxYear.entries.toList(),
                selected = taxYear,
                label = { it.label },
                onSelected = onTaxYearChange
            )

            OptionGroup(
                title = "Residency",
                options = Residency.entries.toList(),
                selected = residency,
                label = { if (it == Residency.Resident) "Resident" else "Non-resident" },
                onSelected = onResidencyChange
            )
        }
    }
}

@Composable
private fun MoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { char -> char.isDigit() || char == ',' || char == '.' }) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun <T> OptionGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF31443C))
        options.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = selected == option,
                    onClick = { onSelected(option) }
                )
                Text(label(option))
            }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F5B45)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = if (mode == CalculatorMode.GrossToNet) "Estimated Net Pay" else "Required Gross Pay",
                color = Color(0xFFCFEADF),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (mode == CalculatorMode.GrossToNet) {
                    result.netPay.formatUgx()
                } else {
                    reverseResult.requiredGrossPay.formatUgx()
                },
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${result.taxYear.label} - ${if (result.residency == Residency.Resident) "Resident" else "Non-resident"}",
                color = Color(0xFFCFEADF),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun Breakdown(result: SalaryResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            BreakdownRow("Basic / gross salary", result.grossPay.formatUgx())
            BreakdownRow("Taxable allowances", result.taxableAllowances.formatUgx())
            BreakdownRow("Non-taxable reimbursements", result.nonTaxableReimbursements.formatUgx())
            BreakdownRow("Cash earnings", result.cashEarnings.formatUgx())
            BreakdownRow("Taxable income", result.taxableIncome.formatUgx())
            BreakdownRow("NSSF contribution base", result.nssfContributionBase.formatUgx())
            BreakdownRow("PAYE", result.paye.formatUgx())
            BreakdownRow("Employee NSSF", result.employeeNssf.formatUgx())
            BreakdownRow("Employer NSSF", result.employerNssf.formatUgx())
            BreakdownRow("Other deductions", result.otherDeductions.formatUgx())
            BreakdownRow("Net pay", result.netPay.formatUgx(), strong = true)
            BreakdownRow("Effective tax rate", "${(result.effectiveTaxRate * 100).formatPercent()}%")
            BreakdownRow("Take-home rate", "${(result.takeHomeRate * 100).formatPercent()}%")
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, strong: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF52645D))
        Text(
            value,
            fontWeight = if (strong) FontWeight.Bold else FontWeight.SemiBold,
            color = if (strong) Color(0xFF0F5B45) else Color(0xFF13251E)
        )
    }
}

private fun String.moneyValue(): Double {
    return replace(",", "").toDoubleOrNull() ?: 0.0
}

private fun Double.formatPercent(): String {
    return String.format("%.1f", this)
}
