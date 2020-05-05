package com.qadomy.eatit.adapter

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatit.R
import com.qadomy.eatit.model.CommentModel

class MyCommentAdapter(
    internal var context: Context,
    internal var commentList: List<CommentModel>
) : RecyclerView.Adapter<MyCommentAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.layout_comment_item, parent, false)
        )
    }

    override fun getItemCount() = commentList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val timeStamp =
            commentList.get(position).commentTimeStamp!!["timeStamp"]!!.toString().toLong()
        holder.commentDate!!.text = DateUtils.getRelativeTimeSpanString(timeStamp)
        holder.commentName!!.text = commentList.get(position).name
        holder.comment!!.text = commentList.get(position).comment
        holder.ratingBar!!.rating = commentList.get(position).ratingValue!!

    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var commentName: TextView? = null
        var commentDate: TextView? = null
        var comment: TextView? = null
        var ratingBar: RatingBar? = null

        init {
            commentName = itemView.findViewById(R.id.txt_comment_name) as TextView
            commentDate = itemView.findViewById(R.id.txt_comment_data) as TextView
            comment = itemView.findViewById(R.id.txt_comment) as TextView
            ratingBar = itemView.findViewById(R.id.comment_rating_bar) as RatingBar
        }
    }

}