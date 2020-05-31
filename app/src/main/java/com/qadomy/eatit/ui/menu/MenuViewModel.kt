package com.qadomy.eatit.ui.menu

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.qadomy.eatit.callback.ICategoryCallback
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.CategoryModel

class MenuViewModel : ViewModel(), ICategoryCallback {

    private var categoriesListMutable: MutableLiveData<List<CategoryModel>>? = null
    private var messageError: MutableLiveData<String> = MutableLiveData()
    private val categoryCallbackListener: ICategoryCallback

    init {
        categoryCallbackListener = this
    }

    fun getCategoryList(): MutableLiveData<List<CategoryModel>> {
        if (categoriesListMutable == null) {
            categoriesListMutable = MutableLiveData()
            loadCategory()
        }
        return categoriesListMutable!!
    }


    fun getMessageError(): MutableLiveData<String> {
        return messageError
    }

    fun loadCategory() {
        val tempList = ArrayList<CategoryModel>()
        val categoryRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                categoryCallbackListener.onCategoryLoadFailed(p0.message!!)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (itemSnaphot in p0!!.children) {
                    val model = itemSnaphot.getValue<CategoryModel>(CategoryModel::class.java)
                    model!!.menuId = itemSnaphot.key
                    tempList.add(model!!)
                }
                categoryCallbackListener.onCategoryLoadSuccess(tempList)
            }

        })

    }


    override fun onCategoryLoadSuccess(categoryList: List<CategoryModel>) {
        categoriesListMutable!!.value = categoryList
    }

    override fun onCategoryLoadFailed(message: String) {
        messageError.value = message
    }
}
