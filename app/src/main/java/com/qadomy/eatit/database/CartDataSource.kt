package com.qadomy.eatit.database

import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

interface CartDataSource {
    fun getAllCart(uid: String): Flowable<List<CartItem>>

    fun countItemInCart(uid: String): Single<Int>

    fun sumPrice(uid: String): Single<Long>

    fun getItemInCart(foodId: String, uid: String): Single<CartItem>

    fun insertOrReplace(vararg cartItem: CartItem): Completable

    fun updateCart(cart: CartItem): Single<Int>

    fun deleteCart(cart: CartItem): Single<Int>

    fun cleanCart(uid: String): Single<Int>

}