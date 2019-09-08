package com.rogertalk.roger.repo

import com.rogertalk.roger.models.json.Session
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.constant.NO_TIME
import java.util.*
import kotlin.properties.Delegates

/**
 * Repository for the current user's session and associated account
 */
object SessionRepo {

    //
    // CALCULATED PROPERTIES
    //

    var session: Session? by Delegates.observable(initSession()) {
        prop, old, new ->
        new?.let { persistSession(old, new) }
    }

    /**
     * @return True if access token has expired and needs refresh
     */
    val accessTokenExpired: Boolean
        get() {
            val currentTime = Date().time
            val sessionExpirationTime = (session?.expiresIn ?: NO_TIME) * 1000
            val lastUpdate = PrefRepo.lastAccessTokenRefresh
            return currentTime > (sessionExpirationTime + lastUpdate)
        }


    //
    // PUBLIC METHODS
    //


    /**
     * Return the current session ID, -1 in case session is invalid
     */
    fun sessionId(): Long {
        return session?.getId() ?: NO_ID
    }

    fun accessToken(): String {
        return session?.accessToken ?: ""
    }

    /**
     * @return True if user is logged, False otherwise
     */
    fun loggedIn(): Boolean {
        val accessToken = session?.accessToken ?: ""
        val accessTokenIsValid = if (accessToken == "null") false else accessToken.isNotBlank()

        return PrefRepo.loggedIn && accessTokenIsValid
    }


    //
    // PRIVATE METHODS
    //

    /**
     * Persists session, returns True if it was active, false otherwise
     */
    private fun persistSession(oldSession: Session?, newSession: Session) {
        // Update streams if available
        if (newSession.streams.isNotEmpty()) {
            StreamCacheRepo.updateCache(newSession.streams)
        }

        // Try to retain access token if for some reason it vanished!
        val accessToken: String? = newSession.accessToken
        if (accessToken == null) {
            // previous session had access token?
            if (oldSession != null) {

                // it had, update this new session object by merging them together
                newSession.accessToken = accessToken()
            }
        } else {
            // Update last access token refresh
            PrefRepo.lastAccessTokenRefresh = Date().time
        }

        // Persist session itself
        PrefRepo.session = session

        // Flag user as being logged in
        PrefRepo.loggedIn = true
    }

    private fun initSession(): Session? {
        val tmpSession: Session? = PrefRepo.session
        return tmpSession
    }

}