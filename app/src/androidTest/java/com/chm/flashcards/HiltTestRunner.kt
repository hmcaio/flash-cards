package com.chm.flashcards

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom instrumentation runner so `@HiltAndroidTest` tests (e.g.
 * [MainActivityTest]) boot a [HiltTestApplication] instead of the real
 * [FlashCardsApplication].
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
