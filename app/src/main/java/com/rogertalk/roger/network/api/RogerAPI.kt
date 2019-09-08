package com.rogertalk.roger.network.api

import com.rogertalk.roger.models.json.*
import com.rogertalk.roger.utils.constant.API_BASE_PATH
import com.rogertalk.roger.utils.constant.API_PROFILE_ME
import com.rogertalk.roger.utils.constant.API_VERSION
import com.rogertalk.roger.utils.constant.AudioConstants.AUDIO_FILE_EXTENSION
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.*
import java.util.*


interface RogerAPI {

    // AUTHENTICATION

    @POST("$API_BASE_PATH/register")
    fun register(@Query("display_name") displayName: String?,
                 @Query("stream_participant") participants: List<String>?): Call<Session>

    @POST("$API_BASE_PATH/challenge")
    fun challenge(@Query("identifier") id: String,
                  @Query("call") call: String): Call<Challenge>

    @POST("$API_BASE_PATH/challenge/respond")
    fun challengeRespond(@Query("identifier") id: String,
                         @Query("secret") secret: String): Call<Session>

    @POST("$API_BASE_PATH/token/{code}")
    fun smsToken(@Path("code") code: String): Call<SmsTokenAnswer>

    @POST("/oauth2/token")
    fun refreshToken(@Query("refresh_token") refreshToken: String,
                     @Query("grant_type") grantType: String = "refresh_token",
                     @Query("api_version") apiVersion: String = "$API_VERSION",
                     @Query("client_id") clientId: String = "android"): Call<Session>


    // USER PROFILE

    @POST(API_PROFILE_ME)
    fun changeDisplayName(@Query("display_name") displayName: String): Call<Account>

    @POST(API_PROFILE_ME)
    fun changeUsername(@Query("username") username: String): Call<Account>

    @POST(API_PROFILE_ME)
    fun changeShareLocation(@Query("share_location") shareLocation: Boolean): Call<Account>

    @POST(API_PROFILE_ME)
    fun updateLocation(@Query("location") formattedLocation: String): Call<Account>

    @Multipart
    @POST(API_PROFILE_ME)
    fun changeUserImage(@Part("image\"; filename=\"image.jpg") image: RequestBody): Call<Account>

    // OTHER USER PROFILES

    @GET("$API_BASE_PATH/profile/{account-id}")
    fun userProfile(@Path("account-id") accountHandle: String): Call<Profile>

    // GCM

    @FormUrlEncoded
    @POST("$API_BASE_PATH/device")
    fun device(@Field("device_token") deviceToken: String,
               @Query("device_id") deviceUUID: String?,
               @Query("platform") platform: String): Call<DeviceRegister>


    // STREAMS

    @POST("$API_BASE_PATH/streams")
    fun createConversation(
            @Query("title") title: String?,
            @Query("show_in_recents") visible: Boolean = false,
            @Query("shareable") shareable: Boolean = true): Call<Stream>

    @POST("$API_BASE_PATH/streams")
    fun createStream(@Query("participant") participantMetadata: ArrayList<String>,
                     @Query("show_in_recents") showInRecents: Boolean,
                     @Query("shareable") shareable: Boolean): Call<Stream>

    @POST("$API_BASE_PATH/streams")
    fun joinOpenGroup(@Query("invite_token") inviteToken: String): Call<Stream>

    @DELETE("$API_BASE_PATH/streams/{stream-id}")
    fun leaveGroup(@Path("stream-id") streamId: Long): Call<Any>

    @POST("$API_BASE_PATH/streams/{stream-id}/participants")
    fun streamAddParticipants(@Path("stream-id") streamId: Long,
                              @Query("participant") participants: List<String>): Call<Stream>


    @DELETE("$API_BASE_PATH/streams/{stream-id}/participants")
    fun streamRemoveParticipants(@Path("stream-id") streamId: Long,
                                 @Query("participant") participants: List<String>): Call<Stream>

