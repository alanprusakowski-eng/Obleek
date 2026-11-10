package com.vbrazhnik.obleek.data

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class ParsedPaymentNotification(
    val bankAppName: String,
    val rawText: String,
    val merchant: String,
    val amount: Double,
    val currency: String,
    val cardDigits: String,
    val suggestedCategory: String
)

object NotificationParser {

    data class SampleBankPush(
        val bankName: String,
        val rawText: String
    )

    val sampleBankNotifications = listOf(
        SampleBankPush(
            bankName = "mBank",
            rawText = "mBank: Płatność kartą *4821 na kwotę 54,90 PLN w Biedronka Warszawa. Dostępne środki: 4 120,50 PLN."
        ),
        SampleBankPush(
            bankName = "Revolut",
            rawText = "Revolut: You spent €16.50 at Bolt EU with card *9012."
        ),
        SampleBankPush(
            bankName = "PKO BP (IKO)",
            rawText = "IKO: Płatność BLIK 129,00 PLN w Allegro.pl została wykonana."
        ),
        SampleBankPush(
            bankName = "Google Wallet",
            rawText = "Portfel Google: Zapłacono 24,50 PLN w Starbucks Coffee kartą Visa *4821."
        ),
        SampleBankPush(
            bankName = "ING Bank Śląski",
            rawText = "Moje ING: Obciążenie karty *4821 kwotą 189,99 PLN. Orlen Stacja Paliw."
        )
    )

    fun parse(bankAppName: String, rawText: String): ParsedPaymentNotification {
        val cleanText = rawText.trim()

        // 1. Extract card last 4 digits (*1234, ••1234, * 1234)
        val cardRegex = Regex("""(?:\*|••|karta\s+\*?|card\s+\*?)(\d{4})""", RegexOption.IGNORE_CASE)
        val cardMatch = cardRegex.find(cleanText)
        val cardDigits = cardMatch?.groupValues?.getOrNull(1) ?: ""

        // 2. Extract amount and currency
        // Handles "54,90 PLN", "€16.50", "$29.99", "129.00 EUR", "1 250,00 zł"
        val symbolFirstRegex = Regex("""([€$£])\s*(\d+(?:[.,]\d{1,2})?)""")
        val amountCurrencyRegex = Regex(
            """(\d[\d\s]*(?:[.,]\d{1,4})?)\s*(PLN|zł|EUR|USD|GBP|USDT|USDC|BTC|ETH)\b""",
            RegexOption.IGNORE_CASE
        )

        var amount = 0.0
        var currency = "PLN"

        val symbolMatch = symbolFirstRegex.find(cleanText)
        val afterMatch = amountCurrencyRegex.find(cleanText)

        if (symbolMatch != null) {
            val sym = symbolMatch.groupValues[1]
            currency = when (sym) {
                "€" -> "EUR"
                "$" -> "USD"
                "£" -> "GBP"
                else -> "EUR"
            }
            amount = symbolMatch.groupValues[2].replace(",", ".").toDoubleOrNull() ?: 0.0
        } else if (afterMatch != null) {
            val rawNum = afterMatch.groupValues[1].replace(" ", "").replace(",", ".")
            amount = rawNum.toDoubleOrNull() ?: 0.0
            val rawCurr = afterMatch.groupValues[2].uppercase()
            currency = if (rawCurr == "ZŁ") "PLN" else rawCurr
        }

        // 3. Extract merchant
        val merchantPatterns = listOf(
            Regex("""\bw\s+([A-ZŻŹĆĄŚĘŁÓŃ0-9][A-Za-zżźćńółęąśŻŹĆĄŚĘŁÓŃ0-9\s.&-]{2,28}?)(?:\.|,| z | kartą| została|$)"""),
            Regex("""\bat\s+([A-Za-z0-9\s.&-]{2,28}?)(?:\.|,| with | card |$|\s+on\b)""", RegexOption.IGNORE_CASE),
            Regex("""PLN\.\s*([A-ZŻŹĆĄŚĘŁÓŃ0-9][A-Za-zżźćńółęąśŻŹĆĄŚĘŁÓŃ0-9\s.&-]{2,28}?)(?:\.|$)""")
        )
        var merchant = ""
        for (pattern in merchantPatterns) {
            val m = pattern.find(cleanText)
            if (m != null) {
                merchant = m.groupValues[1].trim()
                break
            }
        }
        if (merchant.isBlank()) {
            merchant = when {
                cleanText.contains("Biedronka", ignoreCase = true) -> "Biedronka"
                cleanText.contains("Żabka", ignoreCase = true) -> "Żabka"
                cleanText.contains("Lidl", ignoreCase = true) -> "Lidl"
                cleanText.contains("Netflix", ignoreCase = true) -> "Netflix"
                cleanText.contains("Spotify", ignoreCase = true) -> "Spotify"
                cleanText.contains("Bolt", ignoreCase = true) -> "Bolt"
                cleanText.contains("Uber", ignoreCase = true) -> "Uber"
                cleanText.contains("Orlen", ignoreCase = true) -> "Orlen"
                cleanText.contains("Allegro", ignoreCase = true) -> "Allegro"
                cleanText.contains("Starbucks", ignoreCase = true) -> "Starbucks"
                else -> "Płatność bezgotówkowa"
            }
        }

        val suggestedCategory = guessCategory(merchant, cleanText)
        val detectedBank = if (bankAppName.isNotBlank()) bankAppName else guessBankName(cleanText)

        return ParsedPaymentNotification(
            bankAppName = detectedBank,
            rawText = cleanText,
            merchant = merchant,
            amount = if (amount > 0.0) amount else 25.00,
            currency = currency,
            cardDigits = cardDigits,
            suggestedCategory = suggestedCategory
        )
    }

