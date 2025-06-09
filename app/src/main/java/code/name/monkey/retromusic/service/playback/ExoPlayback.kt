package code.name.monkey.retromusic.service.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import code.name.monkey.retromusic.util.PreferenceUtil.playbackPitch
import code.name.monkey.retromusic.util.PreferenceUtil.playbackSpeed

class ExoPlayback(val context: Context) : Playback, Player.Listener {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer = ExoPlayer.Builder(context).build()

    override val audioSessionId: String?
        get() = mediaSession?.id

    override fun create() {
        player = ExoPlayer.Builder(context).build()
        mediaSession = MediaSession.Builder(context, player)
            .setId("retro_media_session")
            .build()
    }

    override fun getPlayer(): Player {
        return player
    }

    override fun setPlayingQueue(
        playingQueue: List<Uri>,
        position: Int,
        completion: (success: Boolean) -> Unit,
    ) {
        val mediaItems = playingQueue.map { uri -> MediaItem.fromUri(uri) }
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

    override fun start() {
        if (!player.isPlaying) player.play()
    }

    override fun playSongAt(position: Int) {
        seek(position, 0)
        start()
    }

    override fun stop() {
        player.stop()
    }

    override fun pause() {
        player.pause()
    }

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun seek(position: Int, millis: Long) {
        player.seekTo(position, millis)
    }

    override fun release() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}
