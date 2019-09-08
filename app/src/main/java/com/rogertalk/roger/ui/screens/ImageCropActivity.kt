package com.rogertalk.roger.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.event.failure.ChangeDisplayPicFailEvent
import com.rogertalk.roger.event.success.ChangeDisplayPicSuccessEvent
import com.rogertalk.roger.manager.EventTrackingManager
import com.rogertalk.roger.models.holder.ImagePickHolder
import com.rogertalk.roger.network.request.AttachmentPhotoRequest
import com.rogertalk.roger.network.request.UpdateProfilePicRequest
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.ui.cta.doneToast
import com.rogertalk.roger.ui.screens.base.EventAppCompatActivity
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.constant.RuntimeConstants.Companion.DEFAULT_PROFILE_PIC_SIZE
import com.rogertalk.roger.utils.extensions.materialize
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.image_crop_screen.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.jetbrains.anko.toast
import kotlin.LazyThreadSafetyMode.NONE


class ImageCropActivity : EventAppCompatActivity(true) {

    companion object {

        private val EXTRA_URI = "imageUri"
        private val EXTRA_STREAM_ID = "streamId"
        private val EXTRA_SAVE_TO_MEMORY_ONLY = "saveToMemoryOnly"
        private val EXTRA_CROPPING_TO_SHARE = "croppingToShare"

        /**
         * @param streamId if set will update the photo for this stream, otherwise will set
         * user's profile picture
         */
        fun start(context: Context, imageURI: Uri, streamId: Long? = null): Intent {
            val startIntent = Intent(context, ImageCropActivity::class.java)
            startIntent.putExtra(EXTRA_URI, imageURI)
            if (streamId != null) {
                startIntent.putExtra(EXTRA_STREAM_ID, streamId)
            }
            return startIntent
        }

        /**
         * This image will be cropped with the intent of sharing to a stream or external source
         */
        fun startToShare(context: Context, imageURI: Uri, streamId: Long): Intent {
            val startIntent = Intent(context, ImageCropActivity::class.java)
            startIntent.putExtra(EXTRA_URI, imageURI)
            startIntent.putExtra(EXTRA_STREAM_ID, streamId)
            startIntent.putExtra(EXTRA_CROPPING_TO_SHARE, true)
            return startIntent
        }

        fun startSaveToMemory(context: Context, imageURI: Uri): Intent {
            val startIntent = Intent(context, ImageCropActivity::class.java)
            startIntent.putExtra(EXTRA_URI, imageURI)
            startIntent.putExtra(EXTRA_SAVE_TO_MEMORY_ONLY, true)
            return startIntent
        }

    }

    private val streamId: Long by lazy(NONE) { intent.getLongExtra(EXTRA_STREAM_ID, NO_ID) }
    private val saveToMemory: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_SAVE_TO_MEMORY_ONLY, false) }
    private val croppingToShare: Boolean by lazy(NONE) { intent.getBooleanExtra(EXTRA_CROPPING_TO_SHARE, false) }

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_crop_screen)
        handleIntent()
    }

    //
    // PUBLIC METHODS
    //

    //
    // PRIVATE METHODS
    //

    private fun handleIntent() {
        if (intent == null) {
            finish()
            return
        }

        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_URI)
        cropImageView.setImageUriAsync(imageUri)

        setupUI()
    }

    private fun setupUI() {
        cropImageButton.setOnClickListener { cropImagePressed() }

        if (croppingToShare) {
            cropImageView.cropShape = CropImageView.CropShape.RECTANGLE
        } else {
            cropImageView.cropShape = CropImageView.CropShape.OVAL
            cropImageView.setFixedAspectRatio(true)
        }

        // set material animation for previous versions of Android
            cropImageButton.materialize(Color.BLACK)
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            cropImageButton.isEnabled = false
            cropImageButton.text = ""
            progressWheel.makeVisible()
        } else {
            cropImageButton.isEnabled = true
            cropImageButton.text = getString(R.string.settings_crop_button)
            progressWheel.beGone()
        }
    }

    private fun cropImagePressed() {
        setLoading(true)
        val croppedBitmap = if (croppingToShare) {
            // Slightly bigger image when cropping with the intent of sharing
            cropImageView.croppedImage
        } else {
            cropImageView.getCroppedImage(DEFAULT_PROFILE_PIC_SIZE, DEFAULT_PROFILE_PIC_SIZE)
        }

        if (croppedBitmap != null) {
            if (saveToMemory) {
                logDebug { "Picked image saved to memory" }
                ImagePickHolder.croppedImage = croppedBitmap
                finish()
                return
            }
            if (streamId == NO_ID) {
                // Setting picture for user
                ImagePickHolder.croppedImage = croppedBitmap
                UpdateProfilePicRequest(croppedBitmap).enqueueRequest()
            } else {
                // Track this event
                EventTrackingManager.setAttachmentPhoto()

                // Send picture as an attachment to selected stream
                AttachmentPhotoRequest(streamId, croppedBitmap).enqueueRequest()

                if (croppingToShare) {
                    doneToast()
                }

                // Terminate app this screen immediately
                finish()
            }
        }
    }

    //
    // EVENT METHODS
    //

    @Subscribe(threadMode = MAIN)
    fun onChangeProfilePictureSuccess(event: ChangeDisplayPicSuccessEvent) {
        logDebug { "Changed picture with success!" }
        UserAccountRepo.updateAccount(event.account)
        finish()
    }

    @Subscribe(threadMode = MAIN)
    fun onChangeDisplayPictureFail(event: ChangeDisplayPicFailEvent) {
        logError { "Failed to change display pic!" }
        toast(R.string.settings_upload_pic_failed)
        setLoading(false)
    }
}