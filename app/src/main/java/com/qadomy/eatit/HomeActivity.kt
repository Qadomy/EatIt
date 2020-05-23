package com.qadomy.eatit

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.database.CartDataSource
import com.qadomy.eatit.database.CartDatabase
import com.qadomy.eatit.database.LocalCartDataSource
import com.qadomy.eatit.eventbus.*
import com.qadomy.eatit.model.CategoryModel
import com.qadomy.eatit.model.FoodModel
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var cartDataSource: CartDataSource
    private lateinit var drawerLayout: DrawerLayout
    private var dialog: AlertDialog? = null
    private val TAG = "HomeActivity"

    // navController
    private lateinit var navController: NavController


    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // init dialog
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        /** init cartDataSource */
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDAO())


        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            // when click on fab button in home screen to see all carts
            navController.navigate(R.id.nav_cart)

        }
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_menu, R.id.nav_cart
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // for header view
        var headerView = navView.getHeaderView(0)
        var textUser = headerView.findViewById<TextView>(R.id.txt_user)
        Common.setSpanString("Hey, ", Common.CURRENT_USER!!.name, textUser)


        // when click on items in menu
        navView.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {

                item.isChecked = true
                drawerLayout!!.closeDrawers() // Close all currently open drawer views by animating them out of view.


                when (item.itemId) {
                    R.id.nav_sign_out -> {
                        // when click on sign out menu
                        signOut()

                    }
                    R.id.nav_home -> {
                        navController.navigate(R.id.nav_home)
                    }
                    R.id.nav_menu -> {
                        navController.navigate(R.id.nav_menu)
                    }
                    R.id.nav_cart -> {
                        navController.navigate(R.id.nav_cart)
                    }
                }
                return true
            }
        })

        counterCartItem()
    }

    // onResume
    override fun onResume() {
        super.onResume()
        counterCartItem()
    }

    // function when click on sign out menu
    private fun signOut() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Sign out")
            .setMessage("Are You Sure Want Exit?")
            .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
            .setPositiveButton("OK") { dialogInterface, _ ->
                // when click on ok for sign out

                Common.FOOD_SELECTED = null
                Common.CATEGORY_SELECTED = null
                Common.CURRENT_USER = null

                // sign out form firebase
                FirebaseAuth.getInstance().signOut()

                // make intent to main activity "Register"
                val intent = Intent(this@HomeActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()


            }

        val dialog = builder.create()
        dialog.show()
    }


    /**
     * Menu
     */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Event Bus
     */

    // register event bus in onStart
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    // unregister event bus in onStart
    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    // here what happened when click on any item in category items
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event: CategoryClick) {
        if (event.isSuccess) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_foodList)
        }
    }

    // event here what happened when click on any item in category items
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onFoodSelected(event: FoodItemClick) {
        if (event.isSuccess) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_details)
        }
    }


    // event for update counter fab when add any item to cart
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCounterCartEvent(event: CountCartEvent) {
        if (event.isSuccess) {
            counterCartItem()
        }
    }


    // event when click on Popular food items
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPopularFoodItemClick(event: PopularFoodItemClick) {
        if (event.popularCategoryModel != null) {
            //
            Log.d(TAG, "onPopularFoodItemClick")
            dialog!!.show()

            // todo[NOT FIXED]: not worked
            try {
                FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.popularCategoryModel!!.menuId!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                            Log.d(TAG, "onCancelled 1")

                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity, "" + p0.message, Toast.LENGTH_SHORT)
                                .show()
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            if (p0.exists()) {
                                Log.d(TAG, "onDataChange exists")

                                Common.CATEGORY_SELECTED = p0.getValue(CategoryModel::class.java)
                                Common.CATEGORY_SELECTED!!.menuId = p0.key

                                // load food from firebase
                                FirebaseDatabase.getInstance()
                                    .getReference("Category")
                                    .child(event.popularCategoryModel!!.menuId!!)
                                    .child("foods")
                                    .orderByChild("id")
                                    .equalTo(event.popularCategoryModel.foodId)
                                    .limitToLast(1)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onCancelled(p0: DatabaseError) {

                                            Log.d(TAG, "onCancelled 2")

                                            dialog!!.dismiss()
                                            Toast.makeText(
                                                this@HomeActivity,
                                                "" + p0.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        override fun onDataChange(p0: DataSnapshot) {
                                            if (p0.exists()) {
                                                Log.d(TAG, "onDataChange exists 2")

                                                for (foodSnapShot in p0.children) {
                                                    Common.FOOD_SELECTED =
                                                        foodSnapShot.getValue(FoodModel::class.java)
                                                    Common.FOOD_SELECTED!!.key = foodSnapShot.key
                                                }
                                                navController!!.navigate(R.id.nav_food_details)
                                            } else {
                                                Toast.makeText(
                                                    this@HomeActivity,
                                                    "Item doesn't exist",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            dialog!!.dismiss()

                                        }
                                    })

                            } else {
                                dialog!!.dismiss()
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Item doesn't exist",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            } catch (e: Exception) {
                dialog!!.dismiss()
                Log.d(TAG, "message " + e.message)
            }
        }
    }


    // event when click on Best Deal food items
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBestDealFoodItemClick(event: BestDealItemClick) {
        if (event.bestDealModel != null) {
            //
            Log.d(TAG, "onPopularFoodItemClick")
            dialog!!.show()

            // todo[NOT FIXED]: not worked
            try {
                FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.bestDealModel!!.menuId!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                            Log.d(TAG, "onCancelled 1")

                            dialog!!.dismiss()
                            Toast.makeText(this@HomeActivity, "" + p0.message, Toast.LENGTH_SHORT)
                                .show()
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            if (p0.exists()) {
                                Log.d(TAG, "onDataChange exists")

                                Common.CATEGORY_SELECTED = p0.getValue(CategoryModel::class.java)
                                Common.CATEGORY_SELECTED!!.menuId = p0.key

                                // load food from firebase
                                FirebaseDatabase.getInstance()
                                    .getReference("Category")
                                    .child(event.bestDealModel!!.menuId!!)
                                    .child("foods")
                                    .orderByChild("id")
                                    .equalTo(event.bestDealModel.foodId)
                                    .limitToLast(1)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onCancelled(p0: DatabaseError) {

                                            Log.d(TAG, "onCancelled 2")

                                            dialog!!.dismiss()
                                            Toast.makeText(
                                                this@HomeActivity,
                                                "" + p0.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }

                                        override fun onDataChange(p0: DataSnapshot) {
                                            if (p0.exists()) {
                                                Log.d(TAG, "onDataChange exists 2")

                                                for (foodSnapShot in p0.children) {
                                                    Common.FOOD_SELECTED =
                                                        foodSnapShot.getValue(FoodModel::class.java)
                                                    Common.FOOD_SELECTED!!.key = foodSnapShot.key
                                                }
                                                navController!!.navigate(R.id.nav_food_details)
                                            } else {
                                                Toast.makeText(
                                                    this@HomeActivity,
                                                    "Item doesn't exist",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            dialog!!.dismiss()

                                        }
                                    })

                            } else {
                                dialog!!.dismiss()
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Item doesn't exist",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            } catch (e: Exception) {
                dialog!!.dismiss()
                Log.d(TAG, "message " + e.message)
            }
        }
    }

    // event for remove fab button when go to cart screen
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onHideFABcartEvent(event: HideFABcart) {
        if (event.isHide) {
            fab.hide()
        } else {
            fab.show()
        }
    }

    private fun counterCartItem() {
        // create RxJava
        cartDataSource.countItemInCart(Common.CURRENT_USER!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Int> {
                override fun onSuccess(t: Int) {
                    fab.count = t
                }

                override fun onSubscribe(d: Disposable) {
                }

                override fun onError(e: Throwable) {

                    if (!e.message!!.contains("Query returned empty")) {
                        Toast.makeText(
                            this@HomeActivity,
                            "[COUNT CART]" + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        fab.count = 0
                    }
                }

            })
    }


}
