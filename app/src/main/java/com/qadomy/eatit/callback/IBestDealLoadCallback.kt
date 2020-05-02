package com.qadomy.eatit.callback

import com.qadomy.eatit.model.BestDealModel

interface IBestDealLoadCallback {
    fun onBestDealLoadSuccess(bestDealModel: List<BestDealModel>)
    fun onBestDealLoadFailed(message: String)
}