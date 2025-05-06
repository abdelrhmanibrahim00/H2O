package com.h2o.store

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.h2o.store.Graph.Graph
import com.h2o.store.Utils.ContextHolder

class H2OApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Graph
        Graph.provide(this)

        // Initialize ContextHolder
        ContextHolder.initialize(this)

        // Enable Firestore offline persistence
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
       // PaymentRepository().initializeStripe(this)

    }
}