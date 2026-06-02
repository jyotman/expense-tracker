package app.spent

import androidx.compose.ui.window.ComposeUIViewController
import app.spent.ui.App

fun MainViewController() = ComposeUIViewController {
    AppInitializer.init()
    // Seed + roll recurring forward (cheap; iOS has no widget/service entry point).
    ServiceLocator.onStart()
    App()
}
