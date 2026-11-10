package com.vbrazhnik.obleek.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vbrazhnik.obleek.data.AccountEntity
import com.vbrazhnik.obleek.data.CurrencyConverter

@Composable
fun AddTransactionDialog(
    initialType: String,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        merchant: String,
        amount: Double,
        currency: String,
        type: String,
        category: String,
        accountId: Int,
        targetAccountId: Int?
    ) -> Unit
) {
    BackHandler { onDismiss() }

    var txType by remember { mutableStateOf(initialType) }
    var merchant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedAccountId by remember {
        mutableIntStateOf(accounts.firstOrNull()?.id ?: 1)
    }
    var targetAccountId by remember {
        mutableIntStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1)
    }
    val defaultAccountCurrency = accounts.find { it.id == selectedAccountId }?.currency ?: "PLN"
    var currency by remember(selectedAccountId) { mutableStateOf(defaultAccountCurrency) }

    val availableCategories = when (txType) {
        "INCOME" -> CurrencyConverter.incomeCategories
        "TRANSFER" -> listOf("Przelew własny", "Wymiana walut")
        else -> CurrencyConverter.expenseCategories
    }
    var selectedCategory by remember(txType) { mutableStateOf(availableCategories.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (txType) {
                    "INCOME" -> "Nowy przychód"
                    "TRANSFER" -> "Przelew wielowalutowy"
                    else -> "Nowy wydatek"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Transaction type switcher
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EXPENSE" to "Wydatek", "INCOME" to "Przychód", "TRANSFER" to "Przelew").forEach { (typeKey, label) ->
                        FilterChip(
                            selected = txType == typeKey,
                            onClick = { txType = typeKey },
                            label = { Text(label) },
                            modifier = Modifier.testTag("dialog_tx_type_$typeKey")
                        )
                    }
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = {
                        Text(
                            if (txType == "TRANSFER") "Tytuł przelewu"
                            else "Odbiorca / Sklep (np. Żabka, Orlen)"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_merchant_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Kwota") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tx_amount_input")
                    )

                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase() },
                        label = { Text("Waluta") },
                        singleLine = true,
                        modifier = Modifier
                            .width(96.dp)
                            .testTag("tx_currency_input")
                    )
                }

                // Account picker
                Text(
                    text = if (txType == "TRANSFER") "Z konta:" else "Konto:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = {
                                selectedAccountId = acc.id
                                currency = acc.currency
                            },
                            label = { Text("${acc.name} (${acc.currency})") },
                            modifier = Modifier.testTag("select_source_account_${acc.id}")
                        )
                    }
                }

                if (txType == "TRANSFER" && accounts.size > 1) {
                    Text(
                        text = "Na konto docelowe:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            FilterChip(
                                selected = targetAccountId == acc.id,
                                onClick = { targetAccountId = acc.id },
                                label = { Text("${acc.name} (${acc.currency})") },
                                modifier = Modifier.testTag("select_target_account_${acc.id}")
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Kategoria:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                modifier = Modifier.testTag("select_tx_category_$cat")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (parsedAmount > 0.0) {
                        val cleanMerchant = merchant.ifBlank { selectedCategory }
                        onConfirm(
                            cleanMerchant,
                            cleanMerchant,
                            parsedAmount,
                            currency.ifBlank { "PLN" },
                            txType,
                            selectedCategory,
                            selectedAccountId,
                            if (txType == "TRANSFER") targetAccountId else null
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_transaction_button")
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_transaction_button")
            ) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        accountType: String,
        currency: String,
        balance: Double,
        cardLastFour: String
    ) -> Unit
) {
    BackHandler { onDismiss() }

    var name by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("CARD") }
    var currency by remember { mutableStateOf("PLN") }
    var balanceText by remember { mutableStateOf("1000.00") }
    var cardLastFour by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj konto lub portfel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa konta (np. ING Konto, Ledger BTC)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "CARD" to "Karta / Bank",
                        "CASH" to "Gotówka",
                        "SAVINGS" to "Oszczędności",
                        "CRYPTO" to "Krypto"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = accountType == key,
                            onClick = { accountType = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("account_type_chip_$key")
                        )
                    }
                }

                Text("Waluta:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CurrencyConverter.supportedCurrencies.forEach { curr ->
                        FilterChip(
                            selected = currency == curr,
                            onClick = { currency = curr },
                            label = { Text(curr) },
                            modifier = Modifier.testTag("account_currency_chip_$curr")
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Saldo początkowe") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_balance_input")
                )

                if (accountType == "CARD") {
                    OutlinedTextField(
                        value = cardLastFour,
                        onValueChange = { cardLastFour = it.take(4) },
                        label = { Text("Ostatnie 4 cyfry karty (do auto-detekcji)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_card_digits_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, accountType, currency, bal, cardLastFour)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_account_button")
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        personName: String,
        totalAmount: Double,
        currency: String,
        isLentByMe: Boolean,
        note: String,
        dueDays: Int
    ) -> Unit
) {
    BackHandler { onDismiss() }

    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PLN") }
    var isLentByMe by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy dług lub pożyczka") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isLentByMe,
                        onClick = { isLentByMe = true },
                        label = { Text("Pożyczyłem komuś") },
                        modifier = Modifier.testTag("debt_type_lent")
                    )
                    FilterChip(
                        selected = !isLentByMe,
                        onClick = { isLentByMe = false },
                        label = { Text("Pożyczyłem od kogoś") },
                        modifier = Modifier.testTag("debt_type_borrowed")
                    )
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Osoba (Imię / Nazwa)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_person_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Kwota") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("debt_amount_input")
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase() },
                        label = { Text("Waluta") },
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notatka / Za co") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (personName.isNotBlank() && amt > 0.0) {
                        onConfirm(personName, amt, currency.ifBlank { "PLN" }, isLentByMe, note, 14)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_debt_button")
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun AddRecurringDialog(
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        currency: String,
        category: String,
        cycle: String,
        isIncome: Boolean,
        nextDueInDays: Int,
        accountId: Int
    ) -> Unit
) {
    BackHandler { onDismiss() }

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PLN") }
    var cycle by remember { mutableStateOf("MONTHLY") }
    var isIncome by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj płatność cykliczną") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Subskrypcja / Rachunek") }
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Stały przychód") }
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nazwa (np. YouTube Premium, Czynsz)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recurring_title_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Kwota") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recurring_amount_input")
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase() },
                        label = { Text("Waluta") },
                        singleLine = true,
                        modifier = Modifier.width(96.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MONTHLY" to "Miesięcznie", "WEEKLY" to "Tygodniowo", "YEARLY" to "Rocznie").forEach { (key, label) ->
                        FilterChip(
                            selected = cycle == key,
                            onClick = { cycle = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        onConfirm(
                            title,
                            amt,
                            currency.ifBlank { "PLN" },
                            if (isIncome) "Wynagrodzenie" else "Subskrypcje",
                            cycle,
                            isIncome,
                            7,
                            accounts.firstOrNull()?.id ?: 1
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_recurring_button")
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text("Anuluj")
            }
        }
    )
}
