package com.tukaanso.shopper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Hal shay oo ku jira cart-ka app-ka.
 *
 * Cart-kani kuma jiro SHEIN - waa cart gaar ah oo Tukaanso Shopper leeyahay.
 * totalPrice waxa la xisaabiyaa marka kasta: unitPrice * quantity.
 */
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageUrl: String,
    val productUrl: String,
    val unitPrice: Double,
    val currency: String,
    val size: String,
    val color: String,
    var quantity: Int,
    val store: String = ""
) {
    /** Total Price = Unit Price x Quantity */
    val totalPrice: Double
        get() = unitPrice * quantity
}
