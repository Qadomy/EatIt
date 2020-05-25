package com.qadomy.eatit.ui.fooddetails

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.google.gson.Gson
import com.qadomy.eatit.R
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.database.CartDataSource
import com.qadomy.eatit.database.CartDatabase
import com.qadomy.eatit.database.CartItem
import com.qadomy.eatit.database.LocalCartDataSource
import com.qadomy.eatit.eventbus.CountCartEvent
import com.qadomy.eatit.model.CommentModel
import com.qadomy.eatit.model.FoodModel
import com.qadomy.eatit.ui.comment.CommentFragment
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.food_details_fragment.*
import org.greenrobot.eventbus.EventBus

class FoodDetailsFragment : Fragment(), TextWatcher {

    private lateinit var addonBottomSheetDialog: BottomSheetDialog

    private lateinit var foodDetailsViewModel: FoodDetailsViewModel
    private var imgFood: ImageView? = null
    private var btnCart: CounterFab? = null
    private var btnRating: FloatingActionButton? = null
    private var foodName: TextView? = null
    private var foodPrice: TextView? = null
    private var foodDescription: TextView? = null
    private var numberButton: ElegantNumberButton? = null
    private var ratingBar: RatingBar? = null
    private var btnShowComment: Button? = null
    private var radioGroupSize: RadioGroup? = null

    // for addon section
    private var imgAddOn: ImageView? = null
    private var chipGroupUserSelectedAddon: ChipGroup? = null

    // Addon layout
    private var chipGroupAddon: ChipGroup? = null
    private var edtSearchAddon: EditText? = null

    // waiting dialog
    private var waitingDialog: android.app.AlertDialog? = null


    // compositeDisposable
    private val compositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource: CartDataSource


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodDetailsViewModel = ViewModelProviders.of(this).get(FoodDetailsViewModel::class.java)
        val root = inflater.inflate(R.layout.food_details_fragment, container, false)

        // init views
        initView(root)

        foodDetailsViewModel.getMutableLiveDataFood().observe(viewLifecycleOwner, Observer {
            displayInfo(it)
        })

