package code.name.monkey.retromusic.service

import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.preference.PreferenceManager
import code.name.monkey.retromusic.appwidgets.AppWidgetBig
import code.name.monkey.retromusic.appwidgets.AppWidgetCard
import code.name.monkey.retromusic.appwidgets.AppWidgetCircle
import code.name.monkey.retromusic.appwidgets.AppWidgetClassic
import code.name.monkey.retromusic.appwidgets.AppWidgetMD3
import code.name.monkey.retromusic.appwidgets.AppWidgetSmall
import code.name.monkey.retromusic.appwidgets.AppWidgetText
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.model.Song.Companion.emptySong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MusicService : MediaSessionService() {
    private lateinit var playbackManager: PlaybackManager

    private val serviceScope = CoroutineScope(Job() + Main)

    @JvmField
    var playingQueue = ArrayList<Song>()

    @JvmField
    var position = -1

    @JvmField
    var shuffleMode = 0

    @JvmField
    var pendingQuit = false

    var repeatMode = 0
        private set(value) {
            when (value) {
                REPEAT_MODE_NONE, REPEAT_MODE_ALL, REPEAT_MODE_THIS -> {
                    field = value
                    PreferenceManager.getDefaultSharedPreferences(this).edit {
                        putInt(SAVED_REPEAT_MODE, value)
                    }
                    handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
                }
            }
        }

    private val appWidgetBig = AppWidgetBig.instance
    private val appWidgetCard = AppWidgetCard.instance
    private val appWidgetClassic = AppWidgetClassic.instance
    private val appWidgetSmall = AppWidgetSmall.instance
    private val appWidgetText = AppWidgetText.instance
    private val appWidgetMd3 = AppWidgetMD3.instance
    private val appWidgetCircle = AppWidgetCircle.instance

    override fun onCreate() {
        super.onCreate()

        instance = this

        playbackManager = PlaybackManager(this)
        playbackManager.create()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return playbackManager.onGetSession(controllerInfo)
    }

    val audioSessionId: String
        get() = playbackManager.audioSessionId

    override fun onDestroy() {
        playbackManager.release()
        super.onDestroy()
    }

    fun pause() {
        playbackManager.pause()
    }

    fun play() {
        playbackManager.start()
    }

    fun seek(millis: Int) {
        playbackManager.seek(position, millis)
    }

    fun back(force: Boolean) {
        if (songProgressMillis > 2000) {
            playbackManager.playSongAt(position)
        } else {
            playPreviousSong(force)
        }
    }

    fun quit() {
        pause()
        // TODO:
    }


    val songDurationMillis: Int
        get() = 0 // TODO:

    val songProgressMillis: Int
        get() = 0 // TODO:

    fun getQueueDurationMillis(position: Int): Long {
        var duration: Long = 0
        for (i in position + 1 until playingQueue.size) {
            duration += playingQueue[i].duration
        }
        return duration
    }

    val isPlaying: Boolean
        get() = playbackManager.isPlaying

    val currentSong: Song
        get() = getSongAt(position)

    private fun getSongAt(position: Int): Song {
        return if ((position >= 0) && (position < playingQueue.size)) {
            playingQueue[position]
        } else {
            emptySong
        }
    }

    fun playNextSong(force: Boolean) {
        playSongAt(getNextPosition(force))
    }

    fun playPreviousSong(force: Boolean) {
        playSongAt(getPreviousPosition(force))
    }

    fun playSongAt(position: Int) {
        playbackManager.playSongAt(position)
    }

    private val isLastTrack: Boolean
        get() = position == playingQueue.size - 1

    val nextSong: Song?
        get() = if (isLastTrack && repeatMode == REPEAT_MODE_NONE) {
            null
        } else {
            getSongAt(getNextPosition(false))
        }

    private fun getNextPosition(force: Boolean): Int {
        var position = position + 1
        when (repeatMode) {
            REPEAT_MODE_ALL -> if (isLastTrack) {
                position = 0
            }

            REPEAT_MODE_THIS -> if (force) {
                if (isLastTrack) {
                    position = 0
                }
            } else {
                position -= 1
            }

            REPEAT_MODE_NONE -> if (isLastTrack) {
                position -= 1
            }

            else -> if (isLastTrack) {
                position -= 1
            }
        }
        return position
    }

    private fun getPreviousPosition(force: Boolean): Int {
        var newPosition = position - 1
        when (repeatMode) {
            REPEAT_MODE_ALL -> if (newPosition < 0) {
                newPosition = playingQueue.size - 1
            }

            REPEAT_MODE_THIS -> if (force) {
                if (newPosition < 0) {
                    newPosition = playingQueue.size - 1
                }
            } else {
                newPosition = position
            }

            REPEAT_MODE_NONE -> if (newPosition < 0) {
                newPosition = 0
            }

            else -> if (newPosition < 0) {
                newPosition = 0
            }
        }
        return newPosition
    }

    fun handleAndSendChangeInternal(what: String) {
        handleChangeInternal(what)
        sendChangeInternal(what)
    }

    private fun handleChangeInternal(what: String) {
        // TODO:
    }

    private fun sendChangeInternal(what: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(what))
        appWidgetBig.notifyChange(this, what)
        appWidgetClassic.notifyChange(this, what)
        appWidgetSmall.notifyChange(this, what)
        appWidgetCard.notifyChange(this, what)
        appWidgetText.notifyChange(this, what)
        appWidgetMd3.notifyChange(this, what)
        appWidgetCircle.notifyChange(this, what)
    }

    private var uiThreadHandler: Handler = Handler(Looper.getMainLooper())
    fun runOnUiThread(runnable: Runnable) {
        uiThreadHandler.post(runnable)
    }

    fun openQueue(queue: List<Song>, startPosition: Int, startPlaying: Boolean) {
        playbackManager.setPlayingQueue(queue, startPosition) {
            if (startPlaying) {
                playbackManager.start()
            }
        }
    }

    fun restoreState(completion: () -> Unit = {}) {
        shuffleMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(
            SAVED_SHUFFLE_MODE, 0
        )
        repeatMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(
            SAVED_REPEAT_MODE, 0
        )
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
        handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
        serviceScope.launch {
            restoreQueuesAndPositionIfNecessary()
            completion()
        }
    }

    private suspend fun restoreQueuesAndPositionIfNecessary() {
        // TODO
    }

    companion object {
        private var instance: MusicService? = null
        fun getInstance(): MusicService? = instance

        val TAG: String = MusicService::class.java.simpleName
        const val RETRO_MUSIC_PACKAGE_NAME = "code.name.monkey.retromusic"
        const val MUSIC_PACKAGE_NAME = "com.android.music"
        const val ACTION_TOGGLE_PAUSE = "$RETRO_MUSIC_PACKAGE_NAME.togglepause"
        const val ACTION_PLAY = "$RETRO_MUSIC_PACKAGE_NAME.play"
        const val ACTION_PLAY_PLAYLIST = "$RETRO_MUSIC_PACKAGE_NAME.play.playlist"
        const val ACTION_PAUSE = "$RETRO_MUSIC_PACKAGE_NAME.pause"
        const val ACTION_STOP = "$RETRO_MUSIC_PACKAGE_NAME.stop"
        const val ACTION_SKIP = "$RETRO_MUSIC_PACKAGE_NAME.skip"
        const val ACTION_REWIND = "$RETRO_MUSIC_PACKAGE_NAME.rewind"
        const val ACTION_QUIT = "$RETRO_MUSIC_PACKAGE_NAME.quitservice"
        const val ACTION_PENDING_QUIT = "$RETRO_MUSIC_PACKAGE_NAME.pendingquitservice"
        const val INTENT_EXTRA_PLAYLIST = RETRO_MUSIC_PACKAGE_NAME + "intentextra.playlist"
        const val INTENT_EXTRA_SHUFFLE_MODE =
            "$RETRO_MUSIC_PACKAGE_NAME.intentextra.shufflemode"
        const val APP_WIDGET_UPDATE = "$RETRO_MUSIC_PACKAGE_NAME.appwidgetupdate"
        const val EXTRA_APP_WIDGET_NAME = RETRO_MUSIC_PACKAGE_NAME + "app_widget_name"

        // Do not change these three strings as it will break support with other apps (e.g. last.fm
        // scrobbling)
        const val META_CHANGED = "$RETRO_MUSIC_PACKAGE_NAME.metachanged"
        const val QUEUE_CHANGED = "$RETRO_MUSIC_PACKAGE_NAME.queuechanged"
        const val PLAY_STATE_CHANGED = "$RETRO_MUSIC_PACKAGE_NAME.playstatechanged"
        const val FAVORITE_STATE_CHANGED = "$RETRO_MUSIC_PACKAGE_NAME.favoritestatechanged"
        const val REPEAT_MODE_CHANGED = "$RETRO_MUSIC_PACKAGE_NAME.repeatmodechanged"
        const val SHUFFLE_MODE_CHANGED = "$RETRO_MUSIC_PACKAGE_NAME.shufflemodechanged"
        const val MEDIA_STORE_CHANGED = "$RETRO_MUSIC_PACKAGE_NAME.mediastorechanged"
        const val CYCLE_REPEAT = "$RETRO_MUSIC_PACKAGE_NAME.cyclerepeat"
        const val TOGGLE_SHUFFLE = "$RETRO_MUSIC_PACKAGE_NAME.toggleshuffle"
        const val TOGGLE_FAVORITE = "$RETRO_MUSIC_PACKAGE_NAME.togglefavorite"
        const val SAVED_POSITION = "POSITION"
        const val SAVED_POSITION_IN_TRACK = "POSITION_IN_TRACK"
        const val SAVED_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val SAVED_REPEAT_MODE = "REPEAT_MODE"
        const val SHUFFLE_MODE_NONE = 0
        const val SHUFFLE_MODE_SHUFFLE = 1
        const val REPEAT_MODE_NONE = 0
        const val REPEAT_MODE_ALL = 1
        const val REPEAT_MODE_THIS = 2
        private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO)
    }
}