package com.tukaanso.shopper.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.tukaanso.shopper.data.CartItem
import com.tukaanso.shopper.databinding.ItemCartBinding
import java.util.Locale

/**
 * Adapter-ka cart-ka.
 *
 * Marka quantity la beddelo (+ ama -), waxaan:
 *   1. Cusboonaysiinaa item.quantity
 *   2. Xisaabinaa Total Price = Unit Price x Quantity
 *   3. U dirnaa onQuantityChanged si CartActivity ugu kaydiyo Room
 *      kuna cusboonaysiiyo Grand Total-ka.
 */
class CartAdapter(
    private var items: MutableList<CartItem>,
    private val onQuantityChanged: (CartItem) -> Unit,
    private val onRemove: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    fun updateItems(newItems: List<CartItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun getTotal(): Double = items.sumOf { it.totalPrice }

    inner class CartViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]

        fun renderPrices() {
            holder.binding.tvUnitPrice.text = String.format(Locale.US, "Qiimaha hal shay: %.2f %s", item.unitPrice, item.currency)
            holder.binding.tvQuantity.text = item.quantity.toString()
            holder.binding.tvTotalPrice.text = String.format(Locale.US, "Total: %.2f %s", item.totalPrice, item.currency)
        }

        with(holder.binding) {
            tvName.text = item.name
            tvVariant.text = listOfNotNull(
                item.size.takeIf { it.isNotBlank() }?.let { "Size: $it" },
                item.color.takeIf { it.isNotBlank() }?.let { "Color: $it" },
                item.store.takeIf { it.isNotBlank() }
            ).joinToString("   •   ")

            renderPrices()
            ivProduct.load(item.imageUrl)

            btnIncrease.setOnClickListener {
                item.quantity += 1
                renderPrices()
                onQuantityChanged(item)
            }
            btnDecrease.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity -= 1
                    renderPrices()
                    onQuantityChanged(item)
                }
            }
            btnRemove.setOnClickListener { onRemove(item) }
        }
    }

    override fun getItemCount() = items.size
}
