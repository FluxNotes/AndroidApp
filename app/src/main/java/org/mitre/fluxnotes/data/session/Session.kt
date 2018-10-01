package org.mitre.fluxnotes.data.session

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import java.text.SimpleDateFormat

import java.util.*

@Entity(tableName = "session_table")
class Session() {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: String = SimpleDateFormat("yyyMMdd_hhmmss", Locale.ENGLISH).format(Date())

    @ColumnInfo(name = "audio_file_path")
    var audioFilePath: String = ""

    @ColumnInfo(name = "sample_rate")
    var sampleRate: Int = 0

    @ColumnInfo(name = "size")
    var size: Int = 0

    @ColumnInfo(name = "byte_size")
    var byteSize: Long = 0

    @ColumnInfo(name = "transcription_text")
    var transcriptionText: String = ""

    @ColumnInfo(name = "nlp_results")
    var nlpResults: String = ""

    @Ignore
    constructor(audioFilePath: String): this() {
        this.audioFilePath = audioFilePath
    }
}