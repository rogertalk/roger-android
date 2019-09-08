package com.rogertalk.roger.utils.contact

import android.provider.ContactsContract

class ContactLabelMatch() {

    companion object {
        fun getPhoneNumberLabel(phoneNumberType: Int): String {
            when (phoneNumberType) {
                ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> return "home"
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> return "mobile"
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> return "work"
                ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT -> return "assistant"
                ContactsContract.CommonDataKinds.Phone.TYPE_CAR -> return "car"
                ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK -> return "callback"
                ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> return "company_main"
                ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> return "fax_home"
                ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> return "fax_work"
                ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX -> return "fax_other"
                ContactsContract.CommonDataKinds.Phone.TYPE_ISDN -> return "isdn"
                ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> return "main"
                ContactsContract.CommonDataKinds.Phone.TYPE_MMS -> return "mms"
                ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> return "pager"
                ContactsContract.CommonDataKinds.Phone.TYPE_RADIO -> return "radio"
                ContactsContract.CommonDataKinds.Phone.TYPE_TELEX -> return "telex"
                ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD -> return "tty_tdd"
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> return "work_mobile"
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER -> return "work_pager"
                ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> return "custom"

                else -> return "other"
            }
        }

        fun getEmailLabel(emailType: Int): String {
            when (emailType) {
                ContactsContract.CommonDataKinds.Email.TYPE_HOME -> return "home"
                ContactsContract.CommonDataKinds.Email.TYPE_MOBILE -> return "mobile"
                ContactsContract.CommonDataKinds.Email.TYPE_WORK -> return "work"
                else -> return "other"
            }
        }
    }
}