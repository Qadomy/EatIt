package com.qadomy.eatit.remote

import com.qadomy.eatit.model.FCMResponse
import com.qadomy.eatit.model.FCMSendData
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMService {

    /**
     *
     *
     * This interface for send notification for this app to display it in server app when make palace order
     *
     *
     *
     */


    /**
     * the Authorization:key == Server key ->  we get it from cloud messaging from setting in firebase website
     */
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAAqL6trlI:APA91bHMcxhOFpXlmN9ejZkamriOuE9I2m7jNEi6_Soy7uTOZ2bU2vnW3_zbQSTRmGFXqMlyyx2Qiu6fIBcNJg4gLBe0hui3drkBCo4A6D-dWtFBp9J_P-ojC0DJuC6y7-klhPbqtqoX"
    )
    @POST("fom/send")
    fun sendNotification(@Body body: FCMSendData): Observable<FCMResponse>
}