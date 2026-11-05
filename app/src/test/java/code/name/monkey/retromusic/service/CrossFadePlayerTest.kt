package code.name.monkey.retromusic.service

import android.animation.Animator
import android.media.MediaPlayer
import androidx.preference.PreferenceManager
import code.name.monkey.retromusic.CROSS_FADE_DURATION
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CrossFadePlayerTest {

    @Test
    fun errorDuringCrossFadeStopsTheFade() {
        val context = RuntimeEnvironment.getApplication()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(CROSS_FADE_DURATION, 5).commit()
        val player = CrossFadePlayer(context)
        val fadeIn = player.field<MediaPlayer>("player2")
        val fadeOut = player.field<MediaPlayer>("player1")
        CrossFadePlayer::class.java
            .getDeclaredMethod("crossFade", MediaPlayer::class.java, MediaPlayer::class.java)
            .apply { isAccessible = true }
            .invoke(player, fadeIn, fadeOut)
        val fade = player.field<Animator>("crossFadeAnimator")
        assertTrue(fade.isStarted)

        // onError releases the failing MediaPlayer. A fade that keeps running
        // calls setVolume() on it and crashes with IllegalStateException (#2044).
        player.onError(fadeOut, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)

        assertFalse("crossfade still running after onError", fade.isStarted)
        assertFalse(player.isCrossFading)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> CrossFadePlayer.field(name: String): T =
        CrossFadePlayer::class.java.getDeclaredField(name)
            .apply { isAccessible = true }
            .get(this) as T
}
