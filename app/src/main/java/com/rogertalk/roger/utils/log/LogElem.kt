package com.rogertalk.roger.utils.log

/**
 * Log elements
 */
class LogElem(val stackTraceElements: Array<StackTraceElement>) {

    val methodName: String
    val className: String
    val lineNumber: Int

    init {
        val pos = if (stackTraceElements.size > 1) {
            1
        } else {
            0
        }
        className = removeClassExtensionName(stackTraceElements[pos].fileName ?: "Unknown")
        methodName = stackTraceElements[pos].methodName ?: "Unknown"
        lineNumber = stackTraceElements[pos].lineNumber
    }

    private fun removeClassExtensionName(completeClassName: String): String {
        if (completeClassName.length > 3) {
            return completeClassName.substring(0, completeClassName.length - 3)
        }
        return completeClassName
    }

    /**
     * Transverse the stacktrace looking for the original caller for this event
     */
    fun getEventCallerTrace(): String {
        var isNext = false
        for (stacktrace in stackTraceElements) {
            if (isNext) {
                return "${stacktrace.fileName.removeSuffix(".kt")}.${stacktrace.methodName}"
            }

            // We know it is the element next to EventExtensions since all event calls flow trough there
            if (stacktrace.fileName == "EventExtensions.kt") {
                isNext = true
            }
        }

        return "N.A."
    }
}