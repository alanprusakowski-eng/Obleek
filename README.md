# Obleek (`com.vbrazhnik.obleek`)

Wielowalutowy menedżer finansów, budżetu i automatycznego wykrywania transakcji z powiadomień bankowych na system Android (Kotlin, Jetpack Compose, Room).

## Główne moduły aplikacji
- **Pulpit i Portfele Wielowalutowe**: Obsługa kont bankowych, kart płatniczych (`*4821`, `*9012`), gotówki oraz kryptowalut (`PLN`, `EUR`, `USD`, `GBP`, `USDT`, `BTC`, `ETH`) z przeliczaniem majątku netto w czasie rzeczywistym i przelewami międzywalutowymi.
- **Smart Skaner Powiadomień Bankowych**: Lokalna detekcja i analiza powiadomień płatniczych (mBank, Revolut, PKO BP IKO, ING, Portfel Google) z rozpoznawaniem kwoty, waluty, sprzedawcy, ostatnich 4 cyfr karty oraz kategorii.
- **Długi i Pożyczki**: Śledzenie pożyczonych środków (wierzytelności i zobowiązań) dla poszczególnych osób i walut wraz z rozliczaniem 1-klikiem.
- **Płatności Cykliczne i Subskrypcje**: Zarządzanie subskrypcjami, rachunkami i stałymi zleceniami.
- **Analityka**: Wykresy struktury wydatków według kategorii, stopa oszczędności oraz ekspozycja walutowa.
