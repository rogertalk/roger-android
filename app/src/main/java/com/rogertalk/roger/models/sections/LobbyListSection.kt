package com.rogertalk.roger.models.sections

import com.rogertalk.roger.models.sections.LobbySectionType.TYPE_SECTION


abstract class LobbyListSection(val sectionName: String,
                                val elementsType: Int) {

    companion object {
        private val SECTION_MAX_SIZE = 100000L
    }

    //
    // PUBLIC METHODS
    //

    open fun getSectionSize(): Int {
        if (shouldRender() && shouldRenderSectionHeader()) {
            return 1
        }
        return 0
    }

    open fun shouldRenderSectionHeader(): Boolean {
        return true
    }

    open fun filterElements(comparableName: String) {
    }

    open fun getElementForPosition(givenPosition: Int): Any {
        return Any()
    }

    /**
     * Since this is a sectioned list, the way to make sure elements get a unique ID is by
     * going trough each section iteratively and keeping track of each sections's position
     */
    fun getUniqueElementId(realPosition: Int, sectionPosition: Int): Long {
        return sectionPosition * SECTION_MAX_SIZE + realPosition
    }

    fun getItemType(realPosition: Int): Int {
        // Section header
        if (realPosition == 0 && shouldRenderSectionHeader()) {
            return TYPE_SECTION.ordinal
        }

        // Element
        return elementsType
    }

    open fun shouldRender(): Boolean {
        return true
    }

    //
    // PRIVATE METHODS
    //
}