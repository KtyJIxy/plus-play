package com.plusplay.app

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Rational
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Locale
import kotlin.math.abs

class PlayerActivity : AppCompatActivity() {

    private lateinit var videoView: ScalableVideoView
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var controlsOverlay: View
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var totalTimeText: TextView
    private lateinit var videoTitle: TextView
    private lateinit var seekFeedback: TextView
    private lateinit var subtitleText: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnCenterPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnRotate: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnLock: ImageButton
    private lateinit var btnPip: ImageButton
    private lateinit var btnAspect: ImageButton
    private lateinit var lockedIndicator: ImageButton
    private lateinit var settingsDropdown: LinearLayout
    private lateinit var settingCurrentDirSubtitle: TextView
    private lateinit var settingExternalSubtitle: TextView
    private lateinit var subDelayLabel: TextView
    private lateinit var subDelayMinusBig: TextView
    private lateinit var subDelayMinus: TextView
    private lateinit var subDelayPlus: TextView
    private lateinit var subDelayPlusBig: TextView
    private lateinit var subSizeLabel: TextView
    private lateinit var subSizeMinusBig: TextView
    private lateinit var subSizeMinus: TextView
    private lateinit var subSizePlus: TextView
    private lateinit var subSizePlusBig: TextView
    
    private var isPlaying = true
    private var videoPath: String? = null
    private var playlist: ArrayList<VideoFile>? = null
    private var currentVideoIndex = 0
    private var isLocked = false
    private var subtitles: List<SubtitleEntry> = emptyList()
    private var currentSubtitlePath: String? = null
    // Subtitle delay in ms; positive = subtitles appear later
    private var subtitleOffsetMs = 0L
    // Subtitle size in dp (ignores the system font scale), saved for all videos
    private var subtitleSizeDp = SUB_SIZE_DEFAULT
    private var subtitlePreviewUntil = 0L
    private var aspectMode = ScalableVideoView.Mode.FIT
    // Last position seen while playing; the player may already be released in onStop
    private var lastKnownPosition = 0
    private var lastPeriodicSave = 0L

    // Picture-in-picture state
    private var videoAspect = Rational(16, 9)
    // Wide PiP window (experimental), remembered between sessions
    private var pipWide = false
    private var pipActive = false // True from entering PiP until the activity is resumed or stopped
    private var suppressAutoPip = false // Set while we launch another screen (e.g. subtitle picker)
    private var pipReceiver: BroadcastReceiver? = null

    // Variables for accumulated seeking with scroll tracking
    private var accumulatedSeekTime = 0 // in milliseconds
    private var lastSwipeDirection = 0 // 1 for forward, -1 for backward, 0 for none
    private var lastSwipeTime = 0L
    private val swipeResetDelay = 1000L // Reset accumulated seek after 1 second of no swiping
    
