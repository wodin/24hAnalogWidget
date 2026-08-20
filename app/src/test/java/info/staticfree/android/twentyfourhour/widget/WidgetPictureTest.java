package info.staticfree.android.twentyfourhour.widget;

import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isHand;
import static info.staticfree.android.twentyfourhour.dial.Artwork.probe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import info.staticfree.android.twentyfourhour.R;

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

    /** The picture for a widget in the given cell, as the host would inflate it. */
    @NonNull
    private static ImageView inflate(@NonNull Bundle options) {
        Context context = context();
        return WidgetPicture.build(context, options).apply(context, new FrameLayout(context))
                .findViewById(R.id.clock);
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

    @NonNull
    private static Bitmap bitmapOf(@NonNull ImageView clock) {
        return ((BitmapDrawable) clock.getDrawable()).getBitmap();
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
