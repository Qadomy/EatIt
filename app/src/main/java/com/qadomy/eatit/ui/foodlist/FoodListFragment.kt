package com.qadomy.eatit.ui.foodlist

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyFoodListAdapter
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.eventbus.MenuItemBack
import com.qadomy.eatit.model.FoodModel
import org.greenrobot.eventbus.EventBus

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel
    private var recyclerFoodList: RecyclerView? = null
    private var layoutAnimationController: LayoutAnimationController? = null

    private var adapter: MyFoodListAdapter? = null


    // we call onStop Adapter for clear compositeDisposable when fragment stop
    override fun onStop() {
        if (adapter != null) {
            adapter!!.onStop()
        }
        super.onStop()
    }

    // onDestroy, we use inside it an event onMenuItemBack to avoid multiple instance of fragment
    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    // onCreate
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel = ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.food_list_fragment, container, false)

        // init view
        initView(root)

        foodListViewModel.getMutableFoodListData().observe(viewLifecycleOwner, Observer {
            if (it != null) {
                // fix crash when category is empty foods
                adapter = MyFoodListAdapter(requireContext(), it)
                recyclerFoodList!!.adapter = adapter
                recyclerFoodList!!.layoutAnimation = layoutAnimationController
            }
        })

        return root
    }


    /**
     *
     * Init views
     */
    private fun initView(root: View?) {

        // set menu for food list fragment
        setHasOptionsMenu(true)

        // set action bar title the selected category name
        (activity as AppCompatActivity).supportActionBar!!.setTitle(Common.CATEGORY_SELECTED!!.name)

        recyclerFoodList = root!!.findViewById(R.id.recycler_food_list)
        recyclerFoodList!!.setHasFixedSize(true)
        recyclerFoodList!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)


        // change the bar title in food list fragment
        (activity as AppCompatActivity).supportActionBar!!.title = Common.CATEGORY_SELECTED!!.name


    }


    /**
     *
     *
     * Menu
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu, menu)

        val menuItem = menu.findItem(R.id.action_search)
        val searchManager =
            requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as SearchView

        // search view: add search on fragment
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

        // Event, when click on saerch icon
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String?): Boolean {

                startSearch(s!!)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // clear text when click clear button in search view
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView

        // when click
        closeButton.setOnClickListener {
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText

            // clear text
            ed.setText("")

            // clear query
            searchView.setQuery("", false)

            // collapse the action view
            searchView.onActionViewCollapsed()

            // collapse the search widget
            menuItem.collapseActionView()

            // restore result to original
            foodListViewModel.getMutableFoodListData()
        }

    }


    // function for start search in list when click ok
    private fun startSearch(s: String) {
        val resultFood = ArrayList<FoodModel>()

        for (i in 0 until Common.CATEGORY_SELECTED!!.foods!!.size) {

            val categoryModel = Common.CATEGORY_SELECTED!!.foods!![i]
            if (categoryModel.name!!.toLowerCase().contains(s))
                resultFood.add(categoryModel)
        }

        foodListViewModel.getMutableFoodListData().value = resultFood
    }


}
