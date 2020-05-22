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
import com.qadomy.eatit.callback.IRecycleItemClickListener
import com.qadomy.eatit.eventbus.PopularFoodItemClick
import com.qadomy.eatit.model.PopularCategoryModel
import de.hdodenhof.circleimageview.CircleImageView
import org.greenrobot.eventbus.EventBus

class MyPopularCategoriesAdapter(
    internal var context: Context,
    internal var popularCategoryModels: List<PopularCategoryModel>
) : RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder>() {
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        // when click on item
        override fun onClick(v: View?) {
            listener!!.onItemClick(v!!, adapterPosition)
        }


        var categoryImage: ImageView? = null
        var categoryName: TextView? = null

        // for recycle item click
        internal var  listener: IRecycleItemClickListener? = null

        fun setListener(listener: IRecycleItemClickListener) {
            this.listener = listener
        }

        init {
            categoryImage = itemView.findViewById(R.id.category_image) as CircleImageView
            categoryName = itemView.findViewById(R.id.category_name) as TextView
            itemView.setOnClickListener(this)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.layout_popular_category_items, parent, false)
        )
    }

    override fun getItemCount() = popularCategoryModels.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(popularCategoryModels[position].image)
            .into(holder.categoryImage!!)
        holder.categoryName!!.text = popularCategoryModels[position].name

        // when click on any item in food popular recycler view
        holder.setListener(object : IRecycleItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                EventBus.getDefault().postSticky(PopularFoodItemClick(popularCategoryModels[pos]))
            }
        })
    }
}