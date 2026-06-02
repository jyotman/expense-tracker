package app.spent

import android.content.Context

/** Holds the application context for platform features that run outside an Activity
 *  (notification listener service, Glance widget, Drive backup). */
object AppContext {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val context: Context
        get() = appContext ?: error("AppContext not initialized. Call AppContext.init() in Application.onCreate().")
}
