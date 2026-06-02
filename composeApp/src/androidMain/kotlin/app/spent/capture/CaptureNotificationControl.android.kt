package app.spent.capture

actual fun dismissCaptureNotification(capturedId: Long) = CaptureNotifications.cancel(capturedId)
