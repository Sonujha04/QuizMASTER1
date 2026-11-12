package com.example.quizmaster

import java.util.Random

object ShuffleUtility {
    fun generateQuestionOrder(total: Int, seed: Long): List<Int> {
        val list = (0 until total).toList()
        return list.shuffled(Random(seed))
    }

    fun generateOptionOrder(seed: Long): List<Int> {
        val list = listOf(1, 2, 3, 4)
        return list.shuffled(Random(seed))
    }
}