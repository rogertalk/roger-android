package com.rogertalk.roger.utils.changelog

import android.content.Context
import com.rogertalk.kotlinjubatus.utils.AppStatsUtils
import com.rogertalk.roger.models.data.VersionChanges
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.utils.android.EmojiUtils
import com.rogertalk.roger.utils.android.EmojiUtils.backEmoji
import com.rogertalk.roger.utils.android.EmojiUtils.beachWithUmbrella
import com.rogertalk.roger.utils.android.EmojiUtils.bee
import com.rogertalk.roger.utils.android.EmojiUtils.camera
import com.rogertalk.roger.utils.android.EmojiUtils.champagneBottle
import com.rogertalk.roger.utils.android.EmojiUtils.dash
import com.rogertalk.roger.utils.android.EmojiUtils.flagBrazil
import com.rogertalk.roger.utils.android.EmojiUtils.flagFrance
import com.rogertalk.roger.utils.android.EmojiUtils.flagPortugal
import com.rogertalk.roger.utils.android.EmojiUtils.flagSpain
import com.rogertalk.roger.utils.android.EmojiUtils.gemStone
import com.rogertalk.roger.utils.android.EmojiUtils.groups
import com.rogertalk.roger.utils.android.EmojiUtils.guyWithSunglasses
import com.rogertalk.roger.utils.android.EmojiUtils.headphones
import com.rogertalk.roger.utils.android.EmojiUtils.ladybug
import com.rogertalk.roger.utils.android.EmojiUtils.monkeyCoveringEars
import com.rogertalk.roger.utils.android.EmojiUtils.multipleMusicNotes
import com.rogertalk.roger.utils.android.EmojiUtils.musicalNotes
import com.rogertalk.roger.utils.android.EmojiUtils.mute
import com.rogertalk.roger.utils.android.EmojiUtils.partyPopper
import com.rogertalk.roger.utils.android.EmojiUtils.shortCake
import com.rogertalk.roger.utils.android.EmojiUtils.smilingFace
import com.rogertalk.roger.utils.android.EmojiUtils.speakerWithSoundWaves
import com.rogertalk.roger.utils.android.EmojiUtils.victoryHand
import com.rogertalk.roger.utils.android.EmojiUtils.watch
import java.util.*

object ChangeLogHistory {

    fun shouldShowChangelog(context: Context): Boolean {
        val currentAppVersion = AppStatsUtils.getAppVersionNumber(context)
        if (PrefRepo.lastInstalledVersion < currentAppVersion && PrefRepo.lastInstalledVersion != 0) {
            // Ignore current beta
            if (currentAppVersion > 162) {
                return false
            }

            // Minor versions to ignore
            if (PrefRepo.lastInstalledVersion >= 151 && currentAppVersion < 153) {
                return false
            }
            return true
        }
        return false
    }

    /**
     * This marks and persists the state of latest version of the app for which we showed changelog,
     * so it doesn't display again.
     */
    fun changelogDisplayHandled(context: Context) {
        val currentAppVersion = AppStatsUtils.getAppVersionNumber(context)
        PrefRepo.lastInstalledVersion = currentAppVersion
    }

    fun getLatestChangeLogText(): String {
        val changesText = latest().changes
        val strB = StringBuilder(changesText.size * 50)
        for (change in changesText) {
            strB.append("• ")
            strB.append(change)
            strB.append("\n")
        }

        return strB.toString()
    }

    /**
     * Pre-constructed change log history
     */
    fun changeLogHistory(): ArrayList<VersionChanges> {
        val resultingList = ArrayList<VersionChanges>(30)

        // Add all the changes
        resultingList.add(latest())
        resultingList.add(changes_2_0_9())
        resultingList.add(changes_2_0_1())
        resultingList.add(changes1_62_0())
        resultingList.add(changes1_61_0())
        resultingList.add(changes1_60_1())
        resultingList.add(changes1_59_0())
        resultingList.add(changes1_58_0())
        resultingList.add(changes1_57_0())
        resultingList.add(changes1_56_0())
        resultingList.add(changes1_55_3())
        resultingList.add(changes1_54_1())
        resultingList.add(changes1_52_11())
        resultingList.add(changes1_51_0())
        resultingList.add(changes1_49_0())
        resultingList.add(changes1_48_1())
        resultingList.add(changes1_46_0())
        resultingList.add(changes1_44_0())
        resultingList.add(changes1_42_0())
        resultingList.add(changes1_40_0())
        resultingList.add(changes1_39_0())
        resultingList.add(changes1_36_0())
        resultingList.add(changes1_35_0())
        resultingList.add(changes1_33_0())
        resultingList.add(changes1_31_0())
        resultingList.add(changes1_28_0())
        resultingList.add(changes1_24_0())
        resultingList.add(changes1_23_0())
        resultingList.add(changes1_19_0())

        return resultingList
    }

    //
    // PRIVATE METHODS
    //

    private fun latest(): VersionChanges {
        return changes_2_0_11()
    }

