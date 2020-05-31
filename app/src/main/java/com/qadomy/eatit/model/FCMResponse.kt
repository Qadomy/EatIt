package com.qadomy.eatit.model

class FCMResponse {

    var multiCastId: Long? = 0
    var success: Int = 0
    var failure: Int = 0
    var canonicalIds: Int = 0
    var result: List<FCMResult>? = null
    var messageId: Long = 0
}
