package com.tukaanso.shopper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tukaanso.shopper.data.AppDatabase
import com.tukaanso.shopper.data.CartItem
import com.tukaanso.shopper.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Bogga koowaad ee app-ka.
 *
 * - Spinner-ka kor ku yaal wuxuu u oggolaanayaa isticmaaluhu inuu u beddelo
 *   dukaanka (SHEIN / Amazon / Temu / AliExpress).
 * - WebView-ku waa browser ku jira app-ka, halkaas ayuu isticmaaluhu ka
 *   baadhayaa alaabta.
 * - Marka boggu dhammaado soo dejinta (onPageFinished), capture_script.js
 *   waxa lagu shubaa (evaluateJavascript) si window.tukaansoCapture() loo
 *   diyaariyo.
 * - Marka "Capture Current Product & Add to App Cart" la riixo, waxaan
 *   wici doonaa tukaansoCapture() oo soo celisa JSON ah, kaas oo loo
 *   dirayo AndroidCart.onProductCaptured(json) -> handleCapturedProduct().
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private var captureScript: String = ""

    // Dukaamada lagu taageero hadda. Si fudud loogu dari karaa kuwo kale.
    private val stores = linkedMapOf(
        "SHEIN" to "https://www.shein.com",
        "Amazon" to "https://www.amazon.com",
        "Temu" to "https://www.temu.com",
        "AliExpress" to "https://www.aliexpress.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        captureScript = assets.open("capture_script.js").bufferedReader().use { it.readText() }

        setupStoreSelector()
        setupWebView()
        setupButtons()
    }

    private fun setupStoreSelector() {
        val names = stores.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        binding.storeSpinner.adapter = adapter
        binding.storeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val url = stores.values.toList()[position]
                if (webView.url?.startsWith(url) != true) {
                    webView.loadUrl(url)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupWebView() {
        webView = binding.webView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = webView.settings.userAgentString + " TukaansoShopper/1.0"

        // Halkan ayaa lagu daraa "AndroidCart" - capture_script.js wuxuu u
        // yeeraa window.AndroidCart.onProductCaptured(json)
        webView.addJavascriptInterface(ShoppingWebInterface(this), "AndroidCart")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Shub (inject) function-ka qaban kara akhrinta bogga.
                webView.evaluateJavascript(captureScript, null)
            }
        }

        webView.loadUrl(stores.values.first())
    }

    private fun setupButtons() {
        binding.fabCapture.setOnClickListener {
            webView.evaluateJavascript("tukaansoCapture();", null)
        }
        binding.fabCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
    }

    /**
     * Waxa loo yeeraa marka JS-ka bogga (SHEIN/Amazon/Temu/AliExpress) uu
     * soo diro xogta alaabta hadda la xulanayo.
     *
     * JSON-ka soo socda wuxuu ka kooban yahay:
     *   name, url, image, price, currency, size, color, quantity
     */
    fun handleCapturedProduct(json: String) {
        try {
            val obj = JSONObject(json)
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Lama heli karo magaca alaabta. Fadlan tag bogga alaabta oo isku day mar kale.", Toast.LENGTH_LONG).show()
                return
            }

            val currentStore = binding.storeSpinner.selectedItem?.toString() ?: ""

            val item = CartItem(
                name = name,
                imageUrl = obj.optString("image", ""),
                productUrl = obj.optString("url", ""),
                unitPrice = obj.optDouble("price", 0.0),
                currency = obj.optString("currency", "USD"),
                size = obj.optString("size", ""),
                color = obj.optString("color", ""),
                quantity = obj.optInt("quantity", 1).coerceAtLeast(1),
                store = currentStore
            )

            lifecycleScope.launch {
                AppDatabase.getInstance(applicationContext).cartDao().insert(item)
            }

            Toast.makeText(this, "\"${item.name}\" waxaa lagu daray cart-ka ✅", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Khalad ayaa dhacay marka xogta alaabta la akhrinayay.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
