package app.expensetracker.capture

actual fun dismissCaptureNotification(capturedId: Long) = CaptureNotifications.cancel(capturedId)
