package com.uika.navigator.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import com.uika.navigator.data.model.Record

/**
 * Room Database hanya digunakan untuk menyimpan riwayat pencarian (Record).
 * Data graf navigasi (TreeNodeDto) sudah dipindahkan ke Firebase Firestore.
 */
@Database(
    entities = [Record::class],
    version = 2,
    exportSchema = false
)
abstract class Database : RoomDatabase() {

    abstract val recordsDao: RecordsDao

}
