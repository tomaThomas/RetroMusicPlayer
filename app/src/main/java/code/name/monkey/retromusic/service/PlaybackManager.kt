package code.name.monkey.retromusic.service

import android.app.Notification
import android.app.Service.STOP_FOREGROUND_REMOVE
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.playback.ExoPlayback
import code.name.monkey.retromusic.service.playback.Playback

@OptIn(UnstableApi::class)
class PlaybackManager(val context: MediaSessionService) {
    var playback: Playback = ExoPlayback(context)

    private var notificationManager: PlayerNotificationManager? = null

    fun create() {
        playback.create()

        notificationManager = PlayerNotificationManager.Builder(
            context,
            1,
            "playing_notification"
        )
            .setMediaDescriptionAdapter(DescriptionAdapter(context))
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        context.startForeground(notificationId, notification)
                    }
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    context.stopForeground(STOP_FOREGROUND_REMOVE)
                    context.stopSelf()
                }
            })
            .build()

        notificationManager?.setPlayer(playback.getPlayer())
    }

    fun release() {
        playback.release()
    }

    fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return playback.onGetSession(controllerInfo)
    }

    val audioSessionId: String
        get() = playback.audioSessionId ?: ""

    fun setPlayingQueue(
        queue: List<Song>,
        position: Int,
        completion: (success: Boolean) -> Unit,
    ) {
        val songUris = queue.map { q -> q.uri }
        playback.setPlayingQueue(songUris, position, completion)
    }

    fun start() {
        playback.start()
    }

    fun pause() {
        playback.pause()
    }

    fun playSongAt(position: Int) {
        playback.playSongAt(position)
    }

    fun seek(position: Int, millis: Int) {
        playback.seek(position, millis.toLong())
    }

    val isPlaying: Boolean
        get() = false // TODO:
}