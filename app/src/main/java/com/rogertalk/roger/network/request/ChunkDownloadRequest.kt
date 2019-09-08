package com.rogertalk.roger.network.request

import com.rogertalk.roger.android.services.AudioDownloadManager
import com.rogertalk.roger.realm.CachedAudioRepo
import com.rogertalk.roger.utils.constant.NO_ID
import com.rogertalk.roger.utils.constant.RuntimeConstants.Companion.AUDIO_DOWNLOAD_DIR
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.runOnUiThread
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logWarn
import okhttp3.ResponseBody
import java.io.*
import java.util.*

class ChunkDownloadRequest(val chunkId: Long?, val audioURL: String, val streamId: Long?) : BaseRequest() {

    override fun enqueueRequest() {
        val callback = getCallback(ResponseBody::class.java)
        val call = getRogerAPI().audioDownload(audioURL)
        call.enqueue(callback)
    }

    override fun <T : Any> handleSuccess(t: T) {
        val responseBody = t as? ResponseBody ?: return

        val filePath = writeResponseBodyToDisk(responseBody)

        // Continue execution
        runOnUiThread {
            // Persist the entry in the database
            CachedAudioRepo.createEntry(chunkId ?: NO_ID, streamId ?: NO_ID, audioURL, filePath)

            AudioDownloadManager.downloadNext()
        }
    }

    override fun handleFailure(error: ResponseBody?, responseCode: Int) {
        super.handleFailure(error, responseCode)
        logError { "Failed to download file!" }

        runOnUiThread {
            AudioDownloadManager.downloadNext()
        }
    }


    /**
     * @return File path if the audio could be downloaded, empty string is it failed
     */
    private fun writeResponseBodyToDisk(body: ResponseBody): String {
        try {
            val audioFileDir = File(appController().filesDir, AUDIO_DOWNLOAD_DIR)
            if (!audioFileDir.exists()) {
                audioFileDir.mkdir()
            }
            val filename = UUID.randomUUID().toString()

            val targetFile = File(appController().filesDir, "$AUDIO_DOWNLOAD_DIR/$filename")

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                val fileReader = ByteArray(4096)
                var fileSizeDownloaded: Long = 0

                inputStream = body.byteStream()
                outputStream = FileOutputStream(targetFile)

                while (true) {
                    val read = inputStream!!.read(fileReader)
                    if (read == -1) {
                        break
                    }

                    outputStream.write(fileReader, 0, read)
                    fileSizeDownloaded += read.toLong()
                }
                outputStream.flush()
                return targetFile.absolutePath
            } catch (e: IOException) {
                logWarn { "Failed to save chunk file" }
                return ""
            } finally {
                if (inputStream != null) {
                    inputStream.close()
                }

                if (outputStream != null) {
                    outputStream.close()
                }
            }
        } catch (e: IOException) {
            logWarn { "Failed to save chunk file" }
            return ""
        }
    }
}