package com.vbrazhnik.obleek.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vbrazhnik.obleek.data.AccountEntity
import com.vbrazhnik.obleek.data.CurrencyConverter
import com.vbrazhnik.obleek.data.DebtEntity
import com.vbrazhnik.obleek.data.DetectedNotificationEntity
import com.vbrazhnik.obleek.data.ObleekRepository
import com.vbrazhnik.obleek.data.RecurringPaymentEntity
import com.vbrazhnik.obleek.data.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ObleekUiState(
    val mainCurrency: String = "PLN",
    val isDarkTheme: Boolean = true,
    val selectedCategoryFilter: String = "Wszystkie",
    val accounts: List<AccountEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val recurringPayments: List<RecurringPaymentEntity> = emptyList(),
    val pendingNotifications: List<DetectedNotificationEntity> = emptyList(),
    val totalNetWorthInMain: Double = 0.0,
    val monthlyIncomeInMain: Double = 0.0,
    val monthlyExpensesInMain: Double = 0.0,
    val totalLentInMain: Double = 0.0,
    val totalBorrowedInMain: Double = 0.0
)

class ObleekViewModel(private val repository: ObleekRepository) : ViewModel() {

    private val mainCurrencyFlow = MutableStateFlow("PLN")
    private val isDarkThemeFlow = MutableStateFlow(true)
    private val categoryFilterFlow = MutableStateFlow("Wszystkie")

    val uiState: StateFlow<ObleekUiState> = combine(
        repository.accounts,
        repository.transactions,
        repository.debts,
        repository.recurringPayments,
        repository.pendingNotifications,
        mainCurrencyFlow,
        isDarkThemeFlow,
        categoryFilterFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val accounts = values[0] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = values[1] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val debts = values[2] as List<DebtEntity>
        @Suppress("UNCHECKED_CAST")
        val recurring = values[3] as List<RecurringPaymentEntity>
        @Suppress("UNCHECKED_CAST")
        val notifications = values[4] as List<DetectedNotificationEntity>
        val mainCurr = values[5] as String
        val isDark = values[6] as Boolean
        val catFilter = values[7] as String

        val netWorth = accounts.sumOf { acc ->
            CurrencyConverter.convert(acc.balance, acc.currency, mainCurr)
        }

        val monthlyIncome = transactions
            .filter { it.type == "INCOME" }
            .sumOf { CurrencyConverter.convert(it.amount, it.currency, mainCurr) }

        val monthlyExpenses = transactions
            .filter { it.type == "EXPENSE" }
            .sumOf { CurrencyConverter.convert(it.amount, it.currency, mainCurr) }

        val activeDebts = debts.filter { !it.isSettled }
        val totalLent = activeDebts
            .filter { it.isLentByMe }
            .sumOf {
                CurrencyConverter.convert(
                    (it.totalAmount - it.paidAmount).coerceAtLeast(0.0),
                    it.currency,
                    mainCurr
                )
            }
        val totalBorrowed = activeDebts
            .filter { !it.isLentByMe }
            .sumOf {
                CurrencyConverter.convert(
                    (it.totalAmount - it.paidAmount).coerceAtLeast(0.0),
                    it.currency,
                    mainCurr
                )
            }

        ObleekUiState(
            mainCurrency = mainCurr,
            isDarkTheme = isDark,
            selectedCategoryFilter = catFilter,
            accounts = accounts,
            transactions = transactions,
            debts = debts,
            recurringPayments = recurring,
            pendingNotifications = notifications,
            totalNetWorthInMain = netWorth,
            monthlyIncomeInMain = monthlyIncome,
            monthlyExpensesInMain = monthlyExpenses,
            totalLentInMain = totalLent,
            totalBorrowedInMain = totalBorrowed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ObleekUiState()
    )

    fun setMainCurrency(currency: String) {
        mainCurrencyFlow.value = currency
    }

    fun toggleDarkTheme() {
        isDarkThemeFlow.value = !isDarkThemeFlow.value
    }

    fun setCategoryFilter(category: String) {
        categoryFilterFlow.value = category
    }

    fun addAccount(
        name: String,
        accountType: String,
        currency: String,
        balance: Double,
        cardLastFour: String,
        accentHex: String
    ) {
        viewModelScope.launch {
            repository.addAccount(
                AccountEntity(
                    name = name.trim(),
                    accountType = accountType,
                    currency = currency,
                    balance = balance,
                    cardLastFour = cardLastFour.trim(),
                    accentHex = accentHex
                )
            )
        }
    }

    fun deleteAccount(accountId: Int) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun addTransaction(
        title: String,
        merchant: String,
        amount: Double,
        currency: String,
        type: String,
        category: String,
        accountId: Int,
        targetAccountId: Int? = null
    ) {
        viewModelScope.launch {
            repository.recordTransaction(
                title = title,
                merchant = merchant,
                amount = amount,
                currency = currency,
                type = type,
                category = category,
                accountId = accountId,
                targetAccountId = targetAccountId
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun confirmNotification(
        notification: DetectedNotificationEntity,
        chosenAccountId: Int? = null,
        overrideCategory: String? = null
    ) {
        viewModelScope.launch {
            repository.confirmDetectedNotification(
                notification = notification,
                chosenAccountId = chosenAccountId,
                overrideCategory = overrideCategory
            )
        }
    }

    fun dismissNotification(id: Int) {
        viewModelScope.launch {
            repository.dismissNotification(id)
        }
    }

    fun simulateOrParseNotification(bankAppName: String, rawText: String) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            repository.parseAndQueueNotification(bankAppName, rawText)
        }
    }

    fun addDebt(
        personName: String,
        totalAmount: Double,
        currency: String,
        isLentByMe: Boolean,
        note: String,
        dueDays: Int
    ) {
        viewModelScope.launch {
            repository.addDebt(
                DebtEntity(
                    personName = personName.trim(),
                    totalAmount = totalAmount,
                    currency = currency,
                    isLentByMe = isLentByMe,
                    note = note.trim(),
                    dueDays = dueDays
                )
            )
        }
    }

    fun settleDebt(debt: DebtEntity, amount: Double, accountId: Int?) {
        viewModelScope.launch {
            repository.settleDebt(debt, amount, accountId)
        }
    }

    fun deleteDebt(id: Int) {
        viewModelScope.launch {
            repository.deleteDebt(id)
        }
    }

    fun addRecurringPayment(
        title: String,
        amount: Double,
        currency: String,
        category: String,
        cycle: String,
        isIncome: Boolean,
        nextDueInDays: Int,
        accountId: Int
    ) {
        viewModelScope.launch {
            repository.addRecurringPayment(
                RecurringPaymentEntity(
                    title = title.trim(),
                    amount = amount,
                    currency = currency,
                    category = category,
                    cycle = cycle,
                    isIncome = isIncome,
                    nextDueInDays = nextDueInDays,
                    accountId = accountId
                )
            )
        }
    }

    fun payRecurringNow(payment: RecurringPaymentEntity) {
        viewModelScope.launch {
            repository.executeRecurringPaymentNow(payment)
        }
    }

    fun deleteRecurringPayment(id: Int) {
        viewModelScope.launch {
            repository.deleteRecurringPayment(id)
        }
    }
}

class ObleekViewModelFactory(private val repository: ObleekRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ObleekViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ObleekViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
