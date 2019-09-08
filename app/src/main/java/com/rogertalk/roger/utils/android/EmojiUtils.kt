package com.rogertalk.roger.utils.android

import java.util.*

object EmojiUtils {

    val randomFaceEmoji: String
        get() {
            // TODO : This method needs improvement
            val random = Random().nextInt(5)
            return when (random) {
                0 -> guyWithSunglasses
                1 -> grinningFace
                2 -> winkingFace
                3 -> smilingFace
                else -> happyPersonRaisingHand
            }
        }

    val sentRandomEmoji: String
        get() {
            // TODO: Thematic (Christmas), Contextual (Flag of person you're talking with)
            // TODO: Score-based - if person talks with the other very frequently in a short span of time show emojis that portrait that
            // TODO: Weather based
            // TODO: Time of day based
            val random = Random().nextInt(7)
            return when (random) {
                0 -> guyWithSunglasses
                1 -> grinningFace
                2 -> winkingFace
                3 -> smilingFace
                4 -> fishBump
                5 -> partyPopper
                6 -> slightlySmillingFace
                else -> victoryHand
            }
        }

    val autumnLeave: String
        get() {
            return getEmoji(0x1F342)
        }

    val mailbox: String
        get() {
            return getEmoji(0x1F4EB)
        }

    val guyWithSunglasses: String
        get() {
            return getEmoji(0x1F60E)
        }

    val winkingFace: String
        get() {
            return getEmoji(0x1F609)
        }

    val dash: String
        get() {
            return getEmoji(0x1F4A8)
        }

    val smilingFace: String
        get() {
            return getEmoji(0x1F642)
        }

    val checkmark: String
        get() {
            return getEmoji(0x2714)
        }

    val eyes: String
        get() {
            return getEmoji(0x1F440)
        }

    val fishBump: String
        get() {
            return getEmoji(0x1F44A)
        }

    val smilingFaceWithHeartEyes: String
        get() {
            return getEmoji(0x1F60D)
        }

    val thinkingFace: String
        get() {
            return getEmoji(0x1F914)
        }

    val grinningFace: String
        get() {
            return getEmoji(0x1F600)
        }

    val slightlySmillingFace: String
        get() {
            return getEmoji(0x1F642)
        }

    val happyPersonRaisingHand: String
        get() {
            return getEmoji(0x1F64B)
        }

    val multipleMusicNotes: String
        get() {
            return getEmoji(0x1F3B6)
        }

    val bigHeart: String
        get() {
            return getEmoji(0x2764)
        }

    val backEmoji: String
        get() {
            return getEmoji(0x1F519)
        }

    val beachWithUmbrella: String
        get() {
            return getEmoji(0x1F3D6)
        }

    val flagPortugal: String
        get() {
            return getEmoji(0x1F1F5)
        }

    val flagSpain: String
        get() {
            return getEmoji(0x1F1EA)
        }

    val flagFrance: String
        get() {
            return getEmoji(0x1F1EB)
        }

    val flagBrazil: String
        get() {
            return "${getEmoji(0x1F1E7)}${getEmoji(0x1F1F7)}"
        }

    val gemStone: String
        get() {
            return getEmoji(0x1F48E)
        }

    val ladybug: String
        get() {
            return getEmoji(0x1F41E)
        }

    val groups: String
        get() {
            return getEmoji(0x1F465)
        }

    val gift: String
        get() {
            return getEmoji(0x1F381)
        }

    val headphones: String
        get() {
            return getEmoji(0x1F3A7)
        }

    val speakerWithSoundWaves: String
        get() {
            return getEmoji(0x1F50A)
        }

    val bee: String
        get() {
            return getEmoji(0x1F41D)
        }

    val trophy: String
        get() {
            return getEmoji(0x1F3C6)
        }

    val monkeyCoveringEars: String
        get() {
            return getEmoji(0x1F649)
        }

    val musicalNotes: String
        get() {
            return getEmoji(0x1F3B6)
        }

    val mute: String
        get() {
            return getEmoji(0x1F507)
        }

    val partyPopper: String
        get() {
            return getEmoji(0x1F389)
        }

    val phone: String
        get() {
            return getEmoji(0x1F4F1)
        }

    val raiseHands: String
        get() {
            return getEmoji(0x1F64C)
        }

    val balloon: String
        get() {
            return getEmoji(0x1F388)
        }

    val relievedFace: String
        get() {
            return getEmoji(0x1F60C)
        }

    val shortCake: String
        get() {
            return getEmoji(0x1F370)
        }

    val speechBubble: String
        get() {
            return getEmoji(0x1F4AC)
        }

    val victoryHand: String
        get() {
            return getEmoji(0x270C)
        }

    val champagneBottle: String
        get() {
            return getEmoji(0x1F37E)
        }

    val watch: String
        get() {
            return getEmoji(0x231A)
        }

    val camera: String
        get() {
            return getEmoji(0x1F4F8)
        }


    private fun getEmoji(emojiCode: Int): String {
        return String(Character.toChars(emojiCode))
    }
}
