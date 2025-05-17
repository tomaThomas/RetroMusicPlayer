package code.name.monkey.retromusic.service

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.playback.Playback.PlaybackCallbacks
import code.name.monkey.retromusic.util.PreferenceUtil.playbackPitch
import code.name.monkey.retromusic.util.PreferenceUtil.playbackSpeed
import code.name.monkey.retromusic.util.logE
import java.util.ArrayList

class RetroExoPlayer(context: Context) : AudioManagerPlayback(context), Player.Listener {
    private var player: ExoPlayer = ExoPlayer.Builder(context).build()
    override var callbacks: PlaybackCallbacks? = null

    /**
     * @return True if the player is ready to go, false otherwise
     */
    override var isInitialized = false
        private set

    init {
        player.setWakeMode(C.WAKE_MODE_LOCAL)
    }

    override fun setPlayingQueue(
        playingQueue: List<Song>,
        position: Int,
        completion: (success: Boolean) -> Unit,
    ) {
        isInitialized = false
        val mediaItems = playingQueue.map { song -> MediaItem.fromUri(song.uri) }
        try {
            Handler(Looper.getMainLooper()).post {
                player.setMediaItems(mediaItems)
                player.seekTo(position, 0)
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                player.playbackParameters = PlaybackParameters(playbackSpeed, playbackPitch)

                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            player.removeListener(this)
                            isInitialized = true
                            completion(true)
                        }
                    }
                })
                player.addListener(this)
                player.prepare()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            completion(false)
        }
    }

    /**
     * Starts or resumes playback.
     */
    override fun start(): Boolean {
        super.start()
        return try {
            player.play()
            true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Resets the MediaPlayer to its uninitialized state.
     */
    override fun stop() {
        super.stop()
        player.stop()
        isInitialized = false
    }

    /**
     * Releases resources associated with this MediaPlayer object.
     */
    override fun release() {
        stop()
        player.release()
    }

    /**
     * Pauses playback. Call start() to resume.
     */
    override fun pause(): Boolean {
        super.pause()
        return try {
            player.pause()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * Checks whether ExoPlayer is playing.
     */
    override val isPlaying: Boolean
        get() = isInitialized && exoIsPlaying

    private var exoIsPlaying = false

    /**
     * Gets the duration of the file.
     *
     * @return The duration in milliseconds
     */
    override fun duration(): Int {
        return if (!this.isInitialized) {
            -1
        } else try {
            player.duration.toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Gets the current playback position.
     *
     * @return The current position in milliseconds
     */
    override fun position(): Int {
        return if (!this.isInitialized) {
            -1
        } else try {
            player.currentPosition.toInt()
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Gets the current playback position.
     *
     * @param whereto The offset in milliseconds from the start to seek to
     * @return The offset in milliseconds from the start to seek to
     */
    override fun seek(whereto: Int, force: Boolean): Int {
        return try {
            player.seekTo(whereto.toLong())
            whereto
        } catch (e: Exception) {
            -1
        }
    }

    override fun setVolume(vol: Float): Boolean {
        return try {
            player.volume = vol
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sets the audio session ID.
     *
     * @param sessionId The audio session ID
     */
    @OptIn(UnstableApi::class)
    override fun setAudioSessionId(sessionId: Int): Boolean {
        return try {
            player.audioSessionId = sessionId
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Returns the audio session ID.
     *
     * @return The current audio session ID.
     */
    override val audioSessionId: Int
        @OptIn(UnstableApi::class)
        get() = player.audioSessionId

    override fun onPlaybackStateChanged(state: Int) {
        if (state == Player.STATE_ENDED) {
            callbacks?.onTrackEnded()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        logE(error)
        isInitialized = false
        player.release()
        player = ExoPlayer.Builder(context).build()
        player.setWakeMode(C.WAKE_MODE_LOCAL)
        context.showToast(R.string.unplayable_file)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            callbacks?.onTrackWentToNext()
            return
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        super.onEvents(player, events)
        if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
            exoIsPlaying = player.isPlaying
            callbacks?.onPlayStateChanged()
        }
    }

    override fun setCrossFadeDuration(duration: Int) {}

    override fun setPlaybackSpeedPitch(speed: Float, pitch: Float) {
        player.playbackParameters = PlaybackParameters(speed, pitch)
    }

    fun setPosition(position: Int, completion: (success: Boolean) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            player.seekTo(position, 0)
            completion(true)
        }
    }

    companion object {
        val TAG: String = RetroExoPlayer::class.java.simpleName
    }
}