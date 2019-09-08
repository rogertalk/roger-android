package com.rogertalk.roger.android.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.rogertalk.roger.event.failure.LocationUpdateFailEvent
import com.rogertalk.roger.event.success.LocationUpdateSuccessEvent
import com.rogertalk.roger.network.request.UpdateLocationRequest
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.google.PlayServices
import com.rogertalk.roger.utils.log.logEvent
import com.rogertalk.roger.utils.log.logMethodCall
import com.rogertalk.roger.utils.log.logWarn
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import java.util.*

/**
 * This service is responsible for periodic location reporting to the server for Glimpses.
 */
class GlimpsesService : EventService(),
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    companion object {
        private val EXTRA_RUN: String = "run"
        private val RUN_INTERVAL = 300000 // 5 min in milliseconds

        fun start(context: Context): Intent {
            val intent = Intent(context, GlimpsesService::class.java)
            intent.putExtra(EXTRA_RUN, true)
            return intent
        }

    }

    private var googleApiClient: GoogleApiClient? = null

    //
    // OVERRIDE METHODS
    //

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handleRequests(intent)
            return Service.START_STICKY
        }
        return Service.START_NOT_STICKY
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        logWarn { "Failed to connect to PlayServices!" }
        stopSelf()
    }

    override fun onConnected(bundle: Bundle?) {
        // Alright, get last known location!
        val lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
        if (lastLocation != null) {
            lastLocation.let {
                UpdateLocationRequest(lastLocation.latitude.toString(), lastLocation.longitude.toString()).enqueueRequest()
            }

        } else {
            // Couldn't get location, nothing else to do here
            stopSelf()
        }
    }

    override fun onConnectionSuspended(state: Int) {
        logMethodCall()
        stopSelf()
    }

    //
    // PRIVATE METHODS
    //

    private fun handleRequests(intent: Intent) {
        if (intent.hasExtra(EXTRA_RUN)) {
            refreshLocation()
        } else {
            // no intent info? give up..
            stopSelf()
        }
    }

    private fun refreshLocation() {
        // Only upload location every so often.
        val currentTimestamp = Date().time
        val lastExecution = PrefRepo.lastLocationUpload
        if (lastExecution != NO_TIME
                && (currentTimestamp - lastExecution) < RUN_INTERVAL) {
            stopSelf()
            return
        }

        // Make sure Google Play Services are available
        if (!PlayServices.checkPlayServices(this)) {
            stopSelf()
            return
        }

        // Create an instance of GoogleAPIClient
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build()
        }

        googleApiClient?.connect()

    }

    /**
     * Call if a new location was successfully uploaded to the server.
     */
    private fun finishWithSuccess() {
        logMethodCall()

        // Save timestamp of this execution.
        PrefRepo.lastLocationUpload = Date().time

        // All done :)
        stopSelf()
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onLocationUpdateSuccess(event: LocationUpdateSuccessEvent) {
        logEvent(event)

        UserAccountRepo.updateAccount(event.account)

        finishWithSuccess()
    }

    @Subscribe(threadMode = MAIN)
    fun onLocationUpdateFailure(event: LocationUpdateFailEvent) {
        // Upload failed this once, but no worries since this is going to be requested fairly often
        stopSelf()
    }

}