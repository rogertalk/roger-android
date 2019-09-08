package com.rogertalk.roger.utils.android

import android.annotation.TargetApi
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import android.provider.Telephony
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.kotlinjubatus.isSafe
import com.rogertalk.kotlinjubatus.isSafeChooser
import com.rogertalk.roger.R
import com.rogertalk.roger.models.json.Account
import com.rogertalk.roger.repo.UserAccountRepo
import com.rogertalk.roger.utils.constant.WEBSITE_BASE_ROOT
import com.rogertalk.roger.utils.constant.WEBSITE_GROUP_PATH
import com.rogertalk.roger.utils.constant.WEBSITE_ROOT
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.log.logError
import com.rogertalk.roger.utils.log.logWarn
import org.jetbrains.anko.longToast

object ShareUtils {

    /**
     * Experimental feature for sending raw audio recorded in Roger to any app in the OS
     */
    @TargetApi(23)
    fun shareAudioToExternalApp(context: Context, filename: String) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "audio/*"
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://com.rogertalk.roger.fileprovider/rogersounds/$filename"))
        //TODO : refactor this text
        share.putExtra(Intent.EXTRA_TEXT, "Reply to me on https://rogertalk.com/pedro")

        // Grant permission to all apps
        val matcher = if (AndroidVersion.fromApiVal(M, true)) PackageManager.MATCH_ALL else PackageManager.MATCH_DEFAULT_ONLY
        val resInfoList = context.packageManager.queryIntentActivities(share, matcher)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            // TODO : include URL encoded permission so that whatsapp doesn't complain
            context.grantUriPermission(packageName, Uri.parse("content://com.rogertalk.roger.fileprovider/rogersounds/$filename?caption=Reply%20to%20me%20on%20https%3A%2F%2Frogertalk.com%2Fpedro"), Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }


        context.startActivity(Intent.createChooser(share, "Send Roger with"))
        return
    }

    /**
     * Issue a sharable text via SMS
     */
    fun shareViaSMS(message: String, phoneList: List<String>, context: Context) {
        val address = phoneList.joinToString(";")
        logDebug { "Address to send SMS to: $address" }

        if (AndroidVersion.fromApiVal(KITKAT, true)) {
            val defaultSMSpackage = Telephony.Sms.getDefaultSmsPackage(context)

            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_TEXT, message)
            sendIntent.putExtra("sms_body", message)
            sendIntent.putExtra("address", address)
            sendIntent.putExtra("exit_on_sent", true)

            // Can be null in case that there is no default, then the user would be able to choose any app that support this intent
            if (defaultSMSpackage != null) {
                sendIntent.`package` = defaultSMSpackage
            }
            try {
                context.startActivity(sendIntent)
            } catch(e: ActivityNotFoundException) {
                logError(e) { "No activity to handle SMS" }
            }
        } else {
            // Send SMS on previous version of Android
            val smsIntent = Intent(Intent.ACTION_VIEW)
            smsIntent.type = "vnd.android-dir/mms-sms"
            smsIntent.putExtra("address", address)
            smsIntent.putExtra("sms_body", message)
            smsIntent.putExtra("exit_on_sent", true)
            if (smsIntent.isSafe(context)) {
                context.startActivity(smsIntent)
            } else {
                logWarn { "No app to handle SMS" }
            }
        }
    }


    /**
     * Share top talker text
     */
    fun shareTopTalker(context: Context, rankPosition: Int) {
        var shareText = R.string.share_text_top_talkers.stringResource(rankPosition.toString())
        val rogerHashTag = R.string.talk_more_hash_tag.stringResource()

        var userShareLink = ""
        val userAccount = UserAccountRepo.current()
        if (userAccount != null) {
            userShareLink = getCompleteShareUrl(userAccount, isOpenGroup = false)
        }

        shareText = "${EmojiUtils.trophy} $shareText ${EmojiUtils.partyPopper} $rogerHashTag $userShareLink"


        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

        if (shareIntent.isSafeChooser(context)) {
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.invite_talk_other_share_with)))
        } else {
            logWarn { "No apps for sharing" }
        }
    }

    /**
     * Share top talker text
     */
    fun shareUserProfile(context: Context, account: Account) {
        var shareText = R.string.contact_options_share_profile_copy.stringResource(account.displayName)
        shareText = "$shareText ${getCompleteShareUrl(account, isOpenGroup = false)}"

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)

        if (shareIntent.isSafeChooser(context)) {
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.invite_talk_other_share_with)))
        } else {
            logWarn { "No apps for sharing" }
        }
    }

    /**
     * Share audio link with any given app
     */
    fun shareAudioLink(message: String, context: Context) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, message)

        if (shareIntent.isSafeChooser(context)) {
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.invite_talk_other_share_with)))
        } else {
            logWarn { "No apps for sharing" }
        }
    }

    /**
     * Share URL with the user's username appended to it
     */
    fun getCompleteShareUrl(account: Account, token: String = "", isOpenGroup: Boolean): String {
        var result: String
        if (isOpenGroup) {
            result = WEBSITE_GROUP_PATH
        } else {
            result = "$WEBSITE_ROOT/${account.handle}"
        }

        if (token.isNotEmpty()) {
            result += "/$token"
        }
        return result
    }

    /**
     * This is the display-friendly share URL with the user's username appended to it.
     * To share user @getShareUrl instead.
     */
    fun getShortProfileUrl(account: Account): String {
        return "$WEBSITE_BASE_ROOT/${account.handle}"
    }

    /**
     * Tries to open an external link. If fail, will copy to clipboard instead.
     */
    fun openExternalLink(context: Context, sharableText: String) {
        val openIntent = Intent(Intent.ACTION_VIEW)
        try {
            openIntent.data = Uri.parse(sharableText)
            context.startActivity(openIntent)
        } catch (e: Exception) {
            val clip = ClipData.newPlainText("RogerLink", sharableText)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = clip

            // Display toast regarding this copy
            context.longToast(R.string.attachments_copied_to_clipboard.stringResource(sharableText))
        }
    }
}
