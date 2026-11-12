package com.example.quizmaster

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlayerProgressAdapter(private val totalQuestions: Int) :
    RecyclerView.Adapter<PlayerProgressAdapter.ViewHolder>() {

    private var players = listOf<PlayerState>()

    fun updatePlayers(newPlayers: List<PlayerState>) {
        players = newPlayers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_progress, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = players[position]
        holder.tvName.text = player.name
        holder.tvProgress.text = "${player.progress}/$totalQuestions"
        holder.progressBar.max = totalQuestions
        holder.progressBar.progress = player.progress
    }

    override fun getItemCount() = players.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPlayerName)
        val tvProgress: TextView = view.findViewById(R.id.tvProgressText)
        val progressBar: ProgressBar = view.findViewById(R.id.playerProgressBar)
    }
}
