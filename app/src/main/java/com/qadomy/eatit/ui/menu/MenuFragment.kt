package com.qadomy.eatit.ui.menu

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
import com.qadomy.eatit.model.CategoryModel
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

    /**
     *
     * Init views
     */
    private fun initView(root: View) {

        // set menu in fragment
        setHasOptionsMenu(true)


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
            menuViewModel.loadCategory()
        }

    }


    // function for start search in list when click ok
    private fun startSearch(s: String) {
        val resultCategory = ArrayList<CategoryModel>()

        for (i in 0 until adapter!!.getCategoryList().size) {

            val categoryModel = adapter!!.getCategoryList()[i]
            if (categoryModel.name!!.toLowerCase().contains(s))
                resultCategory.add(categoryModel)
        }

        menuViewModel.getCategoryList().value = resultCategory
    }
}
