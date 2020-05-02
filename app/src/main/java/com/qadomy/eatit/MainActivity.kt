package com.qadomy.eatit

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.qadomy.eatit.common.Common
import com.qadomy.eatit.model.UserModel
import dmax.dialog.SpotsDialog
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private lateinit var userRef: DatabaseReference
    private lateinit var dialog: AlertDialog
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private val compositeDisposable = CompositeDisposable()
    private lateinit var firebaseAuth: FirebaseAuth
    private var providers: List<AuthUI.IdpConfig>? = null


    companion object {
        private const val APP_REQUEST_CODE = 14123
    }

    // onStart
    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    // onStop
    override fun onStop() {
        if (listener != null) {
            firebaseAuth.removeAuthStateListener(listener)
        }
        compositeDisposable.clear()
        super.onStop()
    }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init() {
        providers = listOf(AuthUI.IdpConfig.PhoneBuilder().build())

        userRef = FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCE)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        listener =
            FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                // if user login with phone number, check if it's in database
                if (user != null) {
                    checkUserFromFirebase(user)
                } else {
                    // if not login
                    phoneLogin()
                }
            }
    }

    private fun phoneLogin() {
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers!!)
                .build(),
            APP_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(this@MainActivity, "Failed ti sign in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** function for check if the user exists in the database or not */
    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog.show()
        userRef.child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@MainActivity, "" + p0.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    // if user exists in database get the full info from database and send it to home page (Profile)
                    val userModel = p0.getValue(UserModel::class.java)
                    goToHomeActivity(userModel)

                } else {
                    // if user not exists open register dialog
                    showRegisterDialog(user)

                }

                // dismiss the dialog
                dialog.dismiss()
            }

        })
    }

    /** function for showing dialog with register new account */
    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("REGISTER")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(this@MainActivity)
            .inflate(R.layout.layout_register, null)

        val edtName = itemView.findViewById<EditText>(R.id.edt_name)
        val edtAddress = itemView.findViewById<EditText>(R.id.edt_address)
        val edtPhone = itemView.findViewById<EditText>(R.id.edt_phone)

        // set the phone number directly cause we already login with phone number
        edtPhone.setText(user.phoneNumber)

        builder.setView(itemView)

        /** when click on cancel button */
        builder.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }

        /** when click on register button */
        builder.setNegativeButton("REGISTER") { dialogInterface, _ ->
            if (TextUtils.isDigitsOnly(edtName.text.toString())) {
                Toast.makeText(this@MainActivity, "Please enter your name", Toast.LENGTH_SHORT)
                    .show()
                return@setNegativeButton
            } else if (TextUtils.isDigitsOnly(edtAddress.text.toString())) {
                Toast.makeText(this@MainActivity, "Please enter your address", Toast.LENGTH_SHORT)
                    .show()
                return@setNegativeButton
            }

            // we collect all data in class object
            val userModel = UserModel()
            userModel.uid = user.uid
            userModel.name = edtName.text.toString()
            userModel.address = edtAddress.text.toString()
            userModel.phone = edtPhone.text.toString()

            // and save data in firebase
            userRef.child(user.uid).setValue(userModel)
                .addOnCompleteListener { task ->
                    /** if data saved successfully in database */
                    if (task.isSuccessful) {
                        dialogInterface.dismiss()
                        Toast.makeText(
                            this@MainActivity,
                            "Congratulations! Register success",
                            Toast.LENGTH_SHORT
                        ).show()
                        // and go to home profile
                        goToHomeActivity(userModel)
                    }
                }
        }

        /** show dialog */
        val dialog = builder.create()
        dialog.show()

    }

    private fun goToHomeActivity(userModel: UserModel?) {
        Common.currentUser = userModel!!
        startActivity(Intent(this@MainActivity,HomeActivity::class.java))
        finish()
    }
}
