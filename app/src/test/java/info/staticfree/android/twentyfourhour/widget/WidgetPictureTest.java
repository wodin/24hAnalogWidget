package info.staticfree.android.twentyfourhour.widget;

import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isHand;
import static info.staticfree.android.twentyfourhour.dial.Artwork.probe;
import static info.staticfree.android.twentyfourhour.dial.Artwork.probeAlong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.TimeZone;

import info.staticfree.android.twentyfourhour.R;
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
 * java.time.ZoneId#systemDefault()} as it draws, so without this the dial would
 * be turned by wherever the suite happened to run. Which zone is not arbitrary
 * either: {@link #DIAL_ZONE} explains what UTC buys.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class WidgetPictureTest {

    /**
     * The first activity {@link info.staticfree.android.twentyfourhour.ClockUtil}
     * looks for that is present on a stock device. Which of its six candidates
     * is used does not matter here; that one of them is installed does.
     */
    private static final ComponentName DESK_CLOCK =
            new ComponentName("com.android.deskclock", "com.android.deskclock.AlarmClock");

    /**
     * The zone the picture is drawn in for the sun tests below, and the reason
     * they hold on any date.
     *
     * <p>{@code build} draws at {@link java.time.Instant#now()} and there is no
     * seam to hand it a fixed one, so a probe angle has to be daylight — or
     * night — in Buenos Aires every day of the year. Read in its own zone it
     * could not be: the city's sunset wanders 2h21m across the seasons, which is
     * wider than its disagreement with the nominal night the Placeholder draws.
     *
     * <p>Read in UTC it can, because a longitude displaces a day further than a
     * season does. Buenos Aires stands 58.38&deg; west of Greenwich, so its noon
     * falls at 15:53 UTC and its whole night with it — a shift of nearly four
     * hours against the Placeholder's fixed 18:00 to 06:00, where the seasons
     * move each edge by little more than one.
     */
    private static final TimeZone DIAL_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * The wedge of the dial where the Placeholder shades a night that Buenos
     * Aires is still in daylight through: 18:20 to 20:20 UTC, which is 15:20 to
     * 17:20 in the city.
     *
     * <p>Both edges are measured rather than chosen. The near edge stands
     * 5&deg; inside the Placeholder's night, which begins at 90&deg;. The far
     * edge stands 7.35&deg; short of 132.35&deg;, the earliest the sun sets in
     * Buenos Aires on a UTC dial on any day of 2026 — 11 June, computed with
     * this app's own {@code SunPath} over all 365 of them. A degree here is four
     * minutes and a sunset drifts about that much from one year to the next, so
     * the margins are wide enough that the year need not be named.
     */
    private static final double AFTERNOON_FROM = 95;

    private static final double AFTERNOON_TO = 125;

    /**
     * And the wedge where the Placeholder's night has ended while Buenos Aires
     * is still deep inside its own: 06:20 to 08:00 UTC, which is 03:20 to 05:00
     * in the city. The near edge is 5&deg; past where the Placeholder's night
     * ends at 270&deg;, and the far edge 8.34&deg; short of 308.34&deg;, the
     * <em>earliest</em> the city's sun rises on a UTC dial all year — 5
     * December, measured the same way. Earliest is the bound that binds: a wedge
     * ending before the soonest sunrise of the year ends before every other one
     * too.
     *
     * <p>The two wedges flip in opposite directions, which is what makes the
     * pair a measurement: one of them alone would be satisfied by a picture that
     * had simply stopped drawing the overlay at all.
     */
    private static final double SMALL_HOURS_FROM = 275;

    private static final double SMALL_HOURS_TO = 300;

    /**
     * How finely each wedge is sampled: thirteen angles across the afternoon's
     * 30&deg;, eleven across the small hours' 25&deg;.
     */
    private static final double WEDGE_STEP_DEGREES = 2.5;

    /**
     * The ring of radii sampled along each of those angles, in pixels from the
     * centre. The shading is a wedge drawn from the centre out to the inset
     * edge, which the overlay's own default scale of one half puts at 98px of
     * the 196px that is half of 392 — so the ring stops 2px inside it, and no
     * sample sits in an antialiased boundary.
     *
     * <p>Out at the rim rather than in towards the middle because a hand of a
     * fixed width hides the more of a wedge the nearer the centre it is read:
     * the hands are 8px wide, so at 80px each spans 5.7&deg; where at 30px it
     * would span 15&deg;.
     */
    private static final int RING_FROM_PX = 80;

    private static final int RING_TO_PX = 96;

    private static final int RING_STEP_PX = 4;

    /**
     * The share of a wedge that has to flip for it to count as flipped, rather
     * than any single pixel doing. Loose against what was measured when this
     * landed — 70% and 71% of the afternoon over two runs, and 91% to 100% of
     * the small hours as the hands moved between them — and still tight enough
     * that shading which merely clipped the edge of a wedge would fail.
     * It cannot be raised much: the Placeholder inks about three-quarters of
     * what it covers, and two hands can hide half a wedge between them.
     */
    private static final int A_QUARTER_OF_THE_WEDGE = 25;

    private TimeZone deviceZone;

    @Before
    public void pinTheDialsZone() {
        deviceZone = TimeZone.getDefault();
        TimeZone.setDefault(DIAL_ZONE);
    }

    @After
    public void restoreTheDevicesZone() {
        TimeZone.setDefault(deviceZone);
    }

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
     * A tap opens the device's clock app, when the probing in {@code ClockUtil}
     * finds one. Whether it is attached afresh each time, and why it has to be,
     * {@code build} explains.
     */
    @Test
    public void thePictureOpensTheClockAppWhenTheDeviceHasOne() {
        shadowOf(context().getPackageManager()).addActivityIfNotPresent(DESK_CLOCK);

        assertTrue("a device with a clock app should give the picture a tap",
                inflate().hasOnClickListeners());
    }

    /**
     * And carries no tap when there is no clock app to open — the widget is
     * then a picture and nothing else, rather than one that swallows a tap.
     */
    @Test
    public void thePictureHasNoTapWhenTheDeviceHasNoClockApp() {
        assertFalse("with no clock app installed there is nothing to tap through to",
                inflate().hasOnClickListeners());
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
     * What share of the pixels sampled across a wedge of the dial, as a
     * percentage rounded down, the first picture paints darker than the second
     * does. Zero means the first added no ink at all in that wedge that the
     * second had not added already — a single pixel of the sixty-odd sampled
     * still rounds to one per cent rather than to none.
     *
     * <p>The red channel alone decides it. Everything either picture paints over
     * the Face here is black at some alpha — the shading, its nested twilights
     * and the Placeholder's hatch alike — so all three channels move together,
     * and the one colour on the dial that does not, the high noon marker, stands
     * between 54&deg; and 62&deg; all year and reaches neither wedge.
     */
    private static int percentageDarkerIn(@NonNull Bitmap first, @NonNull Bitmap second,
            double fromAngle, double toAngle) {
        int darker = 0;
        int sampled = 0;

        for (double angle = fromAngle; angle <= toAngle; angle += WEDGE_STEP_DEGREES) {
            for (int radius = RING_FROM_PX; radius <= RING_TO_PX; radius += RING_STEP_PX) {
                if (Color.red(probeAlong(first, angle, radius))
                        < Color.red(probeAlong(second, angle, radius))) {
                    darker++;
                }

                sampled++;
            }
        }

        return 100 * darker / sampled;
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

    @NonNull
    private static Bitmap bitmapOf(@NonNull ImageView clock) {
        return ((BitmapDrawable) clock.getDrawable()).getBitmap();
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
