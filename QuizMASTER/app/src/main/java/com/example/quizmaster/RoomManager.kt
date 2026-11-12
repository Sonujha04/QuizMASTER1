package com.example.quizmaster

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot

object RoomManager {
    private val database = FirebaseDatabase.getInstance().getReference("rooms")
    private val auth = FirebaseAuth.getInstance()

    fun createRoom(category: String, questions: List<Question>, onResult: (String?, String?) -> Unit) {
        val roomCode = "QZ" + (1000..9999).random().toString()
        val hostId = auth.currentUser?.uid ?: ""
        
        val room = Room(
            hostId = hostId,
            status = RoomStatus.WAITING,
            category = category,
            totalQuestions = questions.size,
            questions = questions,
            players = emptyMap(),
            currentQuestion = 0
        )

        database.child(roomCode).setValue(room).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(roomCode, null)
            } else {
                onResult(null, task.exception?.message ?: "Unknown database error")
            }
        }
    }

    fun joinRoom(roomCode: String, playerName: String, onResult: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return onResult("Authentication error")
        
        database.child(roomCode).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                onResult("Room not found")
                return@addOnSuccessListener
            }
            
            val statusStr = snapshot.child("status").getValue(String::class.java)
            if (statusStr != RoomStatus.WAITING.name) {
                onResult("Quiz has already started")
                return@addOnSuccessListener
            }

            val playersSnapshot = snapshot.child("players")
            for (p in playersSnapshot.children) {
                val name = p.child("name").getValue(String::class.java)
                if (name != null && name.equals(playerName, ignoreCase = true) && p.key != userId) {
                    onResult("Player name already taken in this room")
                    return@addOnSuccessListener
                }
            }
            
            val playerState = PlayerState(
                name = playerName,
                status = "WAITING"
            )
            
            database.child(roomCode).child("players").child(userId).setValue(playerState)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(null)
                    } else {
                        onResult("Failed to join room")
                    }
                }
        }.addOnFailureListener {
            onResult("Database error: ${it.message}")
        }
    }

    fun updatePlayerProgress(roomCode: String, progress: Int, score: Int) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "progress" to progress,
            "score" to score
        )
        database.child(roomCode).child("players").child(userId).updateChildren(updates)
    }

    fun updatePlayerStatus(roomCode: String, status: String) {
        val userId = auth.currentUser?.uid ?: return
        database.child(roomCode).child("players").child(userId).child("status").setValue(status)
    }

    fun updatePlayerStateWithShuffle(roomCode: String, qOrder: List<Int>, optShuffle: Map<String, List<Int>>) {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "qOrder" to qOrder,
            "optShuffle" to optShuffle
        )
        database.child(roomCode).child("players").child(userId).updateChildren(updates)
    }

    fun updateRoomQuestions(roomCode: String, category: String, questions: List<Question>, onResult: (Boolean, String?) -> Unit) {
        val updates = mapOf(
            "category" to category,
            "totalQuestions" to questions.size,
            "questions" to questions
        )
        database.child(roomCode).updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message ?: "Failed to save questions to room")
            }
        }
    }

    fun startQuiz(roomCode: String) {
        val updates = mapOf(
            "status" to RoomStatus.STARTED.name,
            "currentQuestion" to 0
        )
        database.child(roomCode).updateChildren(updates)
    }

    fun nextQuestion(roomCode: String, newIndex: Int) {
        database.child(roomCode).child("currentQuestion").setValue(newIndex)
    }
}