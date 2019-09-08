package com.rogertalk.roger.utils.phone

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES.LOLLIPOP_MR1
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.rogertalk.kotlinjubatus.AndroidVersion
import com.rogertalk.roger.repo.DeviceProfileRepo
import com.rogertalk.roger.utils.extensions.appController
import com.rogertalk.roger.utils.extensions.hasPhoneStatePermission
import com.rogertalk.roger.utils.extensions.hasSMSPermission
import com.rogertalk.roger.utils.log.logWarn


class PhoneUtils() {

    companion object {

        fun getPhoneMCC(context: Context): String? {
            if (AndroidVersion.fromApiVal(LOLLIPOP_MR1, true) && !context.hasPhoneStatePermission()) {
                logWarn { "No phone permission" }
                return null
            }
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (manager.networkOperator.length >= 3) {
                return manager.networkOperator.substring(0, 3)
            }
            return null
        }

        fun getPhoneMNC(context: Context): String? {
            if (AndroidVersion.fromApiVal(LOLLIPOP_MR1, true) && !context.hasPhoneStatePermission()) {
                logWarn { "No phone permission" }
                return null
            }
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (manager.networkOperator.length > 3) {
                return manager.networkOperator.substring(3)
            }
            return null
        }

        fun getPhoneOperator(context: Context): String? {
            if (AndroidVersion.fromApiVal(LOLLIPOP_MR1, true) && !context.hasPhoneStatePermission()) {
                logWarn { "No phone permission" }
                return null
            }
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return manager.networkOperatorName
        }

        /**
         * True if the device is capable of sending SMS
         * (only tells us about the device capability to do so, doesn't take into account if the device
         * actually has an active SIM card as that would require extra permissions on M)
         */
        fun canSendSms(): Boolean {
            val pm = appController().packageManager
            return pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) || pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)
        }

        @TargetApi(22)
        private fun getInternalPhoneNumber(context: Context): String {
            // on new Android version, try multiple SIM cards
            if (AndroidVersion.fromApiVal(LOLLIPOP_MR1, true)) {
                if (!context.hasPhoneStatePermission()) {
                    logWarn { "No phone state permission" }
                    // we don't have the necessary permission
                    return ""
                }

                val subManager = SubscriptionManager.from(context)
                val simList = subManager.activeSubscriptionInfoList ?: return ""
                for (sim in simList) {
                    if (sim.number != null) {
                        return sim.number
                    }
                }
            }

            // old way of getting the phone number (it still works on newer APIs for some cases, that's why
            // this instruction is not in an "else" statement
            if (context.hasSMSPermission()) {
                val telManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                return telManager.line1Number ?: ""
            }

            return ""
        }

        /**
         * Get the number prefix for the a given number.
         */
        fun getCountryPrefixForNumber(rawPhoneNumber: String): String {
            val phoneNumberUtil = PhoneNumberUtil.getInstance()
            val phoneNumber: Phonenumber.PhoneNumber
            val region = PhoneUtils.getOwnPhoneCountryCode(appController())

            try {
                phoneNumber = phoneNumberUtil.parse(rawPhoneNumber, region)
            } catch (e: NumberParseException) {
                // nothing we can do
                return ""
            }

            val num = phoneNumberUtil.getCountryCodeForRegion(phoneNumberUtil.getRegionCodeForNumber(phoneNumber))
            return num.toString()
        }

        /**
         * Get the number prefix for the user's home country.
         * Will return something like "+1"
         */
        fun getCountryPrefix(ctx: Context): String {
            val util = PhoneNumberUtil.getInstance()
            val region = PhoneUtils.getOwnPhoneCountryCode(ctx)
            val countryCode = util.getCountryCodeForRegion(region)
            return "+$countryCode"
        }

        /**
         * Get an example number of the user's home country.
         * To be used mostly in Hint fields.
         */
        fun getCountryExamplePhoneNumber(ctx: Context): String {
            val util = PhoneNumberUtil.getInstance()
            val region = PhoneUtils.getOwnPhoneCountryCode(ctx)
            util.getExampleNumberForType(region, PhoneNumberUtil.PhoneNumberType.MOBILE)?.let {
                return util.format(it, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            }
            return ""
        }

        /**
         * The phone's region based on the network operator. For example, "US" for the United States.
         */
        @TargetApi(22)
        fun getOwnPhoneCountryCode(context: Context): String {
            var countryCode: String? = null
            // New way of obtaining the country code ISO.
            if (AndroidVersion.fromApiVal(LOLLIPOP_MR1, true) && context.hasPhoneStatePermission()) {
                SubscriptionManager.from(context).activeSubscriptionInfoList?.let {
                    countryCode = it.map { it.countryIso }.filter { !it.isEmpty() }.firstOrNull()
                }
            }
            if (countryCode == null) {
                val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (!manager.networkCountryIso.isEmpty()) {
                    countryCode = manager.networkCountryIso
                }
            }
            // We need to upper case the entire country ISO, because that's what libPhone expects.
            return countryCode?.toUpperCase() ?: "US"
        }

        /**
         * Get own number properly formatted, that includes the country prefix as well
         */
        fun getOwnPhoneNumber(ctx: Context): String {
            var internalPhoneNumber = getInternalPhoneNumber(ctx)
            if (internalPhoneNumber.isBlank()){
               val possibleNumbers = DeviceProfileRepo.getCachedProfile().possiblePhoneNumbers()
                if(possibleNumbers.isNotEmpty()){
                    internalPhoneNumber = possibleNumbers.first()
                }
            }

            return getProperPhoneNumber(internalPhoneNumber)
        }

        /**
         * Get a properly formatted E164 number based on the provided raw phone number.
         * This will also automatically try to add the country prefix if the number doesn't
         * have one yet.
         *
         * Will return empty string if number is not possible
         */
        fun getProperPhoneNumber(rawPhoneNumber: String): String {
            val ctx = appController()
            val phoneNumberUtil = PhoneNumberUtil.getInstance()
            val phoneNumber: Phonenumber.PhoneNumber
            val countryCode = getOwnPhoneCountryCode(ctx)
            val countryNumberPrefix = getCountryPrefix(ctx)

            try {
                phoneNumber = phoneNumberUtil.parseAndKeepRawInput(rawPhoneNumber, countryCode)
            } catch (e: NumberParseException) {
                if (countryNumberPrefix.isEmpty()) {
                    // we don't have the current country code for some reason, nothing we can do
                    return ""
                }

                if (e.errorType === NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                    // we're missing the country code, let's try again with the default country code now
                    return getProperPhoneNumber(countryNumberPrefix + rawPhoneNumber)
                }

                // nothing we can do
                return ""
            }

            val formattedNumber = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
            val possible = phoneNumberUtil.isPossibleNumberWithReason(phoneNumber)
            if (possible == PhoneNumberUtil.ValidationResult.IS_POSSIBLE) {
                return formattedNumber
            }
            return ""
        }

    }
}