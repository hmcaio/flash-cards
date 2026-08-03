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
import com.chm.flashcards.ui.cardeditor.CardEditorScreen
import com.chm.flashcards.ui.sessionconfig.SessionConfigScreen
import com.chm.flashcards.ui.sessionplay.SessionPlayScreen
import com.chm.flashcards.ui.sessionresults.SessionResultsScreen
import com.chm.flashcards.ui.setdetail.SetDetailScreen
import com.chm.flashcards.ui.setlist.SetListScreen
import kotlin.uuid.Uuid

/**
 * App-wide nav graph. Every destination is a placeholder until its owning
 * feature replaces it with real screen content -- F03 replaces `SetDetail`
 * and `CardEditor`; the rest stay placeholders until F05+.
 */
@Composable
fun FlashCardsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Screen.SetList.route, modifier = modifier) {
        composable(Screen.SetList.route) {
            SetListScreen(
                onSetClick = { setId: Uuid ->
                    navController.navigate(Screen.SetDetail.createRoute(setId.toString()))
                },
            )
        }
        composable(
            route = Screen.SetDetail.route,
            arguments = listOf(navArgument(Screen.ARG_SET_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val setId = backStackEntry.arguments?.getString(Screen.ARG_SET_ID).orEmpty()
            SetDetailScreen(
                onCardClick = { cardId: Uuid? ->
                    navController.navigate(Screen.CardEditor.createRoute(setId, cardId?.toString()))
                },
                onStartPracticeClick = {
                    navController.navigate(Screen.SessionConfig.createRoute(setId))
                },
            )
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
            CardEditorScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.SessionConfig.route,
            arguments = listOf(navArgument(Screen.ARG_SET_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val setId = backStackEntry.arguments?.getString(Screen.ARG_SET_ID).orEmpty()
            SessionConfigScreen(
                onStartSession = { navController.navigate(Screen.SessionPlay.createRoute(setId)) },
            )
        }
        composable(
            route = Screen.SessionPlay.route,
            arguments = listOf(navArgument(Screen.ARG_SET_ID) { type = NavType.StringType }),
        ) {
            SessionPlayScreen(
                onSessionComplete = { sessionId ->
                    navController.navigate(Screen.SessionResults.createRoute(sessionId.toString()))
                },
            )
        }
        composable(
            route = Screen.SessionResults.route,
            arguments = listOf(navArgument(Screen.ARG_SESSION_ID) { type = NavType.StringType }),
        ) {
            SessionResultsScreen()
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
