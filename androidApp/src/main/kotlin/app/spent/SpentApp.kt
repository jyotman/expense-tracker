package app.spent

import android.app.Application

/** Initializes storage + database for all entry points (Activity, notification service, widget). */
class SpentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppInitializer.init(this)
        // Seed default categories + roll recurring expenses forward, off the main thread.
        Thread { ServiceLocator.onStart() }.start()
    }
}
