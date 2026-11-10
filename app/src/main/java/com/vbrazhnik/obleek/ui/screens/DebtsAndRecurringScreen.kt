package com.vbrazhnik.obleek.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vbrazhnik.obleek.R
import com.vbrazhnik.obleek.data.CurrencyConverter
import com.vbrazhnik.obleek.data.DebtEntity
import com.vbrazhnik.obleek.data.RecurringPaymentEntity
import com.vbrazhnik.obleek.ui.ObleekUiState
import com.vbrazhnik.obleek.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsAndRecurringScreen(
    uiState: ObleekUiState,
    onOpenAddDebt: () -> Unit,
    onSettleDebt: (DebtEntity) -> Unit,
    onDeleteDebt: (Int) -> Unit,
    onOpenAddRecurring: () -> Unit,
    onPayRecurringNow: (RecurringPaymentEntity) -> Unit,
    onDeleteRecurring: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.debts_tab_title)) },
                icon = { Icon(Icons.Default.People, contentDescription = null) },
                modifier = Modifier.testTag("tab_debts")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.recurring_tab_title)) },
                icon = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
                modifier = Modifier.testTag("tab_recurring")
            )
        }

        if (selectedTab == 0) {
            DebtsListSection(
                uiState = uiState,
                onOpenAddDebt = onOpenAddDebt,
                onSettleDebt = onSettleDebt,
                onDeleteDebt = onDeleteDebt
            )
        } else {
            RecurringPaymentsSection(
                uiState = uiState,
                onOpenAddRecurring = onOpenAddRecurring,
                onPayRecurringNow = onPayRecurringNow,
                onDeleteRecurring = onDeleteRecurring
            )
        }
    }
}

@Composable
private fun DebtsListSection(
    uiState: ObleekUiState,
    onOpenAddDebt: () -> Unit,
    onSettleDebt: (DebtEntity) -> Unit,
    onDeleteDebt: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("debts_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Winni Tobie",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+" + CurrencyConverter.formatAmount(
                                uiState.totalLentInMain,
                                uiState.mainCurrency
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color(0xFFA3E635)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Ty jesteś winien",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-" + CurrencyConverter.formatAmount(
                                uiState.totalBorrowedInMain,
                                uiState.mainCurrency
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color(0xFFF87171)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rejestr długów według osób i walut",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = onOpenAddDebt,
                    modifier = Modifier.testTag("open_add_debt_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dodaj dług")
                }
            }
        }

        items(uiState.debts, key = { it.id }) { debt ->
            DebtCardItem(
                debt = debt,
                onSettle = { onSettleDebt(debt) },
                onDelete = { onDeleteDebt(debt.id) }
            )
        }
    }
}

@Composable
private fun DebtCardItem(
    debt: DebtEntity,
    onSettle: () -> Unit,
    onDelete: () -> Unit
) {
    val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0.0)
    val progress = if (debt.totalAmount > 0.0) {
        (debt.paidAmount / debt.totalAmount).toFloat().coerceIn(0f, 1f)
    } else 1f
    val badgeColor = if (debt.isLentByMe) Color(0xFFA3E635) else Color(0xFFF87171)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_card_${debt.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = debt.personName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = badgeColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (debt.isLentByMe) "Pożyczono komuś" else "Pożyczono od",
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (debt.note.isNotBlank()) {
                        Text(
                            text = debt.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyConverter.formatAmount(remaining, debt.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = if (debt.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else badgeColor
                    )
                    Text(
                        text = "z ${CurrencyConverter.formatAmount(debt.totalAmount, debt.currency)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = badgeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (debt.isSettled) "Rozliczone w całości" else "Termin: za ${debt.dueDays} dni",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_debt_${debt.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Usuń dług",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!debt.isSettled) {
                        Button(
                            onClick = onSettle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("settle_debt_${debt.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settle_debt_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringPaymentsSection(
    uiState: ObleekUiState,
    onOpenAddRecurring: () -> Unit,
    onPayRecurringNow: (RecurringPaymentEntity) -> Unit,
    onDeleteRecurring: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("recurring_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subskrypcje, rachunki i stałe zlecenia",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Przypomnienia o nadchodzących płatnościach i opłacanie 1-klikiem",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenAddRecurring,
                    modifier = Modifier.testTag("open_add_recurring_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dodaj")
                }
            }
        }

        items(uiState.recurringPayments, key = { it.id }) { payment ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_card_${payment.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = payment.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Za ${payment.nextDueInDays} dni",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${payment.category} • Cykl: ${payment.cycle}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = (if (payment.isIncome) "+" else "-") +
                                CurrencyConverter.formatAmount(payment.amount, payment.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = if (payment.isIncome) Color(0xFFA3E635) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { onDeleteRecurring(payment.id) },
                            modifier = Modifier.testTag("delete_recurring_${payment.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Usuń płatność cykliczną",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onPayRecurringNow(payment) },
                            modifier = Modifier.testTag("pay_recurring_now_${payment.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (payment.isIncome) "Zaksięguj" else stringResource(R.string.pay_recurring_now))
                        }
                    }
                }
            }
        }
    }
}
