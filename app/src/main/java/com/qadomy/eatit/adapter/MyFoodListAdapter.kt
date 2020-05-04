package com.qadomy.eatit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.qadomy.eatit.R
import com.qadomy.eatit.model.FoodModel

class MyFoodListAdapter(
    internal var context: Context,
    internal var foodList: List<FoodModel>
) : RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>() {

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

    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var txtFoodName: TextView? = null
        var txtFoodPrice: TextView? = null
        var imgFoodimage: ImageView? = null
        var imgFav: ImageView? = null
        var imgCart: ImageView? = null


        init {
            txtFoodName = itemView.findViewById(R.id.txt_food_name)
            txtFoodPrice = itemView.findViewById(R.id.txt_food_price)
            imgFoodimage = itemView.findViewById(R.id.img_food_image)
            imgFav = itemView.findViewById(R.id.img_fav)
            imgCart = itemView.findViewById(R.id.img_quick_cart)

        }

    }

}