package app.spent

import android.app.Activity
import java.lang.ref.WeakReference

/** Weakly holds the foreground Activity so flows that need a UI host (Google authorization
 *  consent) can launch from shared code. Set from MainActivity's lifecycle. */
object CurrentActivity {
    private var ref: WeakReference<Activity>? = null

    fun set(activity: Activity?) {
        ref = activity?.let { WeakReference(it) }
    }

    fun get(): Activity? = ref?.get()
}
