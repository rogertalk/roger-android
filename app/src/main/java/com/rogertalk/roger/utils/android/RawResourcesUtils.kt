package com.rogertalk.roger.utils.android

import android.content.Context
import android.support.annotation.Keep
import com.rogertalk.roger.utils.extensions.appController
import java.io.File
import java.io.FileOutputStream

class RawResourcesUtils {

    companion object {
        /**
         * Read a raw resource as a String
         */
        fun readResourceAsString(context: Context, resourceId: Int): String {
            try {
                val rawRes = context.resources.openRawResource(resourceId)
                val b = ByteArray(rawRes.available())
                rawRes.read(b)
                return String(b)
            } catch (e: Exception) {
                return ""
            }
        }

        /**
         * Get file from RAW resources
         */
        @Keep
        private fun getFileFromRawResources(filename: String): File? {
            val path = appController().filesDir.absolutePath

            // If file is already in internal storage, re-use it
            val fileTest = File(path + "/$filename")
            if (fileTest.exists()) {
                return fileTest
            }
            // Copy file to internal storage first


            try {
                val myOutput = FileOutputStream(path + "/$filename")
                val buffer = ByteArray(1024)
                var length: Int
                val myInput = appController().assets.open(filename)
                length = myInput.read(buffer)
                while (length > 0) {
                    myOutput.write(buffer, 0, length)
                    length = myInput.read(buffer)
                }
                myInput.close()
                myOutput.flush()
                myOutput.close()
            } catch(e: Exception) {
                return null
            }

            val file = File(path + "/$filename")
            return file
        }
    }
}
