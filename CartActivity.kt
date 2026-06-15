package com.tukaanso.shopper

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tukaanso.shopper.adapter.CartAdapter
import com.tukaanso.shopper.data.AppDatabase
import com.tukaanso.shopper.databinding.ActivityCartBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Cart-ka gaarka ah ee app-ka (kama tirsana SHEIN/Amazon/Temu/AliExpress).
 *
 * - Liiska alaabta ee la soo qaaday waxa lagu kaydiyaa Room (AppDatabase).
 * - Grand Total = isu geynta totalPrice ee dhammaan alaabta cart-ka ku jirta.
 * - Checkout-ku wuxuu ku socdaa app-ka, kama dhacayo SHEIN/Amazon/iwm.
 */
class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val dao by lazy { AppDatabase.getInstance(applicationContext).cartDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CartAdapter(
            items = mutableListOf(),
            onQuantityChanged = { item ->
                lifecycleScope.launch {
                    dao.update(item)
                    updateTotal()
                }
            },
            onRemove = { item ->
                lifecycleScope.launch { dao.delete(item) }
            }
        )

        binding.recyclerCart.layoutManager = LinearLayoutManager(this)
        binding.recyclerCart.adapter = adapter

        lifecycleScope.launch {
            dao.getAll().collectLatest { items ->
                adapter.updateItems(items)
                updateTotal()
                binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerCart.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        binding.btnCheckout.setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
        }
    }

    private fun updateTotal() {
        binding.tvGrandTotal.text = String.format(Locale.US, "Total: $%.2f", adapter.getTotal())
    }
}
