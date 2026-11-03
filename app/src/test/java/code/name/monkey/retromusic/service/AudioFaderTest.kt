package code.name.monkey.retromusic.service

import android.os.Looper
import code.name.monkey.retromusic.service.playback.Playback
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowChoreographer
import java.lang.reflect.Proxy
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AudioFaderTest {

    private val volumes = mutableListOf<Float>()

    private val playback = Proxy.newProxyInstance(
        Playback::class.java.classLoader,
        arrayOf(Playback::class.java),
    ) { _, method, args ->
        if (method.name == "setVolume") volumes += args[0] as Float
        when (method.returnType) {
            Boolean::class.javaPrimitiveType -> true
            Int::class.javaPrimitiveType -> 0
            else -> null
        }
    } as Playback

    @Before
    fun setUp() {
        // No display frames, like a service whose app has no visible window.
        ShadowChoreographer.setPaused(true)
    }

    @After
    fun tearDown() {
        ShadowChoreographer.setPaused(false)
    }

    @Test
    fun fadeOutFinishesWithoutDisplayFrames() {
        var ended = false
        AudioFader.startFadeAnimator(playback, false, 400L) { ended = true }
        assertEquals(1f, volumes.first(), 0f)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))

        assertTrue("pause callback never ran", ended)
        assertEquals(0f, volumes.last(), 0f)
    }

    @Test
    fun fadeInReachesFullVolumeWithoutDisplayFrames() {
        AudioFader.startFadeAnimator(playback, true, 400L, null)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1000))

        assertEquals(1f, volumes.last(), 0f)
    }

    @Test
    fun zeroDurationRunsCallbackImmediately() {
        var ended = false
        AudioFader.startFadeAnimator(playback, false, 0L) { ended = true }
        assertTrue(ended)
        assertTrue(volumes.isEmpty())
    }
}