        foodDetailsViewModel.getMutableLiveDataComment().observe(viewLifecycleOwner, Observer {
            submitRatingToFirebase(it)
        })
        return root
    }

    // save rating value in Firebase database
    private fun submitRatingToFirebase(commentModel: CommentModel?) {
        waitingDialog!!.show()

        // First , we will submit ti Comment Ref
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
            .child(Common.FOOD_SELECTED!!.id!!).push().setValue(commentModel)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    addRatingToFood(commentModel!!.ratingValue.toDouble())
                }
                waitingDialog!!.dismiss()
            }

    }

    // here we add the rating value in database in firebase
    private fun addRatingToFood(ratingValue: Double) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF!!) // select category
            .child(Common.CATEGORY_SELECTED!!.menuId!!) // select menu in category
            .child("foods") // select food arrays
            .child(Common.FOOD_SELECTED!!.key!!) // select key
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    waitingDialog!!.dismiss()
                    Toast.makeText(context!!, "" + p0.message, Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val foodModel = dataSnapshot.getValue(FoodModel::class.java)
                        foodModel!!.key = Common.FOOD_SELECTED!!.key

                        //Apply rating
                        val sumRating = foodModel.ratingValue!! + ratingValue
                        val ratingCount = foodModel.ratingCount + 1


                        val updateData = HashMap<String, Any>()
                        updateData["ratingValue"] = sumRating
                        updateData["reatingCount"] = ratingCount

                        // Update data is variable
                        foodModel.ratingCount = ratingCount
                        foodModel.ratingValue = sumRating


                        dataSnapshot.ref.updateChildren(updateData)
                            .addOnCompleteListener { task ->
                                waitingDialog!!.dismiss()
                                if (task.isSuccessful) {
                                    Common.FOOD_SELECTED = foodModel
                                    foodDetailsViewModel!!.setFoodModel(foodModel)
                                    Toast.makeText(context!!, "Thank You", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }

                    } else {
                        waitingDialog!!.dismiss()
                    }
                }

            })
    }

    private fun displayInfo(it: FoodModel?) {
        Glide.with(requireContext()).load(it!!.image).into(imgFood!!)
        foodName!!.text = StringBuilder(it!!.name!!)
        foodDescription!!.text = StringBuilder(it!!.description!!)
        foodPrice!!.text = StringBuilder(it!!.price!!.toString())

        // set rating value in rating bar in screen
        ratingBar!!.rating = it!!.ratingValue.toFloat() / it!!.ratingCount

        // set size for radio group
        for (sizeModel in it!!.size) {
            val radioButton = RadioButton(context)
            radioButton.setOnCheckedChangeListener { compoundButton, b ->
                if (b) {
                    Common.FOOD_SELECTED!!.userSelectedSize = sizeModel
                }
                calculateTotalPrice()
            }

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            radioButton.layoutParams = params
            radioButton.text = sizeModel.name
            radioButton.tag = sizeModel.price

            radioGroupSize!!.addView(radioButton)

            // Default first radio button select
            if (radioGroupSize!!.childCount > 0) {
                val radioButton = radioGroupSize!!.getChildAt(0) as RadioButton
                radioButton.isChecked = true
            }
        }
    }

    // function for calculate the total price of order
    private fun calculateTotalPrice() {
        var totalprice = Common.FOOD_SELECTED!!.price.toDouble()
        var displatPrice = 0.0

        // Addon
        if (Common.FOOD_SELECTED!!.userSelectedAddon != null
            && Common.FOOD_SELECTED!!.userSelectedAddon!!.size > 0
        ) {
            for (addonModel in Common.FOOD_SELECTED!!.userSelectedAddon!!) {
                totalprice += addonModel.price!!.toDouble()
            }
        }

        // size
        totalprice += Common.FOOD_SELECTED!!.userSelectedSize!!.price!!.toDouble()

        displatPrice = totalprice * number_button.number.toInt()
        displatPrice = Math.round(displatPrice * 100.0) / 100.0

        foodPrice!!.text =
            java.lang.StringBuilder("").append(Common.formatPrice(displatPrice)).toString()

    }

    private fun initView(root: View?) {
        // addon bottom sheet dialog 
        addonBottomSheetDialog = BottomSheetDialog(requireContext(), R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display, null)
        chipGroupAddon =
            layout_user_selected_addon.findViewById(R.id.chip_groupa_addon) as ChipGroup
        edtSearchAddon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addonBottomSheetDialog.setContentView(layout_user_selected_addon)
        addonBottomSheetDialog.setOnDismissListener { dialogInterface ->
            displayUserSelectAddon()
            calculateTotalPrice()
        }


        // dialog
        waitingDialog =
            SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        btnCart = root!!.findViewById(R.id.btn_cart) as CounterFab
        imgFood = root!!.findViewById(R.id.img_food) as ImageView
        btnRating = root!!.findViewById(R.id.btn_rating) as FloatingActionButton
        foodName = root!!.findViewById(R.id.food_name) as TextView
        foodDescription = root!!.findViewById(R.id.food_description) as TextView
        foodPrice = root!!.findViewById(R.id.food_price) as TextView
        numberButton = root!!.findViewById(R.id.number_button) as ElegantNumberButton
        ratingBar = root!!.findViewById(R.id.ratingBar) as RatingBar
        btnShowComment = root!!.findViewById(R.id.btnShowComment) as Button

        // For addon section
        imgAddOn = root!!.findViewById(R.id.img_add_addon) as ImageView
        chipGroupUserSelectedAddon =
            root!!.findViewById(R.id.chip_group_user_selected_addon) as ChipGroup

        // radio group
        radioGroupSize = root!!.findViewById(R.id.radio_group_size)


        // cart data source
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(requireContext()).cartDAO())

        /** Events */
        // when click on add button for Addon
        imgAddOn!!.setOnClickListener {
            if (Common.FOOD_SELECTED!!.addon!! != null) {
                displayAllAddon()
                addonBottomSheetDialog.show()
            }
        }

        // When click on add rating and comment button (star button in design)
        btnRating!!.setOnClickListener {
            showDialogRating()
        }

        // when click on Show comments button in design
        btnShowComment!!.setOnClickListener {
            val commentFragment = CommentFragment.getInstance()
            commentFragment.show(requireActivity().supportFragmentManager, "CommentFragment")
        }


        // when click on add to cart "shopping" button in food details screen
        btnCart!!.setOnClickListener {
            val cartItem = CartItem()

            cartItem.uid = Common.CURRENT_USER!!.uid
            cartItem.userPhone = Common.CURRENT_USER!!.phone

            cartItem.foodId = Common.FOOD_SELECTED!!.id!!
            cartItem.foodName = Common.FOOD_SELECTED!!.name!!
            cartItem.foodImage = Common.FOOD_SELECTED!!.image!!
            cartItem.foodPrice = Common.FOOD_SELECTED!!.price!!.toDouble()
            cartItem.foodQuantity = numberButton!!.number.toInt()
            cartItem.foodExtraPrice = Common.calculateExtraPrice(
                Common.FOOD_SELECTED!!.userSelectedSize,
                Common.FOOD_SELECTED!!.userSelectedAddon
            )


            if (Common.FOOD_SELECTED!!.userSelectedAddon != null)
                cartItem.foodAddon = Gson().toJson(Common.FOOD_SELECTED!!.userSelectedSize)
            else
                cartItem.foodAddon = "Default"

            if (Common.FOOD_SELECTED!!.userSelectedSize != null)
                cartItem.foodSize = Gson().toJson(Common.FOOD_SELECTED!!.userSelectedSize)
            else
                cartItem.foodSize = "Default"


            /**
             * no for add to database
             */


            cartDataSource.getItemWithAllOptionsInCart(
                Common.CURRENT_USER!!.uid!!,
                cartItem.foodId,
                cartItem.foodSize!!,
                cartItem.foodAddon!!
            ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<CartItem> {
                    override fun onSuccess(cartItemFromDB: CartItem) {
                        if (cartItemFromDB.equals(cartItem)) {
                            // if item already in database, just update
                            cartItemFromDB.foodExtraPrice = cartItem.foodExtraPrice
                            cartItemFromDB.foodAddon = cartItem.foodAddon
                            cartItemFromDB.foodSize = cartItem.foodSize

                            cartItemFromDB.foodQuantity =
                                cartItemFromDB.foodQuantity + cartItem.foodQuantity

                            cartDataSource.updateCart(cartItemFromDB)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object : SingleObserver<Int> {
                                    override fun onSuccess(t: Int) {
                                        /** if updated cart success */
                                        Toast.makeText(
                                            context,
                                            "Update Cart Success",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }

                                    override fun onSubscribe(d: Disposable) {

                                    }

                                    override fun onError(e: Throwable) {
                                        // if update cart failed
                                        Toast.makeText(
                                            context,
                                            "[Update Cart]" + e.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        } else {
                            // if item not available in database, just insert
                            compositeDisposable.add(
                                cartDataSource.insertOrReplace(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        Toast.makeText(
                                            context,
                                            "Add to cart success",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // here we send notify to Home Activity to update counter fab
                                        EventBus.getDefault().postSticky(CountCartEvent(true))

                                    }, { t: Throwable? ->
                                        Toast.makeText(
                                            context,
                                            "[INSERT CART]" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                            )
                        }
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        if (e.message!!.contains("empty")) {
                            compositeDisposable.add(
                                cartDataSource.insertOrReplace(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        Toast.makeText(
                                            context,
                                            "Add to cart success",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // here we send notify to Home Activity to update counter fab
                                        EventBus.getDefault().postSticky(CountCartEvent(true))

                                    }, { t: Throwable? ->
                                        Toast.makeText(
                                            context,
                                            "[INSERT CART]" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                            )
                        } else {
                            Toast.makeText(context, "[CART ERROR]" + e.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                })

        }

    }


    // function for display dialog and select addon to order
    private fun displayUserSelectAddon() {
        if (Common.FOOD_SELECTED!!.userSelectedAddon!! != null
            && Common.FOOD_SELECTED!!.userSelectedAddon!!.size > 0
        ) {
            chipGroupUserSelectedAddon!!.removeAllViews()
            for (addonModel in Common.FOOD_SELECTED!!.userSelectedAddon!!) {
                val chip =
                    layoutInflater.inflate(R.layout.layout_chip_with_delete, null, false) as Chip

                chip.text =
                    StringBuilder(addonModel!!.name!!).append("(+$").append(addonModel!!.price!!)
                        .append(")").toString()

                chip.isCheckable = false
                chip.setOnCloseIconClickListener { view ->
                    chip_group_user_selected_addon!!.removeView(view)
                    Common.FOOD_SELECTED!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                chipGroupUserSelectedAddon!!.addView(chip)
            }
        } else
//            if (Common.FOOD_SELECTED!!.userSelectedAddon!!.size == 0)
        {
            // todo[Event-Bus NOT FIXED]: error when close addon without chosen anything
            //  userSelectedAddon is null ! This food is not have add on
            chipGroupUserSelectedAddon!!.removeAllViews()
        }
    }

    // function for show display rating dialog
    private fun showDialogRating() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Rating Food")
        builder.setMessage("please fill information's")
        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_rating_commnet, null)


        val ratingBarFromDialog = itemView.findViewById<RatingBar>(R.id.rating_bar)
        val edtCommnet = itemView.findViewById<TextView>(R.id.edt_comment)

        builder.setView(itemView)
        builder.setNegativeButton("CACNCEL") { dialogInterface, i -> dialogInterface.dismiss() }
        builder.setPositiveButton("OK") { dialogInterface, i ->
            val commentModel = CommentModel()
            commentModel.name = Common.CURRENT_USER!!.name
            commentModel.uid = Common.CURRENT_USER!!.uid
            commentModel.comment = edtCommnet.text.toString()
            commentModel.ratingValue = ratingBarFromDialog.rating

            val serverTimeStamp = HashMap<String, Any>()
            serverTimeStamp["timeStamp"] = ServerValue.TIMESTAMP
            commentModel.commentTimeStamp = (serverTimeStamp)

            foodDetailsViewModel!!.setCommnetModel(commentModel)
        }

        val dialog = builder.create()
        dialog.show()

    }


    // function for display all addons for order
    private fun displayAllAddon() {
        if (Common.FOOD_SELECTED!!.addon!!.size > 0) {
            chipGroupAddon!!.clearCheck()
            chipGroupAddon!!.removeAllViews()

            edtSearchAddon!!.addTextChangedListener(this)

            for (addonModel in Common.FOOD_SELECTED!!.addon!!) {
                val chip = layoutInflater.inflate(R.layout.layout_chip, null, false) as Chip
                chip.text =
                    StringBuilder(addonModel.name!!).append("(+$").append(addonModel.price!!)
                        .append(")").toString()

                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        if (Common.FOOD_SELECTED!!.userSelectedAddon == null) {
                            Common.FOOD_SELECTED!!.userSelectedAddon = ArrayList()
                        }
                        Common.FOOD_SELECTED!!.userSelectedAddon!!.add(addonModel)
                    }
                }
                chipGroupAddon!!.addView(chip)

            }
        }
    }

    // this 3 methods for addTextChanged
    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(charSequesnce: CharSequence?, start: Int, before: Int, count: Int) {
        chipGroupAddon!!.clearCheck()
        chipGroupAddon!!.removeAllViews()
        for (addonModel in Common.FOOD_SELECTED!!.addon!!) {
            if (addonModel.name!!.toLowerCase().contains(charSequesnce.toString().toLowerCase())) {
                val chip = layoutInflater.inflate(R.layout.layout_chip, null, false) as Chip
                chip.text =
                    StringBuilder(addonModel.name!!).append("(+$").append(addonModel.price!!)
                        .append(")").toString()

                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        if (Common.FOOD_SELECTED!!.userSelectedAddon == null) {
                            Common.FOOD_SELECTED!!.userSelectedAddon = ArrayList()
                        }
                        Common.FOOD_SELECTED!!.userSelectedAddon!!.add(addonModel)
                    }
                }
                chipGroupAddon!!.addView(chip)
            }
        }
    }


}
