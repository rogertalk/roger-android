package com.rogertalk.roger.helper

import co.mobiwise.materialintro.MaterialIntroConfiguration
import co.mobiwise.materialintro.shape.Focus
import co.mobiwise.materialintro.shape.FocusGravity
import co.mobiwise.materialintro.view.MaterialIntroView
import com.rogertalk.roger.R
import com.rogertalk.roger.ui.screens.TalkActivity
import com.rogertalk.roger.utils.extensions.colorResource
import com.rogertalk.roger.utils.extensions.dimensionResourceInPixels
import com.rogertalk.roger.utils.extensions.stringResource
import kotlinx.android.synthetic.main.talk_avatar_generic.*
import kotlinx.android.synthetic.main.talk_screen.*
import org.jetbrains.anko.px2sp
import java.util.*

class OverlayHelper(val talkScreen: TalkActivity) {

    private var overlayShowing = false

    private var tapToListerOverlay: MaterialIntroView? = null

    //
    // PUBLIC METHODS
    //

    fun displayMicrophoneOverlay() {
        //TODO : Fork this library to configure the look further to our needs
        if (overlayShowing) {
            return
        }
        MaterialIntroView.Builder(talkScreen)
                .setConfiguration(tutorialConfiguration())
                .setListener {
                    overlayShowing = false
                    talkScreen.microphoneTutorialPressed()
                }
                .enableIcon(false)
                .displaySkip(true)
                .skipText(R.string.overlay_later.stringResource())
                .setInfoTextSize(tutorialDefaultFontSize())
                .setInfoText(R.string.overlay_tap_to_talk.stringResource())
                .setTarget(talkScreen.recordingButton)
                .setUsageId(Date().time.toString())
                .show()
        overlayShowing = true
    }

    fun displayTapToListenOverlay() {
        if (overlayShowing) {
            return
        }
        tapToListerOverlay = MaterialIntroView.Builder(talkScreen)
                .setConfiguration(tutorialConfiguration())
                .setListener {
                    overlayShowing = false
                    talkScreen.tapToListenTutorialPressed()
                }
                .enableIcon(false)
                .setInfoTextSize(tutorialDefaultFontSize())
                .setInfoText(R.string.overlay_tap_to_listen.stringResource())
                .setTarget(talkScreen.avatarContainerShadow)
                .setUsageId(Date().time.toString())
                .show()
        overlayShowing = true
    }

    /**
     * Top-right corner, add members to an existing group
     */
    fun displayManageMembersOverlay() {
        if (overlayShowing) {
            return
        }
        MaterialIntroView.Builder(talkScreen)
                .setConfiguration(tutorialConfiguration())
                .setListener {
                    overlayShowing = false
                    talkScreen.tapManageMembers()
                }
                .enableIcon(false)
                .setTargetPadding(5)
                .setInfoTextSize(tutorialDefaultFontSize())
                .setInfoText(R.string.overlay_manage_conversation.stringResource())
                .setTarget(talkScreen.groupManagementElement)
                .setUsageId(Date().time.toString())
                .show()
        overlayShowing = true
    }

    fun forceDismissTapToListen() {
        if (!overlayShowing) {
            return
        }
        tapToListerOverlay?.let {
            if (it.isShown) {
                it.dismissNoAction()
                overlayShowing = false
            }
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun tutorialDefaultFontSize(): Int {
        return talkScreen.px2sp(R.dimen.font_l.dimensionResourceInPixels(talkScreen)).toInt()
    }

    /**
     * Generic display configuration that applies to all overlays
     */
    private fun tutorialConfiguration(): MaterialIntroConfiguration {
        val materialIntroConfiguration = MaterialIntroConfiguration()
        materialIntroConfiguration.focusType = Focus.MINIMUM
        materialIntroConfiguration.focusGravity = FocusGravity.CENTER
        materialIntroConfiguration.delayMillis = 500
        materialIntroConfiguration.isFadeAnimationEnabled = true
        materialIntroConfiguration.isDotViewEnabled = false
        materialIntroConfiguration.isDismissOnTouch = false
        materialIntroConfiguration.maskColor = R.color.black_80.colorResource(talkScreen)
        return materialIntroConfiguration
    }

}