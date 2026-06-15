package com.tukaanso.shopper

import android.webkit.JavascriptInterface

/**
 * Bridge u dhexeeya JavaScript-ka bogga (SHEIN/Amazon/Temu/AliExpress) iyo
 * Android-ka. capture_script.js wuxuu u yeeraa window.AndroidCart.onProductCaptured(json)
 * marka isticmaaluhu riixo "Capture Current Product & Add to App Cart".
 */
class ShoppingWebInterface(private val activity: MainActivity) {

    @JavascriptInterface
    fun onProductCaptured(json: String) {
        // JS callbacks arrive on a non-UI thread - hop back to the UI thread.
        activity.runOnUiThread {
            activity.handleCapturedProduct(json)
        }
    }
}
