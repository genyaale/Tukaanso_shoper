/*
 * Tukaanso Shopper - Product Capture Script
 * -----------------------------------------
 * Waxa lagu shubaa (inject) bogagga dukaamada (SHEIN, Amazon, Temu, AliExpress)
 * markay WebView-ku dhammeeyo soo dejinta bogga (onPageFinished).
 *
 * window.tukaansoCapture() waxay akhridaa bogga hadda furan oo soo celisaa
 * macluumaadka alaabta loo baahan yahay si loogu daro app cart-ka.
 *
 * Habka uu u shaqeeyo (ordered fallbacks):
 *   1. JSON-LD <script type="application/ld+json"> (schema.org/Product)
 *   2. OpenGraph / product meta tags (og:title, og:image, og:price:amount...)
 *   3. Generic DOM scan for price / size / color elements
 */
(function () {

  function getMeta(name) {
    var el = document.querySelector('meta[property="' + name + '"]') ||
             document.querySelector('meta[name="' + name + '"]');
    return el ? el.getAttribute('content') : null;
  }

  function getJsonLdProduct() {
    var scripts = document.querySelectorAll('script[type="application/ld+json"]');
    for (var i = 0; i < scripts.length; i++) {
      try {
        var data = JSON.parse(scripts[i].textContent);
        var items = Array.isArray(data) ? data : [data];
        for (var j = 0; j < items.length; j++) {
          var item = items[j];
          if (item && item['@graph']) {
            var found = item['@graph'].filter(function (g) {
              return g && (g['@type'] === 'Product' || (Array.isArray(g['@type']) && g['@type'].indexOf('Product') !== -1));
            });
            if (found.length) item = found[0];
          }
          if (item && (item['@type'] === 'Product' || (item.offers && item.name))) {
            return item;
          }
        }
      } catch (e) { /* ignore malformed JSON-LD blocks */ }
    }
    return null;
  }

  // Looks for the currently-selected Size or Color option on the page.
  // Most shopping sites mark the chosen variant with classes/attributes
  // like "selected", "active", "chosen", aria-selected="true", etc.
  function findSelectedOption(keywords) {
    var candidates = document.querySelectorAll(
      '[aria-selected="true"], [aria-checked="true"], ' +
      '[class*="selected" i], [class*="active" i], [class*="chosen" i], [class*="checked" i]'
    );
    for (var i = 0; i < candidates.length; i++) {
      var el = candidates[i];
      var container = el.closest(
        '[class*="size" i], [class*="color" i], [class*="colour" i], ' +
        '[data-qa-anchor*="size" i], [data-qa-anchor*="color" i], section, fieldset, div'
      );
      var contextText = ((container ? container.textContent : '') + ' ' + (el.getAttribute('class') || '')).toLowerCase();
      for (var k = 0; k < keywords.length; k++) {
        if (contextText.indexOf(keywords[k]) !== -1) {
          var label = el.getAttribute('title') ||
                      el.getAttribute('aria-label') ||
                      (el.textContent || '').trim();
          if (label && label.length < 60) return label;
        }
      }
    }
    return null;
  }

  function parsePriceText(text) {
    if (!text) return null;
    var match = text.replace(/,/g, '').match(/(\d+(\.\d+)?)/);
    if (!match) return null;
    var num = parseFloat(match[1]);
    return isNaN(num) ? null : num;
  }

  function extractPrice() {
    var jsonLd = getJsonLdProduct();
    if (jsonLd && jsonLd.offers) {
      var offers = Array.isArray(jsonLd.offers) ? jsonLd.offers[0] : jsonLd.offers;
      if (offers && offers.price) {
        var p = parsePriceText(String(offers.price));
        if (p !== null) return { price: p, currency: offers.priceCurrency || 'USD' };
      }
    }

    var ogPrice = getMeta('og:price:amount') || getMeta('product:price:amount');
    var ogCurrency = getMeta('og:price:currency') || getMeta('product:price:currency');
    if (ogPrice) {
      var p2 = parsePriceText(ogPrice);
      if (p2 !== null) return { price: p2, currency: ogCurrency || 'USD' };
    }

    // Generic fallback: scan common "price" elements on the page.
    var priceEls = document.querySelectorAll(
      '[class*="price" i] [class*="amount" i], ' +
      '[itemprop="price"], [data-price], [class*="price" i]'
    );
    for (var i = 0; i < priceEls.length; i++) {
      var el = priceEls[i];
      var raw = el.getAttribute('content') || el.getAttribute('data-price') || el.textContent;
      var p3 = parsePriceText(raw);
      if (p3 !== null && p3 > 0) {
        return { price: p3, currency: ogCurrency || 'USD' };
      }
    }

    return { price: 0, currency: ogCurrency || 'USD' };
  }

  function extractImage(jsonLd) {
    var img = jsonLd && jsonLd.image;
    if (Array.isArray(img)) img = img[0];
    if (img && typeof img === 'object' && img.url) img = img.url;
    if (!img) img = getMeta('og:image');
    if (!img) {
      var ogImgEl = document.querySelector('img[class*="main" i], img[class*="product" i]');
      if (ogImgEl) img = ogImgEl.src;
    }
    return img || '';
  }

  function extractName(jsonLd) {
    var name = (jsonLd && jsonLd.name) || getMeta('og:title');
    if (!name) {
      var h1 = document.querySelector('h1');
      name = h1 ? h1.textContent : document.title;
    }
    return (name || '').trim().slice(0, 250);
  }

  window.tukaansoCapture = function () {
    var jsonLd = getJsonLdProduct();
    var priceInfo = extractPrice();

    var product = {
      name: extractName(jsonLd),
      url: window.location.href,
      image: extractImage(jsonLd),
      price: priceInfo.price,
      currency: priceInfo.currency,
      size: findSelectedOption(['size']) || '',
      color: findSelectedOption(['color', 'colour']) || '',
      quantity: 1
    };

    if (window.AndroidCart && window.AndroidCart.onProductCaptured) {
      window.AndroidCart.onProductCaptured(JSON.stringify(product));
    }
    return product;
  };

  true; // evaluateJavascript needs a return value
})();
