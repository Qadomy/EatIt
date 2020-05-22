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
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.eventbus.CategoryClick
import com.qadomy.eatit.model.CategoryModel
import org.greenrobot.eventbus.EventBus

class MyCategoriesAdapter(
    internal var context: Context,
    internal var categoriesList: List<CategoryModel>
) : RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var categoryName: TextView? = null
        var categoryIamge: ImageView? = null

        // for recycle item click
        internal var listener: IRecycleItemClickListener? = null

        fun setListener(listener: IRecycleItemClickListener) {
            this.listener = listener
        }

        init {
            categoryIamge = itemView.findViewById(R.id.category_image)
            categoryName = itemView.findViewById(R.id.category_name)

            // here when click in item
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!, adapterPosition)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context)
                .inflate(R.layout.layout_category_item, parent, false)
        )
    }

    override fun getItemCount() = categoriesList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(categoriesList.get(position).image)
            .into(holder.categoryIamge!!)
        holder.categoryName!!.text = categoriesList.get(position).name

        // Event
        holder.setListener(object : IRecycleItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.CATEGORY_SELECTED = categoriesList[pos]
                EventBus.getDefault().postSticky(CategoryClick(true, categoriesList[pos]))
            }
        })
    }


    // Return the view type of the item at position for the purposes of view recycling
    override fun getItemViewType(position: Int): Int {
        return if (categoriesList.size == 1) {
            Common.DEFAULT_COLUMN_COUNT
        } else {
            if (categoriesList.size % 2 == 0) {
                Common.DEFAULT_COLUMN_COUNT
            } else {
                if (position > 1 && position == categoriesList.size - 1) {
                    Common.FULL_WIDTH_COLUMN
                } else {
                    Common.DEFAULT_COLUMN_COUNT
                }
            }
        }
    }


}