package com.qadomy.eatit.database

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
interface CartDAO {
    // query
    @Query("SELECT * FROM Cart WHERE uid=:uid")
    fun getAllCart(uid: String): Flowable<List<CartItem>>

    @Query("SELECT COUNT(*) FROM Cart WHERE uid=:uid")
    fun countItemInCart(uid: String): Single<Int>

    @Query("SELECT SUM(foodQuantity*foodPrice)+(foodExtraPrice*foodQuantity) FROM Cart WHERE uid=:uid")
    fun sumPrice(uid: String): Single<Long>

    @Query("SELECT * FROM Cart WHERE foodId=:foodId AND uid=:uid")
    fun getItemInCart(foodId: String, uid: String): Single<CartItem>

    // insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(vararg cartItem: CartItem): Completable

    // update
    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCart(cart: CartItem): Single<Int>

    // delete
    @Delete
    fun deleteCart(cart: CartItem): Single<Int>

    // delete query
    @Query("DELETE FROM Cart WHERE uid=:uid")
    fun cleanCart(uid: String): Single<Int>

}