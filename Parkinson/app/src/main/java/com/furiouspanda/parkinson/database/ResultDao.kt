package com.harsh.parkinson.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ResultDao {
    @Query("SELECT * FROM results")
    suspend fun getAllResults(): List<ResultTremor>

    @Query("SELECT * FROM results ORDER BY id DESC LIMIT 1")
    suspend fun getLatestResult(): ResultTremor?

    @Insert
    suspend fun insert(result: ResultTremor)

    @Query("DELETE FROM results")
    suspend fun deleteAllResults()
}