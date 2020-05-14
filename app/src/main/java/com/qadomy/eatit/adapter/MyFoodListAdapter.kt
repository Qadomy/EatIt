package com.qadomy.eatit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.qadomy.eatit.R
import com.qadomy.eatit.callback.IRecycleItemClickListener
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.database.CartDataSource
import com.qadomy.eatit.database.CartDatabase
import com.qadomy.eatit.database.CartItem
import com.qadomy.eatit.database.LocalCartDataSource
import com.qadomy.eatit.eventbus.CountCartEvent
import com.qadomy.eatit.eventbus.FoodItemClick
import com.qadomy.eatit.model.FoodModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

class MyFoodListAdapter(
    internal var context: Context,
    internal var foodList: List<FoodModel>
) : RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {


    private val compositeDisposable: CompositeDisposable
    private val cartDataSource: CartDataSource

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyFoodListAdapter.MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.layout_food_item, parent, false)
        )
    }

    override fun getItemCount() = foodList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(foodList.get(position).image)
            .into(holder.imgFoodimage!!)
        holder.txtFoodName!!.text = foodList.get(position).name
        holder.txtFoodPrice!!.text = foodList.get(position).price.toString()


        // Event Bus
        holder.setListener(object : IRecycleItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.FOOD_SELECTED = foodList.get(pos)
                Common.FOOD_SELECTED!!.key = pos.toString()
                EventBus.getDefault().postSticky(FoodItemClick(true, foodList[pos]))
            }
        })


        // when user click on cart icon in food list, we will add item to cart
        holder.imgCart!!.setOnClickListener {
            val cartItem = CartItem()

            cartItem.uid = Common.currentUser!!.uid
            cartItem.userPhone = Common.currentUser!!.phone

            cartItem.foodId = foodList[position].id!!
            cartItem.foodName = foodList[position].name!!
            cartItem.foodImage = foodList[position].image!!
            cartItem.foodPrice = foodList[position].price!!.toDouble()
            cartItem.foodQuantity = 1
            cartItem.foodExtraPrice = 0.0
            cartItem.foodAddon = "Default"
            cartItem.foodSize = "Default"


            //
            compositeDisposable.add(
                cartDataSource.insertOrReplace(cartItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Toast.makeText(context, "Add to cart success", Toast.LENGTH_SHORT).show()

                        // here we send notify to Home Activity to update counter fab
                        EventBus.getDefault().postSticky(CountCartEvent(true))

                    }, { t: Throwable? ->
                        Toast.makeText(context, "[INSERT CART]" + t!!.message, Toast.LENGTH_SHORT)
                            .show()
                    })
            )
        }

    }

    // fun for disconnect compositeDisposable
    fun onStop() {
        compositeDisposable?.clear()
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var txtFoodName: TextView? = null
        var txtFoodPrice: TextView? = null
        var imgFoodimage: ImageView? = null
        var imgFav: ImageView? = null
        var imgCart: ImageView? = null


        // for recycle item click
        internal var listener: IRecycleItemClickListener? = null

        fun setListener(listener: IRecycleItemClickListener) {
            this.listener = listener
        }


        init {
            txtFoodName = itemView.findViewById(R.id.txt_food_name)
            txtFoodPrice = itemView.findViewById(R.id.txt_food_price)
            imgFoodimage = itemView.findViewById(R.id.img_food_image)
            imgFav = itemView.findViewById(R.id.img_fav)
            imgCart = itemView.findViewById(R.id.img_quick_cart)

            // here when click in item
            itemView.setOnClickListener(this)

        }

        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!, adapterPosition)
        }

    }

}