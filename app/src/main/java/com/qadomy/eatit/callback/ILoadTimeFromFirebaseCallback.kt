package com.qadomy.eatit.callback

import com.qadomy.eatit.model.Order

interface ILoadTimeFromFirebaseCallback {
    /**
     *
     * This callback for sync local time with server time
     * Sync our phone time with server rime eo make sure this time is real
     */


    fun onLoadTimeSuccess(order: Order, estimatedTimeMs: Long)
    fun onLoadTimeFailed(message: String)
}