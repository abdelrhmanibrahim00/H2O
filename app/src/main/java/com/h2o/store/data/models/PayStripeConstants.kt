package com.h2o.store.data.models

/**
 * Constants for PayMob integration
 */
object PayMobConstants {
    // API Key for PayMob integration
    const val API_KEY = "ZXlKaGJHY2lPaUpJVXpVeE1pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SmpiR0Z6Y3lJNklrMWxjbU5vWVc1MElpd2ljSEp2Wm1sc1pWOXdheUk2TVRBek9EZzJOU3dpYm1GdFpTSTZJakUzTkRZek1EY3pOVE11TWpRek9UUTVJbjAucUFwTzRxUF96MzJhY0JhOWktdjg3QnB3SWF2MXRCSEI3TkFhdHVhTENkRVBDSFFWLWNpRUh4RmtlVV9vcDRhaC1PTVhxV1BSdnZQVGNsZmNaQUhDQ3c="

    // Card Integration ID (found in PayMob dashboard)
    const val CARD_INTEGRATION_ID = 5059326

    // Iframe ID for card payments (set in PayMob dashboard)
    const val IFRAME_ID = 915321  // This needs to be changed to your actual iframe ID

    // Test card information
    object TestCard {
        const val NUMBER = "5123456789012346"
        const val NAME = "Test Account"
        const val EXPIRY = "05/25"
        const val CVV = "123"
    }

    // API Endpoints
    object Endpoints {
        const val BASE_URL = "https://accept.paymob.com/api/"
        const val AUTH = "auth/tokens"
        const val ORDER = "ecommerce/orders"
        const val PAYMENT_KEY = "acceptance/payment_keys"
        const val IFRAME_URL = "https://accept.paymob.com/api/acceptance/iframes/"
    }
}