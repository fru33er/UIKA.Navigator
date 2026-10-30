package com.uika.navigator.data.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.uika.navigator.data.data_source.FirestoreGraphDataSource
import com.uika.navigator.data.model.TreeNodeDto
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.romainguy.kotlin.math.Quaternion
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper untuk migrasi data node navigasi dari SQLite asset (UIKA_DB.db)
 * ke Firebase Firestore.
 *
 * Migrasi hanya berjalan sekali — ditandai dengan SharedPreferences flag.
 * Setelah berhasil, data tersedia di Firestore collection "nodes".
 */
@Singleton
class GraphDataMigrationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: FirestoreGraphDataSource
) {

    companion object {
        private const val TAG = "GraphMigration"
        private const val ASSET_DB_PATH = "database/UIKA_DB.db"
        private const val TEMP_DB_NAME = "migration_temp.db"
        private const val PREFS_NAME = "migration_prefs"
        private const val KEY_NODES_MIGRATED = "nodes_migrated_to_firestore"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Jalankan migrasi jika belum pernah dilakukan.
     * Aman dipanggil berkali-kali — hanya akan berjalan pada pemanggilan pertama.
     */
    suspend fun migrateIfNeeded() {
        if (prefs.getBoolean(KEY_NODES_MIGRATED, false)) {
            Log.d(TAG, "Migration already done, skipping.")
            return
        }

        Log.d(TAG, "Starting migration from SQLite asset to Firestore...")

        try {
            val nodes = readNodesFromAsset()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No nodes found in asset database. Migration skipped.")
                markMigrationDone()
                return
            }

            Log.d(TAG, "Read ${nodes.size} nodes from asset, uploading to Firestore...")
            uploadNodesToFirestore(nodes)

            markMigrationDone()
            Log.d(TAG, "Migration completed successfully! ${nodes.size} nodes uploaded.")
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed: ${e.message}", e)
            // Jangan mark done agar bisa retry pada launch berikutnya
        }
    }

    /**
     * Baca semua node dari file SQLite di assets.
     * Salin ke internal storage sementara, buka dengan SQLiteDatabase, lalu hapus file temp.
     */
    private fun readNodesFromAsset(): List<TreeNodeDto> {
        val tempFile = copyAssetToTempFile()

        return try {
            val db = SQLiteDatabase.openDatabase(
                tempFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            val nodes = readNodesFromDb(db)
            db.close()
            nodes
        } finally {
            tempFile.delete()
            Log.d(TAG, "Temp SQLite file deleted.")
        }
    }

    /**
     * Salin file asset database ke direktori internal agar bisa dibuka SQLiteDatabase.
     */
    private fun copyAssetToTempFile(): File {
        val tempFile = File(context.cacheDir, TEMP_DB_NAME)
        context.assets.open(ASSET_DB_PATH).use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        Log.d(TAG, "Asset DB copied to: ${tempFile.absolutePath}")
        return tempFile
    }

    /**
     * Baca semua baris dari tabel TreeNodeDto dalam SQLiteDatabase.
     */
    private fun readNodesFromDb(db: SQLiteDatabase): List<TreeNodeDto> {
        val nodes = mutableListOf<TreeNodeDto>()

        val cursor = db.rawQuery("SELECT * FROM TreeNodeDto", null)
        cursor.use {
            while (it.moveToNext()) {
                try {
                    val id = it.getInt(it.getColumnIndexOrThrow("id"))
                    val x = it.getFloat(it.getColumnIndexOrThrow("x"))
                    val y = it.getFloat(it.getColumnIndexOrThrow("y"))
                    val z = it.getFloat(it.getColumnIndexOrThrow("z"))
                    val type = it.getString(it.getColumnIndexOrThrow("type"))
                        ?: TreeNodeDto.TYPE_PATH
                    val number = it.getString(it.getColumnIndexOrThrow("number"))
                    val neighboursStr = it.getString(it.getColumnIndexOrThrow("neighbours"))
                    val forwardVectorStr = it.getString(it.getColumnIndexOrThrow("forwardVector"))

                    val neighbours = parseNeighbours(neighboursStr)
                    val forwardVector = parseQuaternion(forwardVectorStr)

                    nodes.add(
                        TreeNodeDto(
                            id = id,
                            x = x,
                            y = y,
                            z = z,
                            type = type,
                            number = number,
                            neighbours = neighbours,
                            forwardVector = forwardVector
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading row: ${e.message}", e)
                }
            }
        }

        Log.d(TAG, "Read ${nodes.size} nodes from SQLite")
        return nodes
    }

    /**
     * Upload semua node ke Firestore dalam batch (maks 500 per batch).
     */
    private suspend fun uploadNodesToFirestore(nodes: List<TreeNodeDto>) {
        nodes.chunked(500).forEachIndexed { batchIndex, chunk ->
            val batch = dataSource.firestore.batch()
            chunk.forEach { dto ->
                val docRef = dataSource.nodesCollection.document(dto.id.toString())
                batch.set(docRef, dto.toMap())
            }
            batch.commit().await()
            Log.d(TAG, "Batch $batchIndex uploaded (${chunk.size} nodes)")
        }
    }

    /**
     * Parse neighbours dari format string yang disimpan TypeConverter Room.
     * Format: "1,2,3" atau "" (kosong)
     */
    private fun parseNeighbours(value: String?): MutableList<Int> {
        if (value.isNullOrEmpty()) return mutableListOf()
        return value.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toMutableList()
    }

    /**
     * Parse Quaternion dari format string yang disimpan GraphTypeConverters.
     * Format: "x,y,z,w" atau null/kosong jika tidak ada.
     */
    private fun parseQuaternion(value: String?): Quaternion? {
        if (value.isNullOrEmpty()) return null
        return try {
            val parts = value.split(",").map { it.trim().toFloat() }
            if (parts.size == 4) Quaternion(parts[0], parts[1], parts[2], parts[3]) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing quaternion '$value': ${e.message}")
            null
        }
    }

    private fun markMigrationDone() {
        prefs.edit().putBoolean(KEY_NODES_MIGRATED, true).apply()
    }
}
