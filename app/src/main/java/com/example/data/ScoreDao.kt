package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC, timestamp DESC LIMIT 10")
    fun getTopScores(): Flow<List<GameScore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: GameScore)

    @Query("DELETE FROM leaderboard")
    suspend fun clearAllScores()
}
