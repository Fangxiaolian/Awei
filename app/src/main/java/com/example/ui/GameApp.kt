package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.UiScreen

@Composable
fun GameApp(viewModel: GameViewModel, currentScreen: UiScreen) {
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        modifier = Modifier.fillMaxSize(),
        label = "screen_routing"
    ) { screen ->
        when (screen) {
            is UiScreen.Menu -> {
                MenuScreen(
                    viewModel = viewModel,
                    onStartGame = { viewModel.navigateTo(UiScreen.Game) },
                    onViewLeaderboard = { viewModel.navigateTo(UiScreen.Leaderboard) },
                    onGoToSkins = { viewModel.navigateTo(UiScreen.SkinShop) }
                )
            }
            is UiScreen.Game -> {
                GamePlayScreen(
                    viewModel = viewModel,
                    onBackToMenu = { viewModel.navigateTo(UiScreen.Menu) },
                    onViewLeaderboard = { viewModel.navigateTo(UiScreen.Leaderboard) }
                )
            }
            is UiScreen.Leaderboard -> {
                LeaderboardScreen(
                    viewModel = viewModel,
                    onBackToMenu = { viewModel.navigateTo(UiScreen.Menu) }
                )
            }
            is UiScreen.SkinShop -> {
                SkinShopScreen(
                    viewModel = viewModel,
                    onBackToMenu = { viewModel.navigateTo(UiScreen.Menu) },
                    onStartGame = { viewModel.navigateTo(UiScreen.Game) }
                )
            }
        }
    }
}