    private fun guessBankName(text: String): String = when {
        text.contains("mBank", ignoreCase = true) -> "mBank"
        text.contains("Revolut", ignoreCase = true) -> "Revolut"
        text.contains("IKO", ignoreCase = true) || text.contains("PKO", ignoreCase = true) -> "PKO BP (IKO)"
        text.contains("ING", ignoreCase = true) -> "ING Bank Śląski"
        text.contains("Santander", ignoreCase = true) -> "Santander"
        text.contains("Pekao", ignoreCase = true) -> "Bank Pekao"
        text.contains("Portfel", ignoreCase = true) || text.contains("Google", ignoreCase = true) -> "Google Wallet"
        else -> "Powiadomienie bankowe"
    }

    fun guessCategory(merchant: String, fullText: String = ""): String {
        val combined = "$merchant $fullText".lowercase()
        return when {
            combined.contains("biedronka") || combined.contains("żabka") || combined.contains("lidl") ||
                combined.contains("auchan") || combined.contains("carrefour") -> "Spożywcze"
            combined.contains("starbucks") || combined.contains("restauracja") || combined.contains("pyszne") ||
                combined.contains("mcdonald") || combined.contains("kawiarnia") || combined.contains("sushi") -> "Restauracje"
            combined.contains("bolt") || combined.contains("uber") || combined.contains("orlen") ||
                combined.contains("jakdojade") || combined.contains("pkp") || combined.contains("paliw") -> "Transport"
            combined.contains("netflix") || combined.contains("spotify") || combined.contains("youtube") ||
                combined.contains("icloud") || combined.contains("chatgpt") -> "Subskrypcje"
            combined.contains("allegro") || combined.contains("amazon") || combined.contains("zara") ||
                combined.contains("media expert") || combined.contains("empik") -> "Zakupy"
            combined.contains("czynsz") || combined.contains("prąd") || combined.contains("internet") -> "Dom i Rachunki"
            else -> "Zakupy"
        }
    }
}

class ObleekNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn?.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val combined = "$title: $text".trim()

        if (combined.length < 8) return
        val looksLikePayment = combined.contains("PLN", ignoreCase = true) ||
            combined.contains("EUR", ignoreCase = true) ||
            combined.contains("USD", ignoreCase = true) ||
            combined.contains("zł", ignoreCase = true) ||
            combined.contains("€") ||
            combined.contains("płatność", ignoreCase = true) ||
            combined.contains("spent", ignoreCase = true)

        if (!looksLikePayment) return

        val parsed = NotificationParser.parse(sbn.packageName.substringAfterLast('.'), combined)
        val db = ObleekDatabase.getInstance(applicationContext)
        serviceScope.launch {
            db.obleekDao().insertDetectedNotification(
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
    }
}