    private fun changes_2_0_11(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("You'll now get a reminder to listen to expiring conversations")
        changesList.addLast("Attachments are now easier to access from notifications")
        changesList.addLast("Various improvements and bug-fixes ${EmojiUtils.autumnLeave}")
        return VersionChanges("2.0.11", changesList)
    }

    private fun changes_2_0_9(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Audio playback is now more robust! ${EmojiUtils.speakerWithSoundWaves}")
        changesList.addLast("Whole new UI that is easier to use ${EmojiUtils.smilingFace}")
        changesList.addLast("Easily Buzz $bee someone to get their attention")
        changesList.addLast("Fixed a wide range of issues ${EmojiUtils.ladybug}")
        return VersionChanges("2.0.9", changesList)
    }

    private fun changes_2_0_1(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Roger TalkHeads! - Talk with anyone from outside the app")
        changesList.addLast("Learn more about it here: https://www.youtube.com/watch?v=OgAob6Fr39k")
        changesList.addLast("Various improvements to photo sharing, state notifications, playback and more ${EmojiUtils.smilingFace}")
        return VersionChanges("2.0.1", changesList)
    }

    private fun changes1_62_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Various improvements to photo sharing $camera")
        changesList.addLast("Show person details as they speak in a group")
        changesList.addLast("Fix a specific issue that occurred to people updating to Android 7 (N) $shortCake")
        changesList.addLast("Various improvements and bug-fixes $beachWithUmbrella")
        return VersionChanges("1.62.5", changesList)
    }

    private fun changes1_61_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Listen to audio at different speeds. $dash (Only available for Android Marshmallow and up)")
        changesList.addLast("Several improvements to attachments. Should now be easier to manage and view them $partyPopper")
        changesList.addLast("Several bug-fixes and improvements to talkback, bluetooth, photos and others")
        return VersionChanges("1.61.5", changesList)
    }

    private fun changes1_60_1(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Easily toggle live mode $smilingFace")
        changesList.addLast("Updated translations for portuguese variants")
        changesList.addLast("Various other enhancements")
        return VersionChanges("1.60.1", changesList)
    }

    private fun changes1_59_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Introducing attachments to conversations! Attach simple links or photos to a conversation $camera")
        changesList.addLast("Bug fixes and other improvements")
        return VersionChanges("1.59.2", changesList)
    }

    private fun changes1_58_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Translation updates ${EmojiUtils.flagBrazil}")
        changesList.addLast("Bug fixes and several other minor improvements $smilingFace")
        return VersionChanges("1.58.0", changesList)
    }


    private fun changes1_57_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Autoplay functionality - New messages will automatically play when you have the app open $smilingFace")
        changesList.addLast("Bug fixes and other improvements")
        return VersionChanges("1.57.2", changesList)
    }

    private fun changes1_56_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("We radically improved the experience for budget phones and devices with smaller screens")
        changesList.addLast("Conversations are now easier to create and manage $smilingFace")
        changesList.addLast("Support for badge notification count on Huawei and Solid Launchers ${EmojiUtils.phone}")
        changesList.addLast("Fix dozens of issues and made the app feel more snappier than ever ${EmojiUtils.champagneBottle}")
        return VersionChanges("1.56.4", changesList)
    }

    private fun changes1_55_3(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("We’ve further simplified the way you manage your conversations")
        changesList.addLast("We've made Roger easier for new users")
        changesList.addLast("Bug fixes and performance improvements $ladybug")
        return VersionChanges("1.55.3", changesList)
    }

    private fun changes1_54_1(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("We’ve simplified the way you manage your conversations")
        changesList.addLast("It’s now much easier to add members via address book, handle, or external link $groups")
        changesList.addLast("You can unmute a conversation $speakerWithSoundWaves")
        changesList.addLast("More bug fixes  $ladybug")
        return VersionChanges("1.54.1", changesList)
    }

    private fun changes1_52_11(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("New navigation. You can now add and invite people from the conversations list")
        changesList.addLast("We now have a tutorial for new users to Roger")
        changesList.addLast("Various fixes and translations improvements")

        return VersionChanges("1.52.11", changesList)
    }

    private fun changes1_51_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("You can now skip Rogers. It’s amazing when groups get super chatty")
        changesList.addLast("We’ve made changes to audio caching. That should save your storage usage $smilingFace")
        changesList.addLast("You can mute a conversation and leave a group $mute")
        changesList.addLast("And we fixed an issue we had when sharing Rogers on other apps")

        return VersionChanges("1.51.0", changesList)
    }

    private fun changes1_49_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("We’re introducing Featured Groups. It’s a fun new way to talk with " +
                "people from around the world. Imagine having conversations with French people during " +
                "the Euro Cup or about the weather with an American in Arizona. These will be spontaneous " +
                "conversations, a whole new experience. Give them a try! ")
        changesList.addLast("Now we're displaying unread badges in the new Sony Launcher")

        return VersionChanges("1.49.0", changesList)
    }

    private fun changes1_48_1(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Roger is now friendly with (RTL) Arabic devices")
        changesList.addLast("We fixed some issues that were happening on older devices (high-speed voice or failure to record) $musicalNotes $smilingFace")
        changesList.addLast("We improved listening to rogers for Samsung devices")
        changesList.addLast("Improved the way push notifications are handled")
        changesList.addLast("We Improved the onboarding experience for Android M and N")
        changesList.addLast("[New] Manage a group (it’s now possible to remove members from an existing group) $groups")
        changesList.addLast("Other bugfixes and performance improvements $ladybug")

        return VersionChanges("1.48.1", changesList)
    }

    private fun changes1_46_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("Groups have been greatly improved. It’s now easier to create and manage them, and they now feel part of the core experience on Roger $groups")
        changesList.addLast("The overall app experience has been improved for tablets and talkback users $partyPopper $partyPopper")
        changesList.addLast("Fixed several issues, including those related to listening and pause")
        changesList.addLast("Updated and improved translations $flagFrance $flagPortugal $flagSpain $flagBrazil")

        return VersionChanges("1.46.0", changesList)
    }

    private fun changes1_44_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("You will now see the Roger smile instead of the microphone.")
        changesList.addLast("Now when you’re talking your music stops and starts again when you’re done. $musicalNotes")
        changesList.addLast("Check out your lockscreen and wear devices while listening to a roger. We added ways to interact with the app from there. $watch")
        changesList.addLast("And best for last, it’s now possible to control (rewind, play, pause, talk) Roger from any headphones. $headphones")

        return VersionChanges("1.44.0", changesList)
    }

    private fun changes1_42_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("We’ve made easier to invite anyone to a group conversation, even if they are not on Roger. $groups")
        changesList.addLast("We fixed some issues when listening to Rogers")
        changesList.addLast("Some Rogers were being heard out of order. Now it’s fixed!")

        return VersionChanges("1.42.0", changesList)
    }

    private fun changes1_40_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.addLast("We improved the experience for groups $groups")
        changesList.addLast("You can now connect your phone\'s voicemail to Roger! ${EmojiUtils.mailbox}")
        changesList.addLast("Bug fixes and performance improvements")

        return VersionChanges("1.40.0", changesList)
    }

    private fun changes1_39_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("You can now connect services like SoundCloud, Dropbox, Slack and many others (via IFTTT) $guyWithSunglasses")
        changesList.addLast("Start groups with people who are not on Roger $groups")
        changesList.addLast("You'll now be notified when friends join Roger")
        changesList.addLast("Bug fixes and performance improvements")

        return VersionChanges("1.39.0", changesList)
    }

    private fun changes1_36_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("We’ve changed navigation. It’s at the top now! $monkeyCoveringEars")
        changesList.addLast("New logic for speaker vs earpiece playing. Once you select one it won’t switch for that session")
        changesList.addLast("For Talkback users, we now play “information” tones even on loudspeaker")
        changesList.addLast("Various fixes to on-boarding")

        return VersionChanges("1.36.0", changesList)
    }

    private fun changes1_35_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("All new users will now be able to get instant access into the app $partyPopper")
        changesList.addLast("We made improvements to offline behavior")
        changesList.addLast("Now you will see your active contacts in recents")

        return VersionChanges("1.35.0", changesList)
    }

    private fun changes1_33_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("Raise to ear detection wasn't working on some devices so we fixed it! $victoryHand")
        changesList.addLast("Notifications should be more reliable now and not go missing!")
        changesList.addLast("Accessibility improvements and better controls")
        changesList.addLast("Updated translations")
        changesList.addLast("Bug fixes across the whole app $champagneBottle")

        return VersionChanges("1.33.0", changesList)
    }

    private fun changes1_31_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("Play, Pause and Rewind conversations $backEmoji")
        changesList.addLast("You can now see when someone is talking to you in REAL TIME! $multipleMusicNotes")
        changesList.addLast("Share crystal clear voice links to any app $gemStone")
        changesList.addLast("Roger is now available in Spanish, French, Portuguese and Japanese")

        return VersionChanges("1.31.0", changesList)
    }

    private fun changes1_28_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("Groups: see participants, add new ones")
        changesList.addLast("Various improvements to accessibility")
        changesList.addLast("Fixed a lot of crash-related issues $ladybug")

        return VersionChanges("1.28.0", changesList)
    }

    private fun changes1_24_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("Easier to start a conversation or a GROUP!")
        changesList.addLast("NEW: you can now talk to Amazon Alexa even without owning an Echo")
        changesList.addLast("$ladybug fixes all around, the best Roger experience ever.")

        return VersionChanges("1.24.0", changesList)
    }

    private fun changes1_23_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("You can now create a group with your favorite people $groups")
        changesList.addLast("We now support binaural audio (try with your headphones on) $headphones")
        changesList.addLast("We made some improvements to our audio quality $speakerWithSoundWaves")

        return VersionChanges("1.23.0", changesList)
    }

    private fun changes1_19_0(): VersionChanges {
        val changesList = LinkedList<String>()
        changesList.add("Improved experience for talkback users")
        changesList.addLast("Fixed some issues with notifications")
        changesList.addLast("The application behaves better when offline")

        return VersionChanges("1.19.0", changesList)
    }

}
