package code.name.monkey.retromusic.service.playback

import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.session.MediaSession

interface Playback {
    val audioSessionId: String?

    fun create()

    fun getPlayer(): Player

    fun setPlayingQueue(
        playingQueue: List<Uri>,
        position: Int,
        completion: (success: Boolean) -> Unit,
    )

    fun release()

    fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession?

    fun start()

    fun playSongAt(position: Int)

    fun stop()

    fun pause()

    fun setVolume(volume: Float)

    fun seek(position: Int, millis: Long)
}
