package com.chm.flashcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chm.flashcards.ui.cardeditor.CardEditorScreen
import com.chm.flashcards.ui.history.HistoryDetailScreen
import com.chm.flashcards.ui.history.HistoryListScreen
import com.chm.flashcards.ui.importexport.ImportExportScreen
import com.chm.flashcards.ui.sessionconfig.SessionConfigScreen
import com.chm.flashcards.ui.sessionplay.SessionPlayScreen
import com.chm.flashcards.ui.sessionresults.SessionResultsScreen
import com.chm.flashcards.ui.setdetail.SetDetailScreen
import com.chm.flashcards.ui.setlist.SetListScreen
import kotlin.uuid.Uuid

/**
 * App-wide nav graph. As of F07 every destination has real screen content --
 * F03 replaced `SetDetail`/`CardEditor`, F05/F06 replaced the session and
 * history screens, and F07 replaces `ImportExport`, the last placeholder.
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
                onImportExportClick = {
                    navController.navigate(Screen.ImportExport.route)
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
                onHistoryClick = {
                    navController.navigate(Screen.HistoryList.createRoute(setId))
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
                onNavigateBack = { navController.popBackStack() },
                onSessionComplete = { sessionId ->
                    navController.navigate(Screen.SessionResults.createRoute(sessionId.toString())) {
                        // Collapse the SessionConfig/SessionPlay sub-stack so a single pop
                        // (system back or the Results screen's own back button) lands on
                        // SetDetail, not back on the session the user just finished.
                        popUpTo(Screen.SetDetail.route) { inclusive = false }
                    }
                },
            )
        }
        composable(
            route = Screen.SessionResults.route,
            arguments = listOf(navArgument(Screen.ARG_SESSION_ID) { type = NavType.StringType }),
        ) {
            SessionResultsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.HistoryList.route,
            arguments = listOf(navArgument(Screen.ARG_SET_ID) { type = NavType.StringType }),
        ) {
            HistoryListScreen(
                onSessionClick = { sessionId: Uuid ->
                    navController.navigate(Screen.HistoryDetail.createRoute(sessionId.toString()))
                },
            )
        }
        composable(
            route = Screen.HistoryDetail.route,
            arguments = listOf(navArgument(Screen.ARG_SESSION_ID) { type = NavType.StringType }),
        ) {
            HistoryDetailScreen()
        }
        composable(Screen.ImportExport.route) {
            ImportExportScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