    // Variables for continuous scroll seeking
    private var scrollStartX = 0f
    private var scrollStartY = 0f
    private var totalScrolledDistance = 0f
    private val scrollThresholdPerSeek = 150f // Pixels to scroll for each 10s increment
    private var isScrolling = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            updateSeekBar()
            updateSubtitle()
            savePositionPeriodically()
            // Tick faster while subtitles are showing so they stay in step
            handler.postDelayed(this, if (subtitles.isEmpty()) 500 else 250)
        }
    }
    
    private val hideControlsRunnable = Runnable {
        hideControls()
    }
    
    private val resetAccumulatedSeekRunnable = Runnable {
        accumulatedSeekTime = 0
        lastSwipeDirection = 0
    }
    
    companion object {
        private const val REQUEST_SUBTITLE_FILE = 100
        private const val PREFS_NAME = "VideoPlayerPrefs"
        private const val KEY_VIDEO_POSITION = "video_position_"
        private const val KEY_WAS_PLAYING = "was_playing_"
        private const val KEY_TIMESTAMP = "video_timestamp_"
        private const val KEY_RECENT_VIDEOS = "recent_videos"
        private const val STATE_VIDEO_PATH = "state_video_path"
        private const val STATE_VIDEO_POSITION = "state_video_position"
        private const val STATE_WAS_PLAYING = "state_was_playing"
        private const val MAX_RECENT_VIDEOS = 30
        private const val KEY_ASPECT_MODE = "aspect_mode"
        private const val KEY_SUB_OFFSET = "sub_offset_"
        // Positions closer than this to the start or the end are not worth resuming
        private const val MIN_RESUME_POSITION = 5000
        private const val END_MARGIN_MS = 5000
        private const val PERIODIC_SAVE_INTERVAL = 10000L
        private const val SUB_STEP_SMALL = 100L
        private const val SUB_STEP_BIG = 1000L
        private const val KEY_SUB_SIZE = "sub_size_dp"
        private const val SUB_SIZE_DEFAULT = 16
        private const val SUB_SIZE_MIN = 8
        private const val SUB_SIZE_MAX = 48
        private const val SUB_SIZE_STEP_SMALL = 1
        private const val SUB_SIZE_STEP_BIG = 4
        // In picture-in-picture subtitles are drawn at this fraction of the chosen size
        private const val PIP_SUB_SIZE_FACTOR = 0.6f
        private const val PIP_SUB_SIZE_MIN = 8f
        private const val SUB_PREVIEW_MS = 2000L

        // Picture-in-picture remote actions
        private const val ACTION_PIP_CONTROL = "com.plusplay.app.PIP_CONTROL"
        private const val EXTRA_PIP_CONTROL = "pip_control"
        private const val PIP_REWIND = 1
        private const val PIP_PLAY_PAUSE = 2
        private const val PIP_FORWARD = 3
        private const val PIP_SIZE = 4
        private const val KEY_PIP_WIDE = "pip_wide"
        // Android only accepts PiP aspect ratios between 1:2.39 and 2.39:1
        private const val PIP_MAX_RATIO = 2.38f
    }
    
    data class SubtitleEntry(
        val startTime: Long,
        val endTime: Long,
        val text: String
    )
    
    data class VideoPlaybackState(
        val videoPath: String,
        val position: Int,
        val timestamp: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable fullscreen
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()
        
        setContentView(R.layout.activity_player)
        enterImmersiveMode()

        initializeViews()
        
        videoPath = intent.getStringExtra("VIDEO_PATH")
        playlist = intent.getParcelableArrayListExtra("PLAYLIST")
        currentVideoIndex = intent.getIntExtra("VIDEO_INDEX", 0)

        if (videoPath == null) {
            finish()
            return
        }
        
        setVideoTitle()
        setupGestureDetector()
        setupVideoView()
        setupControls()
        loadSubtitles()
    }
    
    private fun initializeViews() {
        videoView = findViewById(R.id.videoView)
        controlsOverlay = findViewById(R.id.controlsOverlay)
        seekBar = findViewById(R.id.seekBar)
        currentTimeText = findViewById(R.id.currentTime)
        totalTimeText = findViewById(R.id.totalTime)
        videoTitle = findViewById(R.id.videoTitle)
        seekFeedback = findViewById(R.id.seekFeedback)
        subtitleText = findViewById(R.id.subtitleText)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnCenterPlayPause = findViewById(R.id.btnCenterPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnRewind = findViewById(R.id.btnRewind)
        btnForward = findViewById(R.id.btnForward)
        btnRotate = findViewById(R.id.btnRotate)
        btnBack = findViewById(R.id.btnBack)
        btnSettings = findViewById(R.id.btnSettings)
        btnLock = findViewById(R.id.btnLock)
        btnPip = findViewById(R.id.btnPip)
        btnAspect = findViewById(R.id.btnAspect)
        lockedIndicator = findViewById(R.id.lockedIndicator)
        settingsDropdown = findViewById(R.id.settingsDropdown)
        settingCurrentDirSubtitle = findViewById(R.id.settingCurrentDirSubtitle)
        settingExternalSubtitle = findViewById(R.id.settingExternalSubtitle)
        subDelayLabel = findViewById(R.id.subDelayLabel)
        subDelayMinusBig = findViewById(R.id.subDelayMinusBig)
        subDelayMinus = findViewById(R.id.subDelayMinus)
        subDelayPlus = findViewById(R.id.subDelayPlus)
        subDelayPlusBig = findViewById(R.id.subDelayPlusBig)
        subSizeLabel = findViewById(R.id.subSizeLabel)
        subSizeMinusBig = findViewById(R.id.subSizeMinusBig)
        subSizeMinus = findViewById(R.id.subSizeMinus)
        subSizePlus = findViewById(R.id.subSizePlus)
        subSizePlusBig = findViewById(R.id.subSizePlusBig)
    }
    
    private fun setVideoTitle() {
        videoPath?.let { path ->
            val file = File(path)
            videoTitle.text = file.name
        }
    }
    
    private fun setupControls() {
        // Play/Pause buttons
        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnCenterPlayPause.setOnClickListener { togglePlayPause() }
        
        // Navigation buttons
        btnPrevious.setOnClickListener { playPreviousVideo() }
        btnNext.setOnClickListener { playNextVideo() }
        
        // Seek buttons
        btnRewind.setOnClickListener { seekRelative(-10000, true) }
        btnForward.setOnClickListener { seekRelative(10000, true) }
        
        // Rotate button
        btnRotate.setOnClickListener { toggleOrientation() }
        
        // Back button
        btnBack.setOnClickListener { 
            finish() 
        }
        
        // Settings button
        btnSettings.setOnClickListener { toggleSettingsDropdown() }
        
        // Settings - Subtitle from current directory
        settingCurrentDirSubtitle.setOnClickListener {
            settingsDropdown.visibility = View.GONE
            showCurrentDirectorySubtitles()
        }
        
        // Settings - External subtitle
        settingExternalSubtitle.setOnClickListener {
            settingsDropdown.visibility = View.GONE
            openSubtitleFilePicker()
        }
        
        // Lock button
        btnLock.setOnClickListener { toggleLock() }

        // Picture-in-picture button (hidden on devices that can't do PiP)
        btnPip.visibility = if (supportsPip()) View.VISIBLE else View.GONE
        btnPip.setOnClickListener { enterPip() }
        pipWide = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_PIP_WIDE, false)
        
        // Aspect mode (fit / crop / stretch), remembered between videos
        aspectMode = loadAspectMode()
        videoView.mode = aspectMode
        btnAspect.setOnClickListener { cycleAspectMode() }
        
        // Subtitle delay controls in the settings menu (tap the label to reset)
        subDelayLabel.setOnClickListener { setSubtitleDelay(0L) }
        subDelayMinusBig.setOnClickListener { setSubtitleDelay(subtitleOffsetMs - SUB_STEP_BIG) }
        subDelayMinus.setOnClickListener { setSubtitleDelay(subtitleOffsetMs - SUB_STEP_SMALL) }
        subDelayPlus.setOnClickListener { setSubtitleDelay(subtitleOffsetMs + SUB_STEP_SMALL) }
        subDelayPlusBig.setOnClickListener { setSubtitleDelay(subtitleOffsetMs + SUB_STEP_BIG) }

        // Subtitle size controls (tap the label to reset)
        subtitleSizeDp = loadSubtitleSize()
        applySubtitleSize(isInPip())
        updateSubSizeLabel()
        subSizeLabel.setOnClickListener { setSubtitleSize(SUB_SIZE_DEFAULT) }
        subSizeMinusBig.setOnClickListener { setSubtitleSize(subtitleSizeDp - SUB_SIZE_STEP_BIG) }
        subSizeMinus.setOnClickListener { setSubtitleSize(subtitleSizeDp - SUB_SIZE_STEP_SMALL) }
        subSizePlus.setOnClickListener { setSubtitleSize(subtitleSizeDp + SUB_SIZE_STEP_SMALL) }
        subSizePlusBig.setOnClickListener { setSubtitleSize(subtitleSizeDp + SUB_SIZE_STEP_BIG) }

        // Locked indicator (to unlock)
        lockedIndicator.setOnClickListener { toggleLock() }
        
        // SeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                    currentTimeText.text = formatTime(progress)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                scheduleHideControls()
            }
        })
        
        // Update navigation button states
        updateNavigationButtons()
        
        // Show controls initially
        showControls()
    }
    
    private fun toggleSettingsDropdown() {
        settingsDropdown.visibility = if (settingsDropdown.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
        // Keep controls visible when settings are open
        if (settingsDropdown.visibility == View.VISIBLE) {
            handler.removeCallbacks(hideControlsRunnable)
        } else {
            scheduleHideControls()
        }
    }
    
    private fun openSubtitleFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/x-subrip", "text/plain", "*/*"))
        }
        // Launching the picker triggers onUserLeaveHint; don't treat that as "go to PiP"
        suppressAutoPip = true
        startActivityForResult(intent, REQUEST_SUBTITLE_FILE)
    }
    
    private fun showCurrentDirectorySubtitles() {
        videoPath?.let { path ->
            val videoFile = File(path)
            val directory = videoFile.parentFile
            
            if (directory != null && directory.exists()) {
                // Find all .srt files in the same directory
                val srtFiles = directory.listFiles { file ->
                    file.isFile && file.extension.equals("srt", ignoreCase = true)
                }?.sortedBy { it.name } ?: emptyList()
                
                if (srtFiles.isEmpty()) {
                    // Show message if no subtitles found
                    AlertDialog.Builder(this)
                        .setTitle("No Subtitles Found")
                        .setMessage("No .srt subtitle files found in the current directory.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // Show dialog with subtitle files
                    val subtitleNames = srtFiles.map { it.name }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Select Subtitle")
                        .setItems(subtitleNames) { dialog, which ->
                            loadSubtitleFile(srtFiles[which])
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }
    
    private fun loadSubtitleFile(file: File) {
        try {
            val content = file.readText()
            subtitles = parseSubtitles(content)
            if (subtitles.isNotEmpty()) {
                subtitleText.visibility = View.VISIBLE
                currentSubtitlePath = file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Failed to load subtitle file: ${e.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SUBTITLE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val content = inputStream?.bufferedReader()?.use { it.readText() }
                    inputStream?.close()
                    
                    content?.let {
                        subtitles = parseSubtitles(it)
                        if (subtitles.isNotEmpty()) {
                            subtitleText.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun parseSubtitles(content: String): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        // Strip a UTF-8 byte order mark, otherwise the first cue is silently dropped
        val lines = content.removePrefix("\uFEFF").split("\n")
        var i = 0
        
        while (i < lines.size) {
            // Skip subtitle number
            if (lines[i].trim().toIntOrNull() != null) {
                i++
                if (i >= lines.size) break
                
                // Parse time
                val timeLine = lines[i].trim()
                val times = timeLine.split("-->")
                if (times.size == 2) {
                    val startTime = parseTime(times[0].trim())
                    val endTime = parseTime(times[1].trim())
                    i++
                    
                    // Parse text
                    val textLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }
                    
                    if (textLines.isNotEmpty()) {
                        entries.add(SubtitleEntry(startTime, endTime, textLines.joinToString("\n")))
                    }
                }
            }
            i++
        }
        
        return entries
    }
    
    private fun parseTime(timeString: String): Long {
        // Format: 00:00:00,000 or 00:00:00.000
        val parts = timeString.replace(",", ".").split(":")
        if (parts.size == 3) {
            val hours = parts[0].toLongOrNull() ?: 0
            val minutes = parts[1].toLongOrNull() ?: 0
            val secondsParts = parts[2].split(".")
            val seconds = secondsParts[0].toLongOrNull() ?: 0
            val millis = if (secondsParts.size > 1) secondsParts[1].toLongOrNull() ?: 0 else 0
            
            return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
        }
        return 0
    }
    
    private fun updateSubtitle() {
        // Leave the size preview alone while it is showing
        if (SystemClock.elapsedRealtime() < subtitlePreviewUntil) return
        if (subtitles.isEmpty()) {
            subtitleText.visibility = View.GONE
            return
        }
        
        val currentPosition = videoView.currentPosition.toLong() - subtitleOffsetMs
        val currentSubtitle = subtitles.find { it.startTime <= currentPosition && currentPosition <= it.endTime }
        
        if (currentSubtitle != null) {
            subtitleText.text = currentSubtitle.text
            subtitleText.visibility = View.VISIBLE
        } else {
            subtitleText.visibility = View.GONE
        }
    }
    
    private fun updateNavigationButtons() {
        val hasPlaylist = playlist != null && playlist!!.size > 1
        btnPrevious.isEnabled = hasPlaylist && currentVideoIndex > 0
        btnNext.isEnabled = hasPlaylist && currentVideoIndex < (playlist?.size ?: 1) - 1
        
        btnPrevious.alpha = if (btnPrevious.isEnabled) 1.0f else 0.3f
        btnNext.alpha = if (btnNext.isEnabled) 1.0f else 0.3f
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isLocked) return false
                
                // The video view can be wider than the screen in crop mode
                val screenWidth = (videoView.parent as View).width
                val x = e.x
                val middle = screenWidth / 3f
                
                when {
                    x < middle -> {
                        // Left side - seek backward 10 seconds
                        seekRelative(-10000, true)
                    }
                    x > screenWidth - middle -> {
                        // Right side - seek forward 10 seconds
                        seekRelative(10000, true)
                    }
                    else -> {
                        // Middle - play/pause
                        togglePlayPause()
                    }
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (isLocked) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                // Check if this is a horizontal scroll (swipe left/right for seeking)
                if (abs(diffX) > abs(diffY) && abs(diffX) > 50) {
                    if (!isScrolling) {
                        // Start new scroll session
                        isScrolling = true
                        scrollStartX = e1.x
                        totalScrolledDistance = 0f
                        accumulatedSeekTime = 0
                        lastSwipeDirection = 0
                    }
                    
                    // Calculate seek amount based on total scroll distance from start
                    val direction = if (diffX > 0) 1 else -1
                    totalScrolledDistance = abs(diffX)
                    
                    // Every 100 pixels = 10 seconds (more responsive than 150)
                    val seekSeconds = (totalScrolledDistance / 100f).toInt()
                    val targetSeekTime = seekSeconds * 10000 * direction
                    
                    // Only update if we've reached a new 10-second increment
                    if (targetSeekTime != accumulatedSeekTime) {
                        // Check if direction changed
                        if (direction != lastSwipeDirection && lastSwipeDirection != 0) {
                            // Direction changed, start fresh
                            accumulatedSeekTime = 0
                            totalScrolledDistance = 0f
                        }
                        
                        // Calculate the increment since last update
                        val increment = targetSeekTime - accumulatedSeekTime
                        accumulatedSeekTime = targetSeekTime
                        lastSwipeDirection = direction
                        lastSwipeTime = System.currentTimeMillis()
                        
                        // Seek the video
                        seekRelative(increment, false)
                        
                        // Show visual feedback
                        showSeekFeedback(accumulatedSeekTime)
                        
                        // Reset accumulated seek after delay
                        handler.removeCallbacks(resetAccumulatedSeekRunnable)
                        handler.postDelayed(resetAccumulatedSeekRunnable, swipeResetDelay)
                    }
                    
                    return true
                }
                return false
            }
            
            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Reset scrolling flag on fling
                isScrolling = false
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isLocked) {
                    // When locked, only show/hide the lock indicator
                    lockedIndicator.visibility = if (lockedIndicator.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                } else {
                    // Toggle controls visibility
                    // Hide settings dropdown if visible
                    if (settingsDropdown.visibility == View.VISIBLE) {
                        settingsDropdown.visibility = View.GONE
                    }
                    toggleControls()
                }
                return true
            }
        })
    }

    private fun setupVideoView() {
        val uri = Uri.parse(videoPath)
        videoView.setVideoURI(uri)
        
        videoView.setOnPreparedListener { mediaPlayer ->
            // Disable looping - video ends and closes player
            mediaPlayer.isLooping = false
            
            // Set seekbar max
            val duration = videoView.duration
            seekBar.max = duration
            totalTimeText.text = formatTime(duration)

            // Remember the video's shape so the PiP window matches it
            if (mediaPlayer.videoWidth > 0 && mediaPlayer.videoHeight > 0) {
                videoAspect = clampAspect(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
                videoView.setContentSize(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
            }
            updatePipParams()

            // Check for saved position and show resume dialog if needed
            val savedPosition = getSavedPosition()
            if (savedPosition > MIN_RESUME_POSITION && savedPosition < duration - END_MARGIN_MS) {
                showResumeDialog(savedPosition)
            } else {
                // Start playback normally
                mediaPlayer.start()
                isPlaying = true
                updatePlayPauseButton()
            }
            
            // Start updating seekbar
            handler.post(updateSeekBarRunnable)
        }

        videoView.setOnCompletionListener {
            // Clear saved position when video completes
            clearSavedPosition()
            
            if (playlist != null && currentVideoIndex < playlist!!.size - 1) {
                playNextVideo()
            } else {
                finish()
            }
        }

        videoView.setOnErrorListener { _, _, _ ->
            // Usually an unsupported codec/profile (e.g. 10-bit H.264): say so instead of just closing
            Toast.makeText(this, "Can't play this video (unsupported format?)", Toast.LENGTH_LONG).show()
            finish()
            true
        }
    }
    
    private fun updateSeekBar() {
        if (videoView.duration > 0) {
            val current = videoView.currentPosition
            lastKnownPosition = current
            seekBar.progress = current
            currentTimeText.text = formatTime(current)
        }
    }
    
    private fun formatTime(milliseconds: Int): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun loadSubtitles() {
        subtitleOffsetMs = getSavedSubtitleOffset()
        updateSubDelayLabel()
        videoPath?.let { path ->
            val videoFile = File(path)
            val subtitlePath = path.substring(0, path.lastIndexOf('.')) + ".srt"
            val subtitleFile = File(subtitlePath)
            
            if (subtitleFile.exists()) {
                try {
                    val content = subtitleFile.readText()
                    subtitles = parseSubtitles(content)
                    if (subtitles.isNotEmpty()) {
                        subtitleText.visibility = View.VISIBLE
                        currentSubtitlePath = subtitlePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            videoView.pause()
            isPlaying = false
        } else {
            videoView.start()
            isPlaying = true
        }
        updatePlayPauseButton()
        showControls()
    }
    
    private fun updatePlayPauseButton() {
        val iconRes = if (!isPlaying) R.drawable.ic_pp_play else R.drawable.ic_pp_pause
        btnPlayPause.setImageResource(iconRes)
        btnCenterPlayPause.setImageResource(iconRes)
        // Keep the PiP play/pause action and auto-enter flag in sync
        updatePipParams()
    }

    private fun seekRelative(milliseconds: Int, showFeedback: Boolean = false) {
        val currentPosition = videoView.currentPosition
        val duration = videoView.duration
        
        // Only seek if both duration and position are available
        if (duration > 0 && currentPosition >= 0) {
            val newPosition = (currentPosition + milliseconds).coerceIn(0, duration)
            videoView.seekTo(newPosition)
            
            if (showFeedback) {
                showSeekFeedback(milliseconds)
            }
        }
    }
    
    private fun showSeekFeedback(milliseconds: Int) {
        val seconds = milliseconds / 1000
        showFeedbackText(if (seconds > 0) "+${seconds}s" else "${seconds}s")
    }
    
    private fun showFeedbackText(text: String) {
        seekFeedback.text = text
        seekFeedback.visibility = View.VISIBLE
        
        // Fade out animation
        val fadeOut = AlphaAnimation(1.0f, 0.0f)
        fadeOut.duration = 800
        fadeOut.startOffset = 400
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                seekFeedback.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animation?) {}
        })
        seekFeedback.startAnimation(fadeOut)
    }
    
    private fun playPreviousVideo() {
        if (playlist != null && currentVideoIndex > 0) {
            currentVideoIndex--
            loadVideoAtIndex(currentVideoIndex)
        }
    }
    
    private fun playNextVideo() {
        if (playlist != null && currentVideoIndex < playlist!!.size - 1) {
            currentVideoIndex++
            loadVideoAtIndex(currentVideoIndex)
        }
    }
    
    private fun loadVideoAtIndex(index: Int) {
        playlist?.get(index)?.let { video ->
            // Remember where we were in the video we are leaving
            savePositionIfMeaningful()
            videoPath = video.path
            videoTitle.text = video.name
            
            // Stop current playback
            videoView.stopPlayback()
            handler.removeCallbacks(updateSeekBarRunnable)
            
            // Clear subtitles for new video
            subtitles = emptyList()
            subtitleText.visibility = View.GONE
            
            // Load new video
            setupVideoView()
            loadSubtitles()
            
            // Update navigation buttons
            updateNavigationButtons()
        }
    }
    
    private fun toggleOrientation() {
        requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    
    private fun toggleControls() {
        if (controlsOverlay.visibility == View.VISIBLE) {
            hideControls()
        } else {
            showControls()
        }
    }
    
    private fun showControls() {
        // The PiP window is too small for the overlay; it has its own system controls
        if (isInPip()) return
        controlsOverlay.visibility = View.VISIBLE
        scheduleHideControls()
    }
    
    private fun hideControls() {
        controlsOverlay.visibility = View.GONE
        settingsDropdown.visibility = View.GONE
        handler.removeCallbacks(hideControlsRunnable)
    }
    
    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 3000)
    }
    
    private fun toggleLock() {
        isLocked = !isLocked
        
        if (isLocked) {
            // Hide controls and show lock indicator
            hideControls()
            lockedIndicator.visibility = View.VISIBLE
        } else {
            // Show controls and hide lock indicator
            lockedIndicator.visibility = View.GONE
            showControls()
        }
    }

    private fun toggleSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            toggleSystemUIModern()
        } else {
            toggleSystemUILegacy()
        }
    }

    @Suppress("DEPRECATION")
    private fun toggleSystemUILegacy() {
        val decorView = window.decorView
        val currentVisibility = decorView.systemUiVisibility
        
        if (currentVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
            // Hide system UI
            decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            // Show system UI
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun toggleSystemUIModern() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                val systemBars = android.view.WindowInsets.Type.systemBars()
                if (window.decorView.rootWindowInsets?.isVisible(systemBars) == true) {
                    controller.hide(systemBars)
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(systemBars)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Reset scrolling flag when touch is released
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isScrolling = false
        }
        
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onPause() {
        super.onPause()
        // Save current position and playing state (crucial for Bluetooth disconnect scenarios)
        if (savePositionIfMeaningful()) {
            saveWasPlaying(isPlaying)
        }
        
        // In picture-in-picture the activity is paused but still on screen: keep playing
        val inPip = pipActive || isInPip()
        if (isPlaying && !inPip) {
            videoView.pause()
        }
        if (!inPip) {
            handler.removeCallbacks(updateSeekBarRunnable)
        }
        handler.removeCallbacks(hideControlsRunnable)
    }

    override fun onResume() {
        super.onResume()
        suppressAutoPip = false
        if (!isInPip()) {
            pipActive = false
        }
        if (isPlaying) {
            videoView.start()
        }
        // Remove first so a loop kept alive during PiP isn't doubled up
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.post(updateSeekBarRunnable)
    }

    override fun onStop() {
        super.onStop()
        if (pipActive) {
            // Stopped while in PiP: either the PiP window was closed or the screen turned off.
            // The video kept playing after onPause saved its position, so save the latest one.
            pipActive = false
            savePositionIfMeaningful(lastKnownPosition, seekBar.max)
            videoView.pause()
            isPlaying = false
            updatePlayPauseButton()
            if (!isInPip()) {
                // PiP window was dismissed: close the player; the saved position is kept
                finish()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 12+ enters PiP by itself via setAutoEnterEnabled; Android 8-11 need this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            !suppressAutoPip &&
            videoView.isPlaying
        ) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            pipActive = true
            videoView.mode = if (pipWide) ScalableVideoView.Mode.CROP else ScalableVideoView.Mode.FIT
            hideControls()
            // With auto-enter (Android 12+) onPause runs before we know we are in PiP and
            // stops the update loop; restart it so subtitles and the position keep moving
            handler.removeCallbacks(updateSeekBarRunnable)
            handler.post(updateSeekBarRunnable)
            lockedIndicator.visibility = View.GONE
            seekFeedback.visibility = View.GONE
            applyPipSubtitleStyle(true)
            registerPipReceiver()
            // onPause may have paused playback while the system moved us into PiP
            if (isPlaying && !videoView.isPlaying) {
                videoView.start()
            }
        } else {
            unregisterPipReceiver()
            videoView.mode = aspectMode
            applyPipSubtitleStyle(false)
            if (isLocked) {
                lockedIndicator.visibility = View.VISIBLE
            }
        }
    }

    // ---- Picture-in-picture helpers ----

    private fun supportsPip(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun isInPip(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!supportsPip() || isInPip()) return
        hideControls()
        try {
            pipActive = enterPictureInPictureMode(buildPipParams())
        } catch (e: IllegalStateException) {
            pipActive = false
        } catch (e: IllegalArgumentException) {
            pipActive = false
        }
    }

    private fun clampAspect(width: Int, height: Int): Rational {
        val ratio = width.toFloat() / height.toFloat()
        return when {
            ratio > PIP_MAX_RATIO -> Rational(238, 100)
            ratio < 1f / PIP_MAX_RATIO -> Rational(100, 238)
            else -> Rational(width, height)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(pipAspectRatio())
            .setActions(buildPipActions())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Only slide into PiP on Home gesture while something is actually playing
            builder.setAutoEnterEnabled(videoView.isPlaying)
        }
        return builder.build()
    }

    // Android sizes the PiP window from its aspect ratio (the app cannot set a size), and a wider
    // ratio gives a wider window. In wide mode ask for the widest ratio Android accepts and crop
    // the video to fill it, trading some of the top and bottom of the picture for window size.
    private fun pipAspectRatio(): Rational = if (pipWide) Rational(238, 100) else videoAspect

    private fun togglePipWide() {
        pipWide = !pipWide
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PIP_WIDE, pipWide).apply()
        videoView.mode = if (pipWide) ScalableVideoView.Mode.CROP else ScalableVideoView.Mode.FIT
        updatePipParams()
    }

    private fun updatePipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!supportsPip()) return
        try {
            setPictureInPictureParams(buildPipParams())
        } catch (e: IllegalStateException) {
            // Activity not in a state that accepts PiP params; safe to ignore
        } catch (e: IllegalArgumentException) {
            // Invalid aspect ratio; keep the previous params
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipActions(): List<RemoteAction> {
        val playing = videoView.isPlaying
        val actions = mutableListOf(
            pipAction(R.drawable.ic_pp_rewind, "Rewind 10s", PIP_REWIND),
            pipAction(
                if (playing) R.drawable.ic_pp_pause else R.drawable.ic_pp_play,
                if (playing) "Pause" else "Play",
                PIP_PLAY_PAUSE
            ),
            pipAction(R.drawable.ic_pp_fast_forward, "Forward 10s", PIP_FORWARD),
            pipAction(R.drawable.ic_pp_aspect, if (pipWide) "Normal size" else "Wide size", PIP_SIZE)
        )
        // Phones usually allow only 3 actions: drop "forward" first so the size button fits
        val max = getMaxNumPictureInPictureActions()
        if (actions.size > max && actions.size > 2) {
            actions.removeAt(2)
        }
        while (actions.size > max && actions.isNotEmpty()) {
            actions.removeAt(actions.size - 1)
        }
        return actions
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun pipAction(iconRes: Int, title: String, control: Int): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL)
            .setPackage(packageName)
            .putExtra(EXTRA_PIP_CONTROL, control)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            control,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return RemoteAction(Icon.createWithResource(this, iconRes), title, title, pendingIntent)
    }

    private fun registerPipReceiver() {
        if (pipReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val received = intent ?: return
                if (received.action != ACTION_PIP_CONTROL) return
                when (received.getIntExtra(EXTRA_PIP_CONTROL, -1)) {
                    PIP_REWIND -> seekRelative(-10000)
                    PIP_FORWARD -> seekRelative(10000)
                    PIP_PLAY_PAUSE -> togglePlayPause()
                    PIP_SIZE -> togglePipWide()
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        pipReceiver = receiver
    }

    private fun unregisterPipReceiver() {
        pipReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered
            }
        }
        pipReceiver = null
    }

    // Subtitles are sized for a full screen; shrink them so they fit a PiP window
    private fun applyPipSubtitleStyle(inPip: Boolean) {
        applySubtitleSize(inPip)
        val params = subtitleText.layoutParams as? RelativeLayout.LayoutParams ?: return
        val marginDp = if (inPip) 4 else 80
        params.bottomMargin = (marginDp * resources.displayMetrics.density).toInt()
        subtitleText.layoutParams = params
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save state when system kills the activity (e.g., Bluetooth, low memory)
        outState.putString(STATE_VIDEO_PATH, videoPath)
        outState.putInt(STATE_VIDEO_POSITION, videoView.currentPosition)
        outState.putBoolean(STATE_WAS_PLAYING, isPlaying)
    }
    
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore state when activity is recreated by system
        // This ensures video doesn't restart after Bluetooth or other system events
        val savedPath = savedInstanceState.getString(STATE_VIDEO_PATH)
        if (savedPath != null && savedPath == videoPath) {
            // Same video, restore position
            val savedPos = savedInstanceState.getInt(STATE_VIDEO_POSITION, 0)
            val wasPlaying = savedInstanceState.getBoolean(STATE_WAS_PLAYING, true)
            
            // Save to preferences so setupVideoView can restore
            savePosition(savedPos)
            saveWasPlaying(wasPlaying)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // The saved position is kept on every exit (back, PiP closed, app killed).
        // It is only cleared when the video finishes, see setOnCompletionListener.
        unregisterPipReceiver()
        videoView.stopPlayback()
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        handler.removeCallbacks(resetAccumulatedSeekRunnable)
    }
    
    // ---- Fullscreen ----

    private fun enterImmersiveMode() {
        // FLAG_FULLSCREEN only hides the status bar; hide the navigation bar too and let
        // it come back transiently with an edge swipe.
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Dialogs and the system bars swipe-in take the immersive state away; restore it
        if (hasFocus) {
            enterImmersiveMode()
        }
    }

    // ---- Aspect mode ----

    private fun cycleAspectMode() {
        aspectMode = when (aspectMode) {
            ScalableVideoView.Mode.FIT -> ScalableVideoView.Mode.CROP
            ScalableVideoView.Mode.CROP -> ScalableVideoView.Mode.STRETCH
            ScalableVideoView.Mode.STRETCH -> ScalableVideoView.Mode.FIT
        }
        videoView.mode = aspectMode
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ASPECT_MODE, aspectMode.name).apply()
        showFeedbackText(
            when (aspectMode) {
                ScalableVideoView.Mode.FIT -> "Fit"
                ScalableVideoView.Mode.CROP -> "Crop"
                ScalableVideoView.Mode.STRETCH -> "Stretch"
            }
        )
        scheduleHideControls()
    }

    private fun loadAspectMode(): ScalableVideoView.Mode {
        val name = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ASPECT_MODE, null)
        return ScalableVideoView.Mode.values().firstOrNull { it.name == name }
            ?: ScalableVideoView.Mode.FIT
    }

    // ---- Subtitle delay ----

    private fun setSubtitleDelay(newOffsetMs: Long) {
        subtitleOffsetMs = newOffsetMs
        videoPath?.let { path ->
            val editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            if (newOffsetMs == 0L) {
                editor.remove(KEY_SUB_OFFSET + path)
            } else {
                editor.putLong(KEY_SUB_OFFSET + path, newOffsetMs)
            }
            editor.apply()
        }
        updateSubDelayLabel()
        updateSubtitle()
    }

    private fun getSavedSubtitleOffset(): Long {
        return videoPath?.let { path ->
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_SUB_OFFSET + path, 0L)
        } ?: 0L
    }

    private fun updateSubDelayLabel() {
        val seconds = subtitleOffsetMs / 1000.0
        subDelayLabel.text = if (subtitleOffsetMs == 0L) {
            "Delay 0.0s"
        } else {
            String.format(Locale.US, "Delay %+.1fs", seconds)
        }
    }

    // ---- Subtitle size ----

    // Sized in dp, not sp, so it does not follow the system-wide font size setting
    private fun applySubtitleSize(inPip: Boolean) {
        val size = if (inPip) {
            maxOf(PIP_SUB_SIZE_MIN, subtitleSizeDp * PIP_SUB_SIZE_FACTOR)
        } else {
            subtitleSizeDp.toFloat()
        }
        subtitleText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    }

    private fun setSubtitleSize(newSizeDp: Int) {
        subtitleSizeDp = newSizeDp.coerceIn(SUB_SIZE_MIN, SUB_SIZE_MAX)
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SUB_SIZE, subtitleSizeDp).apply()
        applySubtitleSize(isInPip())
        updateSubSizeLabel()
        showSubtitlePreview()
    }

    private fun loadSubtitleSize(): Int {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SUB_SIZE, SUB_SIZE_DEFAULT)
            .coerceIn(SUB_SIZE_MIN, SUB_SIZE_MAX)
    }

    private fun updateSubSizeLabel() {
        subSizeLabel.text = "Size $subtitleSizeDp"
    }

    // Show sample text while adjusting, unless a real subtitle is already on screen
    private fun showSubtitlePreview() {
        val previewActive = SystemClock.elapsedRealtime() < subtitlePreviewUntil
        if (subtitleText.visibility != View.VISIBLE || previewActive) {
            subtitleText.text = "Subtitle size preview"
            subtitleText.visibility = View.VISIBLE
            subtitlePreviewUntil = SystemClock.elapsedRealtime() + SUB_PREVIEW_MS
        }
    }

    // ---- Resume position ----

    /**
     * Saves the playback position unless it is too close to the start to matter.
     * Near the end the video counts as finished, so the saved position is cleared.
     * Returns true if a position was saved.
     */
    private fun savePositionIfMeaningful(
        position: Int = videoView.currentPosition,
        duration: Int = videoView.duration
    ): Boolean {
        if (duration <= 0 || position < 0) return false
        return when {
            position >= duration - END_MARGIN_MS -> {
                clearSavedPosition()
                false
            }
            position > MIN_RESUME_POSITION -> {
                savePosition(position)
                true
            }
            else -> false
        }
    }

    // Also save while playing, so a crash or a killed app still resumes close to where it was
    private fun savePositionPeriodically() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPeriodicSave < PERIODIC_SAVE_INTERVAL) return
        lastPeriodicSave = now
        if (videoView.isPlaying) {
            savePositionIfMeaningful()
        }
    }

    // Helper methods for saving/loading position
    private fun savePosition(position: Int) {
        videoPath?.let { path ->
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt(KEY_VIDEO_POSITION + path, position)
            editor.putLong(KEY_TIMESTAMP + path, System.currentTimeMillis())
            editor.apply()
            
            // Update recent videos list
            updateRecentVideos()
        }
    }
    
    private fun getSavedPosition(): Int {
        return videoPath?.let { path ->
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getInt(KEY_VIDEO_POSITION + path, 0)
        } ?: 0
    }
    
    private fun clearSavedPosition() {
        videoPath?.let { path ->
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(KEY_VIDEO_POSITION + path)
            editor.remove(KEY_WAS_PLAYING + path)
            editor.remove(KEY_TIMESTAMP + path)
            editor.apply()
        }
    }
    
    private fun saveWasPlaying(wasPlaying: Boolean) {
        videoPath?.let { path ->
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_WAS_PLAYING + path, wasPlaying).apply()
        }
    }
    
    private fun getWasPlaying(): Boolean {
        return videoPath?.let { path ->
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_WAS_PLAYING + path, true) // Default to true (auto-play)
        } ?: true
    }
    
    private fun showResumeDialog(savedPosition: Int) {
        val formattedPosition = formatTime(savedPosition)
        
        AlertDialog.Builder(this)
            .setTitle("Resume Playback")
            .setMessage("Do you want to resume from where you left off ($formattedPosition)?")
            .setPositiveButton("Resume") { dialog, _ ->
                videoView.seekTo(savedPosition)
                videoView.start()
                isPlaying = true
                updatePlayPauseButton()
                dialog.dismiss()
            }
            .setNegativeButton("Start from Beginning") { dialog, _ ->
                videoView.seekTo(0)
                videoView.start()
                isPlaying = true
                updatePlayPauseButton()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun updateRecentVideos() {
        videoPath?.let { path ->
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Get current recent videos list
            val recentVideosString = prefs.getString(KEY_RECENT_VIDEOS, "") ?: ""
            val recentVideos = if (recentVideosString.isEmpty()) {
                mutableListOf<String>()
            } else {
                recentVideosString.split("|").toMutableList()
            }
            
            // Remove current video if already in list
            recentVideos.remove(path)
            
            // Add to front of list
            recentVideos.add(0, path)
            
            // Keep only last MAX_RECENT_VIDEOS
            while (recentVideos.size > MAX_RECENT_VIDEOS) {
                val oldestVideo = recentVideos.removeAt(recentVideos.size - 1)
                // Clean up old video data
                editor.remove(KEY_VIDEO_POSITION + oldestVideo)
                editor.remove(KEY_WAS_PLAYING + oldestVideo)
                editor.remove(KEY_TIMESTAMP + oldestVideo)
                editor.remove(KEY_SUB_OFFSET + oldestVideo)
            }
            
            // Save updated list
            editor.putString(KEY_RECENT_VIDEOS, recentVideos.joinToString("|"))
            editor.apply()
        }
    }
}