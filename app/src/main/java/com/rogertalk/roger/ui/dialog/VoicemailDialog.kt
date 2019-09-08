package com.rogertalk.roger.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.afollestad.materialdialogs.MaterialDialog
import com.rogertalk.roger.R
import com.rogertalk.roger.models.json.VoiceMailConfig
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.android.RawResourcesUtils
import com.rogertalk.roger.utils.extensions.possibleString
import com.rogertalk.roger.utils.extensions.stringResource
import com.rogertalk.roger.utils.log.logDebug
import com.rogertalk.roger.utils.phone.PhoneUtils
import org.json.JSONObject

object VoicemailDialog {

    /**
     * Show dialog with option to configure voicemail
     */
    fun show(context: Context) {
        val message: String
        val voiceConfig = readVoiceConfig(context)
        val voiceMailAvailable = voiceConfig != null
        if (voiceMailAvailable) {
            if (PrefRepo.voicemailConfigured) {
                val disableNumber = voiceConfig?.allConditionalDeactivation ?: ""
                message = R.string.voice_mail_description_set.stringResource(disableNumber)
            } else {
                message = context.getString(R.string.voice_mail_description_unset)
            }
        } else {
            message = context.getString(R.string.voice_mail_description_unavailable)
        }

        val builder = MaterialDialog.Builder(context)
                .title(R.string.voice_mail_title)
                .content(message)
                .canceledOnTouchOutside(false)
                .positiveText(android.R.string.ok)
                .onPositive { materialDialog, dialogAction ->
                    if (!PrefRepo.voicemailConfigured && voiceConfig != null) {
                        PrefRepo.voicemailConfigured = true
                        val prefix = voiceConfig.allConditionalPrefix ?: ""
                        val suffix = voiceConfig.allConditionalSuffix ?: ""
                        val number = Uri.encode("${prefix}6468876437$suffix")
                        val intent = Intent(Intent.ACTION_DIAL)
                        intent.data = Uri.parse("tel:$number")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }

        // Only show cancel option if voicemail is available and hasn't been configured yet!
        if (voiceMailAvailable && !PrefRepo.voicemailConfigured) {
            builder.negativeText(android.R.string.cancel)
                    .show()
        } else {
            builder.show()
        }

    }

    private fun readVoiceConfig(context: Context): VoiceMailConfig? {
        val phoneMCC = PhoneUtils.getPhoneMCC(context) ?: ""
        val phoneMCN = PhoneUtils.getPhoneMNC(context) ?: ""
        logDebug { "Phone MCC: $phoneMCC , MNC: $phoneMCN" }

        val configText = RawResourcesUtils.readResourceAsString(context, R.raw.voicemailconfig)

        val topJson = JSONObject(configText)
        var mcnJson: JSONObject
        for (mcc in topJson.keys()) {
            if (phoneMCC == mcc) {
                mcnJson = topJson.getJSONObject(mcc)
                for (mcn in mcnJson.keys()) {
                    if (mcn == phoneMCN) {
                        val voiceConfigJson = mcnJson.getJSONObject(mcn)
                        logDebug { "Found entry" }
                        val brand = voiceConfigJson.possibleString("brand")
                        val operator = voiceConfigJson.possibleString("operator")
                        val prefix = voiceConfigJson.possibleString("all_conditional_prefix")
                        val suffix = voiceConfigJson.possibleString("all_conditional_suffix")
                        val deactivation = voiceConfigJson.possibleString("all_conditional_deactivation")
                        return VoiceMailConfig(brand, operator, prefix, suffix, deactivation)
                    }
                }
            }
        }
        return null
    }

}