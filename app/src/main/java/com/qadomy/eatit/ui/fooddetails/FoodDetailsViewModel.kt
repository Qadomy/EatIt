package com.qadomy.eatit.ui.fooddetails

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.FoodModel

class FoodDetailsViewModel : ViewModel() {

    private var mutableLiveDataFood: MutableLiveData<FoodModel>? = null

    fun getMutableLiveDataFood(): MutableLiveData<FoodModel> {
        if (mutableLiveDataFood == null) {
            mutableLiveDataFood = MutableLiveData()
        }
        mutableLiveDataFood!!.value = Common.FOOD_SELECTED
        return mutableLiveDataFood!!
    }

}
