package com.rogertalk.roger.helper.audio

import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.KeyEvent
import com.bumptech.glide.Glide
import com.rogertalk.roger.R
import com.rogertalk.roger.android.receivers.MediaKeysReceiver
import com.rogertalk.roger.android.services.AudioService
import com.rogertalk.roger.manager.RuntimeVarsManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logMethodCall
import nl.komponents.kovenant.*

class MediaSessionHelper(var audioService: AudioService) :
        MediaSessionCompat.Callback() {

    companion object {
        private val DEBUG_TAG = "RogerMediaSession"
    }

    private val mediaSession: MediaSessionCompat
    private val dispatcher: Dispatcher
    private val taskContext: Context

    init {
        val mediaButtonReceiver = ComponentName(audioService, MediaKeysReceiver::class.java)
        mediaSession = MediaSessionCompat(audioService, DEBUG_TAG, mediaButtonReceiver, null)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(this)

        dispatcher = createTaskDispatcher()
        taskContext = createTaskContext()
    }

    //
    // OVERRIDE METHODS
    //

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        if (mediaButtonEvent != null) {
            val event = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
            logDebug { "User pressed media key: ${event.keyCode} - ${event.action}" }
            val result = MediaKeysReceiver.handleMediaKey(event.keyCode, event.action)
            if (result) {
                // We handled this event, don't cascade!
                return true
            }
        }
        logDebug { "Media Button event" }

        return super.onMediaButtonEvent(mediaButtonEvent)
    }

    override fun onPlay() {
        logMethodCall()
        audioService.playCurrentStream()
    }

    override fun onStop() {
        logMethodCall()
        super.onStop()
    }

    override fun onPause() {
        logMethodCall()
        super.onPause()
    }

    override fun onFastForward() {
        logMethodCall()
        super.onFastForward()
    }

    override fun onRewind() {
        logMethodCall()
        audioService.rewindPlayback()
        super.onRewind()
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        logMethodCall()
        audioService.playCurrentStream()
        super.onPlayFromMediaId(mediaId, extras)
    }

    override fun onSkipToPrevious() {
        logMethodCall()
        audioService.rewindPlayback()
        super.onSkipToPrevious()
    }

    //
    // PUBLIC METHODS
    //

    /**
     * Set media playback controls when playing
     */
    fun playing(stream: Stream) {
        logMethodCall()
        task(taskContext) {
            val metadata = MediaMetadataCompat.Builder()
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Roger")
            metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, stream.title)

            // Album art
            if (stream.imageURL != null) {
                val dimen = RuntimeVarsManager.getDimensionForAvatarSize(AvatarSize.BIG)

                val streamImage = try {
                    Glide.with(appController())
                            .load(stream.imageURL)
                            .asBitmap()
                            .into(dimen, dimen)
                            .get()
                } catch (e: Exception) {
                    logError(e) { "Could not set photo for media session" }
                    null
                }

                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, streamImage)
            } else {
                val bitmap = BitmapFactory.decodeResource(appController().resources, R.mipmap.ic_launcher)
                metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
            }

            // TODO: For Android Auto prefer METADATA_KEY_ALBUM_ART and pass a URI instead of a bitmap (which will be scaled down)
            // TODO : URI should be local content, http links will be deprecated

            mediaSession.setMetadata(metadata.build())

            val stateBuilder = Builder()
            stateBuilder.setActions(getAvailableActions())
            stateBuilder.setState(STATE_PLAYING,
                    0, 1.0f, SystemClock.elapsedRealtime())

            mediaSession.setPlaybackState(stateBuilder.build())

            mediaSession.isActive = true
        }
    }


    fun stopped() {
        mediaSession.isActive = false
        mediaSession.setMetadata(null)
    }

    /**
     * Call this to free up media session handling
     */
    fun destroy() {
        mediaSession.release()
    }

    //
    // PRIVATE METHODS
    //

    private fun createTaskDispatcher(): Dispatcher {
        return buildDispatcher {
            name = "mediaSessionDispatcher"
            concurrentTasks = 1

            pollStrategy {
                yielding(numberOfPolls = 1000)

                sleeping(numberOfPolls = 100,
                        sleepTimeInMs = 10)
                blocking()
            }
        }
    }

    private fun createTaskContext(): Context {
        return Kovenant.createContext {
            callbackContext.dispatcher = buildDispatcher { name = "mediaSessionDispatcher" }
            workerContext.dispatcher = buildDispatcher { name = "mediaSessionDispatcher" }
        }

    }


    // TODO : send playback state of buffering so UI can display that information as well
    private fun getAvailableActions(): Long {
        var actions = ACTION_PLAY or
                ACTION_PLAY_FROM_MEDIA_ID or
                ACTION_SKIP_TO_PREVIOUS
        if (PlaybackStateManager.playing) {
            actions = actions or ACTION_PAUSE
        }
        return actions
    }
}
