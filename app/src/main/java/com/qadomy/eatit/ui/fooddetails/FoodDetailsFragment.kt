package com.qadomy.eatit.ui.fooddetails

import android.os.Bundle
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.qadomy.eatit.R
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.CommentModel
import com.qadomy.eatit.model.FoodModel
import com.qadomy.eatit.ui.comment.CommentFragment
import dmax.dialog.SpotsDialog

class FoodDetailsFragment : Fragment() {

    private lateinit var foodDetailsViewModel: FoodDetailsViewModel
    private var imgFood: ImageView? = null
    private var btnCart: CounterFab? = null
    private var btnRating: FloatingActionButton? = null
    private var foodName: TextView? = null
    private var foodPrice: TextView? = null
    private var foodDescriptoin: TextView? = null
    private var numberButton: ElegantNumberButton? = null
    private var ratingBar: RatingBar? = null
    private var btnShowComment: Button? = null

    private var waitingDialog: android.app.AlertDialog? = null


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
                        val result = sumRating / ratingCount

                        val updateData = HashMap<String, Any>()
                        updateData["ratingValue"] = result
                        updateData["reatingCount"] = ratingCount

                        // Update data is variable
                        foodModel.ratingCount = ratingCount
                        foodModel.ratingValue = result


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
        foodDescriptoin!!.text = StringBuilder(it!!.description!!)
        foodPrice!!.text = StringBuilder(it!!.price!!.toString())

        ratingBar!!.rating = it!!.ratingValue.toFloat()
    }

    private fun initView(root: View?) {
        // dialog
        waitingDialog =
            SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()

        btnCart = root!!.findViewById(R.id.btn_cart)
        imgFood = root!!.findViewById(R.id.img_food)
        btnRating = root!!.findViewById(R.id.btn_rating)
        foodName = root!!.findViewById(R.id.food_name)
        foodDescriptoin = root!!.findViewById(R.id.food_description)
        foodPrice = root!!.findViewById(R.id.food_price)
        numberButton = root!!.findViewById(R.id.number_button)
        ratingBar = root!!.findViewById(R.id.ratingBar)
        btnShowComment = root!!.findViewById(R.id.btnShowComment)


        // When click on add rating and comment button (star button in design)
        btnRating!!.setOnClickListener {
            showDialogRating()
        }

        // when click on Show comments button in design
        btnShowComment!!.setOnClickListener {
            val commentFragment = CommentFragment.getInstance()
            commentFragment.show(requireActivity().supportFragmentManager, "CommentFragment")
        }


    }

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
            commentModel.name = Common.currentUser!!.name
            commentModel.uid = Common.currentUser!!.uid
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


}
