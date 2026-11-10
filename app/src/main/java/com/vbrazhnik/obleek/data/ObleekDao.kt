package com.vbrazhnik.obleek.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ObleekDao {
    // Accounts
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Int): AccountEntity?

    @Query("SELECT * FROM accounts WHERE cardLastFour = :cardDigits LIMIT 1")
    suspend fun getAccountByCardDigits(cardDigits: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Int)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Int)

    // Debts
    @Query("SELECT * FROM debts ORDER BY isSettled ASC, timestamp DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebt(id: Int)

    // Recurring Payments
    @Query("SELECT * FROM recurring_payments ORDER BY nextDueInDays ASC")
    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringPayment(payment: RecurringPaymentEntity): Long

    @Update
    suspend fun updateRecurringPayment(payment: RecurringPaymentEntity)

    @Query("DELETE FROM recurring_payments WHERE id = :id")
    suspend fun deleteRecurringPayment(id: Int)

    // Detected Notifications
    @Query("SELECT * FROM detected_notifications WHERE isConfirmed = 0 ORDER BY timestamp DESC")
    fun getPendingNotifications(): Flow<List<DetectedNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetectedNotification(notification: DetectedNotificationEntity): Long

    @Query("UPDATE detected_notifications SET isConfirmed = 1 WHERE id = :id")
    suspend fun markNotificationConfirmed(id: Int)

    @Query("DELETE FROM detected_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)
}
