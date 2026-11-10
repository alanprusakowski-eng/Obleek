package com.vbrazhnik.obleek.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vbrazhnik.obleek.R
import com.vbrazhnik.obleek.data.CurrencyConverter
import com.vbrazhnik.obleek.ui.ObleekUiState
import com.vbrazhnik.obleek.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

data class CategorySpendSlice(
    val category: String,
    val amountInMain: Double,
    val share: Float,
    val color: Color
)

private val chartPalette = listOf(
    Color(0xFF38BDF8),
    Color(0xFFA3E635),
    Color(0xFFF59E0B),
    Color(0xFFA855F7),
    Color(0xFFF43F5E),
    Color(0xFF14B8A6),
    Color(0xFF6366F1)
)

@Composable
fun AnalyticsScreen(
    uiState: ObleekUiState,
    modifier: Modifier = Modifier
) {
    val expenses = uiState.transactions.filter { it.type == "EXPENSE" }
    val totalExpenseMain = uiState.monthlyExpensesInMain.coerceAtLeast(0.01)

    val categorySlices = expenses
        .groupBy { it.category }
        .mapValues { (_, list) ->
            list.sumOf { CurrencyConverter.convert(it.amount, it.currency, uiState.mainCurrency) }
        }
        .entries
        .sortedByDescending { it.value }
        .mapIndexed { idx, entry ->
            CategorySpendSlice(
                category = entry.key,
                amountInMain = entry.value,
                share = (entry.value / totalExpenseMain).toFloat().coerceIn(0f, 1f),
                color = chartPalette[idx % chartPalette.size]
            )
        }

    val savingsRate = if (uiState.monthlyIncomeInMain > 0.0) {
        ((uiState.monthlyIncomeInMain - uiState.monthlyExpensesInMain) / uiState.monthlyIncomeInMain * 100.0)
            .coerceIn(-100.0, 100.0)
    } else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.analytics_header_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. Donut Chart Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_donut_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 26.dp.toPx()
                            if (categorySlices.isEmpty()) {
                                drawArc(
                                    color = Color(0xFF1E293B),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            } else {
                                var startAngle = -90f
                                categorySlices.forEach { slice ->
                                    val sweep = (slice.share * 360f).coerceAtLeast(4f)
                                    drawArc(
                                        color = slice.color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep - 3f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Wydatki łącznie",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyConverter.formatAmount(
                                    uiState.monthlyExpensesInMain,
                                    uiState.mainCurrency
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.forLanguageTag("pl-PL"), "Stopa oszczędności: %.1f%%", savingsRate),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (savingsRate >= 0) Color(0xFFA3E635) else Color(0xFFF87171)
                            )
                        }
                    }
                }
            }
        }

        // 2. Category Breakdown Bars
        item {
            Text(
                text = "Struktura wydatków wg kategorii (${uiState.mainCurrency})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        itemsIndexed(categorySlices) { _, slice ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_slice_${slice.category}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Text(
                                text = slice.category,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${CurrencyConverter.formatAmount(slice.amountInMain, uiState.mainCurrency)} (${(slice.share * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    LinearProgressIndicator(
                        progress = { slice.share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50)),
                        color = slice.color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 3. Currency Exposure Breakdown
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Alokacja walutowa portfela",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val currencyAllocation = uiState.accounts
            .groupBy { it.currency }
            .mapValues { (_, accs) -> accs.sumOf { it.balance } }
            .entries.toList()

        itemsIndexed(currencyAllocation) { _, entry ->
            val inMain = CurrencyConverter.convert(entry.value, entry.key, uiState.mainCurrency)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Waluta ${entry.key}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Saldo oryginalne: ${CurrencyConverter.formatAmount(entry.value, entry.key)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyConverter.formatAmount(inMain, uiState.mainCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
