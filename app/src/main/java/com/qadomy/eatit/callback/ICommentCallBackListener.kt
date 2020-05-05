package com.qadomy.eatit.callback

import com.qadomy.eatit.model.CommentModel

interface ICommentCallBackListener {
    fun onCommentLoadSuccess(commentList: List<CommentModel>)
    fun onCommentLoadFailed(message: String)

}
