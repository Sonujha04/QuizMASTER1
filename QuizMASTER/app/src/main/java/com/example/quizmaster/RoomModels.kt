package com.example.quizmaster

import java.io.Serializable

enum class RoomStatus {
    WAITING, STARTED, FINISHED
}

data class PlayerState(
    val name: String = "",
    val progress: Int = 0,
    val score: Int = 0,
    val status: String = "WAITING",
    val qOrder: List<Int> = emptyList(),
    val optShuffle: Map<String, List<Int>> = emptyMap()
) : Serializable

data class Room(
    val hostId: String = "",
    val status: RoomStatus = RoomStatus.WAITING,
    val category: String = "",
    val totalQuestions: Int = 0,
    val questions: List<Question> = emptyList(),
    val players: Map<String, PlayerState> = emptyMap(),
    val currentQuestion: Int = 0
) : Serializable