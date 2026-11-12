package com.example.quizmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class HostDashboardActivity : AppCompatActivity() {

    private lateinit var tvRoomCode: TextView
    private lateinit var tvQuestionCount: TextView
    private lateinit var tvStats: TextView
    private lateinit var rvPlayers: RecyclerView
    private lateinit var btnNextQuestion: Button
    private lateinit var btnFinishQuiz: Button
    private lateinit var btnSeeResults: Button
    
    private lateinit var database: DatabaseReference
    private lateinit var adapter: PlayerProgressAdapter
    private var roomId: String? = null
    private var totalQuestions = 10
    private var currentQuestionIndex = 0

    private var hasRedirected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_dashboard)

        roomId = intent.getStringExtra("ROOM_ID")
        database = FirebaseDatabase.getInstance().getReference("rooms")

        tvRoomCode = findViewById(R.id.tvRoomCode)
        tvQuestionCount = findViewById(R.id.tvQuestionCount)
        tvStats = findViewById(R.id.tvStats)
        rvPlayers = findViewById(R.id.rvPlayers)
        btnNextQuestion = findViewById(R.id.btnNextQuestion)
        btnFinishQuiz = findViewById(R.id.btnFinishQuiz)
        btnSeeResults = findViewById(R.id.btnSeeResults)

        tvRoomCode.text = roomId

        setupRecyclerView()
        fetchTotalQuestions()
        listenForPlayerProgress()
        listenForRoomStatus()
        listenForCurrentQuestion()

        btnNextQuestion.setOnClickListener {
            advanceQuestion()
        }

        btnFinishQuiz.setOnClickListener {
            finishQuiz()
        }

        btnSeeResults.setOnClickListener {
            navigateToResults()
        }
    }

    private fun listenForRoomStatus() {
        roomId?.let { id ->
            database.child(id).child("status").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val statusStr = snapshot.getValue(String::class.java)
                    if (statusStr == RoomStatus.FINISHED.name) {
                        btnSeeResults.visibility = View.VISIBLE
                        if (!hasRedirected) {
                            hasRedirected = true
                            navigateToResults()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun listenForCurrentQuestion() {
        roomId?.let { id ->
            database.child(id).child("currentQuestion").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val idx = snapshot.getValue(Int::class.java) ?: 0
                    currentQuestionIndex = idx
                    tvQuestionCount.text = "Question: ${currentQuestionIndex + 1} / $totalQuestions"
                    
                    if (currentQuestionIndex >= totalQuestions - 1) {
                        btnNextQuestion.visibility = View.GONE
                    } else {
                        btnNextQuestion.visibility = View.VISIBLE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun advanceQuestion() {
        roomId?.let { id ->
            if (currentQuestionIndex < totalQuestions - 1) {
                RoomManager.nextQuestion(id, currentQuestionIndex + 1)
            }
        }
    }

    private fun navigateToResults() {
        val intentToResults = Intent(this, GroupResultActivity::class.java)
        intentToResults.putExtra("ROOM_ID", roomId)
        startActivity(intentToResults)
        finish()
    }

    private fun setupRecyclerView() {
        adapter = PlayerProgressAdapter(totalQuestions)
        rvPlayers.layoutManager = LinearLayoutManager(this)
        rvPlayers.adapter = adapter
    }

    private fun fetchTotalQuestions() {
        roomId?.let { id ->
            database.child(id).child("totalQuestions").get().addOnSuccessListener { snapshot ->
                totalQuestions = snapshot.getValue(Int::class.java) ?: 10
                tvQuestionCount.text = "Question: ${currentQuestionIndex + 1} / $totalQuestions"
                setupRecyclerView()
            }
        }
    }

    private fun listenForPlayerProgress() {
        roomId?.let { id ->
            database.child(id).child("players").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val playersList = mutableListOf<PlayerState>()
                    var completedCount = 0
                    var inProgressCount = 0
                    
                    for (playerSnapshot in snapshot.children) {
                        val player = playerSnapshot.getValue(PlayerState::class.java)
                        if (player != null) {
                            playersList.add(player)
                            if (player.status == "FINISHED") {
                                completedCount++
                            } else {
                                inProgressCount++
                            }
                        }
                    }
                    
                    tvStats.text = "Players: ${playersList.size} | Completed: $completedCount | In Progress: $inProgressCount"
                    adapter.updatePlayers(playersList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HostDashboardActivity, "Error loading players", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun finishQuiz() {
        roomId?.let { id ->
            database.child(id).child("status").setValue(RoomStatus.FINISHED.name)
                .addOnSuccessListener {
                    Toast.makeText(this, "Quiz Finished!", Toast.LENGTH_SHORT).show()
                }
        }
    }
}