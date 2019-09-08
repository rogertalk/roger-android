package com.rogertalk.roger.ui.adapters

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rogertalk.kotlinjubatus.beGone
import com.rogertalk.kotlinjubatus.makeVisible
import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.ChangeLogUIElem
import com.rogertalk.roger.models.data.VersionChanges
import com.rogertalk.roger.utils.extensions.colorResource
import kotlinx.android.synthetic.main.change_log_elem.view.*
import org.jetbrains.anko.textColor
import java.util.*

class ChangeLogAdapter(changesList: List<VersionChanges>) : RecyclerView.Adapter<BaseViewHolder>() {

    private val partitionedChangeList: LinkedList<ChangeLogUIElem>
    private val regularColor: Int
    private val headerColor: Int

    init {
        // Build the actual list
        partitionedChangeList = LinkedList()
        var isFirst = true
        for (change in changesList) {
            val title: String
            if (isFirst) {
                isFirst = false
                title = "${change.versionName} (latest)"
            } else {
                title = change.versionName
            }

            partitionedChangeList.add(ChangeLogUIElem(true, title))
            for (item in change.changes) {
                partitionedChangeList.add(ChangeLogUIElem(false, " • $item"))
            }
        }

        // Load resources
        regularColor = R.color.s_medium_grey.colorResource()
        headerColor = R.color.s_dark_grey.colorResource()
    }

    /**
     *  Whats new VH
     */
    inner class WhatsNewViewHolder(v: View) : BaseViewHolder(v)

    override fun getItemCount(): Int {
        return partitionedChangeList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder?, position: Int) {
        if (holder == null) {
            return
        }

        holder.itemView.textLabel.text = partitionedChangeList[position].text
        if (partitionedChangeList[position].isHeader) {
            holder.itemView.textLabel.textColor = headerColor
            holder.itemView.textLabel.setTypeface(null, Typeface.BOLD)
        } else {
            holder.itemView.textLabel.textColor = regularColor
            holder.itemView.textLabel.setTypeface(null, Typeface.NORMAL)
        }

        // Add padding to top and bottom items
        if (position == 0) {
            holder.itemView.topPadding.makeVisible()
        } else {
            holder.itemView.topPadding.beGone()
        }
        if (position == partitionedChangeList.size - 1) {
            holder.itemView.bottomPadding.makeVisible()
        } else {
            holder.itemView.bottomPadding.beGone()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder? {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.change_log_elem, parent, false)
        return WhatsNewViewHolder(v)
    }

}
