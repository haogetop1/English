package com.xunji.core.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupCodec {
    private val json=Json { encodeDefaults=true; prettyPrint=true; ignoreUnknownKeys=false; isLenient=false; allowSpecialFloatingPointValues=false }
    fun encode(document:BackupDocument):String { BackupValidator.validate(document);return json.encodeToString(document) }
    fun decode(text:String):BackupDocument=json.decodeFromString<BackupDocument>(text).also(BackupValidator::validate)
}
