package app.expensetracker.data

import kotlin.math.roundToLong

/**
 * Converts an amount from one currency to another for the save-time suggestion. All amounts in the app
 * are held as 2-decimal minor units, so a value in the *source* currency and the resulting value in the
 * *target* currency share that representation and the conversion is simply `round(amountMinor * rate)`.
 * Because the 2-dp internal scale is the same on both sides, the source/target currencies' true ISO
 * decimal exponents never distort the math (a ¥ amount stored as 2-dp minor converts just as cleanly as
 * an INR one). This is a convenience estimate, not an accounting figure — latest cached rate, no
 * historical-date lookups. Display precision is handled separately by [CurrencyMeta.format].
 */
object CurrencyConverter {

    /**
     * @param amountMinor amount in the source currency, as 2-decimal minor units
     * @param rate units of the target currency per 1 unit of the source currency
     * @return the equivalent amount in the target currency, as 2-decimal minor units, or null if
     *   [rate] is not a usable positive number
     */
    fun convertMinor(amountMinor: Long, rate: Double): Long? {
        if (!rate.isFinite() || rate <= 0.0) return null
        return (amountMinor * rate).roundToLong()
    }
}
