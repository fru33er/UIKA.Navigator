package com.uika.navigator.data.repository

import com.uika.navigator.data.data_source.Database
import com.uika.navigator.data.model.Record
import com.uika.navigator.domain.repository.RecordsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecordsImpl @Inject constructor(
    private val database: Database
): RecordsRepository {

    private val dao = database.recordsDao

    override suspend fun insertRecord(record: Record) {
        dao.insertRecord(record)
    }

    override fun getRecords(time: Long, limit: Int): Flow<List<Record>> {
        return dao.getRecords(time, limit)
    }

    override fun getRecords(): Flow<List<Record>> {
        return dao.getRecords()
    }
}