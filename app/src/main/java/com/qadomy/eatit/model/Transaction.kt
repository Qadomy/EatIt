package com.qadomy.eatit.model

class Transaction {

    var id: String? = null
    var status: String? = null
    var type: String? = null
    var currencyIsoCode: String? = null
    var amount: String? = null

    /** Merchant: A merchant is a person who trades in commodities produced by other people */
    var merchantAccountId: String? = null
    var subMerchantAccountId: String? = null
    var masterMerchantAccountId: String? = null
    var carderId: String? = null
    var createAt: String? = null
    var updateAt: String? = null

}
