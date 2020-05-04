package com.qadomy.eatit.callback

import android.view.View

interface IRecycleItemClickListener {
    fun onItemClick(view: View, pos: Int)
}