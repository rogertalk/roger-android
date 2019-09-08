package com.rogertalk.roger.android

import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.HawkBuilder
import com.orhanobut.hawk.LogLevel
import com.rogertalk.kotlinjubatus.utils.AppStatsUtils
import com.rogertalk.roger.BuildConfig
import com.rogertalk.roger.manager.GlobalManager
import com.rogertalk.roger.manager.MigrationManager
import com.rogertalk.roger.manager.PushNotificationManager
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.data.NotificationData
import com.rogertalk.roger.network.RogerHttpClient
import com.rogertalk.roger.repo.ActiveContactsRepo
import com.rogertalk.roger.repo.DeviceProfileRepo
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.constant.API_ENDPOINT_PRODUCTION
import com.rogertalk.roger.utils.contact.device.DeviceUserProfile
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.state.TaskStates
import io.realm.Realm
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.EventBus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.LazyThreadSafetyMode.NONE

class AppControllerHelper(val controller: AppController) {

    val retrofit: Retrofit
    val okHttpClient: OkHttpClient
    val deviceUserProfile: DeviceUserProfile by lazy(NONE) { DeviceProfileRepo.getUserProfileToCache(controller) }

    val appVersion: String by lazy(NONE) { AppStatsUtils.getAppVersionCode(appController()) }

    // In-memory notification data
    var unPlayedNotificationList = CopyOnWriteArrayList<NotificationData>()
    var buzzNotificationList = CopyOnWriteArrayList<NotificationData>()

    private var realm: Realm? = null

    // Maintains the state of executing task
    val taskStates = HashMap<Int, Boolean>(TaskStates.TASK_COUNT)

    // Just a memory placeholder for device contacts, so we don't probe it more than once
    // per session. ID is device ID
    var deviceContacts: LinkedHashMap<Long, DeviceContactInfo>

    init {
        okHttpClient = RogerHttpClient.buildHttpClient()

        retrofit = Retrofit.Builder()
                .baseUrl(API_ENDPOINT_PRODUCTION)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        initHawk()
        initEventBus()
        initRealm()

        deviceContacts = PrefRepo.deviceContactsMap.deviceContacts
    }

    private fun initEventBus() {
        if (BuildConfig.DEBUG) {
            EventBus.builder().logNoSubscriberMessages(false)
                    .sendNoSubscriberEvent(false).installDefaultEventBus()
        }
    }

    private fun initRealm() {
        val realmConfigBuilder = RealmConfiguration.Builder(appController())
        realmConfigBuilder.schemaVersion(12)

        // TODO : do proper migration in the future
        realmConfigBuilder.deleteRealmIfMigrationNeeded()
        val realmConfig = realmConfigBuilder.build()

        realm = Realm.getInstance(realmConfig)
    }

    private fun initHawk() {
        Hawk.init(controller)
                // TODO : convert this to highest security and implement callbacks to speed up init
                .setEncryptionMethod(HawkBuilder.EncryptionMethod.MEDIUM)
                .setPassword("roger") // TODO : generate password and store under user wallet
                .setStorage(HawkBuilder.newSharedPrefStorage(controller))
                .setLogLevel(LogLevel.NONE)
                .build()
    }

    fun reInitHawk() {
        Hawk.destroy()
        initHawk()
    }

    /**
     * This method gets called by [AppController] after the class gets initialized.
     * Right now this is the best way for having a post-init method.
     */
    fun postInit() {
        PushNotificationManager.initPushNotifications()

        // Initialize active contacts
        ActiveContactsRepo.activeContacts

        // Recover Seen Attachments
        GlobalManager.initializeSeenAttachment()

        // Execute data migrations now!
        MigrationManager.executeNeededMigrations()
    }

    fun onTerminate() {
        realm?.close()
    }

    fun getRealm(): Realm {
        return realm ?: Realm.getDefaultInstance()
    }
}
