@file:OptIn(kotlin.time.ExperimentalTime::class)

package app.expensetracker.fx

import app.expensetracker.storage.SettingsStorage
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/** A day's exchange rates relative to [base]: `rates[X]` = how many X you get for 1 [base]. */
@Serializable
data class FxSnapshot(
    val base: String,
    val fetchedAtMillis: Long,
    val rates: Map<String, Double>,
)

/** Persists the latest [FxSnapshot] as a single JSON blob under its own settings key. */
class FxRateCache(private val settings: Settings = SettingsStorage.createSettings()) {
    private val json = Json { ignoreUnknownKeys = true }

    fun read(): FxSnapshot? =
        settings.getStringOrNull(KEY)?.let { runCatching { json.decodeFromString<FxSnapshot>(it) }.getOrNull() }

    fun write(snapshot: FxSnapshot) { settings[KEY] = json.encodeToString(snapshot) }

    private companion object {
        const val KEY = "fx_rates_cache"
    }
}

@Serializable
private data class FrankfurterLatest(val base: String = "", val rates: Map<String, Double> = emptyMap())

/**
 * Looks up an exchange rate for the save-time conversion *suggestion*. Rates are fetched at most once
 * a day from frankfurter.dev (ECB data — no API key, and no personal data leaves the device, just
 * "today's rates for <base>"), cached locally, and inverted as needed. Base = the user's home
 * currency. Returns null when offline / a currency isn't covered, so the caller degrades to "rate
 * unavailable" rather than blocking. Latest rate only — no historical dates; this is a convenience
 * estimate, not an accounting figure.
 */
class FxRateService(
    private val cache: FxRateCache = FxRateCache(),
    private val client: HttpClient = defaultClient(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    /** Units of [to] per 1 unit of [from], or null if it can't be determined. */
    suspend fun rate(from: String, to: String): Double? {
        val f = from.uppercase()
        val t = to.uppercase()
        if (f == t) return 1.0
        val snapshot = freshSnapshot(t) ?: return null
        val perBase = snapshot.rates[f]
            ?: derivePeggedRate(f, snapshot.base, snapshot.rates)
            ?: return null // `f` units per 1 `t`
        return if (perBase > 0.0) 1.0 / perBase else null
    }

    /**
     * AED and SAR are hard-pegged to USD and absent from the ECB dataset. Derive their rate from the
     * USD entry in the snapshot. [base] is the snapshot's base currency.
     */
    private fun derivePeggedRate(code: String, base: String, rates: Map<String, Double>): Double? {
        val codePerUsd = USD_PEGS[code] ?: return null
        if (base == "USD") return codePerUsd
        val usdPerBase = rates["USD"] ?: return null
        return usdPerBase * codePerUsd
    }

    private suspend fun freshSnapshot(base: String): FxSnapshot? {
        val cached = cache.read()
        if (cached != null && cached.base == base && now() - cached.fetchedAtMillis < MAX_AGE_MS) return cached
        val fetched = fetch(base)
        if (fetched != null) {
            cache.write(fetched)
            return fetched
        }
        // Offline / fetch failed: a stale snapshot for the same base beats no suggestion at all.
        return cached?.takeIf { it.base == base }
    }

    private suspend fun fetch(base: String): FxSnapshot? = runCatching {
        val latest: FrankfurterLatest = client.get("https://api.frankfurter.dev/v1/latest") {
            parameter("base", base)
        }.body()
        latest.rates.takeIf { it.isNotEmpty() }?.let { FxSnapshot(base, now(), it) }
    }.getOrNull()

    private companion object {
        const val MAX_AGE_MS = 24L * 60 * 60 * 1000

        /** AED and SAR are hard-pegged to USD and absent from the ECB dataset: X units per 1 USD. */
        val USD_PEGS = mapOf("AED" to 3.6725, "SAR" to 3.75)

        fun defaultClient() = HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
}
