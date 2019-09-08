package com.rogertalk.roger.ui.screens.behaviors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.RuntimeVarsManager
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.repo.AppVisibilityRepo
import com.rogertalk.roger.ui.screens.TalkActivity
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.drawableResource
import com.rogertalk.roger.utils.log.logWarn
import kotlinx.android.synthetic.main.talk_screen.*
import java.util.*

class GroupImageGenerator(val talkActivity: TalkActivity) {


    private val fullLeftSideImage = talkActivity.squareImage1
    private val topRightImage = talkActivity.squareImage2
    private val bottomRightImage = talkActivity.squareImage3
    private val fullRightSideImage = talkActivity.squareImage4
    private val squareContainer = talkActivity.groupSquarePreview

    private val cropSize = RuntimeVarsManager.getDimensionForAvatarSize(AvatarSize.BIG)
    private val mrPeeDrawable = R.drawable.pee.drawableResource(talkActivity)
    private var pendingImagesCount = 0
    private var pendingStreamId = NO_ID

    private var pendingTasks = ArrayList<Request>()

    init {
        squareContainer.isDrawingCacheEnabled = true
    }

    //
    // PUBLIC METHODS
    //

    fun loadImages(stream: Stream) {
        val participants = stream.participants
        if (participants.isEmpty()) {
            return
        }

        // Cancel all previous tasks
        pendingTasks.forEach { it.clear() }
        pendingTasks = ArrayList<Request>()

        // Reset values
        pendingImagesCount = when (participants.size) {
            2 -> 2
            else -> 3
        }
        pendingStreamId = stream.id

        // Load 1st image
        loadImageForImageView(fullLeftSideImage, participants[0].imageURL, target0)

        if (participants.size == 2) {
            // Load only 1 more image
            loadImageForImageView(fullRightSideImage, participants[1].imageURL, target3)
        } else if (participants.size > 2) {
            fullRightSideImage.setImageResource(R.color.transparent)
            //Load 2 more images
            loadImageForImageView(topRightImage, participants[1].imageURL, target1)
            loadImageForImageView(bottomRightImage, participants[2].imageURL, target2)
        }

    }

    //
    // PRIVATE METHODS
    //

    private fun checkImageReadiness() {
        if (pendingImagesCount == 0) {
            if (!AppVisibilityRepo.chatIsForeground) {
                // App in running on the background, don't try to update images
                return
            }
            if (squareContainer.measuredWidth == 0) {
                logWarn { "Probably layout as not been properly initialized yet" }
                return
            }

            // TODO: In the future we could cache this and have an invalidation logic for the combined group image
            squareContainer.buildDrawingCache()
            val bitmap = Bitmap.createBitmap(squareContainer.measuredWidth, squareContainer.measuredWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            squareContainer.layout(0, 0, squareContainer.measuredWidth, squareContainer.measuredWidth)
            squareContainer.draw(canvas)

            // Send back to TalkActivity
            talkActivity.groupImageFullyLoaded(bitmap, pendingStreamId)

            // Reset values now
            pendingStreamId = NO_ID
        }
    }

    private fun loadImageForImageView(imageView: ImageView, url: String?, target: SimpleTarget<Bitmap>) {
        if (url != null) {
            val imageRequest = Glide.with(appController())
                    .load(url)
                    .asBitmap()
                    .override(cropSize, cropSize)
                    .centerCrop()
                    .into(target)
                    .request

            // Start task and add to list
            pendingTasks.add(imageRequest)
        } else {
            pendingImagesCount--
            imageView.setImageDrawable(mrPeeDrawable)
        }
    }

    private val target0 = object : SimpleTarget<Bitmap>(cropSize, cropSize) {
        override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
            targetSharedLogic(fullLeftSideImage, resource)
        }
    }

    private val target1 = object : SimpleTarget<Bitmap>(cropSize, cropSize) {
        override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
            targetSharedLogic(topRightImage, resource)
        }
    }

    private val target2 = object : SimpleTarget<Bitmap>(cropSize, cropSize) {
        override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
            targetSharedLogic(bottomRightImage, resource)
        }
    }

    private val target3 = object : SimpleTarget<Bitmap>(cropSize, cropSize) {
        override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
            targetSharedLogic(fullRightSideImage, resource)
        }
    }

    private fun targetSharedLogic(imageView: ImageView, bitmap: Bitmap?) {
        imageView.setImageBitmap(bitmap)
        pendingImagesCount--
        checkImageReadiness()
    }
}