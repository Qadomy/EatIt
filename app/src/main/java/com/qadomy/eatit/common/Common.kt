package com.qadomy.eatit.common

import com.qadomy.eatit.model.CategoryModel
import com.qadomy.eatit.model.UserModel

object Common {
    var CATEGORY_SELECTED: CategoryModel? = null
    val DEFAULT_COLUMN_COUNT: Int = 0
    val FULL_WIDTH_COLUMN: Int = 1
    const val BESST_DEALS_REF: String = "BestDeals"
    const val POPULAR_REF: String = "MostPopular"
    const val USER_REFERENCE = "Users"
    val CATEGORY_REF: String = "Category"
    var currentUser: UserModel? = null
}