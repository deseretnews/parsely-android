package com.parsely.parselyandroid

import androidx.test.core.app.ApplicationProvider
import java.util.Calendar
import java.util.TimeZone
import java.util.Timer
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.api.Assertions.withinPercentage
import org.assertj.core.api.MapAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private typealias Event = MutableMap<String, Any>

@RunWith(RobolectricTestRunner::class)
internal class EngagementManagerTest {

    private lateinit var sut: EngagementManager
    private val tracker = FakeTracker()
    private val parentTimer = Timer()
    private val baseEvent: Event = mutableMapOf(
        "action" to "heartbeat",
        "data" to testData
    )

    @Before
    fun setUp() {
        sut = EngagementManager(
            tracker,
            parentTimer,
            DEFAULT_INTERVAL_MILLIS,
            baseEvent,
            FakeIntervalCalculator()
        )
    }

    @Test
    fun `when starting manager, then record the correct event after interval millis`() {
        // when
        sut.start()
        sleep(DEFAULT_INTERVAL_MILLIS)
        val timestamp = now - THREAD_SLEEPING_THRESHOLD

        // then
        assertThat(tracker.events[0]).isCorrectEvent(
            // Ideally: totalTime should be equal to DEFAULT_INTERVAL_MILLIS
            withTotalTime = { isCloseTo(DEFAULT_INTERVAL_MILLIS, withinPercentage(10)) },
            // Ideally: timestamp should be equal to System.currentTimeMillis() at the time of recording the event
            withTimestamp = { isCloseTo(timestamp, within(20L)) }
        )
    }

    @Test
    fun `when starting manager, then schedule task each interval period`() {
        sut.start()

        sleep(DEFAULT_INTERVAL_MILLIS)
        val firstTimestamp = now - THREAD_SLEEPING_THRESHOLD

        sleep(DEFAULT_INTERVAL_MILLIS)
        val secondTimestamp = now - 2 * THREAD_SLEEPING_THRESHOLD

        sleep(DEFAULT_INTERVAL_MILLIS)
        val thirdTimestamp = now - 3 * THREAD_SLEEPING_THRESHOLD

        sleep(THREAD_SLEEPING_THRESHOLD)

        val firstEvent = tracker.events[0]
        assertThat(firstEvent).isCorrectEvent(
            // Ideally: totalTime should be equal to DEFAULT_INTERVAL_MILLIS
            withTotalTime = { isCloseTo(DEFAULT_INTERVAL_MILLIS, withinPercentage(10)) },
            // Ideally: timestamp should be equal to `now` at the time of recording the event
            withTimestamp = { isCloseTo(firstTimestamp, within(20L)) }
        )
        val secondEvent = tracker.events[1]
        assertThat(secondEvent).isCorrectEvent(
            // Ideally: totalTime should be equal to DEFAULT_INTERVAL_MILLIS * 2
            withTotalTime = { isCloseTo(DEFAULT_INTERVAL_MILLIS * 2, withinPercentage(10)) },
            // Ideally: timestamp should be equal to `now` at the time of recording the event
            withTimestamp = { isCloseTo(secondTimestamp, within(20L)) }
        )
        val thirdEvent = tracker.events[2]
        assertThat(thirdEvent).isCorrectEvent(
            // Ideally: totalTime should be equal to DEFAULT_INTERVAL_MILLIS * 3
            withTotalTime = { isCloseTo(DEFAULT_INTERVAL_MILLIS * 3, withinPercentage(10)) },
            // Ideally: timestamp should be equal to `now` at the time of recording the event
            withTimestamp = { isCloseTo(thirdTimestamp, within(20L)) }
        )
    }

    private fun sleep(millis: Long) = Thread.sleep(millis + THREAD_SLEEPING_THRESHOLD)

    private fun MapAssert<String, Any>.isCorrectEvent(
        withTotalTime: AbstractLongAssert<*>.() -> AbstractLongAssert<*>,
        withTimestamp: AbstractLongAssert<*>.() -> AbstractLongAssert<*>,
    ): MapAssert<String, Any> {
        return containsEntry("action", "heartbeat")
            // Incremental will be always 0 because the interval is lower than 1s
            .containsEntry("inc", 0L)
            .hasEntrySatisfying("tt") { totalTime ->
                totalTime as Long
                assertThat(totalTime).withTotalTime()
            }
            .hasEntrySatisfying("data") { data ->
                @Suppress("UNCHECKED_CAST")
                data as Map<String, Any>
                assertThat(data).hasEntrySatisfying("ts") { timestamp ->
                    timestamp as Long
                    assertThat(timestamp).withTimestamp()
                }.containsAllEntriesOf(testData.minus("ts"))
            }
    }

    private val now: Long
        get() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis

    class FakeTracker : ParselyTracker(
        "",
        0,
        ApplicationProvider.getApplicationContext()
    ) {
        val events = mutableListOf<Event>()

        override fun enqueueEvent(event: Event) {
            events += event
        }
    }

    class FakeIntervalCalculator : HeartbeatIntervalCalculator(Clock()) {
        override fun calculate(startTime: Calendar): Long {
            return DEFAULT_INTERVAL_MILLIS
        }
    }

    private companion object {
        const val DEFAULT_INTERVAL_MILLIS = 100L
        // Additional time to wait to ensure that the timer has fired
        const val THREAD_SLEEPING_THRESHOLD = 50L
        val testData = mutableMapOf<String, Any>(
            "os" to "android",
            "parsely_site_uuid" to "e8857cbe-5ace-44f4-a85e-7e7475f675c5",
            "os_version" to "34",
            "manufacturer" to "Google",
            "ts" to 123L
        )
    }
}
