package com.vbrazhnik.obleek.data

import java.util.Locale

object CurrencyConverter {
    // Rates relative to 1 PLN for instant offline multi-currency & crypto conversion
    private val rateToPln = mapOf(
        "PLN" to 1.0,
        "EUR" to 4.32,
        "USD" to 3.98,
        "GBP" to 5.05,
        "CHF" to 4.55,
        "USDT" to 3.98,
        "USDC" to 3.98,
        "BTC" to 365000.0,
        "ETH" to 11800.0
    )

    val supportedCurrencies = listOf("PLN", "EUR", "USD", "GBP", "USDT", "BTC", "ETH")
    val mainDisplayCurrencies = listOf("PLN", "EUR", "USD", "GBP")

    val expenseCategories = listOf(
        "Spożywcze",
        "Restauracje",
        "Transport",
        "Subskrypcje",
        "Zakupy",
        "Dom i Rachunki",
        "Zdrowie",
        "Rozrywka",
        "Inne"
    )

    val incomeCategories = listOf(
        "Wynagrodzenie",
        "Freelance / Projekt",
        "Zwrot / Cashback",
        "Inwestycje / Krypto",
        "Inny przychód"
    )

    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        val fromInPln = amount * (rateToPln[fromCurrency.uppercase()] ?: 1.0)
        val targetRate = rateToPln[toCurrency.uppercase()] ?: 1.0
        return if (targetRate > 0.0) fromInPln / targetRate else amount
    }

    fun formatAmount(amount: Double, currency: String, includeSign: Boolean = false): String {
        val upper = currency.uppercase()
        val isCrypto = upper == "BTC" || upper == "ETH"
        val formattedNumber = if (isCrypto) {
            String.format(Locale.US, "%.4f", amount)
        } else {
            String.format(Locale.forLanguageTag("pl-PL"), "%,.2f", amount)
        }
        val prefix = when {
            !includeSign -> ""
            amount > 0 -> "+"
            else -> ""
        }
        return "$prefix$formattedNumber $upper"
    }

    fun symbolFor(currency: String): String = when (currency.uppercase()) {
        "PLN" -> "zł"
        "EUR" -> "€"
        "USD" -> "$"
        "GBP" -> "£"
        "BTC" -> "₿"
        "ETH" -> "Ξ"
        "USDT", "USDC" -> "₮"
        else -> currency.uppercase()
    }
}
