package com.rogertalk.roger.ui.screens

import android.Manifest
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.event.broadcasts.audio.AudioAmplitudeEvent
import com.rogertalk.roger.event.broadcasts.audio.AudioCommandEvent
import com.rogertalk.roger.event.broadcasts.audio.AudioServiceStateEvent
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.manager.EventTrackingManager.RecordingReason.TAP_MIC_ATTACHMENTS
import com.rogertalk.roger.manager.GlobalManager
import com.rogertalk.roger.manager.StreamManager
import com.rogertalk.roger.manager.audio.PlaybackStateManager
import com.rogertalk.roger.models.data.AttachmentType
import com.rogertalk.roger.models.data.AudioCommand
import com.rogertalk.roger.models.data.StreamStatus
import com.rogertalk.roger.models.data.VisualizerType
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.network.request.AttachmentLinkRequest
import com.rogertalk.roger.network.request.StreamStatusRequest
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.repo.StreamCacheRepo
import com.rogertalk.roger.ui.cta.doneToast
import com.rogertalk.roger.ui.dialog.AttachmentDialogs
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.ui.screens.behaviors.WhiteToolbar
import com.rogertalk.roger.utils.android.ShareUtils
import com.rogertalk.roger.utils.constant.AttachmentConstants.ATTACHMENT_PHOTO_HEIGHT
import com.rogertalk.roger.utils.constant.AttachmentConstants.ATTACHMENT_PHOTO_WIDTH
import com.rogertalk.roger.utils.constant.MaterialIcon
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.hasWriteToExternalStoragePermission
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.attachments_screen.*
import kotlinx.android.synthetic.main.transparent_toolbar.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.toast
import kotlin.LazyThreadSafetyMode.NONE

