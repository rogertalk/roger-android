package com.rogertalk.roger.ui.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rogertalk.roger.R
import com.rogertalk.roger.models.data.AvatarSize.BIG
import com.rogertalk.roger.models.json.Bot
import com.rogertalk.roger.ui.screens.BotPickActivity
import com.rogertalk.roger.utils.image.RoundImageUtils
import kotlinx.android.synthetic.main.bot_connect_elem.view.*

class BotPickAdapter(val botsList: List<Bot>,
                     val botPickActivity: BotPickActivity) : RecyclerView.Adapter<BaseViewHolder>() {

    companion object {
        const val TYPE_CONTENT = 1
    }

    inner class BotAdapterVH(v: View) : BaseViewHolder(v), View.OnClickListener {
        override fun onClick(view: View?) {
            val pos = adapterPosition
            val bot = botsList[pos]

            // Notify Activity
            botPickActivity.botSelected(bot)
        }

        init {
            v.findViewById(R.id.botBox).setOnClickListener(this)
        }
    }

    //
    // OVERRIDE METHODS
    //

    override fun getItemCount(): Int {
        return botsList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder?, position: Int) {
        if (holder == null) {
            return
        }

        when (holder) {
            is BotAdapterVH -> renderBot(holder, position)
            else -> return
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder? {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.bot_connect_elem, parent, false)
        return BotAdapterVH(v)
    }

    override fun getItemViewType(position: Int): Int {
        return TYPE_CONTENT
    }

    //
    // PUBLIC METHODS
    //


    //
    // PRIVATE METHODS
    //

    private fun renderBot(holder: BaseViewHolder, position: Int) {
        val bot = botsList[position]

        // Name and description
        holder.itemView.botName.text = bot.title
        holder.itemView.botDescription.text = bot.description

        // Avatar
        val photoURI = bot.imageURL
        if (photoURI != null) {
            RoundImageUtils.createRoundImage(botPickActivity, holder.itemView.botAvatar, photoURI, BIG)
        } else {
            holder.itemView.botAvatar.setImageResource(R.drawable.pee)
        }

        // Accessibility support
        holder.itemView.botBox.contentDescription = "${bot.title}, ${bot.description}"
    }
}
