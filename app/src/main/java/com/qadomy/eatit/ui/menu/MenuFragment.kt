package com.qadomy.eatit.ui.menu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyCategoriesAdapter
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.common.SpacesItemDecoration
import com.qadomy.eatit.eventbus.MenuItemBack
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus

class MenuFragment : Fragment() {

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyCategoriesAdapter? = null
    private var recyclerMenu: RecyclerView? = null


    // onDestroy, we use inside it an event onMenuItemBack to avoid multiple instance of fragment
    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        menuViewModel =
            ViewModelProviders.of(this).get(MenuViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_category, container, false)

        // init
        initView(root)

        menuViewModel.getMessageError().observe(viewLifecycleOwner, Observer {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        })

        menuViewModel.getCategoryList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            adapter = MyCategoriesAdapter(requireContext(), it)
            recyclerMenu!!.adapter = adapter
            recyclerMenu!!.layoutAnimation = layoutAnimationController
        })

        return root
    }

    private fun initView(root: View) {
        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        // init recycler_menu
        recyclerMenu = root.findViewById(R.id.recycler_menu)
        recyclerMenu!!.setHasFixedSize(true)
        val layoutManager = GridLayoutManager(context, 2)
        layoutManager.orientation = RecyclerView.VERTICAL
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter != null) {
                    when (adapter!!.getItemViewType(position)) {
                        Common.DEFAULT_COLUMN_COUNT -> 1
                        Common.FULL_WIDTH_COLUMN -> 2
                        else -> -1
                    }
                } else {
                    -1
                }
            }
        }

        recyclerMenu!!.layoutManager = layoutManager
        recyclerMenu!!.addItemDecoration(SpacesItemDecoration(8))

    }
}
