package com.example.quizmaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LeaderboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val rvLeaderboard = findViewById<RecyclerView>(R.id.rvLeaderboard)
        rvLeaderboard.layoutManager = LinearLayoutManager(this)

        val mockList = listOf(
            LeaderboardEntry("Ankit Kumar", 950),
            LeaderboardEntry("Priya Singh", 920),
            LeaderboardEntry("Rahul Verma", 910),
            LeaderboardEntry("Neha Sharma", 880),
            LeaderboardEntry("Sonu Kumar", 850),
            LeaderboardEntry("Aakash Raj", 820),
            LeaderboardEntry("Simran Kaur", 800),
            LeaderboardEntry("Mayank Gupta", 780)
        )

        rvLeaderboard.adapter = LeaderboardAdapter(mockList)
    }

    data class LeaderboardEntry(val name: String, val score: Int)

    class LeaderboardAdapter(private val entries: List<LeaderboardEntry>) :
        RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRank: TextView = view.findViewById(R.id.tvRank)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvScore: TextView = view.findViewById(R.id.tvScore)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            holder.tvRank.text = (position + 1).toString()
            holder.tvName.text = entry.name
            holder.tvScore.text = entry.score.toString()
        }

        override fun getItemCount() = entries.size
    }
}
