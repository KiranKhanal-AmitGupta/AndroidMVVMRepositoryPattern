package com.outcodesoftware.customvideoplayer

import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.custom_video_player.view.*
import java.util.*

class CustomVideoPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnClickListener, View.OnTouchListener {
    private val layoutResId: Int = R.layout.custom_video_player

    lateinit var customPlayerListener: CustomPlayerListener

    lateinit var video: Video

    lateinit var playState: PlayState
    lateinit var playType: PlayType

    lateinit var screenShotImageUrl:String
    /**
     * [LOCAL] for videos in memory, [REMOTE] for videos in Server(Live Streaming)
     */

    lateinit var playLocation: PlayLocation

    val showHideControllerTime: Long = 5000L

    private var controllerHanlder: Handler = Handler()
    private var controllerRunnable: Runnable? = null
    private var progressBarHanlder: Handler = Handler()
    private var progressBarRunnable: Runnable? = null
    private var seekBarHanlder: Handler = Handler()
    private var seekBarRunnable: Runnable? = null

    private var showControllerInStart = true

    private var oldDuration = 0

    var errorPlaybackMessage = ""

    lateinit var errorToast: Toast

    private var mSeekbarTimer: Timer? = null
    private var mControllersTimer: Timer? = null
    private val controllerAndSeekbarHandler = Handler()

    lateinit var playerOriginalLayoutParams: ViewGroup.LayoutParams

    private val updateControllerTimerTask = UpdateControllerTimerTask()

    init {
        LayoutInflater.from(context)
            .inflate(layoutResId, this, true)


        listOf(
            video_view,
            controller,
            error_controller,
            skip_previous,
            play_pause,
            skip_next,
            fullscreen,
            share,
            camera
        ).forEach { it.setOnClickListener(this) }

        listOf(video_view, controller, error_controller).forEach { it.setOnTouchListener(this) }

        seek_bar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (playState == PlayState.PLAYING) {
//                        play(seekBar.progress)
                    } else if (playState != PlayState.IDLE) {
                        video_view.seekTo(seekBar.progress)
                    }
                    video_view.seekTo(seekBar.progress)
//                    startControllersTimer()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
//                    stopTrickplayTimer()
//                    video_view.pause()
//                    stopControllersTimer()
                }