    @Multipart
    @POST("$API_BASE_PATH/streams/{stream-id}/chunks")
    fun sendAudioChunkWithToken(@Path("stream-id") streamId: String,
                                @Query("duration") audioDuration: String,
                                @Query("chunk_token") chunk_token: String?,
                                @Part("audio\"; filename=\"audio.$AUDIO_FILE_EXTENSION") audioBlob: RequestBody,
                                @Query("persist") persist: Boolean = true): Call<Stream>

    @GET
    fun audioDownload(@Url url: String): Call<ResponseBody>

    @GET("$API_BASE_PATH/streams")
    fun streams(): Call<StreamsResponse>

    @GET("$API_BASE_PATH/streams")
    fun nextStreams(@Query("cursor") cursor: String): Call<StreamsResponse>

    @GET("$API_BASE_PATH/streams/{stream-id}")
    fun stream(@Path("stream-id") streamId: Long): Call<Stream>

    @POST("$API_BASE_PATH/streams/{stream-id}")
    fun updateStreamPlayedUntil(@Path("stream-id") streamId: String,
                                @Query("played_until") refreshToken: String): Call<Stream>

    @POST("$API_BASE_PATH/streams/{stream-id}")
    fun updateStreamStatus(@Path("stream-id") streamId: String,
                           @Query("status") status: String,
                           @Query("status_estimated_duration") estimatedDuration: Long?): Call<Stream>

    @POST("$API_BASE_PATH/streams/{stream-id}")
    fun makeStreamShareable(@Path("stream-id") streamId: String,
                            @Query("shareable") shareable: Boolean = true): Call<Stream>

    @POST("$API_BASE_PATH/streams/{stream-id}")
    fun showStream(@Path("stream-id") streamId: String,
                   @Query("visible") visible: Boolean = true): Call<Stream>

    @POST("$API_BASE_PATH/streams/{stream-id}/buzz")
    fun buzz(@Path("stream-id") streamId: Long): Call<Stream>

    @POST("$API_BASE_PATH/contacts")
    fun activeContacts(@Body() contactList: RequestBody): Call<NumberToActiveContactMapContainer>

    @POST("$API_BASE_PATH/streams/{stream-id}")
    fun updateStreamTitle(@Path("stream-id") streamId: Long,
                          @Query("title") newTitle: String): Call<Stream>


    // ATTACHMENTS


    @POST("$API_BASE_PATH/streams/{stream-id}/attachments/{attachment-name}")
    fun sendLinkAttachment(@Path("stream-id") streamId: Long,
                           @Path("attachment-name") attachmentName: String,
                           @Query("data") data: JSONObject): Call<Stream>

    @Multipart
    @POST("$API_BASE_PATH/streams/{stream-id}/attachments/{attachment-name}")
    fun sendImageAttachment(@Path("stream-id") streamId: Long,
                            @Path("attachment-name") attachmentName: String,
                            @Query("data") data: JSONObject,
                            @Part image: MultipartBody.Part): Call<Stream>

    // WEATHER

    @GET("$API_BASE_PATH/weather")
    fun weather(@Query("identifiers") identifiers: String): Call<WeatherList>

    // REPORTING

    @POST("$API_BASE_PATH/report/DeviceEventV1")
    fun reportStereoFailure(@Query("event_name") eventName: String,
                            @Query("device_model") model: String,
                            @Query("system_version") osVersion: Int): Call<Any>


    @POST("$API_BASE_PATH/report/OperatorV1")
    fun reportOperator(@Query("operator_name") operatorName: String,
                       @Query("mcc") mcc: String,
                       @Query("mnc") mnc: String): Call<Any>

    // INVITES

    @POST("$API_BASE_PATH/invite")
    fun inviteViaSMS(@Query("identifier") identifiers: List<String>,
                     @Query("name") names: List<String>,
                     @Query("invite_token") inviteToken: String?): Call<NumberToActiveContactMapContainer>

    // BOTS and SERVICES

    @GET("$API_BASE_PATH/services")
    fun services(): Call<BotList>

    @GET("$API_BASE_PATH/bots")
    fun bots(): Call<BotList>

}