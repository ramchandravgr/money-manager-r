package com.rrparallelstudio.miapp.core.model.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps persisted [iconName] keys to Material icons for categories and subcategories. */
object MmrCategoryIconCatalog {
    const val DEFAULT_ICON_NAME = "category_default"

    val pickerOptions: List<String> =
        listOf(
            DEFAULT_ICON_NAME,
            "restaurant",
            "local_cafe",
            "shopping_cart",
            "shopping_bag",
            "directions_car",
            "local_gas_station",
            "directions_bus",
            "flight",
            "receipt_long",
            "electric_bolt",
            "home",
            "medical_services",
            "local_hospital",
            "movie",
            "sports_esports",
            "fitness_center",
            "payments",
            "trending_up",
            "account_balance",
            "work",
            "school",
            "child_care",
            "pets",
            "eco",
            "card_giftcard",
            "help_outline",
        )

    fun resolve(iconName: String): ImageVector =
        when (iconName) {
            "restaurant" -> Icons.Filled.Restaurant
            "local_cafe" -> Icons.Filled.LocalCafe
            "shopping_cart" -> Icons.Filled.ShoppingCart
            "shopping_bag" -> Icons.Filled.ShoppingBag
            "directions_car" -> Icons.Filled.DirectionsCar
            "local_gas_station" -> Icons.Filled.LocalGasStation
            "directions_bus" -> Icons.Filled.DirectionsBus
            "flight" -> Icons.Filled.Flight
            "receipt_long" -> Icons.Filled.ReceiptLong
            "electric_bolt" -> Icons.Filled.ElectricBolt
            "home" -> Icons.Filled.Home
            "medical_services" -> Icons.Filled.MedicalServices
            "local_hospital" -> Icons.Filled.LocalHospital
            "movie" -> Icons.Filled.Movie
            "sports_esports" -> Icons.Filled.SportsEsports
            "fitness_center" -> Icons.Filled.FitnessCenter
            "payments" -> Icons.Filled.Payments
            "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
            "account_balance" -> Icons.Filled.AccountBalance
            "work" -> Icons.Filled.Work
            "school" -> Icons.Filled.School
            "child_care" -> Icons.Filled.ChildCare
            "pets" -> Icons.Filled.Pets
            "eco" -> Icons.Filled.Eco
            "card_giftcard" -> Icons.Filled.CardGiftcard
            "help_outline" -> Icons.Outlined.HelpOutline
            else -> Icons.Filled.Category
        }
}
