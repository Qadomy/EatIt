package com.qadomy.eatit.callback

import com.qadomy.eatit.model.CategoryModel

interface ICategoryCallback {
    fun onCategoryLoadSuccess(categoryList: List<CategoryModel>)
    fun onCategoryLoadFailed(message: String)
}