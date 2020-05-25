package com.qadomy.eatit.ui.view_order

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyOrderAdapter
import com.qadomy.eatit.callback.ILoadOrderCallbackListener
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.eventbus.MenuItemBack
import com.qadomy.eatit.model.Order
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList

class ViewOrdersFragment : Fragment(), ILoadOrderCallbackListener {

    private var viewOrderModel: ViewOrdersViewModel? = null

    internal lateinit var dialog: AlertDialog
    internal lateinit var recyclerOrder: RecyclerView
    internal lateinit var listener: ILoadOrderCallbackListener

    // onDestroy, we use inside it an event onMenuItemBack to avoid multiple instance of fragment
    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewOrderModel = ViewModelProviders.of(this).get(ViewOrdersViewModel::class.java)
        val root = inflater.inflate(R.layout.view_orders_fragment, container, false)

        // init views
        initViews(root)

        // load from firebase
        loadFromFirebase()


        viewOrderModel!!.mutableLiveDataOrderList.observe(viewLifecycleOwner, Observer {

            // for reverser items in recycler view we reverse items to display the last items ordered first
            Collections.reverse(it!!)

            // create adapter to added to recycler view
            val adapter = MyOrderAdapter(requireContext(), it!!)

            recyclerOrder!!.adapter = adapter

        })


        return root
    }


    /**
     *
     * init views
     */
    private fun initViews(root: View?) {

        listener = this
        dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        recyclerOrder = root!!.findViewById(R.id.recycler_order) as RecyclerView
        recyclerOrder.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(requireContext())

        recyclerOrder.layoutManager = layoutManager
        recyclerOrder.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
        )
    }

    // function for load from firebase database
    private fun loadFromFirebase() {
        dialog!!.show()
        val orderList = ArrayList<Order>()

        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
            .orderByChild("userId")
            .equalTo(Common.CURRENT_USER!!.uid!!)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onLoadOrderFailed(p0.message)
                }

                override fun onDataChange(p0: DataSnapshot) {

                    for (orderSnapShot in p0.children) {

                        val order = orderSnapShot.getValue(Order::class.java)

                        //we need add key to item in order
                        order!!.orderNumber = orderSnapShot.key
                        orderList.add(order!!)
                    }
                    listener.onLoadOrderSuccess(orderList)
                }
            })
    }


    /**
     *
     * methods implemented from ILoadOrderCallbackListener interface
     */

    override fun onLoadOrderSuccess(orderList: List<Order>) {
        dialog!!.dismiss()
        viewOrderModel!!.setMutableLiveDAtaOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

}
