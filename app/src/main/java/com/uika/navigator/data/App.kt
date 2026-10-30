package com.uika.navigator.data

import android.app.Application
import com.uika.navigator.BuildConfig
import com.uika.navigator.data.migration.GraphDataMigrationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var migrationHelper: GraphDataMigrationHelper

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Migrasi data node dari SQLite asset ke Firestore (hanya berjalan sekali)
        appScope.launch {
            migrationHelper.migrateIfNeeded()
        }
    }

    companion object {
        const val ADMIN_MODE = "admin"
        const val USER_MODE = "user"
        const val mode = BuildConfig.FLAVOR
        const val isAdmin = mode == ADMIN_MODE
        const val isUser = mode == USER_MODE
    }
}