class AttachmentsActivity : EventAppCompatActivity(true),
        WhiteToolbar, RequestListener<String, GlideDrawable>,
        PermissionListener {

    override val _toolbar: Toolbar
        get() = toolbar
    override val _context: AppCompatActivity
        get() = this
    override val _toolbarRightActionAnimation: AnimatorSet?
        get() = null

    companion object {

        private val EXTRA_STREAM_ID = "streamId"
        private val EXTRA_CAME_FROM_NOTIFICATION = "cameFromNotification"

        fun start(context: Context, streamId: Long, cameFromNotification: Boolean = false): Intent {
            val startIntent = Intent(context, AttachmentsActivity::class.java)
            startIntent.putExtra(EXTRA_STREAM_ID, streamId)
            startIntent.putExtra(EXTRA_CAME_FROM_NOTIFICATION, cameFromNotification)
            return startIntent
        }
    }

    private val streamId: Long by lazy(NONE) { intent.getLongExtra(EXTRA_STREAM_ID, NO_ID) }
    private val cameFromNotification: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_STREAM_ID, false) }

    // Playback and Recording Audio Visualizer values
    private var audioLevel = 0.0
    private var audioLevelPrevious = 0.0

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (streamId == NO_ID) {
            logError { "StreamID was wrong!" }
            finish()
            return
        }

        if (StreamManager.selectedStreamId != streamId) {
            // Pre-select new stream if possible
            val stream = StreamCacheRepo.getStream(streamId)
            if (stream == null) {
                logError { "Stream is different and cannot be found in cache!" }
                finish()
                return
            }

            // Select this Stream as the new one in focus
            StreamManager.selectedStreamId = streamId
        }

        setContentView(R.layout.attachments_screen)
        setupUI()

        // Don't show CTA on empty attachments anymore
        PrefRepo.didSeeAttachmentsScreen = true

        // Mark this attachment as seen
        GlobalManager.addToSeenAttachments(streamId)

        // Track flow initialized by notification
        if (cameFromNotification) {
            EventTrackingManager.notificationPressed(EventTrackingManager.NotificationType.ATTACHMENTS)
        }
    }

    override fun onResume() {
        super.onResume()
        updateAudioState()
    }

    override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?,
                             isFirstResource: Boolean): Boolean {
        toast(R.string.ob_failed_request)
        return false
    }

    override fun onResourceReady(resource: GlideDrawable?, model: String?,
                                 target: Target<GlideDrawable>?,
                                 isFromMemoryCache: Boolean,
                                 isFirstResource: Boolean): Boolean {
        loadingImageProgress.beGone(true)
        return false
    }


    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        val permissionName = response?.permissionName ?: ""
        logDebug { "Enabled permission: $permissionName" }
        if (hasWriteToExternalStoragePermission()) {
            // Just got the permission, show dialog now
            setAttachmentPhoto()
        }
    }

    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
        token?.continuePermissionRequest()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        val permissionName = response?.permissionName ?: ""
        val permanentlyDenied = response?.isPermanentlyDenied ?: true
        logDebug { "Denied permission: $permissionName, permanently: $permanentlyDenied" }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri = CropImage.getPickImageResultUri(this, data)
            imageUri?.let {
                startActivity(ImageCropActivity.startToShare(this, it, streamId))

                // End this activity
                finish()
            }
        }
    }

    //
    // PUBLIC METHODS
    //

    fun setAttachmentPhoto() {
        if (hasWriteToExternalStoragePermission()) {
            choosePhoto()
        } else {
            // Request needed permission first
            requestStoragePermissions()
        }
    }

    fun setAttachmentLink(stream: Stream, clipboardLink: String) {
        // Track link attachment sent
        EventTrackingManager.setAttachmentLink()

        AttachmentLinkRequest(stream.id, clipboardLink).enqueueRequest()

        doneToast()
        finish()
    }

    //
    // PRIVATE METHODS
    //

    fun scaleRecordingVisualizer(scaleLevel: Float) {
        recordingVisualizer.scaleX = scaleLevel
        recordingVisualizer.scaleY = scaleLevel
    }

    private fun resetAudioLevel() {
        audioLevelPrevious = 0.0
        audioLevel = 0.0
        recordingVisualizer.beGone()
    }

    private fun redrawVisualizer(visualizerType: VisualizerType) {
        if (visualizerType == visualizerType) {
            val recordingLevel = {
                // Smooth out changes in the audio level
                if (audioLevel < audioLevelPrevious) {
                    (audioLevelPrevious * 5 + audioLevel) / 6
                } else {
                    audioLevel
                }
            }()

            audioLevelPrevious = recordingLevel
            val visualizerScale = (1 + recordingLevel / Math.sqrt(recordingLevel + 0.01f)).toFloat()
            scaleRecordingVisualizer(visualizerScale)
        }
    }

    private fun startRecordingAction() {
        val stream = StreamManager.selectedStream ?: return

        // Keep screen on while recording
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Reset visualizer
        recordingVisualizer.makeVisible()
        recordingVisualizer.scaleX = 1f
        recordingVisualizer.scaleY = 1f

        EventTrackingManager.recordingStart(TAP_MIC_ATTACHMENTS)
        postEvent(AudioCommandEvent(AudioCommand.RECORD))
    }

    private fun choosePhoto() {
        CropImage.startPickImageActivity(this)
    }

    private fun requestStoragePermissions() {
        val dialogPermissionListener =
                DialogOnDeniedPermissionListener.Builder
                        .withContext(this)
                        .withTitle(R.string.perm_storage_title)
                        .withMessage(R.string.perm_storage_description)
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build()

        if (!Dexter.isRequestOngoing()) {
            Dexter.checkPermission(dialogPermissionListener, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun setupUI() {
        val stream = StreamCacheRepo.getStream(streamId)
        if (stream == null) {
            logError { "" }
            finish()
            return
        }

        // Toolbar
        setupToolbar("")
        rightTopButton.beGone()
        toolbarHasBackAction { handleBackPressed() }

        displayAttachment(stream)

        // Button actions
        replaceButton.setOnClickListener { newAttachmentPressed() }
        recordingButton.setToggleAction { recordingButtonPressed() }
    }

    private fun displayAttachment(stream: Stream) {
        val attachmentLink = stream.attachmentLink

        if (attachmentLink == null) {
            // There is no attachment yet, hide recording button
            recordingButton.beGone()

            // And display send attachment button
            replaceButton.makeVisible()
            return
        }

        // Hide main UI button
        replaceButton.beGone()

        // Show replace action at top
        setRightActionIcon(MaterialIcon.ATTACH_FILE)
        setRightActionContentDescription(R.string.attachments_replace)
        rightTopLabel.text = R.string.attachments_replace.stringResource()
        rightTopLabel.makeVisible(true)
        rightTopButton.makeVisible(true)
        rightTopLabel.setOnClickListener {
            newAttachmentPressed()
        }
        rightTopButton.setOnClickListener {
            newAttachmentPressed()
        }

        StreamStatusRequest(stream.id, StreamStatus.VIEWING_ATTACHMENT).enqueueRequest()

        when (stream.attachmentType) {
            AttachmentType.IMAGE -> {
                EventTrackingManager.seenAttachment(true)
                renderImage(attachmentLink)
            }

            AttachmentType.LINK -> {
                EventTrackingManager.seenAttachment(false)
                renderLink(attachmentLink)
            }
        }
    }

    private fun recordingButtonPressed() {
        if (PlaybackStateManager.recording) {
            postEvent(AudioCommandEvent(AudioCommand.STOP_RECORDING, recordingStopReason = TAP_MIC_ATTACHMENTS))
        } else {
            PlaybackStateManager.currentStream = StreamCacheRepo.getStream(streamId)
            startRecordingAction()
        }
    }

    private fun newAttachmentPressed() {
        val stream = StreamCacheRepo.getStream(streamId) ?: return
        AttachmentDialogs.attachmentOptions(this, stream)
    }

    private fun renderImage(imageURL: String) {
        emptyMessageContainer.beGone()
        loadingImageProgress.makeVisible(true)

        Glide.with(this)
                .load(imageURL)
                .dontTransform()
                .override(ATTACHMENT_PHOTO_WIDTH, ATTACHMENT_PHOTO_HEIGHT)
                .listener(this)
                .into(attachmentImage)
    }

    private fun renderLink(linkURL: String) {
        // Display link and icon
        val content = SpannableString(linkURL)
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        descriptionLabel.text = content
        attachmentIcon.text = MaterialIcon.LINK.text

        // Make it clickable
        descriptionLabel.setOnClickListener {
            ShareUtils.openExternalLink(this, linkURL)
        }
    }

    private fun handleBackPressed() {
        finish()
    }

    private fun updateAudioState() {
        if (!PlaybackStateManager.recording) {
            recordingButton.displayAsNotRecording()
            resetAudioLevel()
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onAudioServiceState(event: AudioServiceStateEvent) {
        logDebug { "AudioEvent. new: ${event.newState}, old: ${event.oldState}" }
        updateAudioState()
    }

    @Subscribe(threadMode = MAIN)
    fun onAudioAmplitude(event: AudioAmplitudeEvent) {
        // Convert into our own leveling metric
        audioLevel = Math.pow(10.0, event.amplitude / 40)
        redrawVisualizer(event.visualizerType)
    }
}