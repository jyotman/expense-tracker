package app.spent.capture

/** Dismiss a captured-transaction notification once its expense has been saved (Android only). */
expect fun dismissCaptureNotification(capturedId: Long)
