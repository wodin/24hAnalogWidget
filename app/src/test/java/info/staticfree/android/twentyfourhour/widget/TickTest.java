package info.staticfree.android.twentyfourhour.widget;

import static info.staticfree.android.twentyfourhour.widget.Alarms.only;
import static info.staticfree.android.twentyfourhour.widget.Alarms.operationOf;
import static info.staticfree.android.twentyfourhour.widget.Alarms.scheduled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.AlarmManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import info.staticfree.android.twentyfourhour.TwentyFourHourClockWidgetResizable;

/**
 * The redraw schedule, read off the alarm it sets.
 *
 * <p>Scheduling a tick needs no widget, no broadcast and no host — which is the
 * point of it being its own object. What causes a tick to be scheduled is the
 * receiver's test; what a tick <em>is</em> is here.
 *
 * <p>Every expectation is taken from the platform's rules rather than from what
 * the code does, which is what lets these tests disagree with it: a one-shot
 * because the schedule is re-armed per fire, {@code RTC} because a minute hand
 * is not worth waking a device for, explicit because API 26 stopped delivering
 * implicit broadcasts to manifest receivers, immutable because API 31 makes a
 * {@code PendingIntent} that says neither fatal.
 */
@RunWith(RobolectricTestRunner.class)
public class TickTest {

    /** A minute, in milliseconds. */
    private static final long MINUTE_MS = 60_000;

    /**
     * {@code RTC} rather than {@code RTC_WAKEUP}: a minute hand nobody is
     * looking at is not worth spending battery on, and a non-wakeup alarm is
     * <em>defined</em> as delivered at the next moment the device is awake — so
     * "redraw when someone starts looking" is the alarm type's own semantics
     * rather than something this app has to arrange.
     *
     * <p>One-shot rather than a repeat, which is what {@code interval == 0}
     * says. Not because a repeat drifts — it does not, and {@link Tick} carries
     * the measurement that says so — but because a repeat's batching window is
     * derived from its interval and a one-shot's from how far off its own
     * trigger is. The reasoning is stated once, where the code that acts on it
     * lives.
     */
    @Test
    public void theTickIsAOneShotOnTheWallClockThatDoesNotWakeTheDevice() {
        Tick.schedule(context());

        assertEquals("the tick should follow the wall clock and not wake the device",
                AlarmManager.RTC, only(context()).getType());
        assertEquals("the tick should be armed once, not repeated", 0,
                only(context()).getIntervalMs());
    }

    /**
     * Read against the clock rather than against a fixed instant, because that
     * is what the schedule reads. A whole minute is what both halves of the
     * claim need, and every real timezone is a whole number of minutes from
     * UTC, so "the top of a minute" is the same instant in any of them.
     *
     * <p>Bracketed by two readings of the clock because the schedule takes its
     * own somewhere between them. Comparing the upper bound against
     * {@code before} instead would fail whenever a minute turned over
     * mid-call.
     */
    @Test
    public void theTickFallsAtTheTopOfTheNextMinute() {
        long before = System.currentTimeMillis();
        Tick.schedule(context());
        long after = System.currentTimeMillis();

        long triggerAt = only(context()).getTriggerAtMs();

        assertEquals("the tick should fall on a minute boundary", 0, triggerAt % MINUTE_MS);
        assertTrue("the tick should be in the future", triggerAt > before);
        assertTrue("the tick should be no more than a minute away",
                triggerAt <= after + MINUTE_MS);
    }

    /**
     * The tick names the receiver it is for. An implicit broadcast has not been
     * delivered to a manifest receiver since API 26 except by an exemption this
     * action has no claim to — <strong>measured</strong> on the API 34
     * emulator, the same action sent from the shell reached nothing, and the
     * alarm's own copy arrived only because an app may still broadcast
     * implicitly to itself. Naming the receiver stops the tick resting on that.
     */
    @Test
    public void theTickIsAddressedToTheWidgetReceiver() {
        Tick.schedule(context());

        assertEquals("the tick should name the receiver that redraws the widget",
                new ComponentName(context(), TwentyFourHourClockWidgetResizable.class),
                operationOf(only(context())).getSavedIntent().getComponent());
    }

    /**
     * And the receiver can tell it apart from the other broadcasts it is sent.
     * Asked of the intent the schedule actually built rather than of one
     * assembled here, so that the two halves — what is sent and what is
     * recognised — cannot drift apart.
     */
    @Test
    public void theReceiverRecognisesTheTickTheScheduleSends() {
        Tick.schedule(context());

        assertTrue("the tick should be recognisable as one",
                Tick.isTick(operationOf(only(context())).getSavedIntent()));
        assertFalse("a time change is not a tick",
                Tick.isTick(new Intent(Intent.ACTION_TIME_CHANGED)));
    }

    /**
     * Which of the two mutability flags, and why either is needed at all,
     * {@code Tick} explains. What matters here is that the flag is the one part
     * of this the JVM can see at all: Robolectric does not enforce the API 31
     * rule, so the absence of both flags is readable here but the crash it
     * causes on a device is not reproducible.
     */
    @Test
    public void theTicksPendingIntentIsImmutable() {
        Tick.schedule(context());

        assertTrue("the tick's PendingIntent should be immutable",
                operationOf(only(context())).isImmutable());
    }

    /**
     * Scheduling is idempotent, which is what lets every path that redraws the
     * widget arm the next redraw without counting how many times it has. It
     * holds by identity rather than by any flag: both {@code PendingIntent}s are
     * built with the same request code and an intent equal under
     * {@link Intent#filterEquals}, so they are the same one, and
     * {@link AlarmManager} replaces the outstanding alarm rather than adding to
     * it.
     */
    @Test
    public void schedulingAgainReplacesTheTickRatherThanAddingOne() {
        Tick.schedule(context());
        Tick.schedule(context());

        assertEquals("a second scheduling should leave one alarm, not two", 1,
                scheduled(context()).size());
    }

    /**
     * Cancelling reaches the same alarm, which is the other half of that
     * identity: a cancel that built a different {@code PendingIntent} would
     * leave the tick running with nothing left able to stop it.
     */
    @Test
    public void cancellingStopsTheTick() {
        Tick.schedule(context());
        Tick.cancel(context());

        assertTrue("cancelling should leave no alarm behind", scheduled(context()).isEmpty());
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
