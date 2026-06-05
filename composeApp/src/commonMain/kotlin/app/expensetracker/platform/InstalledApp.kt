package app.expensetracker.platform

/** A non-system app installed on the device — used to populate the capture-apps picker. */
data class InstalledApp(val packageName: String, val label: String)
