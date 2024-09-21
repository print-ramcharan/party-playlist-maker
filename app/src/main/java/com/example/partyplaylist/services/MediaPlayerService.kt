package com.example.partyplaylist.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.partyplaylist.HomePageActivity
import com.example.partyplaylist.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player

@Suppress("DEPRECATION")
class MediaPlayerService : Service() {
    private var exoPlayer: ExoPlayer? = null
    private val binder: IBinder = LocalBinder()
    private var currentSongUrl: String? = null
    private var currentSongTitle: String = ""
    private var isPlaying: Boolean = false
    private lateinit var mediaSession: MediaSessionCompat

    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBar = object : Runnable {
        override fun run() {
            exoPlayer?.let {
                val position = it.currentPosition
                val duration = it.duration
                val progress = if (duration > 0) (position * 100 / duration).toInt() else 0
                sendBroadcast(Intent("UPDATE_SEEKBAR").apply {
                    putExtra("progress", progress)
                    putExtra("duration", duration)
                })
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind: Service bound")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "MediaPlayerService")
        mediaSession.isActive = true

        exoPlayer!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "onPlaybackStateChanged: Playback ended")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "onPlayerError: Playback error: ${error.message}")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@MediaPlayerService.isPlaying = isPlaying
                showNotification(currentSongTitle, getProgress(), getDuration())
            }
        })

        Log.d(TAG, "onCreate: MediaPlayerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.action
            Log.d(TAG, "onStartCommand: Action received: $action")
            when (action) {
                "PLAY" -> {
                    val url = it.getStringExtra("SONG_URL")
                    val title = it.getStringExtra("SONG_TITLE") ?: ""
                    play(url, title)
                }
                "PAUSE" -> pause()
                "STOP" -> stop()
                "NEXT" -> next()
                "SEEK_TO" -> {
                    val position = it.getLongExtra("POSITION", 0L)
                    seekTo(position)
                }
            }
        }
        return START_NOT_STICKY
    }

    fun play(url: String?, title: String) {
        if (url != null && url != currentSongUrl) {
            exoPlayer?.setMediaItem(MediaItem.fromUri(url))
            exoPlayer?.prepare()
            exoPlayer?.play()
            currentSongUrl = url
            currentSongTitle = title
            sendSongTitleUpdateBroadcast(title)
            handler.post(updateSeekBar)
        } else {
            exoPlayer?.play()
            handler.post(updateSeekBar)
        }
        showNotification(title, getProgress(), getDuration())
    }

    fun stop() {
        exoPlayer?.stop()
        handler.removeCallbacks(updateSeekBar)
        showNotification("Stopped", 0, 0)
    }

    fun pause() {
        Log.d(TAG, "pause: Pausing playback")
        exoPlayer?.pause()
        handler.removeCallbacks(updateSeekBar)
        showNotification("Paused", getProgress(), getDuration())
    }

    fun next() {
        Log.d(TAG, "next: Playing next song")
        // Implement the logic to play the next song
    }
    fun prev() {
        Log.d(TAG, "next: Playing prev song")
        // Implement the logic to play the next song
    }

    fun seekTo(position: Long) {
        Log.d(TAG, "seekTo: Seeking to position $position")
        exoPlayer?.seekTo(position)
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    private fun getProgress(): Int {
        val position = exoPlayer?.currentPosition ?: 0L
        val duration = exoPlayer?.duration ?: 0L
        return if (duration > 0) (position * 100 / duration).toInt() else 0
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        handler.removeCallbacks(updateSeekBar)
        stopForeground(true)
        Log.d(TAG, "onDestroy: MediaPlayerService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MediaPlayer Channel"
            val description = "Channel for media player notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(title: String, progress: Int, duration: Long) {
        // Create intents for the actions
        val playIntent = Intent(this, MediaPlayerService::class.java).apply {
            action = "PLAY"
        }
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val pauseIntent = Intent(this, MediaPlayerService::class.java).apply {
            action = "PAUSE"
        }
        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, MediaPlayerService::class.java).apply {
            action = "NEXT"
        }
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Build the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText("Duration: ${formatDuration(duration)}")
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_music_note)) // Default image
            .addAction(R.drawable.ic_play_arrow, "Play", playPendingIntent)
            .addAction(R.drawable.pause, "Pause", pausePendingIntent)
            .addAction(R.drawable.next, "Next", nextPendingIntent)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, HomePageActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setSilent(true)
            .build()

        // Start the foreground service with the notification
        startForeground(1, notification)
    }

    private fun formatDuration(durationMillis: Long): String {
        val minutes = (durationMillis / 1000 / 60).toInt()
        val seconds = (durationMillis / 1000 % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }

    inner class LocalBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    private fun sendSongTitleUpdateBroadcast(title: String) {
        val intent = Intent("SONG_TITLE_UPDATE").apply {
            putExtra("title", title)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "MediaPlayerService"
        private const val CHANNEL_ID = "MediaPlayerChannel"
    }
}
