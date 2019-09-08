package com.rogertalk.roger.utils.misc

import java.text.Normalizer

class NameUtils {
    companion object {
        fun comparableName(original: String): String {
            // TODO : make this faster
            if (original.length < 2) {
                return original
            }
            var result = original.trim().toLowerCase()
            result = Normalizer.normalize(result, Normalizer.Form.NFD)
            val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
            result = result.replace(regex, "")
            return result
        }
    }
}
