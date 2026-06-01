package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(
    viewModel: MetronomeViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songsList.collectAsStateWithLifecycle()
    val selectedSongId by viewModel.selectedSongIdState.collectAsStateWithLifecycle()
    val bpm by viewModel.bpmState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlayingState.collectAsStateWithLifecycle()
    val timeSignature by viewModel.timeSignatureState.collectAsStateWithLifecycle()
    val currentBeatIndex by viewModel.currentBeatIndexState.collectAsStateWithLifecycle()
    val flashActive by viewModel.flashActiveState.collectAsStateWithLifecycle()

    var showPresentation by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var songToEdit by remember { mutableStateOf<Song?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showPresentation) {
        PresentationView(
            onEnterApp = { showPresentation = false }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Metrónomo GLF",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Gestor de Ritmo & Setlist",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("btn_show_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { showPresentation = true },
                            modifier = Modifier.testTag("btn_show_presentation")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Ver Presentación",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // High Density Screen background
        ) {
            // --- SECTION 1: SONGS SETLIST ROW HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mi Setlist (${songs.size})",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                if (songs.isNotEmpty()) {
                    Text(
                        text = "Toca para activar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- SECTION 2: THE SCROLLABLE SETLIST & FAB CONTAINER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Sin canciones",
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(bottom = 12.dp),
                                tint = Color(0xFF49454F).copy(alpha = 0.4f)
                            )
                            Text(
                                text = "¡Aún no hay canciones en tu setlist!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D1B20)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Presioná '+' abajo para agregar una nueva.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF49454F),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = songs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            val isSelected = selectedSongId == song.id
                            SongRowItem(
                                song = song,
                                isSelected = isSelected,
                                isFirst = index == 0,
                                isLast = index == songs.lastIndex,
                                onSelect = { viewModel.selectSong(song) },
                                onMoveUp = { viewModel.moveSongUp(song) },
                                onMoveDown = { viewModel.moveSongDown(song) },
                                onEdit = { songToEdit = song },
                                onDelete = { songToDelete = song }
                            )
                        }
                    }
                }

                // High Density stylized Floating Action Button
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xFFEADDFF),
                    contentColor = Color(0xFF21005D),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                        .testTag("add_song_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar Canción",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- SECTION 3: THE HIGH DENSITY METRONOME FOOTER PANEL ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3EDF7)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 26.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Visual pulse sub-division dots
                    val totalDots = when (timeSignature) {
                        "2/4" -> 2
                        "3/4" -> 3
                        "4/4" -> 4
                        else -> 4 // Spaced beats
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (dotIndex in 1..totalDots) {
                            val isCurrent = currentBeatIndex == dotIndex
                            val isFirstBeat = dotIndex == 1

                            val dotScale by animateFloatAsState(
                                targetValue = if (isCurrent && flashActive) 1.5f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f)
                            )

                            val dotColor by animateColorAsState(
                                targetValue = when {
                                    isCurrent && flashActive -> Color(0xFF6750A4) // Dynamic primary accent
                                    isCurrent -> Color(0xFF6750A4).copy(alpha = 0.4f)
                                    else -> Color(0xFFD1D1D1) // Off gray
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(14.dp)
                                    .scale(dotScale)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .testTag("beat_dot_$dotIndex")
                            )
                        }
                    }

                    // Main Controls (BPM values & micro-adjusters)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // -1 BPM Decrement button
                        IconButton(
                            onClick = { viewModel.adjustBpm(-1) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .testTag("btn_minus_one")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Restar 1 BPM",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Big central BPM display text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = bpm.toString(),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("bpm_display_text")
                            )
                            Text(
                                text = "BPM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // +1 BPM Increment button
                        IconButton(
                            onClick = { viewModel.adjustBpm(1) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .testTag("btn_plus_one")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Sumar 1 BPM",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Quick modifiers (-5, +5) & Central Big SQUIRCLE play-pause action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlaying) {
                            val hasPrev = viewModel.hasPreviousSong()
                            IconButton(
                                onClick = { viewModel.selectPreviousSong() },
                                enabled = hasPrev,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(44.dp)
                                    .background(
                                        if (hasPrev) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Pista anterior",
                                    tint = if (hasPrev) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // -5 BPM micro-adjust button
                        Button(
                            onClick = { viewModel.adjustBpm(-5) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(if (isPlaying) 0.8f else 1f)
                                .height(48.dp)
                                .padding(end = if (isPlaying) 4.dp else 8.dp)
                                .testTag("btn_minus_five")
                        ) {
                            Text("-5", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Massive Central play/pause command
                        Button(
                            onClick = { viewModel.togglePlayback() },
                            modifier = Modifier
                                .size(76.dp)
                                .testTag("play_pause_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(24.dp) // Sizable squircle
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Iniciar",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // +5 BPM micro-adjust button
                        Button(
                            onClick = { viewModel.adjustBpm(5) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(if (isPlaying) 0.8f else 1f)
                                .height(48.dp)
                                .padding(start = if (isPlaying) 4.dp else 8.dp)
                                .testTag("btn_plus_five")
                        ) {
                            Text("+5", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }

                        if (isPlaying) {
                            val hasNext = viewModel.hasNextSong()
                            IconButton(
                                onClick = { viewModel.selectNextSong() },
                                enabled = hasNext,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(44.dp)
                                    .background(
                                        if (hasNext) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Pista siguiente",
                                    tint = if (hasNext) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Bottom-most Time Signature Selector (Filter Chips)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val signatures = listOf("Sin acento", "2/4", "3/4", "4/4")
                        signatures.forEach { sig ->
                            val isSelected = timeSignature == sig
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTimeSignature(sig) },
                                label = { Text(sig, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6750A4),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.Transparent,
                                    labelColor = Color(0xFF49454F)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color(0xFF79747E).copy(alpha = 0.4f),
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS REGION ---

    // 0. SETTINGS CONFIGURATION DIALOG
    if (showSettingsDialog) {
        val darkThemePref by viewModel.darkThemePreferenceState.collectAsStateWithLifecycle()
        val uriHandler = LocalUriHandler.current

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Configuración",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme section
                    Text(
                        text = "Tema de la aplicación",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Light theme button
                        val isLightActive = darkThemePref == false
                        FilterChip(
                            selected = isLightActive,
                            onClick = { viewModel.setDarkThemePreference(false) },
                            label = { Text("Claro") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Dark theme button
                        val isDarkActive = darkThemePref == true
                        FilterChip(
                            selected = isDarkActive,
                            onClick = { viewModel.setDarkThemePreference(true) },
                            label = { Text("Oscuro") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // System default button
                    val isSystemActive = darkThemePref == null
                    FilterChip(
                        selected = isSystemActive,
                        onClick = { viewModel.setDarkThemePreference(null) },
                        label = { Text("Valor predeterminado del sistema", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // "Sobre Metrónomo GLF" section
                    Text(
                        text = "Sobre Metrónomo GLF",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "App desarrollada con AI Studio, todos los derechos reservados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ),
                        onClick = {
                            try {
                                uriHandler.openUri("https://bio.link/furly")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mis Redes Sociales",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "bio.link/furly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Abrir enlace",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false }
                ) {
                    Text("Listo")
                }
            }
        )
    }

    // 1. ADD SONG DIALOG
    if (showAddDialog) {
        var titleInput by remember { mutableStateOf("") }
        var bpmInput by remember { mutableStateOf("120") }
        var isTitleError by remember { mutableStateOf(false) }
        var isBpmError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Agregar canción",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = {
                            titleInput = it
                            isTitleError = it.trim().isEmpty()
                        },
                        label = { Text("Nombre de la canción") },
                        placeholder = { Text("Nueva Canción") },
                        isError = isTitleError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_song_name_input")
                    )
                    if (isTitleError) {
                        Text(
                            text = "El nombre no puede estar vacío",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = bpmInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                bpmInput = newValue
                            }
                            val parsed = newValue.toIntOrNull()
                            isBpmError = parsed == null || parsed !in 20..300
                        },
                        label = { Text("BPM (Tempo)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isBpmError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_song_bpm_input")
                    )
                    if (isBpmError) {
                        Text(
                            text = "BPM debe estar entre 20 y 300",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = if (titleInput.isBlank()) "Nueva Canción" else titleInput.trim()
                        val finalBpm = bpmInput.toIntOrNull() ?: 120
                        
                        if (finalBpm in 20..300) {
                            viewModel.addSong(finalTitle, finalBpm)
                            showAddDialog = false
                        } else {
                            isBpmError = true
                        }
                    },
                    modifier = Modifier.testTag("add_song_confirm_btn")
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 2. EDIT SONG DIALOG
    songToEdit?.let { song ->
        var titleInput by remember { mutableStateOf(song.title) }
        var bpmInput by remember { mutableStateOf(song.bpm.toString()) }
        var isTitleError by remember { mutableStateOf(false) }
        var isBpmError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { songToEdit = null },
            title = {
                Text(
                    text = "Editar canción",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = {
                            titleInput = it
                            isTitleError = it.trim().isEmpty()
                        },
                        label = { Text("Nombre de la canción") },
                        isError = isTitleError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_song_name_input")
                    )
                    if (isTitleError) {
                        Text(
                            text = "El nombre no puede estar vacío",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = bpmInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                bpmInput = newValue
                            }
                            val parsed = newValue.toIntOrNull()
                            isBpmError = parsed == null || parsed !in 20..300
                        },
                        label = { Text("BPM (Tempo)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isBpmError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_song_bpm_input")
                    )
                    if (isBpmError) {
                        Text(
                            text = "BPM debe estar entre 20 y 300",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalBpm = bpmInput.toIntOrNull() ?: song.bpm
                        if (titleInput.isNotBlank() && finalBpm in 20..300) {
                            viewModel.editSong(song, titleInput, finalBpm)
                            songToEdit = null
                        } else {
                            if (titleInput.isBlank()) isTitleError = true
                            if (finalBpm !in 20..300) isBpmError = true
                        }
                    },
                    modifier = Modifier.testTag("edit_song_confirm_btn")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { songToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 3. DELETE CONFIRMATION DIALOG
    songToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = {
                Text(
                    text = "¿Eliminar canción?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar '" + song.title + "' del setlist?",
                    color = Color(0xFF49454F)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSong(song)
                        songToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                    modifier = Modifier.testTag("delete_song_confirm_btn")
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
}

@Composable
fun SongRowItem(
    song: Song,
    isSelected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onSelect: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .testTag("song_card_${song.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle icon for reordering (replacing the previous small arrow buttons)
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Arrastrar para reordenar",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(42.dp)
                    .padding(8.dp)
                    .pointerInput(song.id) {
                        detectDragGestures(
                            onDragStart = { offsetY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                                if (offsetY > thresholdPx) {
                                    if (!isLast) {
                                        onMoveDown()
                                        offsetY = 0f
                                    }
                                } else if (offsetY < -thresholdPx) {
                                    if (!isFirst) {
                                        onMoveUp()
                                        offsetY = 0f
                                    }
                                }
                            },
                            onDragEnd = { offsetY = 0f },
                            onDragCancel = { offsetY = 0f }
                        )
                    }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Song contextual description (Title & Position tag)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Track #${song.position + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
 
            // Numeric BPM badge, layout structured side-by-side
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = song.bpm.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "BPM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
 
            // Actions: Edit pen & Delete trash icons
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("song_edit_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar Canción",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
 
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("song_delete_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar Canción",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PresentationView(
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- TOP RETRO COVER TITLE ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "APRENDIENDO",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "GUITARRA PARA",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(
                text = "PRINCIPIANTES",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- STRIKING BRANDED "METRONOMO" EMBLEM ---
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.padding(horizontal = 16.dp).testTag("presentation_metronome_badge")
            ) {
                Text(
                    text = "METRÓNOMO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- THE SPECTACULAR NATIVE COMPOSED COMIC ILLUSTRATION ---
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            MetronomeIllustration(modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- SIGNATURE FOOTER & ACTION TRIGGER ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GERMÁN L. FURLANI",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "RHYTHM COMPANION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Button(
                onClick = onEnterApp,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("btn_enter_metronome")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "INGRESAR AL METRÓNOMO",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun MetronomeIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // Draw a light grey/lavender circular background representing a record/clock
        drawCircle(
            color = Color(0xFFF3EDF7),
            radius = width.coerceAtMost(height) * 0.44f,
            center = Offset(centerX, centerY)
        )

        // Draw dynamic rhythmic circles (beats) radiating out
        drawCircle(
            color = Color(0xFF6750A4).copy(alpha = 0.08f),
            radius = width.coerceAtMost(height) * 0.48f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )

        // --- TICK / SOUND WAVES (RHYTHMIC DESIGN ACCENTS) ---
        // Left wave (faint)
        drawArc(
            color = Color(0xFF6750A4).copy(alpha = 0.2f),
            startAngle = 140f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(centerX - 130.dp.toPx(), centerY - 65.dp.toPx()),
            size = Size(80.dp.toPx(), 130.dp.toPx()),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        // Farther left wave
        drawArc(
            color = Color(0xFF6750A4).copy(alpha = 0.08f),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(centerX - 155.dp.toPx(), centerY - 85.dp.toPx()),
            size = Size(100.dp.toPx(), 170.dp.toPx()),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Right active dynamic tick wave (bold & active)
        drawArc(
            color = Color(0xFF6750A4).copy(alpha = 0.45f),
            startAngle = 320f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(centerX + 50.dp.toPx(), centerY - 65.dp.toPx()),
            size = Size(80.dp.toPx(), 130.dp.toPx()),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        // Farther right wave
        drawArc(
            color = Color(0xFF6750A4).copy(alpha = 0.15f),
            startAngle = 325f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(centerX + 55.dp.toPx(), centerY - 85.dp.toPx()),
            size = Size(100.dp.toPx(), 170.dp.toPx()),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // --- MECHANICAL METRONOME STRUCTURE ---

        // 1. Rubber base feet
        drawCircle(
            color = Color(0xFF1D1B20),
            radius = 6.dp.toPx(),
            center = Offset(centerX - 48.dp.toPx(), centerY + 84.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF1D1B20),
            radius = 6.dp.toPx(),
            center = Offset(centerX + 48.dp.toPx(), centerY + 84.dp.toPx())
        )

        // 2. Windup brass key on the right side
        // Connector pin
        drawLine(
            color = Color(0xFFFFD54F),
            start = Offset(centerX + 52.dp.toPx(), centerY + 35.dp.toPx()),
            end = Offset(centerX + 62.dp.toPx(), centerY + 35.dp.toPx()),
            strokeWidth = 3.1.dp.toPx()
        )
        // Wings loops
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = 7.dp.toPx(),
            center = Offset(centerX + 65.dp.toPx(), centerY + 35.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // 3. Main Pyramid Outer Body (Deep rich walnut/plum color)
        val bodyPath = Path().apply {
            moveTo(centerX - 56.dp.toPx(), centerY + 80.dp.toPx()) // bottom left
            lineTo(centerX + 56.dp.toPx(), centerY + 80.dp.toPx()) // bottom right
            lineTo(centerX + 16.dp.toPx(), centerY - 72.dp.toPx()) // top right
            lineTo(centerX - 16.dp.toPx(), centerY - 72.dp.toPx()) // top left
            close()
        }
        drawPath(path = bodyPath, color = Color(0xFF2E1A47))

        // Body outline accent
        drawPath(
            path = bodyPath,
            color = Color(0xFF1D1B20),
            style = Stroke(width = 3.dp.toPx())
        )

        // 4. Clean Inner Scale Plate (High contrast Face Panel)
        val platePath = Path().apply {
            moveTo(centerX - 38.dp.toPx(), centerY + 70.dp.toPx())
            lineTo(centerX + 38.dp.toPx(), centerY + 70.dp.toPx())
            lineTo(centerX + 11.dp.toPx(), centerY - 58.dp.toPx())
            lineTo(centerX - 11.dp.toPx(), centerY - 58.dp.toPx())
            close()
        }
        drawPath(path = platePath, color = Color(0xFFFFFEEB)) // cream paper ivory

        // Scale outline
        drawPath(
            path = platePath,
            color = Color(0xFF2E1A47).copy(alpha = 0.3f),
            style = Stroke(width = 1.dp.toPx())
        )

        // 5. Vertical center slider slot
        drawLine(
            color = Color(0xFF6750A4).copy(alpha = 0.15f),
            start = Offset(centerX, centerY - 52.dp.toPx()),
            end = Offset(centerX, centerY + 65.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )

        // 6. Horizontal Tempo Scale markings
        for (i in 0..7) {
            val y = (centerY - 45.dp.toPx()) + (i * 13.dp.toPx())
            val halfWidth = 6.dp.toPx() + (i * 2.2f.dp.toPx())
            // left tick
            drawLine(
                color = Color(0xFF2E1A47).copy(alpha = 0.4f),
                start = Offset(centerX - halfWidth, y),
                end = Offset(centerX - halfWidth + 4.dp.toPx(), y),
                strokeWidth = 1.5.dp.toPx()
            )
            // right tick
            drawLine(
                color = Color(0xFF2E1A47).copy(alpha = 0.4f),
                start = Offset(centerX + halfWidth - 4.dp.toPx(), y),
                end = Offset(centerX + halfWidth, y),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // 7. Swinging Pendulum Needle/Rod (Angled dynamically at -16 degrees)
        val angleRad = Math.toRadians(-16.0)
        val length = 112.dp.toPx()
        val startX = centerX
        val startY = centerY + 58.dp.toPx()
        val endX = startX + (length * Math.sin(angleRad)).toFloat()
        val endY = startY - (length * Math.cos(angleRad)).toFloat()

        // Steel Pendulum Arm Line
        drawLine(
            color = Color(0xFF1D1B20),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.5.dp.toPx()
        )

        // 8. Slidable Tempo Weight (Mechanical sliding regulator)
        val weightDistance = 72.dp.toPx()
        val weightX = startX + (weightDistance * Math.sin(angleRad)).toFloat()
        val weightY = startY - (weightDistance * Math.cos(angleRad)).toFloat()

        // Brass Trapezoid slider
        drawCircle(
            color = Color(0xFFFFD54F), // shiny brass
            radius = 9.dp.toPx(),
            center = Offset(weightX, weightY)
        )
        drawCircle(
            color = Color(0xFFE5A93C), // inner weight contour
            radius = 5.dp.toPx(),
            center = Offset(weightX, weightY)
        )

        // 9. Bottom hinge anchor cap
        drawCircle(
            color = Color(0xFF1D1B20),
            radius = 9.dp.toPx(),
            center = Offset(startX, startY)
        )
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = 3.5.dp.toPx(),
            center = Offset(startX, startY)
        )

        // 10. Golden shield logo badge on the lower body
        val logoPath = Path().apply {
            moveTo(centerX - 12.dp.toPx(), centerY + 78.dp.toPx())
            lineTo(centerX + 12.dp.toPx(), centerY + 78.dp.toPx())
            lineTo(centerX + 8.dp.toPx(), centerY + 67.dp.toPx())
            lineTo(centerX - 8.dp.toPx(), centerY + 67.dp.toPx())
            close()
        }
        drawPath(path = logoPath, color = Color(0xFFFFD54F))
    }
}

