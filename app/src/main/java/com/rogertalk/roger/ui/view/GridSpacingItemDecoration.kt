package com.rogertalk.roger.ui.view

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

class GridSpacingItemDecoration(private val spanCount: Int, private val spacing: Int, private val includeEdge: Boolean) :
        RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        var position = parent.getChildAdapterPosition(view) // item position

        position--

        val column = position % spanCount // item column

        if (includeEdge) {
            val extra = if (column == 0) 2 else 1
            val extra2 = if (column == 1) 2 else 1
            outRect.left = (spacing - column * spacing / spanCount) * extra // spacing - column * ((1f / spanCount) * spacing)
            outRect.right = (column + 1) * spacing / spanCount * extra2 // (column + 1) * ((1f / spanCount) * spacing)

            if (position < spanCount) { // top edge
                outRect.top = spacing
            }
            outRect.bottom = spacing // item bottom
        } else {
            outRect.left = column * spacing / spanCount // column * ((1f / spanCount) * spacing)
            outRect.right = spacing - (column + 1) * spacing / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
            if (position >= spanCount) {
                outRect.top = spacing // item top
            }
        }
    }
}