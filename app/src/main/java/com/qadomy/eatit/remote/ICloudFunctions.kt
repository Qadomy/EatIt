package com.qadomy.eatit.remote

import com.qadomy.eatit.model.BraintreeToken
import com.qadomy.eatit.model.BraintreeTransaction
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ICloudFunctions {

    //TODO-Braintree:
    // braintree in this project will not worked because we must first upgrade firebase to Blaze plan
    // NOT FREE!!

    @GET("token")
    fun getToken(): Observable<BraintreeToken>

    @POST("checkout")
    @FormUrlEncoded
    fun submitPayment(
        @Field("amount") amount: Double,
        @Field("payment_method_nonce") nonce: String
    ): Observable<BraintreeTransaction>
}