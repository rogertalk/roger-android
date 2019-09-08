package com.rogertalk.roger.ui.screens

import android.Manifest
import android.os.Bundle
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.rogertalk.roger.R
import com.rogertalk.roger.event.failure.ShareLocationFailEvent
import com.rogertalk.roger.event.success.ShareLocationSuccessEvent
import com.rogertalk.roger.helper.ProgressDialogHelper
import com.rogertalk.roger.network.request.UpdateShareLocationRequest
import com.rogertalk.roger.repo.LocationRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.utils.extensions.materialize
import com.rogertalk.roger.utils.log.logDebug
import kotlinx.android.synthetic.main.enable_weather_screen.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.enabled
import org.jetbrains.anko.longToast
import kotlin.LazyThreadSafetyMode.NONE

class WeatherEnableActivity : EventAppCompatActivity(true),
        PermissionListener {


    private var coarseLocationPermissionListener: PermissionListener? = null
    private val progressDialogHelper: ProgressDialogHelper by lazy(NONE) { ProgressDialogHelper(this) }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.enable_weather_screen)
        setupUI()
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        val permissionName = response?.permissionName ?: ""
        logDebug { "Enabled permission: $permissionName" }
        if (permissionName == Manifest.permission.ACCESS_COARSE_LOCATION) {
            reassessEnableLocation()
        }
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        // TODO: explain to the user why we need this permission
        token?.continuePermissionRequest()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        val permissionName = response?.permissionName ?: ""
        val permanentlyDenied = response?.isPermanentlyDenied ?: true
        logDebug { "Denied permission: $permissionName, permanently: $permanentlyDenied" }
    }

    //
    // PRIVATE METHODS
    //

    private fun setLoading(isLoading: Boolean) {
        enableLocationBtn.setLoadingState(isLoading)
        if (isLoading) {
            progressDialogHelper.showWaiting()
        } else {
            progressDialogHelper.dismiss()
        }
    }


    private fun setupUI() {
        enableLocationBtn.setOnClickListener { enableLocationPressed() }

        // set material animation for previous versions of Android
        enableLocationBtn.materialize()

        laterBtn.setOnClickListener { dismissPressed() }
    }

    private fun enableLocationPressed() {
        enableLocationBtn.enabled = false
        reassessEnableLocation()
    }

    private fun reassessEnableLocation() {
        // Check if device permission is in place
        if (!LocationRepo.locationAtSystemLevel) {
            requestLocationPermission()
            return
        }

        // Check if account has location enabled
        if (!LocationRepo.locationAtSessionLevel) {
            enableLocationForAccount()
            return
        }

        // All Good, finish this screen.
        finish()
    }

    private fun requestLocationPermission() {
        // TODO : Review permission copy
        val dialogOnDeniedPermissionListener =
                DialogOnDeniedPermissionListener.Builder.withContext(this)
                        .withTitle(R.string.glimpses_permission_title)
                        .withMessage(R.string.glimpses_permission_description)
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()

        coarseLocationPermissionListener = CompositePermissionListener(this,
                dialogOnDeniedPermissionListener)

        if (!Dexter.isRequestOngoing()) {
            Dexter.checkPermission(coarseLocationPermissionListener, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun enableLocationForAccount() {
        setLoading(true)
        UpdateShareLocationRequest(true).enqueueRequest()
    }

    private fun dismissPressed() {
        finish()
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateShareLocationSuccess(event: ShareLocationSuccessEvent) {
        logDebug { "Location shared at account level with success" }
        UserAccountRepo.updateAccount(event.account)
        setLoading(false)
        reassessEnableLocation()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateShareLocationFailure(event: ShareLocationFailEvent) {
        setLoading(false)
        longToast(R.string.glimpses_failed_to_update)
    }

}
