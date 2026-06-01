package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val bpm: Int,
    val position: Int = 0
)
