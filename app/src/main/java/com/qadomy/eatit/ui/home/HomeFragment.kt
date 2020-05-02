package com.qadomy.eatit.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asksira.loopingviewpager.LoopingViewPager
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyBestDealsAdapter
import com.qadomy.eatit.adapter.MyPopularCategoriesAdapter

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var viewPager: LoopingViewPager
    private lateinit var recyclerView: RecyclerView

    // layout animation
    var layoutAnimationController: LayoutAnimationController? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        initView(root)
        /**
         * add popular list list
         */
        homeViewModel.popularList.observe(viewLifecycleOwner, Observer {
            val listData = it
            val adapter = MyPopularCategoriesAdapter(requireContext(), listData)
            recyclerView!!.adapter = adapter
            recyclerView!!.layoutAnimation = layoutAnimationController
        })

        /**
         * add best deals list
         */
        homeViewModel.bestDealList.observe(viewLifecycleOwner, Observer {
            val adapter = MyBestDealsAdapter(requireContext(), it, false)
            viewPager!!.adapter = adapter
        })


        return root
    }

    private fun initView(root: View) {
        // layout animation
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)


        // view pager
        viewPager = root.findViewById(R.id.viewpager) as LoopingViewPager

        // recycler view
        recyclerView = root.findViewById(R.id.recycler_popular)
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }


    /**
     * For make sure back to exact position in scroll of viewpager when out from app 'not completely exit'
     * we pause auto scroll in  onPause, and resume it in onResume
     */

    override fun onPause() {
        viewPager!!.pauseAutoScroll()
        super.onPause()
    }

    override fun onResume() {
        viewPager!!.resumeAutoScroll()
        super.onResume()
    }
}
