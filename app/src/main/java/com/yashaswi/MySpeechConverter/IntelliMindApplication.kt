package com.yashaswi.MySpeechConverter

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration


open class IntelliMindApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        configureRealmDb()

    }

    private fun configureRealmDb() {
        val realmConfig = RealmConfiguration.Builder()
            .name("intellimind.realm")
            .schemaVersion(1)
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(realmConfig)
    }
}