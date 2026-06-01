package com.example.data

import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao) {
    val allSongs: Flow<List<Song>> = songDao.getAllSongs()

    suspend fun insert(song: Song) {
        songDao.insertSong(song)
    }

    suspend fun update(song: Song) {
        songDao.updateSong(song)
    }

    suspend fun delete(song: Song) {
        songDao.deleteSong(song)
    }

    suspend fun updateSongs(songs: List<Song>) {
        songDao.updateSongs(songs)
    }
}