                override fun onProgressChanged(
                    seekBar: SeekBar, progress: Int,
                    fromUser: Boolean
                ) {
                    timber("seekbar onProgressChanged")
                    current_duration.text = TimeUtil.formatMillisToHH_MM(progress)
//                    video_view.seekTo(seekBar.progress)
//                    playState = PlayState.PAUSE
//                    updatePlayState(playState)
                }
            })

        //By default the controller must be invisible/gone in xml .
        if (showControllerInStart)
            showOrHideController(true)

    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.video_view -> {

            }
            R.id.controller -> {

            }
            R.id.error_controller -> {

            }
            R.id.skip_previous -> {
                customPlayerListener.onPreviousClick()
            }
            R.id.play_pause -> {
                togglePlayBack()
            }
            R.id.skip_next -> {
                customPlayerListener.onNextClick()
            }
            R.id.fullscreen -> {
                switchOrientation()
            }
            R.id.share -> {
                openShareImageDialog(screenShotImageUrl)
            }
            R.id.camera -> {
                screenShotImageUrl = video_view.takeScreenshot()
            }
        }

    }

    override fun onTouch(view: View, p1: MotionEvent?): Boolean {
        when (view.id) {
            R.id.video_view -> {
                showOrHideController(true)
            }
            R.id.controller -> {
                showOrHideController(false)
            }
            R.id.error_controller -> {
                /**
                 * ReStart the whole Process Again
                 *
                 */
                setVideoData(video)
            }
        }

        return false
    }

    /**
     * This function switches the Orientation, used when [fullscreen] is pressed.
     */
    fun switchOrientation() {
        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            context.changeScreenOrientation(false)
            setLandscapePlayerAndController()
        } else {
            context.changeScreenOrientation(true)
            setPortraitPlayerAndContoller()
        }
    }

    /**
     * This function maintains the Orientation, used when player is setup from other Activity,Fragment,View.
     */
    fun maintainOrientation() {
        val orientation = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            context.changeScreenOrientation(true)
            setPortraitPlayerAndContoller()
        } else {
//            context.changeScreenOrientation(false)
            setLandscapePlayerAndController()
        }
    }


    //Controller will be shown and hidden after 5 secs if user does not hide the controller themself
    private fun showOrHideController(show: Boolean) {
        if (show) {
            if (controller.isShown) {
                controller.hide()
            } else {
                controller.show()
                if (controllerRunnable == null) {
                    controllerRunnable = Runnable {
                        controller.hide()
                    }
                }
                //Remove the previos callback of Runnable as we now need to call after 5 secs
                controllerHanlder.removeCallbacks(controllerRunnable)
                //Start Timer to hide controller after 5 secs
                controllerHanlder.postDelayed(controllerRunnable, showHideControllerTime)
            }
        } else {
            controller.visibility = View.INVISIBLE
        }
    }

    private fun togglePlayBack() {
        if (playState == PlayState.PLAYING) {
            playState = PlayState.PAUSE
            updatePlayState(playState)
        } else if (playState == PlayState.PAUSE) {
            playState = PlayState.PLAYING
            updatePlayState(playState)
        }
    }

    private fun updatePlayState(playState: PlayState) {
        when (playState) {
            PlayState.IDLE -> {
//                setControllerValuesToDefault()
                error_controller.hide()
                if (::errorToast.isInitialized)
                    errorToast.cancel()
                progress_bar.show()
            }

            PlayState.PLAYING -> {
                video_view.show()
                video_view.start()
                if (::errorToast.isInitialized)
                    errorToast.cancel()
                play_pause.setImageResource(R.drawable.ic_pause_black_24dp)
                progress_bar.hide()
            }

            PlayState.PAUSE -> {
                play_pause.setImageResource(R.drawable.ic_play_arrow_black_24dp)
                video_view.pause()
            }

            PlayState.BUFFERING -> {
            }

            PlayState.FINISHED -> {
            }
            PlayState.ERROR -> {
                if (!::errorToast.isInitialized)
                    errorToast = Toast.makeText(context, errorPlaybackMessage, Toast.LENGTH_SHORT)
                errorToast.show()
                controller.hide()
                error_controller.show()
                error_msg.text = errorPlaybackMessage
                progress_bar.hide()
            }
            else -> {

            }
        }
    }

    //Show controller acc. to Live or Recorded
    private fun updatePlayType(playType: PlayType) {
        when (playType) {
            PlayType.LIVE -> {
                skip_previous.hide()
                skip_next.hide()
                seek_bar.hide()
            }
            PlayType.RECORDED -> {
                skip_previous.show()
                skip_next.show()
                seek_bar.show()
            }
        }
    }

    fun updateLandscapeController() {
        video_name.show()
    }

    fun updatePortraitController() {
        video_name.hide()

    }


    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Toast.makeText(context, "Player", Toast.LENGTH_SHORT).show()

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLandscapePlayerAndController()
        } else {
            setPortraitPlayerAndContoller()
        }

    }

    private fun setLandscapePlayerAndController() {
        if (!::playerOriginalLayoutParams.isInitialized)
            playerOriginalLayoutParams = this@CustomVideoPlayer.layoutParams

        val playerParent = (player_parent.parent as CustomVideoPlayer)

        var lp: ViewGroup.LayoutParams? = null

        if (playerParent.parent is LinearLayout) {
            lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            lp.setMargins(-0, 0, 0, -0)
        }
        if (playerParent.parent is ConstraintLayout) {
            lp = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        if (playerParent.parent is RelativeLayout) {
            lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        if (playerParent.parent is FrameLayout) {
            lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        playerParent.layoutParams = lp
        playerParent.invalidate()

        //Set Visibily of Other Views Than Custom GONE
        for (i in 0 until (playerParent.parent as ViewGroup).childCount) {
            val child = (playerParent.parent as ViewGroup).getChildAt(i)
            if (!(child is CustomVideoPlayer))
                child.hide()
        }

        /**
         * TODO
         */


        context.showOrHideStatusBar(false)
    }

    private fun setPortraitPlayerAndContoller() {
        if (!::playerOriginalLayoutParams.isInitialized)
            playerOriginalLayoutParams = this@CustomVideoPlayer.layoutParams

        val playerParent = (player_parent.parent as CustomVideoPlayer)


        var lp: ViewGroup.LayoutParams? = null

//        val scale = resources.displayMetrics.density
//        val pixels = (400 * scale + 0.5f).toInt()

        if (playerParent.parent is LinearLayout) {
            lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, playerOriginalLayoutParams.height
            )
        }
        if (playerParent.parent is ConstraintLayout) {
            lp = ConstraintLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, playerOriginalLayoutParams.height
            )
        }
        if (playerParent.parent is RelativeLayout) {
            lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, playerOriginalLayoutParams.height
            )
        }
        if (playerParent.parent is FrameLayout) {
            lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, playerOriginalLayoutParams.height
            )
        }

        playerParent.layoutParams = lp
        playerParent.invalidate()

        for (i in 0 until (playerParent.parent as ViewGroup).childCount) {
            val child = (playerParent.parent as ViewGroup).getChildAt(i)
            child.show()
        }

        /**
         * TODO
         */
        context.showOrHideStatusBar(true)
    }

    /**
     * Setup The Listeners for the video view
     */
    fun setupPlayer(customPlayerListener: CustomPlayerListener) {
        this.customPlayerListener = customPlayerListener

        video_view.setOnPreparedListener {
            onPreparedToPlay()
        }
        video_view.setOnErrorListener(object : MediaPlayer.OnErrorListener {
            override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
                onErrorPlaying(mp, what, extra)

                return true
            }

        })
        video_view.setOnCompletionListener {
            onCompletePlaying()
        }
    }



    fun setVideoData(video: Video) {
        this.video = video
        video_view.setVideoURI(Uri.parse(this.video.videoUrl))

        playState = PlayState.IDLE
        updatePlayState(playState)

        playType = video.playType
        updatePlayType(playType)
    }

    private fun onErrorPlaying(mp: MediaPlayer?, what: Int, extra: Int) {
        if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
            errorPlaybackMessage = resources.getString(R.string.video_error_media_load_timeout)
        } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            errorPlaybackMessage = resources.getString(R.string.video_error_server_unaccessible)
        } else {
            errorPlaybackMessage = resources.getString(R.string.video_error_unknown_error)
        }

        playState = PlayState.ERROR
        updatePlayState(playState)
    }


    private fun onPreparedToPlay() {
        video_view.start()
        playState = PlayState.PLAYING
        updatePlayState(playState)

        //Set Total Duration
        total_duration.text = TimeUtil.formatMillisToHH_MM(video_view.duration)
        //Set seekbar max to total video duration
        seek_bar.max = video_view.duration

        showProgressForBuffering()
        //Cancel the Previos Task
//        updateControllerTimerTask.cancelTask()
        updateControllerTimerTask.scheduleTask(UpdateControllerTimerTask(),1000, 1000)
//        restartTrickplayTimer()
//        updateSeekbarAndCurrentDuration()
    }

    private fun onCompletePlaying() {

    }

    private fun showProgressForBuffering() {
        timber("showbufferprogress")
        // TO SHOW PROGRESSBAR WHEN BUFFERING(CHECK OLD AND NEW DURATION)
        if (progressBarRunnable == null) {
            progressBarRunnable = Runnable {
                timber("bufferchecking")
                val duration = video_view.currentPosition
                if (oldDuration == duration && playState == PlayState.PLAYING) {
                    progress_bar.show()
                } else {
                    progress_bar.hide()
                }
                oldDuration = duration

                progressBarHanlder.postDelayed(progressBarRunnable, 1000)
            }
        }
        progressBarHanlder.postDelayed(progressBarRunnable, 10)
    }

    fun updateSeekbarAndCurrentDuration() {
        timber("updateseekbar")
        if (seekBarRunnable == null) {
            seekBarRunnable = Runnable {
                timber("updateseekbarrunnable")
                val currentDuration = video_view.currentPosition
                current_duration.text = TimeUtil.formatMillisToHH_MM(currentDuration)
                seek_bar.progress = currentDuration

                seekBarHanlder.postDelayed(seekBarRunnable, 1000)

            }
        }
        seekBarHanlder.postDelayed(seekBarRunnable, 10)
    }

    private inner class UpdateSeekbarTask : TimerTask() {
        override fun run() {
            controllerAndSeekbarHandler.post(Runnable {
                val currentPos = video_view.getCurrentPosition()
                updateSeekbar(currentPos)
            })
        }
    }

    private fun updateSeekbar(videoPosition: Int) {
        seek_bar.setProgress(videoPosition)
        current_duration.setText(TimeUtil.formatMillisToHH_MM(videoPosition))
    }

    //TO update seekbar,starttime and endtime
    private fun restartTrickplayTimer() {
        stopTrickplayTimer()
        mSeekbarTimer = Timer()
        mSeekbarTimer!!.scheduleAtFixedRate(UpdateSeekbarTask(), 100, 1000)
    }

    private fun stopTrickplayTimer() {
        if (mSeekbarTimer != null) {
            mSeekbarTimer!!.cancel()
        }
    }

    fun startWithAutoPlay() {

    }

    fun setControllerForLive() {

    }

    fun setControllerForRecorded() {

    }

    fun clear() {

    }

    fun pauseVideo() {
        playState = PlayState.PAUSE
        updatePlayState(playState)
    }

    fun playVideo() {
        if (video_view.isPlaying) {
            playState = PlayState.PLAYING
            updatePlayState(playState)
        }
    }

    private inner class UpdateControllerTimerTask : BaseTimerTask() {
        override fun run() {
            runTask {
                timber("UpdateControllerTimerTask")
                val currentPos = video_view.getCurrentPosition()
                updateSeekbar(currentPos)
            }

        }

    }

}

interface CustomPlayerListener {
    fun onNextClick()
    fun onPreviousClick()
    fun onVideoFinish()
}