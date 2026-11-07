package com.espe.arlenguadesenas

import android.app.Application
import com.espe.arlenguadesenas.data.local.AppDatabase

class ArLsecApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ArLsecApplication
            private set
    }
}
