package de.csicar.ning

import android.app.Application

class NingApplication : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.createInstance(this)
    }
}
