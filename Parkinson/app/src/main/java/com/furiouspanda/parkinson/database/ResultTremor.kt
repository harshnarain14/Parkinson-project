package com.harsh.parkinson.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "results")
data class ResultTremor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val maxValue: Float,
    val currentDay: String
)