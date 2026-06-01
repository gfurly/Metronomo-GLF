package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Song
import com.example.data.SongRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MetronomeViewModel(private val repository: SongRepository) : ViewModel() {

    // --- Database state ---
    val songsList: StateFlow<List<Song>> = repository.allSongs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSongId = MutableStateFlow<Int?>(null)
    val selectedSongIdState: StateFlow<Int?> = _selectedSongId.asStateFlow()

    // --- Metronome configuration state ---
    private val _bpm = MutableStateFlow(120)
    val bpmState: StateFlow<Int> = _bpm.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _timeSignature = MutableStateFlow("4/4") // "Sin acento", "2/4", "3/4", "4/4"
    val timeSignatureState: StateFlow<String> = _timeSignature.asStateFlow()

    private val _currentBeatIndex = MutableStateFlow(0) // 1-based (e.g. 1, 2, 3, 4), or 0 if "Sin acento"
    val currentBeatIndexState: StateFlow<Int> = _currentBeatIndex.asStateFlow()

    private val _flashActive = MutableStateFlow(false)
    val flashActiveState: StateFlow<Boolean> = _flashActive.asStateFlow()

    // --- Audio components ---
    private var clickTrack: AudioTrack? = null
    private var accentTrack: AudioTrack? = null
    private var metronomeJob: Job? = null

    init {
        initAudio()
    }

    private fun initAudio() {
        viewModelScope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val durationMs = 30
            val numSamples = (sampleRate * durationMs) / 1000

            // Normal woodblock-like tick: 800Hz decaying sine wave
            val normalSnd = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val fraction = i.toDouble() / numSamples
                val envelope = Math.exp(-8.0 * fraction) // exponential decay
                val angle = 2.0 * Math.PI * i * 800.0 / sampleRate
                normalSnd[i] = (Math.sin(angle) * envelope * 32767).toInt().toShort()
            }

            // High pitch accented woodblock: 1300Hz decaying sine wave
            val accentSnd = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val fraction = i.toDouble() / numSamples
                val envelope = Math.exp(-8.0 * fraction)
                val angle = 2.0 * Math.PI * i * 1300.0 / sampleRate
                accentSnd[i] = (Math.sin(angle) * envelope * 32767).toInt().toShort()
            }

            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                clickTrack = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(normalSnd.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build().apply {
                        write(normalSnd, 0, normalSnd.size)
                    }

                accentTrack = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(accentSnd.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build().apply {
                        write(accentSnd, 0, accentSnd.size)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playClickSound(isAccent: Boolean) {
        val track = if (isAccent) accentTrack else clickTrack
        track?.let {
            try {
                it.stop()
                it.reloadStaticData()
                it.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Playback operations ---
    fun togglePlayback() {
        if (_isPlaying.value) {
            stopMetronome()
        } else {
            startMetronome()
        }
    }

    private fun startMetronome() {
        _isPlaying.value = true
        metronomeJob?.cancel()
        metronomeJob = viewModelScope.launch(Dispatchers.Default) {
            var nextBeatTime = System.currentTimeMillis()
            var beatCounter = 0

            while (isActive && _isPlaying.value) {
                val bpm = _bpm.value
                val sig = when (_timeSignature.value) {
                    "2/4" -> 2
                    "3/4" -> 3
                    "4/4" -> 4
                    else -> 0
                }

                val isAccent = sig > 0 && beatCounter == 0

                // Emit state to the UI
                if (sig > 0) {
                    _currentBeatIndex.value = beatCounter + 1
                } else {
                    _currentBeatIndex.value = 0
                }

                // Play procedural low-latency flash & beep
                playClickSound(isAccent)
                _flashActive.value = true

                // Keep flash active for partial beat or maximum of 120 milliseconds
                val beatDurationMs = 60000.0 / bpm
                val flashOffDelay = (beatDurationMs * 0.22).coerceAtMost(120.0).toLong()

                launch(Dispatchers.Main) {
                    delay(flashOffDelay)
                    _flashActive.value = false
                }

                beatCounter = if (sig > 0) {
                    (beatCounter + 1) % sig
                } else {
                    0
                }

                nextBeatTime += beatDurationMs.toLong()
                val sleepTime = nextBeatTime - System.currentTimeMillis()
                if (sleepTime > 0) {
                    delay(sleepTime)
                } else {
                    // Fell behind: catch up instantly to keep absolute grid
                    nextBeatTime = System.currentTimeMillis()
                    delay(10)
                }
            }
        }
    }

    fun stopMetronome() {
        _isPlaying.value = false
        _currentBeatIndex.value = 0
        _flashActive.value = false
        metronomeJob?.cancel()
        metronomeJob = null
    }

    // --- Fast adjusts ---
    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(20, 300)
    }

    fun adjustBpm(delta: Int) {
        setBpm(_bpm.value + delta)
    }

    fun setTimeSignature(signature: String) {
        _timeSignature.value = signature
        // Reset subbeat sequence counter instantly
        if (_isPlaying.value) {
            startMetronome()
        }
    }

    // --- Song actions ---
    fun selectSong(song: Song) {
        _selectedSongId.value = song.id
        setBpm(song.bpm)
    }

    fun addSong(title: String, bpm: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentMaxPosition = songsList.value.maxOfOrNull { it.position } ?: -1
            val newSong = Song(title = title.trim(), bpm = bpm.coerceIn(20, 300), position = currentMaxPosition + 1)
            repository.insert(newSong)
        }
    }

    fun editSong(song: Song, newTitle: String, newBpm: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedSong = song.copy(title = newTitle.trim(), bpm = newBpm.coerceIn(20, 300))
            repository.update(updatedSong)
            // If the edited song is currently selected, adjust metronome tempo
            if (_selectedSongId.value == song.id) {
                setBpm(newBpm)
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(song)
            if (_selectedSongId.value == song.id) {
                _selectedSongId.value = null
            }
        }
    }

    fun moveSongUp(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = songsList.value
            val index = list.indexOfFirst { it.id == song.id }
            if (index > 0) {
                val prevSong = list[index - 1]
                val updatedCurrent = song.copy(position = prevSong.position)
                val updatedPrev = prevSong.copy(position = song.position)
                repository.updateSongs(listOf(updatedCurrent, updatedPrev))
            }
        }
    }

    fun moveSongDown(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = songsList.value
            val index = list.indexOfFirst { it.id == song.id }
            if (index >= 0 && index < list.size - 1) {
                val nextSong = list[index + 1]
                val updatedCurrent = song.copy(position = nextSong.position)
                val updatedNext = nextSong.copy(position = song.position)
                repository.updateSongs(listOf(updatedCurrent, updatedNext))
            }
        }
    }

    // --- Theme preferences ---
    private val _darkThemePreference = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light
    val darkThemePreferenceState: StateFlow<Boolean?> = _darkThemePreference.asStateFlow()

    fun setDarkThemePreference(isDark: Boolean?) {
        _darkThemePreference.value = isDark
    }

    // --- Media setlist controls ---
    fun selectNextSong() {
        val list = songsList.value
        if (list.isEmpty()) return
        val currentId = _selectedSongId.value
        val currentIndex = list.indexOfFirst { it.id == currentId }
        if (currentIndex >= 0 && currentIndex < list.size - 1) {
            selectSong(list[currentIndex + 1])
        }
    }

    fun selectPreviousSong() {
        val list = songsList.value
        if (list.isEmpty()) return
        val currentId = _selectedSongId.value
        val currentIndex = list.indexOfFirst { it.id == currentId }
        if (currentIndex > 0) {
            selectSong(list[currentIndex - 1])
        }
    }

    fun hasNextSong(): Boolean {
        val list = songsList.value
        if (list.isEmpty()) return false
        val currentId = _selectedSongId.value
        val currentIndex = list.indexOfFirst { it.id == currentId }
        return currentIndex >= 0 && currentIndex < list.size - 1
    }

    fun hasPreviousSong(): Boolean {
        val list = songsList.value
        if (list.isEmpty()) return false
        val currentId = _selectedSongId.value
        val currentIndex = list.indexOfFirst { it.id == currentId }
        return currentIndex > 0
    }

    override fun onCleared() {
        super.onCleared()
        stopMetronome()
        try {
            clickTrack?.release()
            accentTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class MetronomeViewModelFactory(private val repository: SongRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MetronomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MetronomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
