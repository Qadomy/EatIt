package com.qadomy.eatit.ui.cart

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
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
import com.braintreepayments.api.dropin.DropInRequest
import com.braintreepayments.api.dropin.DropInResult
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.qadomy.eatit.R
import com.qadomy.eatit.adapter.MyCartAdapter
import com.qadomy.eatit.callback.ILoadTimeFromFirebaseCallback
import com.qadomy.eatit.callback.IMyButtonCallback
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.common.MySwipeHelper
import com.qadomy.eatit.database.CartDataSource
import com.qadomy.eatit.database.CartDatabase
import com.qadomy.eatit.database.CartItem
import com.qadomy.eatit.database.LocalCartDataSource
import com.qadomy.eatit.eventbus.CountCartEvent
import com.qadomy.eatit.eventbus.HideFABcart
import com.qadomy.eatit.eventbus.MenuItemBack
import com.qadomy.eatit.eventbus.UpdateItemInCart
import com.qadomy.eatit.model.Order
import com.qadomy.eatit.remote.ICloudFunctions
import com.qadomy.eatit.remote.RetrofitCloudClient
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback {

    // location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location

    // Room database
    private var cartDataSource: CartDataSource? = null
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private var recyclerViewState: Parcelable? = null

    private lateinit var placeOrderButton: Button
    private lateinit var cartViewModel: CartViewModel

    // views
    var textEmptyCart: TextView? = null
    var textTotalPrice: TextView? = null
    var groupPlaceHolder: CardView? = null
    var recyclerCart: RecyclerView? = null
    var adapter: MyCartAdapter? = null

    // for payment
    internal var address: String = ""
    internal var comment: String = ""

    private val REQUEST_BRAINTREE_CODE: Int = 8888

    lateinit var cloudFunctions: ICloudFunctions

    lateinit var listener: ILoadTimeFromFirebaseCallback

    /**
     *
     *
     *
     *
     */
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

        // request again location when resume to app
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    // onStop, when stop screen we showing fab button
    override fun onStop() {
        cartViewModel!!.onStop()
        compositeDisposable.clear() // dispose all of then at once
        EventBus.getDefault().postSticky(HideFABcart(false))
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }

        // remove location update when stop application
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }

        super.onStop()
    }

    // onDestroy, we use inside it an event onMenuItemBack to avoid multiple instance of fragment
    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
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

        // init location
        initLocation()

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


    /**
     *
     *
     * init views
     */
    private fun initView(root: View) {

        // inflate menu
        setHasOptionsMenu(true)

        // init cloud functions interface
        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions::class.java)

        // implement listener from ILoadTimeFromFirebaseCallback
        listener = this

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
            val edtComment = view.findViewById<View>(R.id.order_edt_comment) as EditText
            val txtAddress = view.findViewById<View>(R.id.order_txt_address_details) as TextView
            val rdiHome = view.findViewById<View>(R.id.rdi_home_address) as RadioButton
            val rdiOther = view.findViewById<View>(R.id.rdi_other_address) as RadioButton
            val rdiShip = view.findViewById<View>(R.id.rdi_ship_this_address) as RadioButton
            val rdiCod = view.findViewById<View>(R.id.rdi_cod) as RadioButton
            val rdiBraintree = view.findViewById<View>(R.id.rdi_braintree) as RadioButton

            // Data

            // By default we checked rdi_home, so will display user address
            edtAddress.setText(Common.CURRENT_USER!!.address!!)


            // Event
            rdiHome.setOnCheckedChangeListener { _, b ->
                if (b) {
                    // if we choose rdi home we set user address in text address
                    edtAddress.setText(Common.CURRENT_USER!!.address!!)
                    txtAddress.visibility = View.GONE
                }
            }

            rdiOther.setOnCheckedChangeListener { _, b ->
                if (b) {
                    // if we choose rdi other we delete the current address in edt address and let user choose another addres
                    edtAddress.setText("")
                    edtAddress.hint = "Enter Your Adderss"
                    txtAddress.visibility = View.GONE
                }
            }

            rdiShip.setOnCheckedChangeListener { _, b ->
                if (b) {

                    // we choose address from current location
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener { e ->
                            /** if failure getting location */
                            txtAddress.visibility = View.GONE
                            Toast.makeText(requireContext(), "" + e.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                        .addOnCompleteListener { task ->
                            /** if complete getting location */
                            val coordinates = StringBuilder()
                                .append(task.result!!.latitude)
                                .append("/")
                                .append(task.result!!.longitude)
                                .toString()


                            // now we convert address form lat/lng to address name
                            val singleAddress = Single.just(
                                getAddressFromLatLng(
                                    task.result!!.latitude,
                                    task.result!!.longitude
                                )
                            )

                            val disposable = singleAddress.subscribeWith(object :
                                DisposableSingleObserver<String>() {
                                override fun onSuccess(t: String) {
                                    edtAddress.setText(coordinates)
                                    txtAddress.visibility = View.VISIBLE
                                    txtAddress.text = t
                                }

                                override fun onError(e: Throwable) {
                                    edtAddress.setText(coordinates)
                                    txtAddress.visibility = View.VISIBLE
                                    txtAddress.text = e.message!!
                                }
                            })


                        }

                }
            }


            builder.setView(view)
            builder.setNegativeButton("NO") { dialogInterface, _ -> dialogInterface.dismiss() }
                .setPositiveButton("Yes") { _, _ ->
                    if (rdiCod.isChecked) {
                        /** if choose payment by Cash on delivery "COD" */
                        paymentCOD(edtAddress.text.toString(), edtComment.text.toString())
                    } else if (rdiBraintree.isChecked) {
                        /** if choose payment by Braintree */
                        address = edtAddress.text.toString()
                        comment = edtComment.text.toString()

                        if (!TextUtils.isDigitsOnly(Common.CURRENT_TOKEN)) {
                            val dropInRequest = DropInRequest().clientToken(Common.CURRENT_TOKEN)

                            startActivityForResult(
                                dropInRequest.getIntent(context),
                                REQUEST_BRAINTREE_CODE
                            )
                        }
                    }
                }


            val dialog = builder.create()
            dialog.show()

        }
    }


    /**
     *
     *
     * Functions related with user location
     */

    // init location
    private fun initLocation() {
        buildLocationRequest()
        buildLocationCallback()

        // fusedLocationProviderClient
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest, locationCallback,
            Looper.getMainLooper() // Returns the application's main looper, which lives in the main thread of the application.
        )

    }

    // build location callback
    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)

                currentLocation = p0!!.lastLocation
            }
        }
    }

    // build location request
    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        /** the smallest displacement in meters the user must move between location updates. */
        locationRequest.smallestDisplacement = 10f
    }


    /**
     *
     * Functions
     */


    // function for payment by Cash on delivery "COD"
    private fun paymentCOD(address: String, comment: String) {
        compositeDisposable.addAll(
            cartDataSource!!.getAllCart(Common.CURRENT_USER!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cartItemList ->

                    // when we have all cart items , we will get total price
                    cartDataSource!!.sumPrice(Common.CURRENT_USER!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<Double> {
                            override fun onSuccess(totalPrice: Double) {

                                val finalPrice = totalPrice
                                val order = Order()
                                order.userId = Common.CURRENT_USER!!.uid
                                order.userName = Common.CURRENT_USER!!.name
                                order.userPhone = Common.CURRENT_USER!!.phone
                                order.shippingAddress = address
                                order.comment = comment

                                if (currentLocation != null) {
                                    order.lat = currentLocation!!.latitude
                                    order.lng = currentLocation!!.longitude
                                }

                                order.cartItemList = cartItemList
                                order.totalPayment = totalPrice
                                order.finalPayment = finalPrice
                                order.discount = 0
                                order.isCod = true
                                order.transactionId = "Cash On Delivery"

                                // submit to server
                                syncLocalTimeWithServer(order)
                            }

                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                            }
                        })

                },
                    { throwable ->
                        Toast.makeText(
                            requireContext(),
                            "" + throwable.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    // function for write orders to firebase database
    private fun writeOrdersToFirebase(order: Order) {
        FirebaseDatabase.getInstance()
            .getReference(Common.ORDER_REF)
            .child(Common.createOrderNumber())
            .setValue(order)
            .addOnFailureListener { e ->
                /** if write to firebase failure */
                Toast.makeText(
                    requireContext(),
                    "" + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnCompleteListener { task ->
                /** if write to firebase successful */
                if (task.isSuccessful) {
                    // first thing we clean the cart items after we press place order button
                    cartDataSource!!.cleanCart(Common.CURRENT_USER!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<Int> {
                            override fun onSuccess(t: Int) {
                                Toast.makeText(
                                    context!!,
                                    "Order Placed Successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            }

    }

    // function for convert to address name
    private fun getAddressFromLatLng(latitude: Double, longitude: Double): String {
        // GeoCoder class make convert from locale lat/lng to address name
        val geoCoder = Geocoder(requireContext(), Locale.getDefault())
        var result: String? = null

        try {
            val addressList = geoCoder.getFromLocation(
                latitude,
                longitude,
                1
            ) // maxResult: max number of addresses to return

            if (addressList != null && addressList.size > 0) {

                val address = addressList[0]
                val sb =
                    StringBuilder(address.getAddressLine(0)) // getAddressLine: Returns a line of the address numbered by the given index

                result = sb.toString()

            } else {
                result = "Address Not Found"
            }

            return result

        } catch (e: IOException) {
            return e.message!!
        }
    }

    // Re-calculate the sum of total price of order after delete items from cart
    private fun sumCart() {
        cartDataSource!!.sumPrice(Common.CURRENT_USER!!.uid!!)
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

    // calculate total price
    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Common.CURRENT_USER!!.uid!!)
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
     * Event Bus
     */

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

            cartDataSource!!.cleanCart(Common.CURRENT_USER!!.uid!!)
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


    /**
     * onActivityResult
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BRAINTREE_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                val result =
                    data!!.getParcelableExtra<DropInResult>(DropInRequest.EXTRA_CHECKOUT_REQUEST)

                val nonce = result!!.paymentMethodNonce


                // calculate sum cart
                cartDataSource!!.sumPrice(Common.CURRENT_USER!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<Double> {
                        override fun onSuccess(totalPrice: Double) {
                            // Get all item to create cart
                            compositeDisposable.add(
                                cartDataSource!!.getAllCart(Common.CURRENT_USER!!.uid!!)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({ cartItem: List<CartItem>? ->

                                        // After have all cart item, we will submit payment
                                        compositeDisposable.add(
                                            cloudFunctions.submitPayment(
                                                totalPrice,
                                                nonce!!.nonce
                                            )
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe({ braintreeTransaction ->

                                                    if (braintreeTransaction.success) {
                                                        // create order
                                                        val finalPrice = totalPrice
                                                        val order = Order()
                                                        order.userId = Common.CURRENT_USER!!.uid
                                                        order.userName = Common.CURRENT_USER!!.name

                                                        order.userPhone =
                                                            Common.CURRENT_USER!!.phone

                                                        order.shippingAddress = address
                                                        order.comment = comment

                                                        if (currentLocation != null) {
                                                            order.lat = currentLocation!!.latitude
                                                            order.lng = currentLocation!!.longitude
                                                        }

                                                        order.cartItemList = cartItem
                                                        order.totalPayment = totalPrice
                                                        order.finalPayment = finalPrice
                                                        order.discount = 0
                                                        order.isCod = false

                                                        order.transactionId =
                                                            braintreeTransaction.transaction!!.id

                                                        // submit to server
                                                        syncLocalTimeWithServer(order)
                                                    }

                                                }, { t: Throwable? ->

                                                    Toast.makeText(
                                                        context!!,
                                                        "" + t!!.message,
                                                        Toast.LENGTH_SHORT
                                                    )
                                                        .show()
                                                })
                                        )

                                    }, { t: Throwable? ->

                                        Toast.makeText(
                                            context!!,
                                            "" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                            )
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(context!!, "" + e.message, Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
    }

    // submit to server
    private fun syncLocalTimeWithServer(order: Order) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.onLoadTimeFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val offset = p0.getValue(Long::class.java)
                // add missing offset to your current time
                val estimatedServerTimeInMs = System.currentTimeMillis() + offset!!
                val sdf = SimpleDateFormat("MMM dd yyyy, HH:mm")

                val date = Date(estimatedServerTimeInMs)

                Log.d("cart fragment", "" + sdf.format(date))

                listener.onLoadTimeSuccess(order, estimatedServerTimeInMs)

            }
        })
    }


    /**
     *
     * implement methods from ILoadTimeFromFirebaseCallback interface
     */
    override fun onLoadTimeSuccess(order: Order, estimatedTimeMs: Long) {
        order.createDate = (estimatedTimeMs)
        // make default order status is 0
        order.orderStatus = 0
        writeOrdersToFirebase(order)
    }

    override fun onLoadTimeFailed(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}
