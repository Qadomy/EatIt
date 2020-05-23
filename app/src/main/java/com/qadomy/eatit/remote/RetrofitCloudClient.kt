package com.qadomy.eatit.remote

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitCloudClient {

    //TODO-Braintree-URL:
    // braintree in this project will not worked because we must first upgrade firebase to Blaze plan
    // NOT FREE!!

    private var instance: Retrofit? = null

    fun getInstance(): Retrofit {

        if (instance == null) {
            instance = Retrofit.Builder()
                .baseUrl("https://us-central1-eatit-c18de.cloudfunctions.net/widgets")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
        }
        return instance!!
    }
}