package com.example.quizmaster

import java.io.Serializable

data class Question(
    val id: String = "",
    val question: String = "",
    val option1: String = "",
    val option2: String = "",
    val option3: String = "",
    val option4: String = "",
    val correctAnswer: Int = 1
) : Serializable