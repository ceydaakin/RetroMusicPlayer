package code.name.monkey.retromusic.service

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.animation.doOnEnd
import code.name.monkey.retromusic.service.playback.Playback
import code.name.monkey.retromusic.util.PreferenceUtil

class AudioFader {
    companion object {
        fun createFadeAnimator(
            context: Context,
            fadeInMp: MediaPlayer,
            fadeOutMp: MediaPlayer,
            endAction: (animator: Animator) -> Unit, /* Code to run when Animator Ends*/
        ): Animator? {
            // Get Global animator scale
            val animScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

            // Set duration according to the global animation scale, so cross-fade actually lasts for the duration set by the user
            val duration = (PreferenceUtil.crossFadeDuration * 1000 ) / animScale
            if (duration == 0F) {
                return null
            }
            return ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = duration.toLong()
                addUpdateListener { animation: ValueAnimator ->
                    fadeInMp.setVolume(
                        animation.animatedValue as Float, animation.animatedValue as Float
                    )
                    fadeOutMp.setVolume(1 - animation.animatedValue as Float,
                        1 - animation.animatedValue as Float)
                }
                doOnEnd {
                    endAction(it)
                }
            }
        }

        fun startFadeAnimator(
            playback: Playback,
            fadeIn: Boolean, /* fadeIn -> true  fadeOut -> false*/
            callback: Runnable? = null, /* Code to run when Animator Ends*/
        ) {
            startFadeAnimator(playback, fadeIn, PreferenceUtil.audioFadeDuration.toLong(), callback)
        }

        internal fun startFadeAnimator(
            playback: Playback,
            fadeIn: Boolean,
            duration: Long,
            callback: Runnable?,
        ) {
            if (duration == 0L) {
                callback?.run()
                return
            }
            val startValue = if (fadeIn) 0f else 1.0f
            val endValue = if (fadeIn) 1.0f else 0f
            // Step the fade with a Handler instead of a ValueAnimator: animators tick on
            // Choreographer frames, which may stop while the app is in the background or the
            // screen is off, so a pause from the notification never reached its callback (#2016).
            val handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
            val startTime = SystemClock.uptimeMillis()
            object : Runnable {
                override fun run() {
                    val fraction =
                        ((SystemClock.uptimeMillis() - startTime).toFloat() / duration).coerceIn(0f, 1f)
                    playback.setVolume(startValue + (endValue - startValue) * fraction)
                    if (fraction < 1f) {
                        handler.postDelayed(this, FADE_STEP_MS)
                    } else {
                        callback?.run()
                    }
                }
            }.run()
        }

        private const val FADE_STEP_MS = 16L
    }
}