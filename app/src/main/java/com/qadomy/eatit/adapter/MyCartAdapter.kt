package com.qadomy.eatit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.qadomy.eatit.R
import com.qadomy.eatit.database.CartDataSource
import com.qadomy.eatit.database.CartDatabase
import com.qadomy.eatit.database.CartItem
import com.qadomy.eatit.database.LocalCartDataSource
import com.qadomy.eatit.eventbus.UpdateItemInCart
import io.reactivex.disposables.CompositeDisposable
import org.greenrobot.eventbus.EventBus

class MyCartAdapter(
    internal var context: Context,
    internal var cartItems: List<CartItem>
) : RecyclerView.Adapter<MyCartAdapter.MyViewHolder>() {

    internal var compositeDisposable: CompositeDisposable
    internal var cartDataSource: CartDataSource

    init {
        compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.layout_cart_item, parent, false)
        )
    }

    override fun getItemCount() = cartItems.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        Glide.with(context).load(cartItems[position].foodImage).into(holder.imgCart)
        holder.textFoodName.text = StringBuilder(cartItems[position].foodName!!)
        holder.textFoodPrice.text =
            StringBuilder("").append(cartItems[position].foodPrice + cartItems[position].foodExtraPrice)
        holder.numberButton.number = cartItems[position].foodQuantity.toString()

        // Event, when click in elegant number button
        holder.numberButton.setOnValueChangeListener { view, oldValue, newValue ->
            cartItems[position].foodQuantity = newValue
            EventBus.getDefault().postSticky(UpdateItemInCart(cartItems[position]))
        }

    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var imgCart: ImageView
        lateinit var textFoodName: TextView
        lateinit var textFoodPrice: TextView
        lateinit var numberButton: ElegantNumberButton

        init {
            imgCart = itemView.findViewById(R.id.img_cart) as ImageView
            textFoodName = itemView.findViewById(R.id.txt_food_name) as TextView
            textFoodPrice = itemView.findViewById(R.id.txt_food_price) as TextView
            numberButton = itemView.findViewById(R.id.elegant_number_button) as ElegantNumberButton
        }
    }

}