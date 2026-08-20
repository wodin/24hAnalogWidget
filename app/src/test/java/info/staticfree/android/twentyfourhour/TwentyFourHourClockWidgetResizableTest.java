package info.staticfree.android.twentyfourhour;

import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isHand;
import static info.staticfree.android.twentyfourhour.dial.Artwork.probe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowPendingIntent;

import java.util.List;

/**
 * A net over the widget receiver: deliver it the broadcasts the framework
 * delivers, then read back the two things it produces — the alarm it schedules
 * and the picture it hands the host.
 *
 * <p>Part of it is a <em>characterisation</em> net, asserting what the receiver
 * does today precisely so that changing it is deliberate: the alarm repeats
 * rather than re-arming, its intent names no receiver, and the bitmap is the
 * artwork's own size whatever the host's cell measures. All three are wrong, all
 * three are about to be fixed, and each one should fail loudly when it is — a
 * flip visible in a diff rather than inferred from a green suite.
 *
 * <p>The rest pins what must <em>survive</em> that work: a wall-clock alarm that
 * does not wake the device, anchored to the top of a minute, carried by an
 * immutable {@link PendingIntent}, cancelled when the last widget goes, and a
 * picture that reaches the host at all. A net woven only from the parts due to
 * change would let the parts not due to change break in silence.
 *
 * <p>What it cannot say: that a {@code PendingIntent} declaring neither
 * mutability flag is fatal. Robolectric's {@code ShadowPendingIntent} does not
 * enforce the API 31 rule that one of {@code FLAG_IMMUTABLE} or
 * {@code FLAG_MUTABLE} must be given, so the flag is readable here but the crash
 * its absence causes on a device is not reproducible. That one needs an
 * emulator.
 *
 * <p>A fresh receiver per delivery, deliberately: Android constructs one for
 * each broadcast and discards it afterwards, so a receiver that remembered
 * anything between deliveries would be remembering it in vain.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class TwentyFourHourClockWidgetResizableTest {

    /** A minute, in milliseconds: the repeat the receiver asks for today. */
    private static final long MINUTE_MS = 60_000;

    @Test
    public void enablingTheWidgetSchedulesOneAlarm() {
        enableWidget();

        assertEquals("enabling the widget should schedule exactly one alarm", 1,
                scheduledAlarms().size());
    }

    /**
     * {@code RTC} rather than {@code RTC_WAKEUP}: the tick is not worth waking
     * the device for, and that much should survive. The minutely
     * {@code interval} should not — a repeat re-uses the anchor it was given, so
     * it keeps whatever lateness the OS added to the first period instead of
     * correcting for it.
     */
    @Test
    public void theTickRepeatsEveryMinuteOnTheWallClockWithoutWakingTheDevice() {
        enableWidget();

        assertEquals("the tick should follow the wall clock and not wake the device",
                AlarmManager.RTC, onlyAlarm().getType());
        assertEquals("the tick should repeat every minute", MINUTE_MS,
                onlyAlarm().getIntervalMs());
    }

    /**
     * Read against the clock rather than against a fixed instant: the receiver
     * anchors to the top of the next minute, and a whole minute is what both
     * halves of that claim need. Every real timezone is a whole number of
     * minutes from UTC, so "the top of a minute" is the same instant in any of
     * them.
     *
     * <p>Bracketed by two readings of the clock because the receiver takes its
     * own somewhere between them. Comparing the upper bound against {@code
     * before} instead would fail whenever a minute turned over mid-delivery.
     */
    @Test
    public void theTickIsAnchoredAtTheTopOfTheNextMinute() {
        long before = System.currentTimeMillis();
        enableWidget();
        long after = System.currentTimeMillis();

        long triggerAt = onlyAlarm().getTriggerAtMs();

        assertEquals("the tick should fall on a minute boundary", 0, triggerAt % MINUTE_MS);
        assertTrue("the tick should be in the future", triggerAt > before);
        assertTrue("the tick should be no more than a minute away",
                triggerAt <= after + MINUTE_MS);
    }

    /**
     * The intent names an action and nothing else. A manifest receiver has not
     * been reachable by an implicit broadcast since API 26, so this alarm fires
     * into nothing — which is why the widget stops at whatever the host has
     * cached. Naming the receiver is the fix, and it lands here.
     */
    @Test
    public void theTickIntentNamesNoReceiver() {
        enableWidget();

        Intent tick = tickPendingIntent().getSavedIntent();

        assertEquals("the tick should carry the update action",
                TwentyFourHourClockWidgetResizable.ACTION_CLOCK_UPDATE, tick.getAction());
        assertNull("the tick should name no component today", tick.getComponent());
        assertNull("the tick should name no package today", tick.getPackage());
    }

    /**
     * Which of the two mutability flags, and why either is needed at all, is
     * {@code createUpdate}'s to explain. What matters here is that the flag is
     * the one thing about this the JVM can see at all.
     */
    @Test
    public void theTicksPendingIntentIsImmutable() {
        enableWidget();

        assertTrue("the tick's PendingIntent should be immutable",
                tickPendingIntent().isImmutable());
    }

    @Test
    public void disablingTheWidgetCancelsTheTick() {
        enableWidget();
        deliver(new Intent(AppWidgetManager.ACTION_APPWIDGET_DISABLED));

        assertTrue("disabling the widget should leave no alarm behind",
                scheduledAlarms().isEmpty());
    }

    /**
     * The picture reaches the host: the {@code RemoteViews} the receiver sends
     * inflates to the widget layout with a rendered dial on it. Robolectric
     * applies it into a real {@code AppWidgetHostView}, so a {@code RemoteViews}
     * the host could not inflate would fail here too.
     *
     * <p>Probed at the centre rather than merely asserted non-null, because the
     * layout already carries the Face as its {@code src}: an update that set no
     * bitmap at all would still leave an image behind, and one of exactly this
     * size. Both hands meet at the centre whatever the time, and the bare Face
     * there is near-white, so a hand at the centre is a render and nothing else.
     */
    @Test
    public void updatingAWidgetShowsARenderedDialAndNotTheBareFace() {
        int widgetId = pinWidget();

        deliver(update(widgetId));

        assertTrue("the hands should meet at the centre of the picture",
                isHand(probe(bitmapShownOn(widgetId), 0, 0)));
    }

    /**
     * The bitmap is square at the size of the Face artwork, whatever the host's
     * cell measures. Sizing it from the cell instead is still to come, at which
     * point this assertion is meant to fail.
     */
    @Test
    public void theBitmapIsSquareAtTheSizeOfTheFaceArtwork() {
        int widgetId = pinWidget();

        deliver(update(widgetId));

        Bitmap shown = bitmapShownOn(widgetId);

        assertEquals("the bitmap should be the size of the Face artwork", INTRINSIC_PX,
                shown.getWidth());
        assertEquals("the bitmap should be square", shown.getWidth(), shown.getHeight());
    }

    /**
     * Registers a widget with the host, as pinning one from the picker would.
     * The shadow calls {@code onUpdate} while doing so, which is why every test
     * that reads a picture back delivers its own update afterwards rather than
     * trusting this one.
     */
    private static int pinWidget() {
        return shadowOf(appWidgetManager())
                .createWidget(TwentyFourHourClockWidgetResizable.class, R.layout.widget);
    }

    /** The broadcast the framework sends when the first widget is added. */
    private static void enableWidget() {
        deliver(new Intent(AppWidgetManager.ACTION_APPWIDGET_ENABLED));
    }

    @NonNull
    private static Intent update(int widgetId) {
        return new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
    }

    /** One broadcast, to a receiver that has never seen another. */
    private static void deliver(@NonNull Intent intent) {
        new TwentyFourHourClockWidgetResizable().onReceive(context(), intent);
    }

    @NonNull
    private static List<ShadowAlarmManager.ScheduledAlarm> scheduledAlarms() {
        return shadowOf(context().getSystemService(AlarmManager.class)).getScheduledAlarms();
    }

    @NonNull
    private static ShadowAlarmManager.ScheduledAlarm onlyAlarm() {
        List<ShadowAlarmManager.ScheduledAlarm> alarms = scheduledAlarms();
        assertEquals("expected exactly one scheduled alarm", 1, alarms.size());
        return alarms.get(0);
    }

    /**
     * The {@code PendingIntent} the alarm would send. Robolectric deprecated the
     * field without putting an accessor in its place, so the one use of it lives
     * here rather than at every call site.
     */
    @NonNull
    @SuppressWarnings("deprecation")
    private static ShadowPendingIntent tickPendingIntent() {
        return shadowOf(onlyAlarm().operation);
    }

    /** The picture the host is showing, from the last {@code RemoteViews} it was sent. */
    @NonNull
    private static Bitmap bitmapShownOn(int widgetId) {
        ImageView clock = shadowOf(appWidgetManager()).getViewFor(widgetId)
                .findViewById(R.id.clock);
        return ((BitmapDrawable) clock.getDrawable()).getBitmap();
    }

    @NonNull
    private static AppWidgetManager appWidgetManager() {
        return AppWidgetManager.getInstance(context());
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
