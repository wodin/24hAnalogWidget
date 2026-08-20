package info.staticfree.android.twentyfourhour;

import static info.staticfree.android.twentyfourhour.widget.Alarms.only;
import static info.staticfree.android.twentyfourhour.widget.Alarms.operationOf;
import static info.staticfree.android.twentyfourhour.widget.Alarms.scheduled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
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

import info.staticfree.android.twentyfourhour.widget.Tick;

/**
 * The receiver is wiring, and wiring is what this asks about: that each
 * broadcast the framework delivers reaches the thing that answers it, and that
 * what comes back reaches the host.
 *
 * <p>Neither of the two things it wires is examined here. What a tick <em>is</em>
 * — its type, its anchor, the {@code PendingIntent} that carries it — belongs to
 * {@code TickTest}; what a picture <em>is</em> — its bitmap's size, the tap on
 * it — belongs to {@code WidgetPictureTest}. Both answer without a broadcast.
 * Left here is only what needs one: that an update arms a tick and draws every
 * id it names, that a resize redraws the one it names, that the last widget
 * leaving stops the clock, and that the tick's own broadcast comes back round to
 * a redraw.
 *
 * <p>A fresh receiver per delivery, deliberately: Android constructs one for
 * each broadcast and discards it afterwards, so a receiver that remembered
 * anything between deliveries would be remembering it in vain.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class TwentyFourHourClockWidgetResizableTest {

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
                scheduled(context()).size());
    }

    /**
     * The round trip: the tick the schedule sends is the tick the receiver
     * recognises. Both halves are {@code Tick}'s and neither is visible from
     * outside it, so nothing but delivering its own intent can show that they
     * meet — and if they ever stopped meeting, the clock would freeze with an
     * alarm still faithfully firing behind it.
     *
     * <p>The intent is taken from the alarm rather than assembled here, which is
     * the whole point: an intent built by the test would only prove the test and
     * {@code isTick} agree.
     */
    @Test
    public void theTickRedrawsEveryWidget() {
        int first = pinWidget();
        int second = pinWidget();
        Tick.schedule(context());
        Bitmap firstBeforeTheTick = bitmapShownOn(first);
        Bitmap secondBeforeTheTick = bitmapShownOn(second);

        deliver(operationOf(only(context())).getSavedIntent());

        assertNotSame("the tick should have redrawn the first widget", firstBeforeTheTick,
                bitmapShownOn(first));
        assertNotSame("the tick should have redrawn the second widget", secondBeforeTheTick,
                bitmapShownOn(second));
    }

    /**
     * A dial left wrong until the next minute turns over looks broken for up to
     * a minute, so a wall clock the user has just moved redraws at once rather
     * than waiting for the tick. Both ways of moving it count: setting the time
     * and changing the zone displace the hands by the same kind of jump, and
     * the manifest filter lists both.
     *
     * <p>Declared in the manifest because the app has no process of its own to
     * register a receiver from, and exempted from the ban on implicit broadcasts
     * to manifest receivers — which is why these two, unlike the tick, may still
     * arrive without naming anybody.
     *
     * <p>Asserted one at a time against a reading taken just before each, or the
     * second delivery would be credited with a redraw the first had already
     * made.
     */
    @Test
    public void movingTheWallClockRedrawsEveryWidget() {
        int widgetId = pinWidget();

        Bitmap beforeTheTimeChange = bitmapShownOn(widgetId);
        deliver(new Intent(Intent.ACTION_TIME_CHANGED));
        assertNotSame("a time change should have redrawn the widget", beforeTheTimeChange,
                bitmapShownOn(widgetId));

        Bitmap beforeTheZoneChange = bitmapShownOn(widgetId);
        deliver(new Intent(Intent.ACTION_TIMEZONE_CHANGED));
        assertNotSame("a timezone change should have redrawn the widget", beforeTheZoneChange,
                bitmapShownOn(widgetId));
    }

    @Test
    public void disablingTheWidgetCancelsTheTick() {
        deliver(update(pinWidget()));
        deliver(new Intent(AppWidgetManager.ACTION_APPWIDGET_DISABLED));

        assertTrue("disabling the widget should leave no alarm behind",
                scheduled(context()).isEmpty());
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
                scheduled(context()).size());
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
