package com.qadomy.eatit.ui.cart

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyCartAdapter
import com.qadomy.eatit.callback.IMyButtonCallback
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.common.MySwipeHelper
import com.qadomy.eatit.database.CartDataSource
import com.qadomy.eatit.database.CartDatabase
import com.qadomy.eatit.database.LocalCartDataSource
import com.qadomy.eatit.eventbus.CountCartEvent
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
    private lateinit var placeOrderButton: Button


    private lateinit var cartViewModel: CartViewModel
    var textEmptyCart: TextView? = null
    var textTotalPrice: TextView? = null
    var groupPlaceHolder: CardView? = null
    var recyclerCart: RecyclerView? = null

    var adapter: MyCartAdapter? = null

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

                adapter = MyCartAdapter(requireContext(), it)
                recyclerCart!!.adapter = adapter
            }
        })

        return root
    }


    private fun initView(root: View) {

        // inflate menu
        setHasOptionsMenu(true)

        // cart data source
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(requireContext()).cartDAO())

        recyclerCart = root.findViewById(R.id.recycler_cart) as RecyclerView
        recyclerCart!!.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        recyclerCart!!.layoutManager = layoutManager
        recyclerCart!!.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))


        /** for attach delete swipe for recycler view items */
        val swipe = object : MySwipeHelper(requireContext(), recyclerCart!!, 200) {
            override fun instantisteMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(
                        context!!,
                        "Delete",
                        30,
                        0,
                        Color.parseColor("#FF3C30"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                // when click on delete button after we swipe, we delete it from cart and database
                                val deleteItem = adapter!!.getItemAtPosition(pos)
                                cartDataSource!!.deleteCart(deleteItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<Int> {
                                        override fun onSuccess(t: Int) {
                                            // here we remove item from recycler view
                                            adapter!!.notifyItemRemoved(pos)

                                            // here we calculate total price after delete items
                                            sumCart()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                            Toast.makeText(
                                                context,
                                                "Delete item Success",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onError(e: Throwable) {
                                            Toast.makeText(
                                                context,
                                                "" + e.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                    })
                            }

                        }
                    )
                )
            }

        }

        textEmptyCart = root.findViewById(R.id.txt_empty_cart) as TextView
        textTotalPrice = root.findViewById(R.id.txt_total_price) as TextView
        groupPlaceHolder = root.findViewById(R.id.group_place_holder) as CardView

        placeOrderButton = root.findViewById(R.id.btn_place_order) as Button

        // Event for Button place order
        placeOrderButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("One more step!")

            val view = LayoutInflater.from(context).inflate(R.layout.layout_place_order, null)

            val edtAddress = view.findViewById<View>(R.id.order_edt_address) as EditText
            val rdiHome = view.findViewById<View>(R.id.rdi_home_address) as RadioButton
            val rdiOther = view.findViewById<View>(R.id.rdi_other_address) as RadioButton
            val rdiShip = view.findViewById<View>(R.id.rdi_ship_this_address) as RadioButton
            val rdiCod = view.findViewById<View>(R.id.rdi_cod) as RadioButton
            val rdiBraintree = view.findViewById<View>(R.id.rdi_braintree) as RadioButton

            // Data

            // By default we checked rdi_home, so will display user address
            edtAddress.setText(Common.currentUser!!.address!!)


            // Event
            rdiHome.setOnCheckedChangeListener { _, b ->
                if (b) {
                    // if we choose rdi home we set user address in text address
                    edtAddress.setText(Common.currentUser!!.address!!)
                }
            }

            rdiOther.setOnCheckedChangeListener { _, b ->
                if (b) {
                    // if we choose rdi other we delete the current address in edt address and let user choose another addres
                    edtAddress.setText("")
                    edtAddress.setHint("Enter Your Adderss")

                }
            }

            rdiShip.setOnCheckedChangeListener { _, b ->
                if (b) {

                    // if choose rdi ship to this address, we choosen later from Google Api
                    Toast.makeText(
                        requireContext(),
                        "Implement late with Google Api",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }


            builder.setView(view)
            builder.setNegativeButton("NO") { dialogInterface, _ -> dialogInterface.dismiss() }
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    Toast.makeText(
                        requireContext(),
                        "Implement later",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            val dialog = builder.create()
            dialog.show()

        }
    }


    /**
     * Recalculate the sum of total price of order after delete items from cart
     */
    private fun sumCart() {
        cartDataSource!!.sumPrice(Common.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double> {
                override fun onSuccess(t: Double) {
                    textTotalPrice!!.text = StringBuilder("Total: $").append(t)
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if (e.message!!.contains("Query returned empty"))
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                }
            })

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
                    textTotalPrice!!.text = StringBuilder("Total: $")
                        .append(Common.formatPrice(price))
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(context, "[SUM CART]" + e.message, Toast.LENGTH_SHORT)
                            .show()
                }
            })
    }


    /**
     *
     * For clear cart menu and delete all items once from cart and database
     */


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater!!.inflate(R.menu.cart_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item!!.itemId == R.id.action_clear_cart) {

            cartDataSource!!.cleanCart(Common.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int> {
                    override fun onSuccess(t: Int) {
                        Toast.makeText(context, "Clear Cart Success", Toast.LENGTH_SHORT).show()
                        EventBus.getDefault().postSticky(CountCartEvent(true))
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
                    }

                })
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    // hide setting menu when in cart screen
    override fun onPrepareOptionsMenu(menu: Menu) {
        menu!!.findItem(R.id.action_settings).isVisible = false
        super.onPrepareOptionsMenu(menu)
    }


}
