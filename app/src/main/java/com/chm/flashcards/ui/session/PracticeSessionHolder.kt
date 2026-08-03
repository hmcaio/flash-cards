package com.chm.flashcards.ui.session

import com.chm.flashcards.data.repository.PracticeSessionDraft
import com.chm.flashcards.data.repository.PracticeSessionSummary
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

/**
 * Passes the in-flight [PracticeSessionDraft] from Session Config (which
 * creates it via `PracticeRepository.startSession`) to Session Play (which
 * consumes it), and the finished [PracticeSessionSummary] from Session Play
 * to Session Results, without a Room round-trip in between.
 *
 * Nav Compose route args are plain `String`s (per [com.chm.flashcards.ui.navigation.Screen]'s
 * convention) so a `Card` list/summary can't ride along in the route itself.
 * spec.md suggests "a shared session-scoped ViewModel" for this -- injecting
 * one `@HiltViewModel` into another isn't a supported Hilt wiring (each
 * lives in its own `ViewModelComponent`), so this is the equivalent: a plain
 * `@ActivityRetainedScoped` holder, injectable into any of the three
 * per-screen ViewModels, alive only for the current `MainActivity` instance
 * -- not an Application-wide singleton, and cleared immediately after each
 * value is read so it can't leak a stale draft/summary into an unrelated
 * later session.
 */
@ActivityRetainedScoped
class PracticeSessionHolder @Inject constructor() {

    private var draft: PracticeSessionDraft? = null
    private var summary: PracticeSessionSummary? = null

    fun setDraft(draft: PracticeSessionDraft) {
        this.draft = draft
    }

    fun consumeDraft(): PracticeSessionDraft? {
        val current = draft
        draft = null
        return current
    }

    fun setSummary(summary: PracticeSessionSummary) {
        this.summary = summary
    }

    fun consumeSummary(): PracticeSessionSummary? {
        val current = summary
        summary = null
        return current
    }
}
