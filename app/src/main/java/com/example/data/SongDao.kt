package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY position ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Transaction
    suspend fun updateSongs(songs: List<Song>) {
        songs.forEach { updateSong(it) }
    }
}
