package app.expensetracker.fx

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FxRateServiceTest {

    private fun jsonClient(body: String, onCall: () -> Unit = {}): HttpClient {
        val engine = MockEngine {
            onCall()
            respond(content = body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    }

    @Test
    fun same_currency_is_identity_and_skips_the_network() = runTest {
        var calls = 0
        val svc = FxRateService(FxRateCache(MapSettings()), jsonClient("{}") { calls++ }, now = { 0L })
        assertEquals(1.0, svc.rate("SGD", "SGD"))
        assertEquals(0, calls)
    }

    @Test
    fun inverts_base_rate_for_foreign_to_home() = runTest {
        // base = SGD; 1 SGD = 63.5 INR, so INR -> SGD is 1/63.5.
        val svc = FxRateService(
            FxRateCache(MapSettings()),
            jsonClient("""{"base":"SGD","rates":{"INR":63.5,"USD":0.74}}"""),
            now = { 1_000L },
        )
        assertEquals(1.0 / 63.5, svc.rate("INR", "SGD")!!, 1e-9)
    }

    @Test
    fun caches_within_a_day_and_does_not_refetch() = runTest {
        var calls = 0
        val cache = FxRateCache(MapSettings())
        val svc = FxRateService(cache, jsonClient("""{"base":"SGD","rates":{"INR":63.5}}""") { calls++ }, now = { 1_000L })
        svc.rate("INR", "SGD")
        svc.rate("INR", "SGD")
        assertEquals(1, calls)
    }

    @Test
    fun currency_not_in_table_returns_null() = runTest {
        val svc = FxRateService(
            FxRateCache(MapSettings()),
            jsonClient("""{"base":"SGD","rates":{"INR":63.5}}"""),
            now = { 1_000L },
        )
        assertNull(svc.rate("JPY", "SGD"))
    }

    @Test
    fun falls_back_to_stale_cache_when_fetch_fails() = runTest {
        val cache = FxRateCache(MapSettings())
        // Seed a snapshot that's older than a day.
        cache.write(FxSnapshot(base = "SGD", fetchedAtMillis = 0L, rates = mapOf("INR" to 63.5)))
        val failing = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        // now() is well past the freshness window, so it tries to refetch, fails, then uses stale cache.
        val svc = FxRateService(cache, failing, now = { 5L * 24 * 60 * 60 * 1000 })
        assertEquals(1.0 / 63.5, svc.rate("INR", "SGD")!!, 1e-9)
    }

    @Test
    fun returns_null_when_offline_with_no_cache() = runTest {
        val failing = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val svc = FxRateService(FxRateCache(MapSettings()), failing, now = { 1_000L })
        assertNull(svc.rate("INR", "SGD"))
    }
}
