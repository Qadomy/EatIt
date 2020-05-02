package com.qadomy.eatit.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.qadomy.eatit.callback.IBestDealLoadCallback
import com.qadomy.eatit.callback.IPopularLoadCallback
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.BestDealModel
import com.qadomy.eatit.model.PopularCategoryModel

class HomeViewModel : ViewModel(), IPopularLoadCallback, IBestDealLoadCallback {


    // for popular category
    private var popularListMutableLiveData: MutableLiveData<List<PopularCategoryModel>>? = null
    private lateinit var messageError: MutableLiveData<String>
    private var popularLoadCallbackListener: IPopularLoadCallback

    // for best deals
    private var bestDealCallbackListener: IBestDealLoadCallback
    private var bestDealListMutableLiveData: MutableLiveData<List<BestDealModel>>? = null


    /** init */
    init {
        popularLoadCallbackListener = this
        bestDealCallbackListener = this
    }

    /**
     * popular list
     * */
    val popularList: LiveData<List<PopularCategoryModel>>
        get() {
            if (popularListMutableLiveData == null) {
                popularListMutableLiveData = MutableLiveData()
                messageError = MutableLiveData()
                loadPopularList()
            }
            return popularListMutableLiveData!!
        }

    private fun loadPopularList() {
        val tempList = ArrayList<PopularCategoryModel>()
        val popularRef = FirebaseDatabase.getInstance().getReference(Common.POPULAR_REF)
        popularRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                popularLoadCallbackListener.onPopularLoadFailed(p0.message!!)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (itemSnapShot in p0!!.children) {
                    val model =
                        itemSnapShot.getValue<PopularCategoryModel>(PopularCategoryModel::class.java)
                    tempList.add(model!!)
                }
                popularLoadCallbackListener.onPopularLoadSuccess(tempList)
            }

        })
    }

    override fun onPopularLoadSuccess(popularModelList: List<PopularCategoryModel>) {
        popularListMutableLiveData!!.value = popularModelList
    }

    override fun onPopularLoadFailed(message: String) {
        messageError.value = message
    }


    /**
     * best deals list
     * */
    val bestDealList: LiveData<List<BestDealModel>>
        get() {
            if (bestDealListMutableLiveData == null) {
                bestDealListMutableLiveData = MutableLiveData()
                messageError = MutableLiveData()
                loadBestDealList()
            }
            return bestDealListMutableLiveData!!
        }

    private fun loadBestDealList() {
        val tempList = ArrayList<BestDealModel>()
        val bestDealRef = FirebaseDatabase.getInstance().getReference(Common.BESST_DEALS_REF)
        bestDealRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                bestDealCallbackListener.onBestDealLoadFailed(p0.message!!)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (itemSnaphot in p0!!.children) {
                    val model = itemSnaphot.getValue<BestDealModel>(BestDealModel::class.java)
                    tempList.add(model!!)
                }
                bestDealCallbackListener.onBestDealLoadSuccess(tempList)
            }

        })
    }

    override fun onBestDealLoadSuccess(bestDealModel: List<BestDealModel>) {
        bestDealListMutableLiveData!!.value = bestDealModel
    }

    override fun onBestDealLoadFailed(message: String) {
        messageError.value = message
    }


}