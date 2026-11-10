package com.vbrazhnik.obleek.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val accountType: String, // "CARD", "CASH", "SAVINGS", "CRYPTO"
    val currency: String, // "PLN", "EUR", "USD", "GBP", "BTC", "USDT"
    val balance: Double,
    val cardLastFour: String = "", // e.g., "4821"
    val accentHex: String = "#38BDF8"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val merchant: String,
    val amount: Double,
    val currency: String,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val category: String, // "Spożywcze", "Restauracje", "Transport", "Subskrypcje", "Zakupy", "Dom i Rachunki", "Rozrywka", "Wynagrodzenie", "Przelew"
    val accountId: Int,
    val targetAccountId: Int? = null,
    val targetAmount: Double? = null,
    val cardDigits: String = "",
    val isFromNotification: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val currency: String = "PLN",
    val isLentByMe: Boolean = true, // true = someone owes me, false = I owe someone
    val note: String = "",
    val dueDays: Int = 14,
    val isSettled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_payments")
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val currency: String = "PLN",
    val category: String = "Subskrypcje",
    val cycle: String = "MONTHLY", // "MONTHLY", "WEEKLY", "YEARLY"
    val isIncome: Boolean = false,
    val nextDueInDays: Int = 5,
    val accountId: Int = 1
)

@Entity(tableName = "detected_notifications")
data class DetectedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankAppName: String,
    val rawNotificationText: String,
    val extractedMerchant: String,
    val extractedAmount: Double,
    val extractedCurrency: String,
    val extractedCardDigits: String,
    val suggestedCategory: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isConfirmed: Boolean = false
)
