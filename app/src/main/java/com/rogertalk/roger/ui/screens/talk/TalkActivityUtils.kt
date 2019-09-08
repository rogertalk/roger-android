package com.rogertalk.roger.ui.screens.talk

import android.content.Context
import android.content.Intent
import com.rogertalk.roger.ui.screens.TalkActivity

object TalkActivityUtils {

    val EXTRA_JUST_ONBOARDED = "justOnboarded"
    val UI_REFRESH_RATE_MILLIS: Long = 10000
    val EXTRA_RANKING_VALUE = "rankingValue"
    val EXTRA_PRE_SELECT_STREAM = "streamId"
    val EXTRA_CAME_FROM_NOTIFICATION = "cameFromNotification"

    val TIMER_REFRESH_UI = 1
    val TIMER_REFRESH_STREAMS = 2
    val MAX_TITLE_CHARACTERS = 40

    fun getStartTalkScreen(context: Context, justOnBoarded: Boolean = false): Intent {
        val startIntent = Intent(context, TalkActivity::class.java)
        startIntent.putExtra(EXTRA_JUST_ONBOARDED, justOnBoarded)
        startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        return startIntent
    }

    fun startWithStream(context: Context, streamId: Long, cameFromNotification: Boolean = false): Intent {
        val startIntent = Intent(context, TalkActivity::class.java)
        startIntent.putExtra(EXTRA_PRE_SELECT_STREAM, streamId)
        startIntent.putExtra(EXTRA_CAME_FROM_NOTIFICATION, cameFromNotification)
        startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        return startIntent
    }

    fun startTalkScreenForRankSharing(context: Context, rankPosition: Int): Intent {
        val startIntent = Intent(context, TalkActivity::class.java)
        startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startIntent.putExtra(EXTRA_RANKING_VALUE, rankPosition)
        return startIntent
    }
}
