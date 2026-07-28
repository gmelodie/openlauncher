package com.openlauncher.app.ui.widget

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.openlauncher.app.ui.theme.GruvLightBg1
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openlauncher.app.data.SoundPadConfig
import com.openlauncher.app.ui.theme.widgetInk
import com.openlauncher.app.ui.theme.widgetSubInk
import com.openlauncher.app.ui.theme.widgetLine

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SoundboardWidget(
    pads: List<SoundPadConfig>,
    accent: Color,
    isDayMode: Boolean = false,
    isEditing: Boolean = false,
    onUpdatePad: (index: Int, pad: SoundPadConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context      = LocalContext.current
    val contentColor = widgetInk(isDayMode)
    val dimColor     = widgetSubInk(isDayMode)
    val borderColor  = widgetLine(isDayMode)

    var activePadIndex by remember { mutableStateOf<Int?>(null) }
    var assigningIndex by remember { mutableStateOf<Int?>(null) }

    val safePads = remember(pads) {
        if (pads.size >= 6) pads.take(6)
        else pads + List(6 - pads.size) { SoundPadConfig() }
    }

    // Outer grid Column with top padding = 22.dp to leave room for card header label "SOUNDBOARD"
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(2) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { col ->
                    val idx = row * 3 + col
                    val pad = safePads[idx]
                    val isActive = activePadIndex == idx

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                1.dp,
                                if (isActive) accent else borderColor,
                                RoundedCornerShape(3.dp)
                            )
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isActive) accent.copy(alpha = 0.12f) else Color.Transparent)
                            .then(
                                if (!isEditing) Modifier.combinedClickable(
                                    onClick = {
                                        if (!pad.isAssigned) {
                                            assigningIndex = idx
                                            return@combinedClickable
                                        }
                                        activePadIndex = idx
                                        playSoundPad(
                                            context = context,
                                            pad = pad,
                                            onDone = { activePadIndex = null }
                                        )
                                    },
                                    onLongClick = { assigningIndex = idx }
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val isEmpty = !pad.isAssigned
                        Text(
                            text = if (isEmpty) "+" else pad.label,
                            color = if (isActive) accent else if (isEmpty) dimColor else contentColor,
                            fontSize = if (isEmpty) 16.sp else 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    assigningIndex?.let { idx ->
        PadAssignDialog(
            pad = safePads[idx],
            accent = accent,
            isDayMode = isDayMode,
            onDismiss = { assigningIndex = null },
            onSave = { updated ->
                onUpdatePad(idx, updated)
                assigningIndex = null
            }
        )
    }
}

@Composable
private fun PadAssignDialog(
    pad: SoundPadConfig,
    accent: Color,
    isDayMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (SoundPadConfig) -> Unit
) {
    val context    = LocalContext.current
    val menuBg     = if (isDayMode) GruvLightBg1 else MaterialTheme.colorScheme.background
    val menuBorder = widgetLine(isDayMode)
    val contentColor = widgetInk(isDayMode)
    val dimColor   = widgetSubInk(isDayMode)
    val fieldBorder = widgetLine(isDayMode)

    var labelText   by remember { mutableStateOf(if (pad.isAssigned) pad.label else "") }
    var soundName   by remember { mutableStateOf(pad.soundName) }
    var audioUri    by remember { mutableStateOf(pad.audioUri) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            audioUri = uri.toString()
            val rawName = uri.path?.substringAfterLast('/') ?: "custom_sound"
            labelText = rawName.substringAfterLast(':').substringBeforeLast('.')
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(menuBg)
                .border(1.dp, menuBorder, RoundedCornerShape(6.dp))
                .padding(18.dp)
                .width(340.dp), // Fixed size: increased from 220dp to 340dp for landscape headunit displays
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "ASSIGN PAD SOUND",
                color = contentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            // Label field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PAD LABEL", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                BasicTextField(
                    value = labelText,
                    onValueChange = { if (it.length <= 12) labelText = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, fieldBorder, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Preloaded Audio Selector (replaces old raw waveform synth generation)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PRELOADED AUDIO", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val preloadedSounds = listOf(
                        "mario_jump" to "mario_jump",
                        "mario_coin" to "mario_coin",
                        "boom" to "boom",
                        "loud_fart" to "loud_fart"
                    )
                    preloadedSounds.forEach { (type, chipLabel) ->
                        val active = soundName == type && audioUri.isEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .border(1.dp, if (active) accent else fieldBorder, RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) accent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    soundName = type
                                    audioUri = ""
                                    labelText = type
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                chipLabel,
                                color = if (active) accent else dimColor,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Custom audio file picker
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CUSTOM AUDIO FILE", color = dimColor, fontSize = 7.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("audio/*")) },
                        modifier = Modifier.weight(1f).height(30.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (audioUri.isNotEmpty()) accent else fieldBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AudioFile, null, tint = accent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (audioUri.isNotEmpty()) "CUSTOM FILE ASSIGNED" else "PICK AUDIO FILE",
                            color = accent,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (audioUri.isNotEmpty()) {
                        IconButton(
                            onClick = { audioUri = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Clear, null, tint = dimColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (audioUri.isNotEmpty()) {
                    Text(
                        audioUri.substringAfterLast('/').take(36),
                        color = dimColor.copy(alpha = 0.6f),
                        fontSize = 6.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (pad.isAssigned) {
                OutlinedButton(
                    onClick = { onSave(SoundPadConfig()) },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF884444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF884444)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CLEAR SOUND", color = Color(0xFF884444), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // Save / Cancel row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = dimColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, fieldBorder),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("CANCEL", color = dimColor, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onSave(SoundPadConfig(
                            label     = labelText.trim().ifEmpty { soundName.ifEmpty { "PAD" } },
                            audioUri  = audioUri,
                            soundName = soundName
                        ))
                    },
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("SAVE SOUND", color = if (isDayMode) Color.White else Color.Black, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// A pad plays over navigation prompts and music, so it takes transient focus and
// lets the other app duck for the length of the effect.
private class AudioFocusHold(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val legacyListener = AudioManager.OnAudioFocusChangeListener { }
    private var request: AudioFocusRequest? = null

    fun acquire() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                legacyListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            return
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(legacyListener)
            .build()
        request = focus
        audioManager.requestAudioFocus(focus)
    }

    fun release() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(legacyListener)
            return
        }
        request?.let { audioManager.abandonAudioFocusRequest(it) }
        request = null
    }
}

private fun playSoundPad(context: Context, pad: SoundPadConfig, onDone: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).post {
        val focus = AudioFocusHold(context)
        val finish = {
            focus.release()
            onDone()
        }
        val player = runCatching { buildPlayer(context, pad, finish) }.getOrNull()
        if (player == null) {
            finish()
            return@post
        }
        focus.acquire()
        // prepareAsync keeps a slow content URI off the main thread. A blocking
        // prepare on head-unit storage is an ANR risk.
        runCatching { player.prepareAsync() }.onFailure {
            runCatching { player.release() }
            finish()
        }
    }
}

private fun buildPlayer(context: Context, pad: SoundPadConfig, finish: () -> Unit): MediaPlayer? {
    val player = MediaPlayer()
    val attached = runCatching {
        if (pad.audioUri.isNotEmpty()) {
            player.setDataSource(context, android.net.Uri.parse(pad.audioUri))
        } else {
            val resId = resolveRawSound(context, pad.soundName) ?: return@runCatching false
            context.resources.openRawResourceFd(resId).use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
        }
        true
    }.getOrDefault(false)

    if (!attached) {
        runCatching { player.release() }
        return null
    }
    player.setOnPreparedListener { it.start() }
    player.setOnCompletionListener { mp ->
        mp.release()
        finish()
    }
    // A revoked SAF grant or a deleted file must also release the player and
    // clear the highlight on the pad.
    player.setOnErrorListener { mp, _, _ ->
        mp.release()
        finish()
        true
    }
    return player
}

private fun resolveRawSound(context: Context, soundName: String): Int? {
    val direct = context.resources.getIdentifier(soundName.lowercase().trim(), "raw", context.packageName)
    if (direct != 0) return direct
    // Names stored by versions that generated tones instead of playing files.
    val legacy = when (soundName.lowercase().trim()) {
        "kick", "snare", "bass" -> "mario_coin"
        else -> "mario_jump"
    }
    return context.resources.getIdentifier(legacy, "raw", context.packageName).takeIf { it != 0 }
}
