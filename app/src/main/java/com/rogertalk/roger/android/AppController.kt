package com.rogertalk.roger.android

import android.app.Application
import android.content.Context
import android.os.Handler
import android.support.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import com.karumi.dexter.Dexter
import com.rogertalk.kotlinjubatus.utils.DeviceUtils
import com.rogertalk.roger.BuildConfig
import com.rogertalk.roger.audio.AudioSystem
import com.rogertalk.roger.manager.PendingNotificationManager
import com.rogertalk.roger.network.request.reporting.OperatorDetailsRequest
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.SessionRepo
import com.rogertalk.roger.utils.extensions.DelegatesExt
import io.fabric.sdk.android.Fabric
import kotlin.LazyThreadSafetyMode.NONE


class AppController : Application() {

    companion object {
        var instance: AppController by DelegatesExt.notNullSingleValue()
    }

    val helper: AppControllerHelper by lazy { AppControllerHelper(this) }

    val applicationHandler: Handler by lazy(NONE) { Handler(applicationContext.mainLooper) }

    val backgroundManager: BackgroundManager by lazy(NONE) { BackgroundManager(this) }

    var lastGCMToken = ""

    var fabricInitialized = false

    fun canUseFabric(): Boolean {
        if (!BuildConfig.DEBUG) {
            return fabricInitialized
        }
        return false
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize helper now
        helper.appVersion

        // This needs to be the first thing we execute
        setupCrashlytics()

        // Call Post init method on AppHelper
        helper.postInit()

        setupDexter()

        // Report operator details
        if (!PrefRepo.reportedOperatorDetails) {
            OperatorDetailsRequest().enqueueRequest()
        }

        // Initialize background manager
        backgroundManager.isInBackground

        // Schedule verification of expiring chunks
        PendingNotificationManager.registerUnheardAudioCheckRepeat()
    }

    private fun setupDexter() {
        Dexter.initialize(this)
    }

    private fun setupCrashlytics() {
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())

            // Add extra info about the user to the report
            val session = SessionRepo.session
            if (session != null) {
                val accountID = session.account.id
                val displayName = session.account.displayName
                Crashlytics.setUserIdentifier(accountID.toString())
                Crashlytics.setUserName(displayName)
            }

            // Add some "could be relevant" info
            Crashlytics.setString("SamplingRateIndex", PrefRepo.samplingRateIndex.toString())
            Crashlytics.setString("Record Stereo", AudioSystem.recordInStereo.toString())
            Crashlytics.setString("Voicemail configured", PrefRepo.voicemailConfigured.toString())
            Crashlytics.setString("Device designation", DeviceUtils.deviceName)
            Crashlytics.setString("Experimental Mode", PrefRepo.godMode.toString())

            fabricInitialized = true
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        helper.onTerminate()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}
