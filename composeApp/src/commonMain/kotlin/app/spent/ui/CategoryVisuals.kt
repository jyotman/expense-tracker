package app.spent.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun iconForKey(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Filled.Restaurant
    "groceries" -> Icons.Filled.ShoppingCart
    "transport" -> Icons.Filled.DirectionsCar
    "shopping" -> Icons.Filled.ShoppingBag
    "bills" -> Icons.Filled.ReceiptLong
    "entertainment" -> Icons.Filled.Movie
    "health" -> Icons.Filled.Favorite
    "travel" -> Icons.Filled.Flight
    "home" -> Icons.Filled.Home
    "education" -> Icons.Filled.School
    "personal" -> Icons.Filled.Person
    "coffee" -> Icons.Filled.LocalCafe
    "fuel" -> Icons.Filled.LocalGasStation
    "gift" -> Icons.Filled.CardGiftcard
    "pets" -> Icons.Filled.Pets
    "fitness" -> Icons.Filled.FitnessCenter
    "phone" -> Icons.Filled.Smartphone
    else -> Icons.Filled.Category
}

/** Parse a "#RRGGBB" hex string to a Compose Color, falling back to grey. */
fun colorFromHex(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    return try {
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> Color(0xFF546E7A)
        }
    } catch (_: Exception) {
        Color(0xFF546E7A)
    }
}
