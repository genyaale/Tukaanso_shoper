package com.tukaanso.shopper

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tukaanso.shopper.data.AppDatabase
import com.tukaanso.shopper.databinding.ActivityCheckoutBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Checkout-ka APP-ka (ma ahan checkout-ka SHEIN/Amazon/Temu/AliExpress).
 *
 * Halkan waxaa lagu xidhi karaa:
 *  - Backend-ka Tukaanso (xareynta dalabka, diiwaan-gelinta macaamiisha, iwm)
 *  - Lacag-bixinta dhabta ah (EVC Plus / Zaad / e-Dahab API)
 */
class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            val items = AppDatabase.getInstance(applicationContext).cartDao().getAll().first()
            val total = items.sumOf { it.totalPrice }
            val itemCount = items.sumOf { it.quantity }

            val lines = StringBuilder()
            items.forEach { item ->
                lines.append("• ${item.name}")
                if (item.size.isNotBlank() || item.color.isNotBlank()) {
                    val variant = listOfNotNull(
                        item.size.takeIf { it.isNotBlank() }?.let { "Size $it" },
                        item.color.takeIf { it.isNotBlank() }?.let { "Color $it" }
                    ).joinToString(", ")
                    lines.append(" ($variant)")
                }
                lines.append(" x${item.quantity} = ")
                lines.append(String.format(Locale.US, "%.2f %s\n", item.totalPrice, item.currency))
            }
            lines.append("\nWadarta Alaabta: $itemCount")
            lines.append(String.format(Locale.US, "\nWadarta Qiimaha: $%.2f", total))

            binding.tvSummary.text = lines.toString()
        }

        binding.btnPlaceOrder.setOnClickListener {
            val paymentMethod = when (binding.paymentGroup.checkedRadioButtonId) {
                binding.rbEvc.id -> "EVC Plus"
                binding.rbZaad.id -> "Zaad"
                binding.rbEdahab.id -> "e-Dahab"
                else -> "Cash on Delivery"
            }

            // TODO: Halkan ku xidh API-ga dalabka (Tukaanso backend) iyo
            // habka lacag-bixinta ee la xushay (paymentMethod).
            Toast.makeText(
                this,
                getString(R.string.order_placed) + " ($paymentMethod)",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
