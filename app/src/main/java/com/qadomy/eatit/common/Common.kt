package com.qadomy.eatit.common

import com.qadomy.eatit.model.*
import java.math.RoundingMode
import java.text.DecimalFormat

object Common {
    fun formatPrice(price: Double): String {
        if (price != 0.toDouble()) {
            val df = DecimalFormat("#,##0.00")
            df.roundingMode = RoundingMode.HALF_UP
            val finalPrice = StringBuilder(df.format(price)).toString()
            return finalPrice.replace(".", ",")
        } else {
            return "0,00"
        }
    }

    fun calculateExtraPrice(
        userSelectedSize: SizeModel?,
        userSelectedAddon: MutableList<AddonModel>?
    ): Double {
        var result: Double = 0.0

        if (userSelectedSize == null && userSelectedAddon == null) return 0.0
        else if (userSelectedSize == null) {
            for (addonModel in userSelectedAddon!!)
                result += addonModel.price!!.toDouble()
            return result
            //
        } else if (userSelectedAddon == null) {
            result = userSelectedSize!!.price.toDouble()
            return result
            //
        } else {
            result = userSelectedSize!!.price.toDouble()
            for (addonModel in userSelectedAddon!!)
                result += addonModel.price!!.toDouble()
            return result
            //
        }
    }

    val COMMENT_REF: String = "Comments"
    var FOOD_SELECTED: FoodModel? = null
    var CATEGORY_SELECTED: CategoryModel? = null
    val DEFAULT_COLUMN_COUNT: Int = 0
    val FULL_WIDTH_COLUMN: Int = 1
    const val BESST_DEALS_REF: String = "BestDeals"
    const val POPULAR_REF: String = "MostPopular"
    const val USER_REFERENCE = "Users"
    val CATEGORY_REF: String = "Category"
    var currentUser: UserModel? = null
}