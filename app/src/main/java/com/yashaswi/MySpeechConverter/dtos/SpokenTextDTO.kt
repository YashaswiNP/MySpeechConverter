package com.yashaswi.MySpeechConverter.dtos

import io.realm.RealmObject
import io.realm.annotations.Required


open class SpokenTextDTO : RealmObject() {

    @Required
    var id: Int? = null

    @Required
    var spokenText: String? = null

}