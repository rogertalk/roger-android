package com.rogertalk.roger.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.RuntimeVarsManager
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.data.AvatarSize.BIG
import java.io.File

object RoundImageUtils {

    //
    // PUBLIC METHODS
    //


    fun createRoundImageMainAvatar(context: Context,
                                   imageView: ImageView,
                                   url: String) {
        val cropSize = RuntimeVarsManager.getDimensionForAvatarSize(BIG)

        Glide.with(context)
                .load(url)
                .asBitmap()
                .dontAnimate()
                .placeholder(R.drawable.pee)
                .override(cropSize, cropSize)
                .centerCrop().into(object : BitmapImageViewTarget(imageView) {
            override fun setResource(resource: Bitmap) {
                imageView.setImageDrawable(makeRoundWithBitmap(context, resource))
            }
        })
    }

    fun createRoundImage(context: Context, imageView: ImageView, url: String, avatarSize: AvatarSize) {
        val cropSize = RuntimeVarsManager.getDimensionForAvatarSize(avatarSize)
        createRoundImage(context, imageView, url, cropSize)
    }

    fun createRoundImageNoPlaceholder(context: Context, imageView: ImageView, url: String, avatarSize: AvatarSize) {
        val cropSize = RuntimeVarsManager.getDimensionForAvatarSize(avatarSize)
        createRoundImageNoPlaceholder(context, imageView, url, cropSize)
    }

    fun createRoundImage(context: Context, imageView: ImageView, image: Bitmap, avatarSize: AvatarSize) {
        val cropSize = RuntimeVarsManager.getDimensionForAvatarSize(avatarSize)
        createRoundImage(context, imageView, image, cropSize)
    }


    fun createRoundImage(context: Context, imageView: ImageView, imageURL: String) {
        val photoURI = Uri.parse(imageURL)
        createRoundImage(context, imageView, photoURI)
    }

    fun createRoundImage(context: Context, imageView: ImageView, uri: Uri?) {
        Glide.with(context).load(uri).asBitmap().centerCrop().into(object : BitmapImageViewTarget(imageView) {
            override fun setResource(resource: Bitmap) {
                imageView.setImageDrawable(makeRoundWithBitmap(context, resource))
            }
        })
    }

    fun makeRoundWithBitmap(context: Context, bitmap: Bitmap): RoundedBitmapDrawable {
        val resources = context.resources
        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
        circularBitmapDrawable.isCircular = true
        return circularBitmapDrawable
    }

    fun createRoundImage(context: Context, imageView: ImageView, file: File, avatarSize: AvatarSize) {
        val cropSize = RuntimeVarsManager.getDimensionForAvatarSize(avatarSize)
        Glide.with(context)
                .load(file)
                .asBitmap()
                .dontAnimate()
                .placeholder(R.drawable.pee)
                .override(cropSize, cropSize)
                .centerCrop()
                .into(object : BitmapImageViewTarget(imageView) {
                    override fun setResource(resource: Bitmap) {
                        imageView.setImageDrawable(makeRoundWithBitmap(context, resource))
                    }
                })
    }

    //
    // PRIVATE METHODS
    //

    private fun createRoundImage(context: Context, imageView: ImageView, url: String, cropSize: Int) {
        Glide.with(context)
                .load(url)
                .asBitmap()
                .dontAnimate()
                .placeholder(R.drawable.pee)
                .override(cropSize, cropSize)
                .centerCrop()
                .into(object : BitmapImageViewTarget(imageView) {
                    override fun setResource(resource: Bitmap) {
                        imageView.setImageDrawable(makeRoundWithBitmap(context, resource))
                    }
                })
    }

    private fun createRoundImageNoPlaceholder(context: Context, imageView: ImageView, url: String, cropSize: Int) {
        Glide.with(context)
                .load(url)
                .asBitmap()
                .dontAnimate()
                .override(cropSize, cropSize)
                .centerCrop()
                .into(object : BitmapImageViewTarget(imageView) {
                    override fun setResource(resource: Bitmap) {
                        imageView.setImageDrawable(makeRoundWithBitmap(context, resource))
                    }
                })
    }

    private fun createRoundImage(context: Context, imageView: ImageView, image: Bitmap, cropSize: Int) {
        // TODO: make use of CropSize
        imageView.setImageDrawable(makeRoundWithBitmap(context, image))
    }

}
