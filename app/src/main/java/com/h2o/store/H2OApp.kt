package com.h2o.store

import android.app.Application
import com.h2o.store.Graph.Graph

class H2OApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}