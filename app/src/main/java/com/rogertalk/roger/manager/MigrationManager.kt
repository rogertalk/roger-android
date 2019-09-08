package com.rogertalk.roger.manager

import android.os.Build
import com.orhanobut.hawk.Hawk
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.android.tasks.DeviceContactQueryTask
import com.rogertalk.roger.models.json.ActiveContact
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.constant.NO_TIME
import com.rogertalk.roger.utils.extensions.appHelper
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logMethodCall
import com.rogertalk.roger.utils.log.logWarn
import java.util.*


object MigrationManager {

    val NO_VERSION = -1
    private val CURRENT_VERSION = 13

    //
    // PUBLIC METHODS
    //

    fun executeNeededMigrations() {
        logMethodCall()
        handleMigration()
    }

    //
    // PRIVATE METHODS
    //


    private fun needsMigration(): Boolean {
        val storedVersion = PrefRepo.lastMigrationVersion

        // first version never needs to migrate
        if (storedVersion == NO_VERSION) {
            PrefRepo.lastMigrationVersion = CURRENT_VERSION

            return false
        }
        return (storedVersion != CURRENT_VERSION)
    }

    private fun handleMigration() {
        // Test Android Nougat encrypted data state independently of the app version
        if (testAndroidNougatData()) {
            // Don't go any further since data was invalidated at this point
            return
        }

        if (!needsMigration()) {
            logDebug { "MIGRATION: no migrations needed" }
            return
        }

        // Execute this operations for all migrations
        executeForAllMigrations()

        val lastVersion = PrefRepo.lastMigrationVersion

        if (lastVersion < 13) {
            // Automatically give permission to match contacts if user came from previous versions
            PrefRepo.permissionToMatchContacts = true
        }

        if (lastVersion < 11) {
            logDebug { "MIGRATION: Clearing existing active contacts records" }

            // Reset active contacts list
            PrefRepo.activeContactsMap = HashMap<String, ActiveContact>()
        }

        // Specific entry and cease entries
        when (lastVersion) {
            in 0..2 -> {
                invalidateAllData()
                persistCurrentMigrationVersion()
                return
            }
            in 8..11 -> {
                PrefRepo.samplingRateIndex = 0
                persistCurrentMigrationVersion()
                return
            }
        }

        // Start contact probing service
        DeviceContactQueryTask().execute()

        persistCurrentMigrationVersion()
    }

    /**
     * @return True if data invalidation was necessary
     */
    private fun testAndroidNougatData(): Boolean {
        if (AndroidVersion.fromApiVal(Build.VERSION_CODES.N, inclusive = true)) {
            // If a user had the app installed and updates to Android N, encrypted data will no longer be writable.
            if (PrefRepo.loggedIn) {
                val previousValue = PrefRepo.lastAccessTokenRefresh
                val NEW_VALUE = 2L
                PrefRepo.lastAccessTokenRefresh = NEW_VALUE
                if (PrefRepo.lastAccessTokenRefresh != NEW_VALUE) {
                    logWarn { "Encrypted data is no longer writable. Log out user!" }
                    invalidateAllData()
                    Hawk.resetCrypto()
                    appHelper().reInitHawk()
                    return true
                } else {
                    // All good, restore value and proceed
                    PrefRepo.lastAccessTokenRefresh = previousValue
                }
            }
        }
        return false
    }

    /**
     * The instructions in this function are executed for all migrations!
     */
    private fun executeForAllMigrations() {
        logDebug { "MIGRATION: Executing generic code for all migrations" }

        // Reset audio sampling index
        PrefRepo.samplingRateIndex = 0

        // Run audio cleanup as soon as possible
        PrefRepo.lastCleanupTimestamp = NO_TIME
    }

    private fun persistCurrentMigrationVersion() {
        // store this migration value
        PrefRepo.lastMigrationVersion = CURRENT_VERSION
    }

    private fun invalidateAllData() {
        PrefRepo.clearAllData()
    }
}