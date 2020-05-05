package com.qadomy.eatit.ui.comment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyCommentAdapter
import com.qadomy.eatit.callback.ICommentCallBackListener
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.CommentModel
import dmax.dialog.SpotsDialog

class CommentFragment : BottomSheetDialogFragment(), ICommentCallBackListener {

    private var commentViewModel: CommentViewModel? = null
    private var listener: ICommentCallBackListener
    private var recyclerComment: RecyclerView? = null
    private var dialog: AlertDialog? = null

    init {
        listener = this

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val itemView = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_comment_fragment, container, false)

        // init views
        initView(itemView)

        loadCommentFromFirebase()
        commentViewModel!!.mutableLiveDataCommentList.observe(this, Observer { commentList ->
            val adapter = MyCommentAdapter(requireContext(), commentList)
            recyclerComment!!.adapter = adapter
        })

        return itemView
    }


    // function from load comments from database
    private fun loadCommentFromFirebase() {
        dialog!!.show()
        val commentModels = ArrayList<CommentModel>()
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
            .child(Common.FOOD_SELECTED!!.id!!)
            .orderByChild("commentTimeStamp")
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onCommentLoadFailed(p0.message)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (commentSnapShot in p0.children) {
                        val commentModel =
                            commentSnapShot.getValue<CommentModel>(CommentModel::class.java)
                        commentModels.add(commentModel!!)
                    }
                    listener.onCommentLoadSuccess(commentModels)
                }

            })

    }

    // init views
    private fun initView(itemView: View?) {

        commentViewModel = ViewModelProviders.of(this).get(CommentViewModel::class.java)

        dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        recyclerComment = itemView!!.findViewById(R.id.recycler_comment)
        recyclerComment!!.setHasFixedSize(true)


        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        recyclerComment!!.layoutManager = layoutManager
        recyclerComment!!.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
        )

    }

    // what happen when comment success
    override fun onCommentLoadSuccess(commentList: List<CommentModel>) {
        dialog!!.dismiss()
        commentViewModel!!.setCommentList(commentList)
    }

    // what happe when comment faild
    override fun onCommentLoadFailed(message: String) {
        Toast.makeText(requireContext(), "" + message, Toast.LENGTH_SHORT).show()
        dialog!!.dismiss()
    }

    companion object {
        private var instance: CommentFragment? = null
        fun getInstance(): CommentFragment {
            if (instance == null) {
                instance = CommentFragment()
            }

            return instance!!
        }
    }

}
