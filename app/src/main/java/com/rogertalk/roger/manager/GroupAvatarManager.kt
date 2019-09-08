package com.rogertalk.roger.manager

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.rogertalk.roger.R
import com.rogertalk.roger.event.broadcasts.GroupAvatarReadyEvent
import com.rogertalk.roger.models.data.AvatarSize
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.models.json.Stream
import com.rogertalk.roger.realm.CachedGroupAvatarRepo
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.constant.RuntimeConstants
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.postEvent
import com.rogertalk.roger.utils.image.RoundImageUtils
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logMethodCall
import com.rogertalk.roger.utils.log.logWarn
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.uiThread
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

object GroupAvatarManager {

    // This contains the list of group hash currently being handled
    private val inProcess = LinkedList<Int>()

    //
    // PUBLIC METHODS
    //

    /**
     * Set the bitmap for the specified ImageView and Stream.
     * If not available, will render and send a broadcast to the app once it is ready
     * for consumption so it can be rendered then.
     */
    fun loadGroupAvatarInto(imageView: ImageView, stream: Stream, size: AvatarSize) {
        logMethodCall()
        // TODO : call this from multiple places

        // Check if this photo is persisted
        val groupHash = groupHashCode(stream)
        val cachedEntry = CachedGroupAvatarRepo.getRecord(stream.id, groupHash)
        if (cachedEntry != null) {
            try {
                val file = File(cachedEntry.persistPath)
                if (file.exists()) {
                    logDebug { "Will load image into imageview" }
                    RoundImageUtils
                            .createRoundImage(appController(), imageView,
                                    file,
                                    size)

                    // All done here
                    return
                } else {
                    // Persisted but file doesn't exist, recall entry
                    CachedGroupAvatarRepo.deleteRecord(stream.id, groupHash)
                }
            } catch (e: Exception) {
                // Invalidate this entry
                CachedGroupAvatarRepo.deleteRecord(stream.id, groupHash)
            }
        }

        // Use Mr. Pee in the meantime
        imageView.setBackgroundResource(R.drawable.pee)

        // Don't execute the same generation task twice
        if (!inProcess.contains(groupHash)) {
            // Generate the bitmap on the background and persist it after
            generateGroupAvatar(stream)
        }
    }

    /**
     * This gives us a unique hash code representation for the combination of the top 3
     * group avatars for reference. (Useful for persistence).
     * @return Unique hash, -1 in case of error
     */
    fun groupHashCode(stream: Stream): Int {
        val others = stream.others?.take(3) ?: return -1
        var result = 0
        for (other in others) {
            result = 31 * result + other.hashCode()
        }
        return result
    }

    //
    // PRIVATE METHODS
    //

    /**
     * Given a stream produces the group avatar, stores it and then propagates that information
     * trough an event.
     */
    private fun generateGroupAvatar(stream: Stream) {
        doAsync {
            val groupAvatarBitmap = groupBitmap(stream)
            val groupHash = groupHashCode(stream)
            if (groupAvatarBitmap == null) {
                logWarn { "Could not generate group image for stream: ${stream.title}" }
                uiThread {
                    inProcess.remove(groupHash)
                }
            } else {
                // Persist image to internal storage
                val persistedPath = storeGroupAvatarBitmap(groupAvatarBitmap)
                if (persistedPath == null) {
                    logWarn { "Could not generate group image for stream: ${stream.title}" }
                    uiThread {
                        inProcess.remove(groupHash)
                    }
                } else {
                    logDebug { "Persisted image to : $persistedPath" }

                    uiThread {
                        val streamHash = groupHashCode(stream)

                        // Persist this entry to database
                        CachedGroupAvatarRepo.createEntry(stream.id, streamHash, persistedPath)

                        // Inform app regarding the availability of the image
                        postEvent(GroupAvatarReadyEvent(stream.id))

                        // Free from being in process
                        inProcess.remove(groupHash)
                    }
                }
            }

        }
    }

    private fun storeGroupAvatarBitmap(bitmap: Bitmap): String? {
        try {
            val avatarDir = File(appController().filesDir, RuntimeConstants.GROUP_AVATAR_IMAGES_DIR)
            if (!avatarDir.exists()) {
                avatarDir.mkdir()
            }
            val filename = UUID.randomUUID().toString()
            val targetFile = File(appController().filesDir, "${RuntimeConstants.GROUP_AVATAR_IMAGES_DIR}/$filename")

            val outStream: OutputStream?
            try {
                // make a new bitmap from your file
                outStream = FileOutputStream(targetFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
                outStream.close()
            } catch (e: Exception) {
                logWarn { "Failed to store group avatar" }
                return null
            }

            return targetFile.absolutePath
        } catch (e: IOException) {
            logWarn { "Failed to store group avatar" }
            return null
        }
    }

    private fun groupBitmap(stream: Stream): Bitmap? {
        val context = appController()

        // Inflate contact circle layout
        val parentView = context.layoutInflater.inflate(R.layout.group_avatar_elem, null, false)

        // Since it won't be actually rendered we need to do some measurements manually
        val specWidth = View.MeasureSpec.makeMeasureSpec(
                context.resources.getDimension(R.dimen.contact_biggest_circle_diameter_shadow).toInt(),
                View.MeasureSpec.AT_MOST)

        parentView.layoutParams = ViewGroup.LayoutParams(0, 0)
        parentView.measure(specWidth, specWidth)

        parentView.buildDrawingCache(true)

        // Sub-views
        val squareHalfLeft = parentView.findViewById(R.id.squareImage1) as ImageView
        val squareTopRight = parentView.findViewById(R.id.squareImage2) as ImageView
        val squareBottomRight = parentView.findViewById(R.id.squareImage3) as ImageView


        // Nowadays everyone has an image on Roger

        val others = stream.others ?: return null
        if (others.size > 0) {
            val photo = getBitmapForContact(others[0])
            squareHalfLeft.setImageBitmap(photo)
        }
        if (others.size > 1) {
            val photo = getBitmapForContact(others[1])
            squareTopRight.setImageBitmap(photo)
        }
        if (others.size > 2) {
            val photo = getBitmapForContact(others[2])
            squareBottomRight.setImageBitmap(photo)
        } else {
            // Use own photo when there are only 2 participants
            val account = UserAccountRepo.current()
            account?.let {
                val photo = getBitmapForContact(it)
                squareBottomRight.setImageBitmap(photo)
            }
        }

        // Export to bitmap
        val bitmap = Bitmap.createBitmap(parentView.measuredWidth, parentView.measuredWidth, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        parentView.layout(0, 0, parentView.measuredWidth, parentView.measuredWidth)
        parentView.draw(canvas)
        return bitmap
    }

    private fun getBitmapForContact(account: Account): Bitmap {
        val imageSize = RuntimeVarsManager.getDimensionForAvatarSize(AvatarSize.BIG)

        return Glide.with(appController())
                .load(account.imageURL)
                .asBitmap()
                .centerCrop()
                .into(imageSize, imageSize)
                .get()
    }
}