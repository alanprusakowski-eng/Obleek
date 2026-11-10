package com.vbrazhnik.obleek.data

import kotlinx.coroutines.flow.Flow

class ObleekRepository(private val dao: ObleekDao) {
    val accounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val transactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val debts: Flow<List<DebtEntity>> = dao.getAllDebts()
    val recurringPayments: Flow<List<RecurringPaymentEntity>> = dao.getAllRecurringPayments()
    val pendingNotifications: Flow<List<DetectedNotificationEntity>> = dao.getPendingNotifications()

    suspend fun addAccount(account: AccountEntity) {
        dao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        dao.updateAccount(account)
    }

    suspend fun deleteAccount(accountId: Int) {
        dao.deleteAccount(accountId)
    }

    suspend fun recordTransaction(
        title: String,
        merchant: String,
        amount: Double,
        currency: String,
        type: String,
        category: String,
        accountId: Int,
        targetAccountId: Int? = null,
        cardDigits: String = "",
        isFromNotification: Boolean = false
    ) {
        val sourceAccount = dao.getAccountById(accountId)
        var calculatedTargetAmount: Double? = null

        if (sourceAccount != null) {
            val deltaInAccountCurrency = CurrencyConverter.convert(amount, currency, sourceAccount.currency)
            when (type) {
                "EXPENSE" -> {
                    dao.updateAccount(sourceAccount.copy(balance = sourceAccount.balance - deltaInAccountCurrency))
                }
                "INCOME" -> {
                    dao.updateAccount(sourceAccount.copy(balance = sourceAccount.balance + deltaInAccountCurrency))
                }
                "TRANSFER" -> {
                    dao.updateAccount(sourceAccount.copy(balance = sourceAccount.balance - deltaInAccountCurrency))
                    if (targetAccountId != null) {
                        val targetAccount = dao.getAccountById(targetAccountId)
                        if (targetAccount != null) {
                            val convertedToTarget = CurrencyConverter.convert(amount, currency, targetAccount.currency)
                            calculatedTargetAmount = convertedToTarget
                            dao.updateAccount(targetAccount.copy(balance = targetAccount.balance + convertedToTarget))
                        }
                    }
                }
            }
        }

        dao.insertTransaction(
            TransactionEntity(
                title = title.ifBlank { merchant.ifBlank { category } },
                merchant = merchant.ifBlank { title },
                amount = amount,
                currency = currency,
                type = type,
                category = category,
                accountId = accountId,
                targetAccountId = targetAccountId,
                targetAmount = calculatedTargetAmount,
                cardDigits = cardDigits,
                isFromNotification = isFromNotification
            )
        )
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        val account = dao.getAccountById(transaction.accountId)
        if (account != null) {
            val delta = CurrencyConverter.convert(transaction.amount, transaction.currency, account.currency)
            val restoredBalance = when (transaction.type) {
                "EXPENSE", "TRANSFER" -> account.balance + delta
                "INCOME" -> account.balance - delta
                else -> account.balance
            }
            dao.updateAccount(account.copy(balance = restoredBalance))
        }
        dao.deleteTransaction(transaction.id)
    }

    suspend fun confirmDetectedNotification(
        notification: DetectedNotificationEntity,
        chosenAccountId: Int?,
        overrideCategory: String? = null
    ) {
        val matchedByCard = if (notification.extractedCardDigits.isNotBlank()) {
            dao.getAccountByCardDigits(notification.extractedCardDigits)
        } else null

        val targetAccountId = chosenAccountId ?: matchedByCard?.id ?: 1

        recordTransaction(
            title = "${notification.extractedMerchant} (${notification.bankAppName})",
            merchant = notification.extractedMerchant,
            amount = notification.extractedAmount,
            currency = notification.extractedCurrency,
            type = "EXPENSE",
            category = overrideCategory ?: notification.suggestedCategory,
            accountId = targetAccountId,
            cardDigits = notification.extractedCardDigits,
            isFromNotification = true
        )
        dao.markNotificationConfirmed(notification.id)
    }

    suspend fun dismissNotification(id: Int) {
        dao.deleteNotification(id)
    }

    suspend fun parseAndQueueNotification(bankAppName: String, rawText: String) {
        val parsed = NotificationParser.parse(bankAppName, rawText)
        dao.insertDetectedNotification(
            DetectedNotificationEntity(
                bankAppName = parsed.bankAppName,
                rawNotificationText = parsed.rawText,
                extractedMerchant = parsed.merchant,
                extractedAmount = parsed.amount,
                extractedCurrency = parsed.currency,
                extractedCardDigits = parsed.cardDigits,
                suggestedCategory = parsed.suggestedCategory
            )
        )
    }

    suspend fun addDebt(debt: DebtEntity) {
        dao.insertDebt(debt)
    }

    suspend fun settleDebt(debt: DebtEntity, paymentAmount: Double, accountIdToCreditOrDebit: Int?) {
        val newPaid = (debt.paidAmount + paymentAmount).coerceAtMost(debt.totalAmount)
        val settled = newPaid >= debt.totalAmount - 0.01
        dao.updateDebt(debt.copy(paidAmount = newPaid, isSettled = settled))

        if (accountIdToCreditOrDebit != null) {
            recordTransaction(
                title = if (debt.isLentByMe) "Zwrot długu: ${debt.personName}" else "Spłata długu: ${debt.personName}",
                merchant = debt.personName,
                amount = paymentAmount,
                currency = debt.currency,
                type = if (debt.isLentByMe) "INCOME" else "EXPENSE",
                category = "Inne",
                accountId = accountIdToCreditOrDebit
            )
        }
    }

    suspend fun deleteDebt(id: Int) {
        dao.deleteDebt(id)
    }

    suspend fun addRecurringPayment(payment: RecurringPaymentEntity) {
        dao.insertRecurringPayment(payment)
    }

    suspend fun executeRecurringPaymentNow(payment: RecurringPaymentEntity) {
        recordTransaction(
            title = payment.title,
            merchant = payment.title,
            amount = payment.amount,
            currency = payment.currency,
            type = if (payment.isIncome) "INCOME" else "EXPENSE",
            category = payment.category,
            accountId = payment.accountId
        )
        val nextDays = when (payment.cycle) {
            "WEEKLY" -> 7
            "YEARLY" -> 365
            else -> 30
        }
        dao.updateRecurringPayment(payment.copy(nextDueInDays = nextDays))
    }

    suspend fun deleteRecurringPayment(id: Int) {
        dao.deleteRecurringPayment(id)
    }
}
