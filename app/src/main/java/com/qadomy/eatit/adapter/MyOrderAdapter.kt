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
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.Order
import java.text.SimpleDateFormat
import java.util.*

class MyOrderAdapter(private val context: Context, private val orderList: List<Order>) :
    RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    internal var calender: Calendar
    internal var simpleDateDormat: SimpleDateFormat

    init {
        calender = Calendar.getInstance()
        simpleDateDormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context!!).inflate(R.layout.layout_order_item, parent, false)
        )
    }

    override fun getItemCount() = orderList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context!!)
            .load(orderList[position].cartItemList!![0].foodImage)
            .into(holder.imageOrder!!)

        calender.timeInMillis = orderList[position].createDate
        val date = Date(orderList[position].createDate)

        holder.textOrderDate!!.text =
            StringBuilder(Common.getDateOfWeek(calender.get(Calendar.DAY_OF_WEEK)))
                .append(" ")
                .append(simpleDateDormat.format(date))

        holder.textOrderNumber!!.text =
            StringBuilder("Order number: ").append(orderList[position].orderNumber)

        holder.textOrderComment!!.text =
            StringBuilder("Comment: ").append(orderList[position].comment)

        holder.textOrderNumber!!.text =
            StringBuilder("Status: ").append(Common.convertStatusToText(orderList[position].orderStatus))
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal var imageOrder: ImageView? = null
        internal var textOrderDate: TextView? = null
        internal var textOrderStatus: TextView? = null
        internal var textOrderNumber: TextView? = null
        internal var textOrderComment: TextView? = null


        init {
            imageOrder = itemView.findViewById(R.id.img_order) as ImageView
            textOrderDate = itemView.findViewById(R.id.txt_order_date) as TextView
            textOrderStatus = itemView.findViewById(R.id.txt_order_status) as TextView
            textOrderNumber = itemView.findViewById(R.id.txt_order_number) as TextView
            textOrderComment = itemView.findViewById(R.id.txt_order_comment) as TextView
        }
    }
}
