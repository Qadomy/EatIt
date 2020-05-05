package com.qadomy.eatit.ui.fooddetails

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.CommentModel
import com.qadomy.eatit.model.FoodModel

class FoodDetailsViewModel : ViewModel() {

    private var mutableLiveDataFood: MutableLiveData<FoodModel>? = null
    private var mutableLiveDataComment: MutableLiveData<CommentModel>? = null


    init {
        mutableLiveDataComment = MutableLiveData()
    }

    fun getMutableLiveDataFood(): MutableLiveData<FoodModel> {
        if (mutableLiveDataFood == null) {
            mutableLiveDataFood = MutableLiveData()
        }
        mutableLiveDataFood!!.value = Common.FOOD_SELECTED
        return mutableLiveDataFood!!
    }

    fun getMutableLiveDataComment(): MutableLiveData<CommentModel> {
        if (mutableLiveDataComment == null) {
            mutableLiveDataComment = MutableLiveData()
        }
        return mutableLiveDataComment!!
    }

    fun setCommnetModel(commentModel: CommentModel) {
        if (mutableLiveDataComment != null) {
            mutableLiveDataComment!!.value = (commentModel)
        }

    }

    fun setFoodModel(foodModel: FoodModel) {
        if (mutableLiveDataFood != null) {
            mutableLiveDataFood!!.value = foodModel
        }
    }

}
