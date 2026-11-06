package code.name.monkey.retromusic.service

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.media.AudioManager
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AudioManagerPlaybackTest {

    @Test
    fun becomingNoisyPausesWithoutStartingTheService() {
        // MusicService.onCreate reads the platform signature through PackageValidator.
        shadowOf(RuntimeEnvironment.getApplication().packageManager).installPackage(
            PackageInfo().apply {
                packageName = "android"
                @Suppress("DEPRECATION")
                signatures = arrayOf(Signature(byteArrayOf(1)))
            }
        )
        val service = Robolectric.buildService(MusicService::class.java).create().get()
        val playback = service.playback as AudioManagerPlayback
        AudioManagerPlayback::class.java
            .getDeclaredMethod("registerBecomingNoisyReceiver")
            .apply { isAccessible = true }
            .invoke(playback)
        val app = shadowOf(RuntimeEnvironment.getApplication())
        app.clearStartedServices()

        // Unplugging headphones or leaving Android Auto sends this broadcast while the
        // app can be in the background, where startService() throws
        // BackgroundServiceStartNotAllowedException on Android 12+ (#2047).
        RuntimeEnvironment.getApplication()
            .sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        shadowOf(service.mainLooper).idle()

        assertNull(app.nextStartedService)
    }
}
