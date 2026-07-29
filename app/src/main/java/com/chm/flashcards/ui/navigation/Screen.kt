package com.chm.flashcards.ui.navigation

/**
 * One object per screen in PRD §5. Navigation Compose route arguments are
 * always [String] (Navigation has no `Uuid` `NavType`) -- any screen taking
 * an id arg (`setId`, `cardId`, `sessionId`) parses it with `Uuid.parse(arg)`
 * immediately at the screen/ViewModel entry point, so `Uuid` is the type
 * used everywhere past that boundary. Screens with no id arg yet (this
 * feature only wires placeholders) expose a plain [route]; screens that
 * will need one expose the arg key + a [createRoute] helper so later
 * features have a single, consistent place to build/parse navigation args.
 */
sealed class Screen(val route: String) {

    data object SetList : Screen("set_list")

    data object SetDetail : Screen("set_detail/{$ARG_SET_ID}") {
        fun createRoute(setId: String) = "set_detail/$setId"
    }

    data object CardEditor : Screen("card_editor/{$ARG_SET_ID}?$ARG_CARD_ID={$ARG_CARD_ID}") {
        fun createRoute(setId: String, cardId: String? = null) =
            "card_editor/$setId?$ARG_CARD_ID=${cardId.orEmpty()}"
    }

    data object SessionConfig : Screen("session_config/{$ARG_SET_ID}") {
        fun createRoute(setId: String) = "session_config/$setId"
    }

    data object SessionPlay : Screen("session_play/{$ARG_SET_ID}") {
        fun createRoute(setId: String) = "session_play/$setId"
    }

    data object SessionResults : Screen("session_results/{$ARG_SESSION_ID}") {
        fun createRoute(sessionId: String) = "session_results/$sessionId"
    }

    data object HistoryList : Screen("history_list")

    data object HistoryDetail : Screen("history_detail/{$ARG_SESSION_ID}") {
        fun createRoute(sessionId: String) = "history_detail/$sessionId"
    }

    data object ImportExport : Screen("import_export")

    companion object {
        const val ARG_SET_ID = "setId"
        const val ARG_CARD_ID = "cardId"
        const val ARG_SESSION_ID = "sessionId"
    }
}
