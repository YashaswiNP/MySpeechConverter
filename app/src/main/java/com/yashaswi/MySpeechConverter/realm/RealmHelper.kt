package com.yashaswi.MySpeechConverter.realm

import android.util.Log
import com.yashaswi.MySpeechConverter.dtos.SpokenTextDTO
import io.realm.Realm
import io.realm.RealmResults
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import java.util.*


class RealmHelper(private var realm: Realm) {

    //WRITE
    fun save(spokenText: SpokenTextDTO) {
        try {
            realm.executeTransaction { realm.copyToRealm(spokenText) }
        } catch (e: RealmPrimaryKeyConstraintException) {
            Log.e(this.javaClass.simpleName, "PrimaryKeyException" + e.stackTrace)
        }
    }

    //READ
    fun retrieve(): ArrayList<String> {
        val spokenTexts =
            ArrayList<String>()
        val spacecrafts: RealmResults<SpokenTextDTO> =
            realm.where(
                SpokenTextDTO::class.java
            ).findAll()
        for (s in spacecrafts) {
            spokenTexts.add(s.spokenText.toString())
        }
        return spokenTexts
    }

    //WRITE
    fun delete(spokenText: SpokenTextDTO) {
        try {
            realm.executeTransaction {
                val realmResult: RealmResults<SpokenTextDTO> = realm.where(
                    SpokenTextDTO::class.java
                )
                    .equalTo("skills.skillName", spokenText.id).findAll()
                realmResult.deleteAllFromRealm()

            }
        } catch (e: RealmPrimaryKeyConstraintException) {
            Log.e(this.javaClass.simpleName, "PrimaryKeyException" + e.stackTrace)
        }
    }

}
