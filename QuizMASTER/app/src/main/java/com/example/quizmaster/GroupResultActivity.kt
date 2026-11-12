package com.example.quizmaster

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class GroupResultActivity : AppCompatActivity() {

    private lateinit var rvGroupResults: RecyclerView
    private lateinit var btnHome: Button
    private lateinit var database: DatabaseReference
    private var roomId: String? = null
    private lateinit var adapter: GroupResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_result)

        roomId = intent.getStringExtra("ROOM_ID")
        database = FirebaseDatabase.getInstance().getReference("rooms")

        rvGroupResults = findViewById(R.id.rvGroupResults)
        btnHome = findViewById(R.id.btnHome)

        rvGroupResults.layoutManager = LinearLayoutManager(this)
        adapter = GroupResultAdapter()
        rvGroupResults.adapter = adapter

        listenForResults()

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun listenForResults() {
        roomId?.let { id ->
            database.child(id).child("players").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val playersList = mutableListOf<PlayerState>()
                    for (playerSnapshot in snapshot.children) {
                        val player = playerSnapshot.getValue(PlayerState::class.java)
                        if (player != null) {
                            playersList.add(player)
                        }
                    }
                    playersList.sortByDescending { it.score }
                    adapter.updatePlayers(playersList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@GroupResultActivity, "Error loading results", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    class GroupResultAdapter : RecyclerView.Adapter<GroupResultAdapter.ViewHolder>() {
        private var players = listOf<PlayerState>()

        fun updatePlayers(newPlayers: List<PlayerState>) {
            players = newPlayers
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val player = players[position]
            holder.bind(player, position + 1)
        }

        override fun getItemCount(): Int = players.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
            private val tvPlayerName: TextView = itemView.findViewById(R.id.tvPlayerName)
            private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            private val tvScoreText: TextView = itemView.findViewById(R.id.tvScoreText)
            private val tvProgressText: TextView = itemView.findViewById(R.id.tvProgressText)

            fun bind(player: PlayerState, rank: Int) {
                tvRank.text = rank.toString()
                tvPlayerName.text = player.name
                tvStatus.text = player.status
                tvScoreText.text = "Score: ${player.score}"
                tvProgressText.text = "Prog: ${player.progress}"

                if (player.status == "FINISHED") {
                    tvStatus.setTextColor(Color.parseColor("#00D215"))
                } else {
                    tvStatus.setTextColor(Color.parseColor("#FF8A00"))
                }
            }
        }
    }
}