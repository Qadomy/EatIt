package com.qadomy.eatit.ui.cart

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyCartAdapter
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.database.CartDataSource
import com.qadomy.eatit.database.CartDatabase
import com.qadomy.eatit.database.LocalCartDataSource
import com.qadomy.eatit.eventbus.HideFABcart
import com.qadomy.eatit.eventbus.UpdateItemInCart
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class CartFragment : Fragment() {

    private var cartDataSource: CartDataSource? = null
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var recyclerViewState: Parcelable? = null


    private lateinit var cartViewModel: CartViewModel
    var textEmptyCart: TextView? = null
    var textTotalPrice: TextView? = null
    var groupPlaceHolder: CardView? = null
    var recyclerCart: RecyclerView? = null


    // onStart
    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }


    // onResume
    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
    }

    // onCreateView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // event for remove fab button from cart screen
        EventBus.getDefault().postSticky(HideFABcart(true))

        cartViewModel =
            ViewModelProviders.of(this).get(CartViewModel::class.java)

        // After create cartViewModel, init data source
        cartViewModel.initCartDataSource(requireContext())

        val root = inflater.inflate(R.layout.fragment_cart, container, false)


        // init views
        initView(root)

        cartViewModel.getMutableLiveDataCartItem().observe(viewLifecycleOwner, Observer {
            if (it == null || it.isEmpty()) {
                // if there any items carts in database "ordered"
                recyclerCart!!.visibility = View.GONE
                groupPlaceHolder!!.visibility = View.GONE
                textEmptyCart!!.visibility = View.VISIBLE
            } else {
                // if there is no items cart in database "not ordered"
                recyclerCart!!.visibility = View.VISIBLE
                groupPlaceHolder!!.visibility = View.VISIBLE
                textEmptyCart!!.visibility = View.GONE

                val adapter = MyCartAdapter(requireContext(), it)
                recyclerCart!!.adapter = adapter
            }
        })

        return root
    }


    private fun initView(root: View) {

        // cart data source
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(requireContext()).cartDAO())

        recyclerCart = root.findViewById(R.id.recycler_cart) as RecyclerView
        recyclerCart!!.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        recyclerCart!!.layoutManager = layoutManager
        recyclerCart!!.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))

        textEmptyCart = root.findViewById(R.id.txt_empty_cart) as TextView
        textTotalPrice = root.findViewById(R.id.txt_total_price) as TextView
        groupPlaceHolder = root.findViewById(R.id.group_place_holder) as CardView
    }


    // onStop, when stop screen we showing fab button
    override fun onStop() {
        super.onStop()
        cartViewModel!!.onStop()
        compositeDisposable.clear()
        EventBus.getDefault().postSticky(HideFABcart(false))
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }


    // Event for change total price
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun inUpdateItemInCart(event: UpdateItemInCart) {
        if (event.cartItem != null) {
            recyclerViewState = recyclerCart!!.layoutManager!!.onSaveInstanceState()
            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSuccess(t: Int) {
                        calculateTotalPrice()
                        recyclerCart!!.layoutManager!!.onRestoreInstanceState(recyclerViewState)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "[UPDATE CART]" + e.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }
    }

    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double> {
                override fun onSuccess(price: Double) {
                    textTotalPrice!!.text = StringBuilder("Total: ")
                        .append(Common.formatPrice(price))
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    Toast.makeText(context, "[SUM CART]" + e.message, Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }


}
