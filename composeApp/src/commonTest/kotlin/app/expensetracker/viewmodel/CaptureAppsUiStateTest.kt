package app.expensetracker.viewmodel

import app.expensetracker.platform.InstalledApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaptureAppsUiStateTest {

    private fun app(label: String, pkg: String = "com.$label") = InstalledApp(packageName = pkg, label = label)

    @Test
    fun selected_apps_appear_before_unselected() {
        // apps arrive pre-sorted alphabetically from PlatformCapabilities
        val state = CaptureAppsUiState(
            apps = listOf(app("Paytm"), app("PhonePe"), app("Zomato")),
            selectedPackages = setOf("com.PhonePe"),
        )
        assertEquals(listOf(app("PhonePe")), state.selectedApps)
        assertEquals(listOf(app("Paytm"), app("Zomato")), state.unselectedApps)
    }

    @Test
    fun selected_and_unselected_preserve_alphabetical_order_from_apps_list() {
        val state = CaptureAppsUiState(
            // apps already sorted alphabetically (as PlatformCapabilities delivers them)
            apps = listOf(app("Amazon"), app("HDFC"), app("Paytm"), app("Zomato")),
            selectedPackages = setOf("com.HDFC", "com.Zomato"),
        )
        assertEquals(listOf(app("HDFC"), app("Zomato")), state.selectedApps)
        assertEquals(listOf(app("Amazon"), app("Paytm")), state.unselectedApps)
    }

    @Test
    fun all_selected_means_empty_unselected() {
        val apps = listOf(app("A"), app("B"))
        val state = CaptureAppsUiState(
            apps = apps,
            selectedPackages = setOf("com.A", "com.B"),
        )
        assertEquals(apps, state.selectedApps)
        assertTrue(state.unselectedApps.isEmpty())
    }

    @Test
    fun none_selected_means_empty_selected() {
        val apps = listOf(app("A"), app("B"))
        val state = CaptureAppsUiState(apps = apps, selectedPackages = emptySet())
        assertTrue(state.selectedApps.isEmpty())
        assertEquals(apps, state.unselectedApps)
    }

    @Test
    fun search_filters_across_both_groups() {
        val state = CaptureAppsUiState(
            apps = listOf(app("HDFC Bank", "com.hdfc"), app("Paytm", "com.paytm"), app("PhonePe", "com.phonepe")),
            selectedPackages = setOf("com.hdfc", "com.paytm"),
            searchQuery = "pay",
        )
        // "Paytm" matches selected; "PhonePe" does not match; "HDFC Bank" does not match
        assertEquals(listOf(app("Paytm", "com.paytm")), state.selectedApps)
        assertEquals(emptyList(), state.unselectedApps)
    }

    @Test
    fun search_by_package_name_is_included_in_both_groups() {
        val state = CaptureAppsUiState(
            apps = listOf(app("Bank App", "com.hdfc.bank"), app("Wallet", "com.paytm.wallet")),
            selectedPackages = setOf("com.hdfc.bank"),
            searchQuery = "hdfc",
        )
        assertEquals(listOf(app("Bank App", "com.hdfc.bank")), state.selectedApps)
        assertTrue(state.unselectedApps.isEmpty())
    }

    @Test
    fun empty_apps_list_produces_empty_groups() {
        val state = CaptureAppsUiState(apps = emptyList(), selectedPackages = emptySet())
        assertTrue(state.selectedApps.isEmpty())
        assertTrue(state.unselectedApps.isEmpty())
    }
}
