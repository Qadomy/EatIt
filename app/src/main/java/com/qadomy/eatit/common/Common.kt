package com.qadomy.eatit.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.qadomy.eatit.R
import com.qadomy.eatit.model.*
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.random.Random

object Common {
    fun formatPrice(price: Double): String {
        if (price != 0.toDouble()) {
            val df = DecimalFormat("#,##0.00")
            df.roundingMode = RoundingMode.HALF_UP
            val finalPrice = StringBuilder(df.format(price)).toString()
            return finalPrice.replace(".", ",")
        } else {
            return "0,00"
        }
    }

    fun calculateExtraPrice(
        userSelectedSize: SizeModel?,
        userSelectedAddon: MutableList<AddonModel>?
    ): Double {
        var result: Double = 0.0

        if (userSelectedSize == null && userSelectedAddon == null) return 0.0
        else if (userSelectedSize == null) {
            for (addonModel in userSelectedAddon!!)
                result += addonModel.price!!.toDouble()
            return result
            //
        } else if (userSelectedAddon == null) {
            result = userSelectedSize!!.price.toDouble()
            return result
            //
        } else {
            result = userSelectedSize!!.price.toDouble()
            for (addonModel in userSelectedAddon!!)
                result += addonModel.price!!.toDouble()
            return result
            //
        }
    }

    fun setSpanString(welcome: String, name: String?, textUser: TextView?) {
        val builder = SpannableStringBuilder()
        builder.append(welcome)

        val txtSpannable = SpannableString(name)
        val boldSpan = StyleSpan(Typeface.BOLD)
        txtSpannable.setSpan(boldSpan, 0, name!!.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append(txtSpannable)

        textUser!!.setText(builder, TextView.BufferType.SPANNABLE)
    }

    // function for create random order number
    fun createOrderNumber(): String {
        return StringBuilder()
            .append(System.currentTimeMillis())// Returns the current time in milliseconds
            .append(abs(Random.nextInt()))
            .toString()
    }

    fun getDateOfWeek(i: Int): String {
        when (i) {
            1 -> return "Monday"
            2 -> return "Tuesday"
            3 -> return "Wednesday"
            4 -> return "Thursday"
            5 -> return "Friday"
            6 -> return "Saturday"
            7 -> return "Sunday"
            else -> return "Unk"

        }
    }

    fun convertStatusToText(orderStatus: Int): String {
        when (orderStatus) {

            0 -> return "Placed"
            1 -> return "Shipping"
            2 -> return "Shipped"
            -1 -> return "Cancelled"
            else -> return "Unk"
        }
    }

    // update token from firebase cloud messaging
    fun updateToken(context: Context, token: String) {
        FirebaseDatabase.getInstance().getReference(Common.TOKEN_REF)
            .child(Common.CURRENT_USER!!.uid!!)
            .setValue(TokenModel(Common.CURRENT_USER!!.phone!!, token))
            .addOnFailureListener { e ->
                Toast.makeText(context, "" + e.message, Toast.LENGTH_SHORT).show()
            }
    }


    // show notification
    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        content: String?,
        intent: Intent?
    ) {
        var pendingIntent: PendingIntent? = null
        if (intent != null)
            pendingIntent =
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val NOTIFICATION_CHANNEL_ID = "qadomy.dev.eatit"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Eat It",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationChannel.description = "Eat It"
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = (Color.RED)
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            // create notification
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)

        // build notification
        builder.setContentTitle(title!!).setContentText(content!!).setAutoCancel(true)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_restaurant_menu_black
                )
            )

        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent)

        val notification = builder.build()

        notificationManager.notify(id, notification)
    }


    fun getNewOrderTopic(): String {
        return StringBuilder("/topics/new_order").toString()
    }


    var AUTHORISE_TOKEN: String? = null
    var CURRENT_TOKEN: String = ""
    var CURRENT_USER: UserModel? = null
    var FOOD_SELECTED: FoodModel? = null
    var CATEGORY_SELECTED: CategoryModel? = null
    const val ORDER_REF: String = "Order"
    const val COMMENT_REF: String = "Comments"
    const val DEFAULT_COLUMN_COUNT: Int = 0
    const val FULL_WIDTH_COLUMN: Int = 1
    const val BEST_DEALS_REF: String = "BestDeals"
    const val POPULAR_REF: String = "MostPopular"
    const val USER_REFERENCE = "Users"
    const val CATEGORY_REF: String = "Category"
    const val TOKEN_REF: String = "Tokens"
    const val NOTI_CONTENT: String = "content"
    const val NOTI_TITLE: String = "title"

}