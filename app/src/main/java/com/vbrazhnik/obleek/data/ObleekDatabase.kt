package com.vbrazhnik.obleek.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        DebtEntity::class,
        RecurringPaymentEntity::class,
        DetectedNotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ObleekDatabase : RoomDatabase() {
    abstract fun obleekDao(): ObleekDao

    companion object {
        @Volatile
        private var INSTANCE: ObleekDatabase? = null

        fun getInstance(context: Context): ObleekDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ObleekDatabase::class.java,
                    "obleek_finance.db"
                )
                    .addCallback(SeedDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    seedInitialData(database.obleekDao())
                }
            }
        }

        private suspend fun seedInitialData(dao: ObleekDao) {
            // 1. Seed Multi-Currency & Crypto Accounts
            val mbankId = dao.insertAccount(
                AccountEntity(
                    name = "mBank eKonto",
                    accountType = "CARD",
                    currency = "PLN",
                    balance = 6840.50,
                    cardLastFour = "4821",
                    accentHex = "#38BDF8"
                )
            ).toInt()

            val revolutId = dao.insertAccount(
                AccountEntity(
                    name = "Revolut Metal",
                    accountType = "CARD",
                    currency = "EUR",
                    balance = 1420.00,
                    cardLastFour = "9012",
                    accentHex = "#A3E635"
                )
            ).toInt()

            val cashId = dao.insertAccount(
                AccountEntity(
                    name = "Portfel Gotówka",
                    accountType = "CASH",
                    currency = "PLN",
                    balance = 650.00,
                    cardLastFour = "",
                    accentHex = "#F59E0B"
                )
            ).toInt()

            dao.insertAccount(
                AccountEntity(
                    name = "Krypto Vault USDT",
                    accountType = "CRYPTO",
                    currency = "USDT",
                    balance = 850.00,
                    cardLastFour = "",
                    accentHex = "#A855F7"
                )
            )

            val now = System.currentTimeMillis()
            val hour = 3600_000L

            // 2. Seed Realistic Transactions
            dao.insertTransaction(
                TransactionEntity(
                    title = "Zakupy spożywcze",
                    merchant = "Biedronka",
                    amount = 142.80,
                    currency = "PLN",
                    type = "EXPENSE",
                    category = "Spożywcze",
                    accountId = mbankId,
                    cardDigits = "4821",
                    isFromNotification = true,
                    timestamp = now - 3 * hour
                )
            )
            dao.insertTransaction(
                TransactionEntity(
                    title = "Wynagrodzenie miesięczne",
                    merchant = "TechStudio Sp. z o.o.",
                    amount = 9500.00,
                    currency = "PLN",
                    type = "INCOME",
                    category = "Wynagrodzenie",
                    accountId = mbankId,
                    isFromNotification = false,
                    timestamp = now - 28 * hour
                )
            )
            dao.insertTransaction(
                TransactionEntity(
                    title = "Subskrypcja chmurowa i AI",
                    merchant = "JetBrains & Cloud",
                    amount = 24.00,
                    currency = "EUR",
                    type = "EXPENSE",
                    category = "Subskrypcje",
                    accountId = revolutId,
                    cardDigits = "9012",
                    isFromNotification = true,
                    timestamp = now - 16 * hour
                )
            )
            dao.insertTransaction(
                TransactionEntity(
                    title = "Kawa i lunch",
                    merchant = "Green Caffè Nero",
                    amount = 38.50,
                    currency = "PLN",
                    type = "EXPENSE",
                    category = "Restauracje",
                    accountId = cashId,
                    isFromNotification = false,
                    timestamp = now - 40 * hour
                )
            )
            dao.insertTransaction(
                TransactionEntity(
                    title = "Tankowanie paliwa",
                    merchant = "Orlen Stacja 412",
                    amount = 265.00,
                    currency = "PLN",
                    type = "EXPENSE",
                    category = "Transport",
                    accountId = mbankId,
                    cardDigits = "4821",
                    isFromNotification = true,
                    timestamp = now - 64 * hour
                )
            )

            // 3. Seed Pending Bank Notifications ready for 1-tap review & confirmation
            dao.insertDetectedNotification(
                DetectedNotificationEntity(
                    bankAppName = "mBank",
                    rawNotificationText = "mBank: Płatność kartą *4821 na kwotę 46,90 PLN w Żabka Polska. Dostępne środki: 6 840,50 PLN.",
                    extractedMerchant = "Żabka Polska",
                    extractedAmount = 46.90,
                    extractedCurrency = "PLN",
                    extractedCardDigits = "4821",
                    suggestedCategory = "Spożywcze",
                    timestamp = now - 15 * 60_000L
                )
            )
            dao.insertDetectedNotification(
                DetectedNotificationEntity(
                    bankAppName = "Revolut",
                    rawNotificationText = "Revolut: You spent €14.99 at Netflix.com with card *9012.",
                    extractedMerchant = "Netflix.com",
                    extractedAmount = 14.99,
                    extractedCurrency = "EUR",
                    extractedCardDigits = "9012",
                    suggestedCategory = "Subskrypcje",
                    timestamp = now - 45 * 60_000L
                )
            )

            // 4. Seed Debts (Lent & Borrowed per person and currency)
            dao.insertDebt(
                DebtEntity(
                    personName = "Kamil Nowak",
                    totalAmount = 320.00,
                    paidAmount = 100.00,
                    currency = "PLN",
                    isLentByMe = true,
                    note = "Wspólny wyjazd weekendowy i paliwo",
                    dueDays = 5
                )
            )
            dao.insertDebt(
                DebtEntity(
                    personName = "Marta Kowalska",
                    totalAmount = 65.00,
                    paidAmount = 0.0,
                    currency = "EUR",
                    isLentByMe = true,
                    note = "Bilety na koncert w Berlinie",
                    dueDays = 10
                )
            )
            dao.insertDebt(
                DebtEntity(
                    personName = "Piotr Wiśniewski",
                    totalAmount = 180.00,
                    paidAmount = 0.0,
                    currency = "PLN",
                    isLentByMe = false,
                    note = "Kolacja zespołowa i rachunek",
                    dueDays = 3
                )
            )

            // 5. Seed Recurring Payments & Subscriptions
            dao.insertRecurringPayment(
                RecurringPaymentEntity(
                    title = "Spotify Duo",
                    amount = 34.99,
                    currency = "PLN",
                    category = "Subskrypcje",
                    cycle = "MONTHLY",
                    isIncome = false,
                    nextDueInDays = 2,
                    accountId = mbankId
                )
            )
            dao.insertRecurringPayment(
                RecurringPaymentEntity(
                    title = "Czynsz i Media",
                    amount = 2450.00,
                    currency = "PLN",
                    category = "Dom i Rachunki",
                    cycle = "MONTHLY",
                    isIncome = false,
                    nextDueInDays = 6,
                    accountId = mbankId
                )
            )
            dao.insertRecurringPayment(
                RecurringPaymentEntity(
                    title = "Apple iCloud+ 2TB",
                    amount = 9.99,
                    currency = "EUR",
                    category = "Subskrypcje",
                    cycle = "MONTHLY",
                    isIncome = false,
                    nextDueInDays = 11,
                    accountId = revolutId
                )
            )
            dao.insertRecurringPayment(
                RecurringPaymentEntity(
                    title = "Wypłata Kontrakt B2B",
                    amount = 9500.00,
                    currency = "PLN",
                    category = "Wynagrodzenie",
                    cycle = "MONTHLY",
                    isIncome = true,
                    nextDueInDays = 14,
                    accountId = mbankId
                )
            )
        }
    }
}
