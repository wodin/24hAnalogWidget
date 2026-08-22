package info.staticfree.android.twentyfourhour.widget;

import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isHand;
import static info.staticfree.android.twentyfourhour.dial.Artwork.probe;
import static info.staticfree.android.twentyfourhour.dial.Shading.AFTERNOON_FROM;
import static info.staticfree.android.twentyfourhour.dial.Shading.AFTERNOON_TO;
import static info.staticfree.android.twentyfourhour.dial.Shading.A_QUARTER_OF_THE_WEDGE;
import static info.staticfree.android.twentyfourhour.dial.Shading.SMALL_HOURS_FROM;
import static info.staticfree.android.twentyfourhour.dial.Shading.SMALL_HOURS_TO;
import static info.staticfree.android.twentyfourhour.dial.Shading.percentageDarkerIn;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.Arrays;
import java.util.List;

import info.staticfree.android.twentyfourhour.R;
import info.staticfree.android.twentyfourhour.dial.Shading;
import info.staticfree.android.twentyfourhour.observer.Observer;
import info.staticfree.android.twentyfourhour.sun.BuenosAires;

/**
 * The picture, read the way the host reads it: inflate the {@link
 * android.widget.RemoteViews} and look at the view that comes out.
 *
 * <p>Assembling a picture needs no receiver, no broadcast and no host, which is
 * the point of it being its own object — everything here is one call and one
 * inflation. What the receiver does with the picture is the receiver's test.
 *
 * <p>Inflated into a bare {@link FrameLayout} rather than an {@code
 * AppWidgetHostView}: {@code apply} wants a parent only for layout parameters,
 * and the host view is what the receiver's test already exercises.
 *
 * <p>The zone is pinned because the picture reads {@link
 * java.time.ZoneId#systemDefault()} as it draws, so without it the dial would be
 * turned by wherever the suite happened to run. Which zone is not arbitrary
 * either, and neither are the wedges its one sun test probes: {@link Shading}
 * holds both, and holds them together because the second is void without the
 * first.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class WidgetPictureTest {

    /**
     * A stock device's clock app, named as the emulator names it. Which package
     * it is does not matter and is never asked: what the picture looks for is
     * the {@link AlarmClock#ACTION_SHOW_ALARMS} handler, so this component is
     * only somewhere for that filter to hang.
     */
    private static final ComponentName CLOCK_APP =
            new ComponentName("com.android.deskclock", "com.android.deskclock.HandleApiCalls");

    @Rule
    public final TestRule dialsZone = Shading.inTheDialsZone();

    /**
     * Probed at the centre rather than merely asserted non-null, because the
     * layout carries the Face as its {@code src}: a picture that set no bitmap
     * would still inflate to an image, and one of exactly this size. Both hands
     * meet at the centre whatever the time, and the bare Face there is a light
     * {@code 0xEE} grey, so a hand at the centre is a render and nothing else.
     */
    @Test
    public void thePictureCarriesADrawnDialAndNotTheBareFace() {
        assertTrue("the hands should meet at the centre of the picture",
                isHand(probe(bitmapOf(inflate()), 0, 0)));
    }

    /**
     * The larger of the two squares the cell implies, at display density. Which
     * two, and why the larger, {@code sideForCell} explains.
     *
     * <p>The cell is deliberately lopsided — 130dp square in portrait, 150dp in
     * landscape — so that the three readings this could have are three different
     * numbers: 300px for the rule, 260px for reading portrait alone, and
     * {@code INTRINSIC_PX} for not reading the cell at all. A square cell would
     * let the portrait-only bug pass.
     */
    @Test
    public void theBitmapTakesTheLargerSquareTheCellImplies() {
        Bitmap drawn = bitmapOf(inflate(cell(130, 300, 150, 220)));

        assertEquals("150dp is the larger square, and 300px is 150dp at this density", 300,
                drawn.getWidth());
        assertEquals("the bitmap should be square", drawn.getWidth(), drawn.getHeight());
    }

    /**
     * A cell no host would report, to reach the ceiling: a bitmap cannot usefully
     * be larger than the screen it is shown on, and the host rejects a
     * {@code RemoteViews} carrying one past a limit that scales with the display.
     * A cell measured <em>on</em> this display cannot exceed the display, so the
     * cap never binds in practice — it is here so that a bundle which is wrong,
     * or from somewhere else, costs a soft dial rather than a rejected picture.
     */
    @Test
    public void theBitmapIsCappedAtTheDisplaysSmallerDimension() {
        Bitmap drawn = bitmapOf(inflate(cell(1000, 1000, 1000, 1000)));

        assertEquals("the bitmap should stop at the display's smaller dimension",
                smallerDisplayDimension(), drawn.getWidth());
    }

    /**
     * With no cell reported — which is what a host that has never sized this
     * widget sends — the bitmap falls back to the artwork's own size. Some
     * resolution has to be chosen, and the one the art was drawn at is the only
     * one that is neither a guess nor a magic number.
     *
     * <p>Probed as well as measured, and this is the one size test that has to
     * be. The layout's {@code src} is the Face at exactly {@code INTRINSIC_PX},
     * so here — and only here, where the expected size is the artwork's own —
     * the dimensions alone cannot tell a render from a picture that set no
     * bitmap at all. The other two ask for sizes the {@code src} does not have.
     */
    @Test
    public void theBitmapFallsBackToTheArtworksSizeWhenNoCellIsReported() {
        Bitmap drawn = bitmapOf(inflate(new Bundle()));

        assertTrue("the fallback should be a drawn dial and not the layout's own Face",
                isHand(probe(drawn, 0, 0)));
        assertEquals("with no cell to read, the bitmap should be the size of the Face artwork",
                INTRINSIC_PX, drawn.getWidth());
        assertEquals("the bitmap should be square", drawn.getWidth(), drawn.getHeight());
    }

    /**
     * The picture shades the observer's own night rather than a nominal one,
     * which is the whole of what this slice owes: that an {@link Observer}
     * handed to {@code build} reaches the overlay at all. Where on the dial a
     * sun path lands is {@code SunPositionOverlayTest}'s question, and what time
     * it rises is {@code SunPathAlmanacTest}'s.
     *
     * <p>Measured as a difference between two pictures rather than against
     * absolute colours, because the Face and the hands lie under the shading
     * here and the hatch is as light a wash as the shading is. Two pictures of
     * the same moment that differ only in whether an observer was given can be
     * compared pixel for pixel: everything they share cancels, and what is left
     * is the sun.
     *
     * <p><strong>A wedge of angles rather than the single radius {@code
     * SunPositionOverlayTest} probes.</strong> There the overlay is alone on an
     * empty bitmap; here the hands are drawn over it at whatever time the suite
     * runs, and a hand parked on one angle hides that whole radius. Across a
     * wedge they cannot — 5.7&deg; each out at this ring, against the narrower
     * wedge's 25&deg; — and where they do fall they cost nothing, because a hand
     * paints the same pixels in both pictures and so reads as no difference
     * rather than as one in the wrong direction.
     *
     * <p>That last holds as long as both pictures are drawn in the same minute,
     * which is the one way these assertions can fail on a picture that is
     * working: each {@code build} reads its own {@link java.time.Instant#now()},
     * and a minute rolling over between two calls milliseconds apart moves the
     * minute hand 6&deg;. It would have to move it into one of the wedges as
     * well, which is about one run in six thousand, and the failure is loud and
     * settled by a rerun. Skipping the pixels a hand paints was the alternative
     * and does not close the hole either, because the fringe it antialiases is
     * not one of them.
     *
     * <p>The picture drawn for nobody is the control rather than a second
     * subject. That the Placeholder is a hatch, and covers exactly a nominal
     * night, is pinned in {@code SunPositionOverlayTest}, where nothing is drawn
     * over it to blur the texture.
     */
    @Test
    public void thePictureShadesTheObserversNightAndNotANominalOne() {
        Bitmap forBuenosAires = bitmapOf(inflate(new Bundle(), BuenosAires.observer()));
        Bitmap forNobody = bitmapOf(inflate(new Bundle(), null));

        assertEquals("Buenos Aires' afternoon should carry no shading at all", 0,
                percentageDarkerIn(forBuenosAires, forNobody, AFTERNOON_FROM, AFTERNOON_TO));
        assertTrue("and should have the nominal night's hatch lifted off most of it",
                percentageDarkerIn(forNobody, forBuenosAires, AFTERNOON_FROM, AFTERNOON_TO)
                        > A_QUARTER_OF_THE_WEDGE);

        assertTrue("the city's small hours should be shaded, though no nominal night reaches them",
                percentageDarkerIn(forBuenosAires, forNobody, SMALL_HOURS_FROM, SMALL_HOURS_TO)
                        > A_QUARTER_OF_THE_WEDGE);
        assertEquals("and nothing there should be lighter than the picture drawn for nobody", 0,
                percentageDarkerIn(forNobody, forBuenosAires, SMALL_HOURS_FROM, SMALL_HOURS_TO));
    }

    /**
     * A tap opens the device's alarm list, when the device has one to open.
     * Whether the tap is attached afresh each time, and why it has to be,
     * {@code build} explains.
     *
     * <p>The handler is registered against the action rather than at a fixed
     * component, because that is the whole of what the picture asks the
     * platform. This cannot fail the way the vendor table it replaced did: that
     * asked six named packages whether they were installed, which stopped being
     * a question this app is allowed to ask at API 30 — and answered null on
     * every modern phone while this suite went on passing, because {@code
     * ShadowPackageManager} does not filter by package visibility.
     *
     * <p><strong>Tapped rather than merely counted.</strong> That a picture has
     * a tap stopped being news the moment every picture had one, so what is
     * asked here is where the tap goes — and the answer has to be <em>straight
     * to the clock app</em>. Nothing is broadcast on this path, which is the
     * assertion that pins the direct intent: routing a working tap through
     * {@link UnansweredTap} would read as working here and would put this app
     * in the way of an activity start it has no business being in the way of.
     */
    @Test
    public void thePictureOpensTheAlarmListWhenTheDeviceHasOne() {
        givenAnAlarmListIsInstalled();

        tap(inflate());

        assertEquals("a tap should open whatever shows alarms", AlarmClock.ACTION_SHOW_ALARMS,
                activityStarted().getAction());
        assertEquals("and should get there without a hop through this app", emptyList(),
                broadcastsSent());
    }

    /**
     * And says so when there is no alarm list to open, rather than being a tap
     * that goes nowhere. Both times this app lost its tap on the phone the
     * symptom was identical to a widget that had never had one, so the picture
     * now always carries a tap and the one it carries here is a complaint.
     *
     * <p>Only the component is asserted. What {@link UnansweredTap} then says
     * is its own test's business; all the picture owes is having addressed the
     * envelope.
     */
    @Test
    public void theTapSaysSoWhenTheDeviceHasNoAlarmList() {
        tap(inflate());

        assertEquals("with nothing to open, a tap should reach the receiver that says so",
                singletonList(new ComponentName(context(), UnansweredTap.class)),
                broadcastsSent().stream().map(Intent::getComponent).collect(toList()));
    }

    /**
     * Attaching the tap is only half of opening the alarm list. The activity
     * that answers {@link AlarmClock#ACTION_SHOW_ALARMS} is guarded by {@link
     * Manifest.permission#SET_ALARM}, so without it the tap resolves, sends,
     * and is turned away by the activity manager — measured on an A34, where
     * the widget looked exactly as it had when it carried no tap at all.
     *
     * <p>Asserted against the manifest because there is nowhere else it can be
     * asserted: the permission is not read by any code here, it is checked by
     * the other app's process, and a build that dropped it would pass every
     * other test in this suite while the tap silently stopped working.
     */
    @Test
    public void theTapCarriesThePermissionTheAlarmListIsGuardedBy() throws NameNotFoundException {
        PackageInfo manifest = context().getPackageManager()
                .getPackageInfo(context().getPackageName(), PackageManager.GET_PERMISSIONS);

        assertTrue("the manifest should ask for the permission the alarm list is guarded by",
                Arrays.asList(manifest.requestedPermissions).contains(Manifest.permission.SET_ALARM));
    }

    /**
     * Registers a handler for {@link AlarmClock#ACTION_SHOW_ALARMS}, which is
     * what every phone with a clock app has and what the picture resolves
     * against.
     *
     * <p>The filter carries {@link android.content.Intent#CATEGORY_DEFAULT} as
     * well as the action, because an implicit intent is only resolvable if some
     * filter accepts it by default — the same rule that decides whether {@code
     * startActivity} would find anyone, which is the question being asked.
     */
    private static void givenAnAlarmListIsInstalled() {
        IntentFilter showsAlarms = new IntentFilter(AlarmClock.ACTION_SHOW_ALARMS);
        showsAlarms.addCategory(Intent.CATEGORY_DEFAULT);

        shadowOf(context().getPackageManager()).addActivityIfNotPresent(CLOCK_APP);
        shadowOf(context().getPackageManager()).addIntentFilterForActivity(CLOCK_APP, showsAlarms);
    }

    /**
     * The picture for a widget in the given cell, seen from the given place, as
     * the host would inflate it.
     */
    @NonNull
    private static ImageView inflate(@NonNull Bundle options, @Nullable Observer observer) {
        Context context = context();
        return WidgetPicture.build(context, options, observer)
                .apply(context, new FrameLayout(context)).findViewById(R.id.clock);
    }

    /**
     * The picture for a widget nobody knows the position of, which is what the
     * receiver hands over whenever the phone has no fix to give it.
     */
    @NonNull
    private static ImageView inflate(@NonNull Bundle options) {
        return inflate(options, null);
    }

    /**
     * The picture for a widget whose cell does not matter to the test at hand.
     * A real host reports one; these tests are about the tap and the render, and
     * an empty bundle keeps the size out of the way of both.
     */
    @NonNull
    private static ImageView inflate() {
        return inflate(new Bundle());
    }

    /**
     * The four numbers a host reports about a cell, in the order the widths and
     * heights pair up: portrait is {@code minWidth}×{@code maxHeight}, landscape
     * is {@code maxWidth}×{@code minHeight}. All in dp, as the host sends them.
     */
    @NonNull
    private static Bundle cell(int minWidth, int maxWidth, int minHeight, int maxHeight) {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minWidth);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight);
        return options;
    }

    private static int smallerDisplayDimension() {
        DisplayMetrics display = context().getResources().getDisplayMetrics();
        return Math.min(display.widthPixels, display.heightPixels);
    }

    /**
     * Taps the widget, the way the host does: the click listener {@code
     * RemoteViews} attached sends the {@link android.app.PendingIntent}, and
     * where that lands is what the two tap tests read back.
     *
     * <p>The precondition is asserted here rather than in either test, because
     * it is the same for both and because a picture with no tap would otherwise
     * fail them as "nothing was sent" — true, and the wrong story.
     */
    private static void tap(@NonNull ImageView clock) {
        assertTrue("every picture should carry a tap", clock.hasOnClickListeners());

        clock.performClick();
    }

    /** Everything this app has broadcast, in order. */
    @NonNull
    private static List<Intent> broadcastsSent() {
        return shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents();
    }

    /**
     * The activity a tap asked for. Fails rather than returning null, the way
     * {@code Alarms.only} does: a tap that went somewhere else should read as
     * that, not as a null dereference three lines later.
     */
    @NonNull
    private static Intent activityStarted() {
        Intent started = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();

        assertNotNull("the tap should have started an activity", started);

        return started;
    }

    @NonNull
    private static Bitmap bitmapOf(@NonNull ImageView clock) {
        return ((BitmapDrawable) clock.getDrawable()).getBitmap();
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
