package com.qadomy.eatit.ui.fooddetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.qadomy.eatit.R
import com.qadomy.eatit.model.FoodModel

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
        return root
    }

    private fun displayInfo(it: FoodModel?) {
        Glide.with(requireContext()).load(it!!.image).into(imgFood!!)
        foodName!!.text = StringBuilder(it!!.name!!)
        foodDescriptoin!!.text = StringBuilder(it!!.description!!)
        foodPrice!!.text = StringBuilder(it!!.price!!.toString())
    }

    private fun initView(root: View?) {
        btnCart = root!!.findViewById(R.id.btn_cart)
        imgFood = root!!.findViewById(R.id.img_food)
        btnRating = root!!.findViewById(R.id.btn_rating)
        foodName = root!!.findViewById(R.id.food_name)
        foodDescriptoin = root!!.findViewById(R.id.food_description)
        foodPrice = root!!.findViewById(R.id.food_price)
        numberButton = root!!.findViewById(R.id.number_button)
        ratingBar = root!!.findViewById(R.id.ratingBar)
        btnShowComment = root!!.findViewById(R.id.btnShowComment)


    }


}
