package com.rogertalk.roger.helper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.rogertalk.roger.ui.screens.ImageCropActivity
import com.rogertalk.roger.utils.extensions.hasWriteToExternalStoragePermission
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.logDebug
import com.theartofdev.edmodo.cropper.CropImage

/**
 * Manager for any screen which provides the ability to set the user photo.
 */
class PhotoSettingUIHelper(val context: Activity) :
        PermissionListener {

    // If set to True, image will not be sent to the server, but rather kept on memory locally for
    // future usage.
    var saveToMemory = false

    //
    // OVERRIDE METHODS
    //

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        val permissionName = response?.permissionName ?: ""
        logDebug { "Enabled permission: $permissionName" }
        if (context.hasWriteToExternalStoragePermission()) {
            runOnUiThread {
                choosePhotoSourcePressed()
            }
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

    //
    // PUBLIC METHODS
    //

    fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri = CropImage.getPickImageResultUri(context, data)
            imageUri?.let {
                if (saveToMemory) {
                    context.startActivity(ImageCropActivity.startSaveToMemory(context, it))
                } else {
                    context.startActivity(ImageCropActivity.start(context, it))
                }
            }
        }
    }

    fun choosePhotoSourcePressed() {
        if (context.hasWriteToExternalStoragePermission()) {
            CropImage.startPickImageActivity(context)
        } else {
            requestStoragePermissions()
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun requestStoragePermissions() {
        if (!Dexter.isRequestOngoing()) {
            Dexter.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}