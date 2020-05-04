package com.qadomy.eatit.ui.foodlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.FoodModel

class FoodListViewModel : ViewModel() {

    private var mutableFoodModelListData: MutableLiveData<List<FoodModel>>? = null


    fun getMutableFoodListData(): MutableLiveData<List<FoodModel>> {
        if (mutableFoodModelListData == null) {
            mutableFoodModelListData = MutableLiveData()
        }
        mutableFoodModelListData!!.value = Common.CATEGORY_SELECTED!!.foods
        return mutableFoodModelListData!!
    }
}
