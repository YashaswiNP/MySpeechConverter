package com.yashaswi.MySpeechConverter.dtos

import io.realm.RealmObject
import io.realm.annotations.Required


open class SpokenTextModel : RealmObject() {

    @Required
    var id: Int? = null

    @Required
    var spokenText: String? = null

}