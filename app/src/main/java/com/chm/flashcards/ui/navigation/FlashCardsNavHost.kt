package com.chm.flashcards.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * App-wide nav graph. Every destination is a placeholder until its owning
 * feature (F02+) replaces it with real screen content -- this feature only
 * establishes the route scaffold and Hilt/Nav wiring.
 */
@Composable
fun FlashCardsNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = Screen.SetList.route, modifier = modifier) {
        composable(Screen.SetList.route) {
            Text("TODO: SetList")
        }
        composable(
            route = Screen.SetDetail.route,
            arguments = listOf(navArgument(Screen.ARG_SET_ID) { type = NavType.StringType }),
        ) {
            Text("TODO: SetDetail")
        }
        composable(
            route = Screen.CardEditor.route,
            arguments = listOf(
                navArgument(Screen.ARG_SET_ID) { type = NavType.StringType },
                navArgument(Screen.ARG_CARD_ID) {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) {
            Text("TODO: CardEditor")
        }
        composable(
            route = Screen.SessionConfig.route,
            arguments = listOf(navArgument(Screen.ARG_SET_ID) { type = NavType.StringType }),
        ) {
            Text("TODO: SessionConfig")
        }
        composable(
            route = Screen.SessionPlay.route,
            arguments = listOf(navArgument(Screen.ARG_SET_ID) { type = NavType.StringType }),
        ) {
            Text("TODO: SessionPlay")
        }
        composable(
            route = Screen.SessionResults.route,
            arguments = listOf(navArgument(Screen.ARG_SESSION_ID) { type = NavType.StringType }),
        ) {
            Text("TODO: SessionResults")
        }
        composable(Screen.HistoryList.route) {
            Text("TODO: HistoryList")
        }
        composable(
            route = Screen.HistoryDetail.route,
            arguments = listOf(navArgument(Screen.ARG_SESSION_ID) { type = NavType.StringType }),
        ) {
            Text("TODO: HistoryDetail")
        }
        composable(Screen.ImportExport.route) {
            Text("TODO: ImportExport")
        }
    }
}
