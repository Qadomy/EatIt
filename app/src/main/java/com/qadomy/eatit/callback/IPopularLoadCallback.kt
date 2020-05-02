package com.qadomy.eatit.callback

import com.qadomy.eatit.model.PopularCategoryModel

interface IPopularLoadCallback {
    fun onPopularLoadSuccess(popularModelList: List<PopularCategoryModel>)
    fun onPopularLoadFailed(message: String)
}