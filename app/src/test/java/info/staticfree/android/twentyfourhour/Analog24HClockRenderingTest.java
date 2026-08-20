package info.staticfree.android.twentyfourhour;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.util.Calendar;
import java.util.TimeZone;

import info.staticfree.android.twentyfourhour.overlay.DialOverlay;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;

/**
 * What the dial actually puts on a canvas, asserted by probing individual
 * pixels of a real render. Robolectric's native graphics mode draws through the
 * platform's own Skia, so these are the pixels a device would produce.
 *
 * <p>Probes rather than golden images: a golden needs a checked-in binary no
 * reviewer can read and that diffs uselessly. A probe says which pixel, and why,
 * in one line.
 *
 * <p>Two kinds of expected value appear below and the difference matters. How
 * the dial is arranged — noon at the top, an hour hand shorter than the minute
 * hand, a nominal night shaded below the centre — comes from the dial's
 * specification, so those assertions are able to disagree with the
 * implementation, as {@link
 * info.staticfree.android.twentyfourhour.overlay.HandsOverlayTest} already does.
 * The radii are instead read off the artwork, the only authority on its own
 * dimensions. They say where to look, never what should be found there.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class Analog24HClockRenderingTest {

    /** The Face artwork is 196dp square; the {@code xhdpi} qualifier makes that 392px. */
    private static final int INTRINSIC_PX = 392;

    /** Half of that: too little room for the artwork, so the draw path must shrink it. */
    private static final int HALVED_PX = INTRINSIC_PX / 2;

    /**
     * Radii in pixels from the centre. The hour hand's artwork reaches 98px and
     * the minute hand's 137px, so the radius of a probe alone decides which
     * hands could possibly be under it.
     */
    private static final int WITHIN_HOUR_HAND = 80;

    private static final int BEYOND_HOUR_HAND = 120;

    /** Just inside the Face's painted circle, whose artwork ends at 187px. */
    private static final int INSIDE_FACE_EDGE = 180;

    /** Just outside that circle once halved, and still inside a half-sized canvas. */
    private static final int OUTSIDE_HALVED_FACE_EDGE = 96;

    /**
     * Outside the 392px box the artwork occupies at intrinsic size, so nothing
     * drawn at intrinsic size can reach here however it is positioned.
     */
    private static final int OUTSIDE_INTRINSIC_BOX = 220;

    /** Puts a probe 60px from the centre on the diagonal, well inside the shaded inset. */
    private static final int DIAGONAL_OFFSET = 42;

    /** Half-width of the patch {@link #meanRed} averages over. */
    private static final int PATCH_RADIUS = 4;

    /** Levels of red. Comfortably more than the hatch's dither, far less than its shading. */
    private static final double SHADING_MARGIN = 1;

    @Test
    public void noonPutsBothHandsAtTheTop() {
        Bitmap dial = render(INTRINSIC_PX, at(12, 0));

        assertTrue("a hand should cover the top of the dial",
                isHand(probe(dial, 0, -WITHIN_HOUR_HAND)));
        assertTrue("the right of the dial should be bare Face",
                isFace(probe(dial, WITHIN_HOUR_HAND, 0)));
    }

    /**
     * The one probe that isolates the hour hand: at 18:00 the minute hand still
     * points up, so anything covering the right can only be the hour hand.
     */
    @Test
    public void sixInTheEveningPutsTheHourHandAtTheRight() {
        Bitmap dial = render(INTRINSIC_PX, at(18, 0));

        assertTrue("the hour hand should cover the right of the dial",
                isHand(probe(dial, WITHIN_HOUR_HAND, 0)));
        assertTrue("the left of the dial should be bare Face",
                isFace(probe(dial, -WITHIN_HOUR_HAND, 0)));
    }

    /**
     * At 18:00 the hour hand points right and the minute hand straight up. One
     * radius is therefore covered above the centre and bare to the right, which
     * is only true because the two hands are different lengths.
     */
    @Test
    public void theHourHandIsShorterThanTheMinuteHand() {
        Bitmap dial = render(INTRINSIC_PX, at(18, 0));

        assertTrue("the minute hand should reach this far",
                isHand(probe(dial, 0, -BEYOND_HOUR_HAND)));
        assertTrue("the hour hand should not reach this far",
                isFace(probe(dial, BEYOND_HOUR_HAND, 0)));
    }

    /**
     * Given less room than the artwork needs, the dial shrinks to fit: a whole
     * dial, smaller, rather than a cropped one. Both the Face and the hands go
     * down together, so a hand sits at half the radius it occupies untouched.
     *
     * <p>Only the draw path is netted here. Measurement is deliberately left
     * out: the View's {@code onMeasure} is an adapter detail that the extraction
     * is expected to replace, whereas the drawing must survive it unchanged.
     */
    @Test
    public void aSmallerCanvasShrinksTheWholeDialToFit() {
        Bitmap dial = render(HALVED_PX, at(18, 0));

        assertOpaque("the Face should have shrunk to fit the canvas", dial,
                INSIDE_FACE_EDGE / 2, 0);
        assertEmpty("the Face should end at half its usual radius, not overflow", dial,
                OUTSIDE_HALVED_FACE_EDGE, 0);
        assertEmpty("the dial should still be a circle in a square", dial,
                -HALVED_PX / 2, -HALVED_PX / 2);
        assertTrue("the hour hand should have shrunk with the Face",
                isHand(probe(dial, WITHIN_HOUR_HAND / 2, 0)));
    }

    /**
     * Given more room than the artwork needs, today's dial centres itself and
     * leaves the rest empty rather than growing into it. Recorded because it is
     * what the extraction has to preserve — and so that the later work making
     * the dial fill its bounds has to replace this test knowingly rather than
     * discover it.
     */
    @Test
    public void aLargerCanvasLeavesTheDialAtItsIntrinsicSizeAndCentred() {
        Bitmap dial = render(2 * INTRINSIC_PX, at(12, 0));

        assertEmpty("the corner should be empty", dial, -INTRINSIC_PX, -INTRINSIC_PX);

        for (int sign = -1; sign <= 1; sign += 2) {
            assertOpaque("the Face should still end where its artwork ends", dial,
                    sign * INSIDE_FACE_EDGE, 0);
            assertOpaque("the Face should still end where its artwork ends", dial,
                    0, sign * INSIDE_FACE_EDGE);
            assertEmpty("the dial should not have grown into the larger canvas", dial,
                    sign * OUTSIDE_INTRINSIC_BOX, 0);
            assertEmpty("the dial should not have grown into the larger canvas", dial,
                    0, sign * OUTSIDE_INTRINSIC_BOX);
        }
    }

    /**
     * With no location known, the sun overlay hatches a nominal night: 18:00
     * through midnight to 06:00. Midnight is at the bottom of a 24-hour dial, so
     * that is the lower half and only the lower half.
     *
     * <p>This is also what proves {@link android.graphics.BitmapShader} renders
     * here — the hatch is a tiled bitmap, and a shader that failed to draw would
     * leave both halves untouched.
     */
    @Test
    public void thePlaceholderShadesTheNightHalfOfTheDial() {
        Bitmap bare = render(INTRINSIC_PX, at(18, 0));
        Bitmap shaded = render(INTRINSIC_PX, at(18, 0), new SunPositionOverlay(context()));

        assertTrue("the night half should be darkened",
                meanRed(shaded, -DIAGONAL_OFFSET, DIAGONAL_OFFSET)
                        < meanRed(bare, -DIAGONAL_OFFSET, DIAGONAL_OFFSET) - SHADING_MARGIN);

        // Exactly equal, deliberately. Identical draw calls produce identical
        // pixels, so any difference at all is the hatch bleeding into the day.
        assertEquals("the day half should be untouched",
                meanRed(bare, -DIAGONAL_OFFSET, -DIAGONAL_OFFSET),
                meanRed(shaded, -DIAGONAL_OFFSET, -DIAGONAL_OFFSET), 0);
    }

    private static Bitmap render(int sizePx, @NonNull Calendar time) {
        return render(sizePx, time, null);
    }

    /**
     * Measures, lays out and draws the View by hand, since nothing here does it
     * for us. The widget drives its own View through a similar sequence but
     * passes a raw size where a MeasureSpec belongs; this passes a well-formed
     * one, because what the net has to preserve is the drawing, not the widget's
     * misuse of measurement.
     */
    private static Bitmap render(int sizePx, @NonNull Calendar time,
            @Nullable DialOverlay overlay) {
        Analog24HClock clock = new Analog24HClock(context());
        if (overlay != null) {
            clock.addDialOverlay(overlay);
        }
        clock.setTime(time);

        int spec = View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY);
        clock.measure(spec, spec);
        clock.layout(0, 0, sizePx, sizePx);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        clock.draw(new Canvas(bitmap));
        return bitmap;
    }

    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }

    /** A fixed instant, in UTC so the host's timezone cannot move the hands. */
    private static Calendar at(int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2026, Calendar.MARCH, 20, hourOfDay, minute, 0);
        return calendar;
    }

    /** The pixel that many pixels right of, and below, the centre of the render. */
    private static int probe(@NonNull Bitmap dial, int dx, int dy) {
        return dial.getPixel(dial.getWidth() / 2 + dx, dial.getHeight() / 2 + dy);
    }

    private static void assertOpaque(String reason, @NonNull Bitmap dial, int dx, int dy) {
        assertEquals(reason, 255, Color.alpha(probe(dial, dx, dy)));
    }

    private static void assertEmpty(String reason, @NonNull Bitmap dial, int dx, int dy) {
        assertEquals(reason, 0, Color.alpha(probe(dial, dx, dy)));
    }

    /** The hands are painted a near-black #141414. */
    private static boolean isHand(int pixel) {
        return Color.alpha(pixel) == 255 && Color.red(pixel) < 0x40;
    }

    /**
     * Bare Face rather than a hand over it. Everywhere these tests probe the
     * Face is a light grey above #A0, so the two never blur together. Stronger
     * than "not a hand", which an unpainted pixel would satisfy just as well.
     */
    private static boolean isFace(int pixel) {
        return Color.alpha(pixel) == 255 && Color.red(pixel) > 0x80;
    }

    /** Mean redness of a small patch, for comparing shaded artwork against bare. */
    private static double meanRed(@NonNull Bitmap dial, int dx, int dy) {
        long total = 0;
        int count = 0;
        for (int x = dx - PATCH_RADIUS; x <= dx + PATCH_RADIUS; x++) {
            for (int y = dy - PATCH_RADIUS; y <= dy + PATCH_RADIUS; y++) {
                total += Color.red(probe(dial, x, y));
                count++;
            }
        }
        return (double) total / count;
    }
}
