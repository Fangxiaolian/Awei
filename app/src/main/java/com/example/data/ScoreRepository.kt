package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ScoreRepository(private val scoreDao: ScoreDao) {
    val topScores: Flow<List<GameScore>> = scoreDao.getTopScores()

    suspend fun insertScore(playerName: String, score: Int, skinName: String) {
        val name = playerName.trim().ifEmpty { "神秘飞天侠" }
        val gameScore = GameScore(
            playerName = name,
            score = score,
            skinName = skinName
        )
        scoreDao.insertScore(gameScore)
    }

    suspend fun clearLeaderboard() {
        scoreDao.clearAllScores()
    }

    suspend fun populateDefaultsIfEmpty() {
        try {
            // Get current first emission
            val scores = scoreDao.getTopScores().first()
            if (scores.isEmpty()) {
                scoreDao.insertScore(GameScore(playerName = "终极无敌阿威", score = 99, skinName = "金光闪闪", timestamp = System.currentTimeMillis() - 86400000))
                scoreDao.insertScore(GameScore(playerName = "暴走阿威", score = 58, skinName = "蓝光喷射", timestamp = System.currentTimeMillis() - 43200000))
                scoreDao.insertScore(GameScore(playerName = "越过沧海", score = 25, skinName = "披风游侠", timestamp = System.currentTimeMillis() - 10000000))
                scoreDao.insertScore(GameScore(playerName = "见习飞天小威", score = 10, skinName = "普通阿威", timestamp = System.currentTimeMillis() - 500000))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
