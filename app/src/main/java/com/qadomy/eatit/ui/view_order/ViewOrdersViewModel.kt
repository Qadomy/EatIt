package com.qadomy.eatit.ui.view_order

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.qadomy.eatit.model.Order

class ViewOrdersViewModel : ViewModel() {
    val mutableLiveDataOrderList: MutableLiveData<List<Order>>

    init {
        mutableLiveDataOrderList = MutableLiveData()
    }

    fun setMutableLiveDAtaOrderList(orderList: List<Order>) {
        mutableLiveDataOrderList.value = orderList
    }
}
