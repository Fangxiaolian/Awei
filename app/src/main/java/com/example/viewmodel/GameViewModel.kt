package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameScore
import com.example.data.ScoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiScreen {
    object Menu : UiScreen
    object Game : UiScreen
    object Leaderboard : UiScreen
    object SkinShop : UiScreen
}

data class Skin(
    val id: String,
    val name: String,
    val description: String,
    val unlockScore: Int,
    val hexColor: Long,
    val secondaryColor: Long
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScoreRepository
    
    // UI Screen state navigation
    private val _currentScreen = MutableStateFlow<UiScreen>(UiScreen.Menu)
    val currentScreen: StateFlow<UiScreen> = _currentScreen.asStateFlow()

    // Player name
    private val _playerName = MutableStateFlow("热心玩家阿威")
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    // Selected Skin ID
    private val _selectedSkinId = MutableStateFlow("classic")
    val selectedSkinId: StateFlow<String> = _selectedSkinId.asStateFlow()

    // Available skins definition
    val skins = listOf(
        Skin("classic", "普通阿威", "经典的红头带、大眼镜、憨态可掬的阿威", 0, 0xFFFF5722, 0xFFFFEB3B),
        Skin("hero", "披风游侠", "身披红色超级英雄飞天斗篷，充满正义感", 10, 0xFF2196F3, 0xFFF44336),
        Skin("neon", "蓝光喷射", "赛博朋克极速风，尾部自带蓝色能量喷火尾焰", 25, 0xFF00E5FF, 0xFFD500F9),
        Skin("golden", "金光闪闪", "至尊无敌纯金阿威，环绕闪耀星光，至高荣耀", 40, 0xFFFFD700, 0xFFFFA000)
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ScoreRepository(database.scoreDao)
        
        // Populates defaults if database is empty on start
        viewModelScope.launch {
            repository.populateDefaultsIfEmpty()
        }
    }

    // Leaderboard flow (Top 10 scores)
    val leaderboardScores: StateFlow<List<GameScore>> = repository.topScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun navigateTo(screen: UiScreen) {
        _currentScreen.value = screen
    }

    fun updatePlayerName(newName: String) {
        _playerName.value = newName.take(12) // Limit length of players' names nicely
    }

    fun selectSkin(skinId: String) {
        val targetSkin = skins.find { it.id == skinId } ?: return
        val currentHighScore = getPersonalHighScore()
        if (currentHighScore >= targetSkin.unlockScore) {
            _selectedSkinId.value = skinId
        }
    }

    fun getPersonalHighScore(): Int {
        val scores = leaderboardScores.value
        val personalScores = scores.filter { it.playerName == _playerName.value }
        if (personalScores.isNotEmpty()) {
            return personalScores.maxOf { it.score }
        }
        // If not in top 10 list specifically under that name, search entire top list for best overall
        return scores.firstOrNull()?.score ?: 0
    }

    fun submitScore(score: Int) {
        viewModelScope.launch {
            val equippedSkin = skins.find { it.id == _selectedSkinId.value }?.name ?: "普通阿威"
            repository.insertScore(
                playerName = _playerName.value,
                score = score,
                skinName = equippedSkin
            )
        }
    }

    fun clearLeadeboard() {
        viewModelScope.launch {
            repository.clearLeaderboard()
        }
    }
}
