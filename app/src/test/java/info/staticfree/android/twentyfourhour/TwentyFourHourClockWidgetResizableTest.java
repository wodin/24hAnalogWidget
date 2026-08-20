package info.staticfree.android.twentyfourhour;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
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
import android.os.Bundle;
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
 * does today precisely so that changing it is deliberate: the alarm carries a
 * repeat interval rather than being armed a minute at a time, and its intent
 * names no receiver. Both are wrong, both are about to be fixed, and each should
 * fail loudly when it is — a flip visible in a diff rather than inferred from a
 * green suite.
 *
 * <p>The rest pins what must <em>survive</em> that work: a wall-clock alarm that
 * does not wake the device, anchored to the top of a minute, carried by an
 * immutable {@link PendingIntent}, cancelled when the last widget goes, and a
 * picture that reaches every widget the update names. A net woven only from the
 * parts due to change would let the parts not due to change break in silence.
 *
 * <p>What the picture <em>is</em> — its bitmap's size, the tap on it — belongs
 * to {@code WidgetPictureTest}, which needs no broadcast to ask. Left here is
 * the wiring: that a picture is built per id and reaches the host.
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

    /**
     * The tick is armed by an update rather than by {@code onEnabled}, so that
     * every path which redraws the widget also schedules the next redraw. Why
     * that is worth having, {@code onUpdate} explains.
     *
     * <p>Here is the case it has to hold for: a reboot, which empties the
     * alarm table — alarms live in the system's memory and nothing writes them
     * to disk. <strong>Measured</strong> on the API 34 emulator on 2026-08-20:
     * across two reboots with the widget pinned, the framework delivered
     * {@code APPWIDGET_ENABLED}, {@code APPWIDGET_UPDATE} and
     * {@code APPWIDGET_UPDATE_OPTIONS} to this receiver, each named explicitly
     * by component, and the {@code RTC} alarm was back with a fresh anchor
     * within seconds of {@code sys.boot_completed}. So the update the host
     * already sends <em>is</em> the re-arm, and a {@code BOOT_COMPLETED}
     * receiver would add nothing but a process start on every boot with no
     * widget pinned.
     */
    @Test
    public void updatingAWidgetArmsTheTick() {
        deliver(update(pinWidget()));

        assertEquals("updating a widget should leave exactly one alarm", 1,
                scheduledAlarms().size());
    }

    /**
     * Arming on every update is only safe if arming twice leaves one alarm — and
     * a widget is updated far more often than it is enabled, so this is the risk
     * the move creates. It holds by identity rather than by any flag: both
     * {@code PendingIntent}s are built with the same request code and an intent
     * equal under {@link Intent#filterEquals}, so they are the same one, and
     * {@link AlarmManager} replaces the outstanding alarm rather than adding to
     * it.
     */
    @Test
    public void armingTheTickAgainReplacesItRatherThanAddingOne() {
        int widgetId = pinWidget();

        deliver(update(widgetId));
        deliver(update(widgetId));

        assertEquals("a second update should leave one alarm, not two", 1,
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
        deliver(update(pinWidget()));

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
        int widgetId = pinWidget();

        long before = System.currentTimeMillis();
        deliver(update(widgetId));
        long after = System.currentTimeMillis();

        long triggerAt = onlyAlarm().getTriggerAtMs();

        assertEquals("the tick should fall on a minute boundary", 0, triggerAt % MINUTE_MS);
        assertTrue("the tick should be in the future", triggerAt > before);
        assertTrue("the tick should be no more than a minute away",
                triggerAt <= after + MINUTE_MS);
    }

    /**
     * The intent names an action and nothing else. It arrives all the same,
     * because the app is sending it to itself: <strong>measured</strong> on the
     * API 34 emulator, the alarm redraws the widget every minute, while the same
     * action broadcast from the shell reaches no receiver at all. Naming the
     * receiver is still the fix — it stops the tick depending on an exemption to
     * the API 26 rule against implicit broadcasts to manifest receivers — and it
     * lands in a later slice.
     */
    @Test
    public void theTickIntentNamesNoReceiver() {
        deliver(update(pinWidget()));

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
        deliver(update(pinWidget()));

        assertTrue("the tick's PendingIntent should be immutable",
                tickPendingIntent().isImmutable());
    }

    @Test
    public void disablingTheWidgetCancelsTheTick() {
        deliver(update(pinWidget()));
        deliver(new Intent(AppWidgetManager.ACTION_APPWIDGET_DISABLED));

        assertTrue("disabling the widget should leave no alarm behind",
                scheduledAlarms().isEmpty());
    }

    /**
     * An update names ids, plural, and each of them is a widget of its own: the
     * receiver builds a picture per id rather than building one and showing it
     * on all of them. Nothing but this test holds it there — the picture does
     * not yet vary by id, so both readings look alike — but it is what stops a
     * second widget from being drawn to the first one's cell once the cell is
     * what decides the picture.
     *
     * <p>Read by identity, not by pixels. Two widgets are drawn from the same
     * artwork at the same moment, so their pictures are indistinguishable to
     * look at, and pinning has already put a rendered one on each: probing
     * either would pass without this update having happened at all. Identity is
     * also what carries the wiring claim this file is left with — a bitmap that
     * changed is one the receiver's own update put there.
     *
     * <p>Only the last of the three assertions tells the two implementations
     * apart; the first two say the update happened at all, without which the
     * last would hold just as well over two untouched widgets.
     */
    @Test
    public void updatingTwoWidgetsGivesEachOneAPictureOfItsOwn() {
        int first = pinWidget();
        int second = pinWidget();
        Bitmap firstAtPinning = bitmapShownOn(first);
        Bitmap secondAtPinning = bitmapShownOn(second);

        deliver(update(first, second));

        assertNotSame("the first widget should have been redrawn", firstAtPinning,
                bitmapShownOn(first));
        assertNotSame("the second widget should have been redrawn", secondAtPinning,
                bitmapShownOn(second));
        assertNotSame("each widget should have a bitmap of its own", bitmapShownOn(first),
                bitmapShownOn(second));
    }

    /**
     * The picture a widget is given is drawn for <em>that widget's</em> cell:
     * the receiver asks the host what it has reported for the id it is about to
     * update, rather than drawing one size for all of them. Which square a cell
     * implies is {@code WidgetPicture}'s rule and its test; what is claimed here
     * is only that the cell reaches it.
     *
     * <p>250dp is used because 500px is nothing else — not the artwork's own
     * size, which is what an ignored bundle would produce, and not the display's
     * smaller dimension, which is what a cap misapplied would.
     *
     * <p>The pinned picture is read after the resize and not before it, because
     * the host's own {@code updateAppWidgetOptions} redraws the widget on the
     * way past. Comparing against that redraw rather than against pinning is
     * what keeps the last assertion from passing on a picture this update did
     * not put there.
     */
    @Test
    public void aWidgetIsDrawnForTheCellTheHostReportsForIt() {
        int widgetId = pinWidget();
        appWidgetManager().updateAppWidgetOptions(widgetId, cell(250));
        Bitmap beforeTheUpdate = bitmapShownOn(widgetId);

        deliver(update(widgetId));

        assertNotSame("the update should have redrawn the widget", beforeTheUpdate,
                bitmapShownOn(widgetId));
        assertEquals("the widget should be drawn for its own 250dp cell", 500,
                bitmapShownOn(widgetId).getWidth());
    }

    /**
     * Resizing a widget redraws it. Without this the host keeps showing the
     * bitmap it was last given, stretched to the new cell — which is exactly the
     * blur that sizing from the cell was meant to remove.
     *
     * <p>The new size comes from the broadcast rather than from the host: the
     * framework puts the fresh bundle in the intent, and reading it back from
     * {@code AppWidgetManager} would be asking a second source about something
     * the first one just said.
     */
    @Test
    public void resizingAWidgetRedrawsItAtItsNewSize() {
        int widgetId = pinWidget();
        Bitmap beforeTheResize = bitmapShownOn(widgetId);

        deliver(resize(widgetId, cell(250)));

        assertNotSame("the resize should have redrawn the widget", beforeTheResize,
                bitmapShownOn(widgetId));
        assertEquals("the widget should be redrawn for its new 250dp cell", 500,
                bitmapShownOn(widgetId).getWidth());
    }

    /**
     * And re-arms the tick while it is there, so that <em>every path that
     * redraws the widget schedules the next redraw</em> has no exception to
     * remember. Why that is worth a line it will never need,
     * {@code onAppWidgetOptionsChanged} explains.
     */
    @Test
    public void resizingAWidgetArmsTheTick() {
        deliver(resize(pinWidget(), cell(250)));

        assertEquals("resizing a widget should leave exactly one alarm", 1,
                scheduledAlarms().size());
    }

    /**
     * Registers a widget with the host, as pinning one from the picker would,
     * and leaves nothing of its own behind.
     *
     * <p>The shadow drives the receiver while registering, so pinning both draws
     * a picture and arms a tick. Every test therefore delivers its own update
     * afterwards rather than trusting that one — and the alarm pinning scheduled
     * is forgotten here, or it would stand in for whatever the test delivers
     * next.
     *
     * <p>Robolectric offers no {@code clear()} for the alarm table and
     * deprecated the consuming {@code getNextScheduledAlarm} without putting a
     * replacement in its place, so draining it is what there is.
     */
    @SuppressWarnings("deprecation")
    private static int pinWidget() {
        int widgetId = shadowOf(appWidgetManager())
                .createWidget(TwentyFourHourClockWidgetResizable.class, R.layout.widget);

        ShadowAlarmManager alarms = shadowOf(context().getSystemService(AlarmManager.class));
        while (alarms.getNextScheduledAlarm() != null) {
            // consumed by the call itself
        }

        return widgetId;
    }

    /**
     * A square cell of the given side in dp, which is all four of the numbers a
     * host reports about one. Real cells are not square and the difference is
     * what {@code WidgetPictureTest} is about; here the cell only has to be
     * recognisable in the render.
     */
    @NonNull
    private static Bundle cell(int sideDp) {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, sideDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, sideDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, sideDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, sideDp);
        return options;
    }

    @NonNull
    private static Intent resize(int widgetId, @NonNull Bundle options) {
        return new Intent(AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
    }

    @NonNull
    private static Intent update(int... widgetIds) {
        return new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
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
