package com.h2o.store.Utils

import android.content.Context

/**
 * Utility class to hold application context for use in repositories
 */
object ContextHolder {
    lateinit var appContext: Context
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}