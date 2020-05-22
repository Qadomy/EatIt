package com.qadomy.eatit.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.asksira.loopingviewpager.LoopingPagerAdapter
import com.bumptech.glide.Glide
import com.qadomy.eatit.R
import com.qadomy.eatit.eventbus.BestDealItemClick
import com.qadomy.eatit.model.BestDealModel
import org.greenrobot.eventbus.EventBus

class MyBestDealsAdapter(
    context: Context,
    itemList: List<BestDealModel>,
    isInfinite: Boolean
) : LoopingPagerAdapter<BestDealModel>(context, itemList, isInfinite) {

    override fun bindView(convertView: View, listPosition: Int, viewType: Int) {
        val imageView = convertView!!.findViewById<ImageView>(R.id.img_best_deal)
        val textView = convertView!!.findViewById<TextView>(R.id.text_best_deal)

        // set data
        Glide.with(context).load(itemList!![listPosition].image).into(imageView)
        textView.text = itemList!![listPosition].name


        // when click on best deals food "Post event"
        convertView.setOnClickListener {
            EventBus.getDefault().postSticky(BestDealItemClick(itemList!![listPosition]))
        }

    }

    override fun inflateView(viewType: Int, container: ViewGroup, listPosition: Int): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.layout_best_deals_items, container!!, false)
    }
}