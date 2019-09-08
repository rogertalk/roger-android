package com.rogertalk.roger.repo

import com.rogertalk.roger.models.json.Account

object UserAccountRepo {

    //
    // CALCULATED PROPERTIES
    //

    /**
     * This tells us if the user is considered brand new
     */
    val isBrandNewUser: Boolean
        get() {
            if (PrefRepo.didTapToTalk && PrefRepo.didTapToListen) {
                return false
            }
            return StreamCacheRepo.getCached().size < 3
        }

    //
    // PUBLIC METHODS
    //

    fun current(): Account? {
        return SessionRepo.session?.account
    }

    /**
     * Update account of the current session
     */
    fun updateAccount(newAccount: Account) {
        val currentSession = SessionRepo.session ?: return

        val newSession = currentSession.copy(account = newAccount)

        // Persist session
        PrefRepo.session = newSession

        // Update in-memory session
        SessionRepo.session = newSession
    }

    /**
     * @return True if account name is set, false otherwise
     */
    fun accountNameSet(): Boolean {
        val currentDisplayName = UserAccountRepo.current()?.customDisplayName ?: ""
        val indexOfAt = currentDisplayName.indexOf("@")

        if (currentDisplayName.isBlank() ||
                currentDisplayName.startsWith("+") ||
                indexOfAt != -1) {
            return false
        }
        return true
    }

    /**
     * @return Account ID or null
     */
    fun id(): Long? {
        return SessionRepo.session?.account?.id ?: return null
    }


}
