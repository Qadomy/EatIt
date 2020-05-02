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
import com.qadomy.eatit.model.PopularCategoryModel

class MyPopularCategoriesAdapter(
    private var context: Context,
    internal var popularCategoryModel: List<PopularCategoryModel>
) : RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder>() {
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var categoryImage: ImageView = itemView.findViewById(R.id.category_image)
        var categoryName: TextView = itemView.findViewById(R.id.category_name)


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.layout_popular_category_items, parent, false)
        )
    }

    override fun getItemCount() = popularCategoryModel.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(popularCategoryModel.get(position).image)
            .into(holder.categoryImage!!)
        holder.categoryName!!.text = popularCategoryModel.get(position).name
    }
}