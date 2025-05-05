package code.name.monkey.retromusic.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.core.net.toUri
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.util.PreferenceUtil.playbackPitch
import code.name.monkey.retromusic.util.PreferenceUtil.playbackSpeed

abstract class LocalPlayback(context: Context) : AudioManagerPlayback(context), MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {
    /**
     * @param player The [MediaPlayer] to use
     * @param path The path of the file, or the http/rtsp URL of the stream you want to play
     * @return True if the <code>player</code> has been prepared and is ready to play, false otherwise
     */
    fun setDataSourceImpl(
        player: MediaPlayer,
        path: String,
        completion: (success: Boolean) -> Unit,
    ) {
        player.reset()
        try {
            if (path.startsWith("content://")) {
                player.setDataSource(context, path.toUri())
            } else {
                player.setDataSource(path)
            }
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            if (VersionUtils.hasMarshmallow())
                player.playbackParams =
                    PlaybackParams().setSpeed(playbackSpeed).setPitch(playbackPitch)

            player.setOnPreparedListener {
                player.setOnPreparedListener(null)
                completion(true)
            }
            player.prepare()
        } catch (e: Exception) {
            completion(false)
            e.printStackTrace()
        }
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }
}