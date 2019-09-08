package com.rogertalk.roger.repo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeInvisible
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.manager.RuntimeVarsManager
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.data.DeviceContactInfo
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.initial
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import org.jetbrains.anko.layoutInflater
import java.io.FileNotFoundException
import java.util.*

object ContactsRepo {

    /**
     * @return First stream found with a given user id, null if none found
     */
    fun streamWithUser(userId: Long): Stream? {
        val streams = StreamCacheRepo.getCachedCopy()
        for (stream in streams) {
            val eligibleOthers = stream.othersOrEmpty.filter { it.active && !it.bot }
            if (eligibleOthers.size == 1 && eligibleOthers.any { it.id == userId }) {
                return stream
            }
        }
        return null
    }

    fun getContactBitmapAvatar(senderId: Long, name: String, imageURL: String?): Bitmap {
        val context = appController()

        // Inflate contact circle layout
        val parentView = context.layoutInflater.inflate(R.layout.notification_contact, null, false)

        // Since it won't be actually rendered we need to do some measurements manually
        val specWidth = View.MeasureSpec.makeMeasureSpec(
                context.resources.getDimension(R.dimen.notification_size).toInt(),
                View.MeasureSpec.AT_MOST)

        parentView.layoutParams = ViewGroup.LayoutParams(0, 0)
        parentView.measure(specWidth, specWidth)

        parentView.buildDrawingCache(true)

        // Sub-views
        val initials = parentView.findViewById(R.id.notificationInitials) as TextView
        val profileImage = parentView.findViewById(R.id.notificationContactImage) as ImageView
        val blackCircle = parentView.findViewById(R.id.blackCircle) as ImageView

        val avatarURI = ContactMapRepo.getContactAvatarURI(senderId)

        if (avatarURI != null) {
            // Has avatar, use that
            blackCircle.makeInvisible()

            val actualURI = Uri.parse(avatarURI)
            try {
                val avatarBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, actualURI)
                val roundedAvatar = RoundImageUtils.makeRoundWithBitmap(context, avatarBitmap)
                profileImage.setImageDrawable(roundedAvatar)

                // hide initials
                initials.text = ""
            } catch(e: FileNotFoundException) {
                logError { "MediaStore contact thumbnail is no longer valid" }

                // TODO : don't use MediaStore for future calls for this contact

                useRemoteImage(name, imageURL, blackCircle, profileImage, initials)
            }
        } else {
            useRemoteImage(name, imageURL, blackCircle, profileImage, initials)
        }

        // Export to bitmap
        val bitmap = Bitmap.createBitmap(parentView.measuredWidth, parentView.measuredWidth, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        parentView.layout(0, 0, parentView.measuredWidth, parentView.measuredWidth)
        parentView.draw(canvas)
        return bitmap
    }

    fun possibleRemoteImage(imageURL: String?): Bitmap? {
        if (imageURL == null) {
            return null
        }
        val context = appController()
        val size = RuntimeVarsManager.getDimensionForAvatarSize(AvatarSize.CONTACT)
        try {
            // Inflate contact circle layout
            val parentView = context.layoutInflater.inflate(R.layout.notification_contact, null, false)

            // Since it won't be actually rendered we need to do some measurements manually
            val specWidth = View.MeasureSpec.makeMeasureSpec(
                    context.resources.getDimension(R.dimen.notification_size).toInt(),
                    View.MeasureSpec.AT_MOST)

            parentView.layoutParams = ViewGroup.LayoutParams(0, 0)
            parentView.measure(specWidth, specWidth)

            parentView.buildDrawingCache(true)

            val myBitmap = Glide.with(context)
                    .load(imageURL)
                    .asBitmap()
                    .centerCrop()
                    .into(size, size)
                    .get()

            val initials = parentView.findViewById(R.id.notificationInitials) as TextView
            val profileImage = parentView.findViewById(R.id.notificationContactImage) as ImageView
            val roundedAvatar = RoundImageUtils.makeRoundWithBitmap(context, myBitmap)
            initials.beGone()
            profileImage.setImageDrawable(roundedAvatar)


            // Export to bitmap
            val bitmap = Bitmap.createBitmap(parentView.measuredWidth, parentView.measuredWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            parentView.layout(0, 0, parentView.measuredWidth, parentView.measuredWidth)
            parentView.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            logError(e) { "Could not obtain remove avatar image: $imageURL" }
        }
        return null
    }

    fun explodeContacts(originalContactsList: MutableCollection<DeviceContactInfo>): ArrayList<DeviceContactInfo> {
        // We know that exploded contacts will have at least the same size
        val explodedContacts = ArrayList<DeviceContactInfo>(originalContactsList.size)

        for (contact in originalContactsList) {
            explodedContacts.addAll(contact.explode())
        }

        return explodedContacts
    }

    private fun useRemoteImage(name: String, imageURL: String?, blackCircle: ImageView,
                               profileImage: ImageView, initials: TextView) {
        val context = appController()
        if (imageURL != null) {
            logDebug { "Loading sender remote image: $imageURL" }
            // try to use remote image
            val size = RuntimeVarsManager.getDimensionForAvatarSize(AvatarSize.CONTACT)

            blackCircle.makeInvisible()

            // Hide initials
            initials.text = ""

            try {
                val myBitmap = Glide.with(context)
                        .load(imageURL)
                        .asBitmap()
                        .centerCrop()
                        .into(size, size)
                        .get()


                val roundedAvatar = RoundImageUtils.makeRoundWithBitmap(context, myBitmap)
                profileImage.setImageDrawable(roundedAvatar)
            } catch (e: Exception) {
                logError(e) { "Could not obtain remove avatar image: $imageURL" }

                // At least show initials
                initials.text = name.initial
            }
        } else {
            // User doesn't have an avatar, so show a circle with initials instead.
            blackCircle.makeVisible()
            profileImage.makeInvisible()
            initials.text = name.initial
        }
    }

}