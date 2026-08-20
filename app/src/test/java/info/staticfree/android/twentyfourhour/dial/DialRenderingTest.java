package info.staticfree.android.twentyfourhour.dial;

import static info.staticfree.android.twentyfourhour.dial.Artwork.BEYOND_HOUR_HAND;
import static info.staticfree.android.twentyfourhour.dial.Artwork.INSIDE_FACE_EDGE;
import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.OUTSIDE_HALVED_FACE_EDGE;
import static info.staticfree.android.twentyfourhour.dial.Artwork.OUTSIDE_INTRINSIC_BOX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.WITHIN_HOUR_HAND;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isFace;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isHand;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

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
 * The radii come instead from {@link Artwork}, which reads them off the
 * drawables: they say where to look, never what should be found there.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class DialRenderingTest {

    /** Half the artwork: too little room, so the draw path must shrink it. */
    private static final int HALVED_PX = INTRINSIC_PX / 2;

    /** Puts a probe 60px from the centre on the diagonal, well inside the shaded inset. */
    private static final int DIAGONAL_OFFSET = 42;

    /** Half-width of the patch {@link #meanRed} averages over. */
    private static final int PATCH_RADIUS = 4;

    /** Levels of red. Comfortably more than the hatch's dither, far less than its shading. */
    private static final double SHADING_MARGIN = 1;

    @Test
    public void theIntrinsicSizeIsTheSizeOfTheFaceArtwork() {
        assertEquals(INTRINSIC_PX, Dial.createStandard(context()).getIntrinsicSize());
    }

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
     */
    @Test
    public void smallerBoundsShrinkTheWholeDialToFit() {
        Bitmap dial = render(HALVED_PX, at(18, 0));

        assertOpaque("the Face should have shrunk to fit the bounds", dial,
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
    public void largerBoundsLeaveTheDialAtItsIntrinsicSizeAndCentred() {
        Bitmap dial = render(2 * INTRINSIC_PX, at(12, 0));

        assertEmpty("the corner should be empty", dial, -INTRINSIC_PX, -INTRINSIC_PX);

        for (int sign = -1; sign <= 1; sign += 2) {
            assertOpaque("the Face should still end where its artwork ends", dial,
                    sign * INSIDE_FACE_EDGE, 0);
            assertOpaque("the Face should still end where its artwork ends", dial,
                    0, sign * INSIDE_FACE_EDGE);
            assertEmpty("the dial should not have grown into the larger bounds", dial,
                    sign * OUTSIDE_INTRINSIC_BOX, 0);
            assertEmpty("the dial should not have grown into the larger bounds", dial,
                    0, sign * OUTSIDE_INTRINSIC_BOX);
        }
    }

    /**
     * The bounds are a rectangle in a canvas, not the canvas itself: the dial
     * goes where it is told and touches nothing outside. This is the one thing
     * the old View could not be asked — it always drew across the whole of
     * itself — so it is an addition to the net rather than a retarget of it.
     */
    @Test
    public void theDialDrawsInsideItsBoundsAndNowhereElse() {
        Bitmap wide = Bitmap.createBitmap(2 * INTRINSIC_PX, INTRINSIC_PX, Bitmap.Config.ARGB_8888);
        Rect rightHalf = new Rect(INTRINSIC_PX, 0, 2 * INTRINSIC_PX, INTRINSIC_PX);

        Dial.createStandard(context())
                .draw(new Canvas(wide), rightHalf, at(12, 0), ZoneOffset.UTC);

        assertTrue("a hand should cover the top of the dial in the right half",
                isHand(wide.getPixel(rightHalf.centerX(),
                        rightHalf.centerY() - WITHIN_HOUR_HAND)));
        assertEquals("the half outside the bounds should be untouched", 0,
                Color.alpha(wide.getPixel(INTRINSIC_PX / 2, INTRINSIC_PX / 2)));
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

    /**
     * A square bitmap with the dial drawn across the whole of it. No View, no
     * measure and no layout: a Dial needs a canvas, a rectangle and a moment,
     * and the whole point of the extraction is that it needs nothing else.
     */
    private static Bitmap render(int sizePx, @NonNull Instant time,
            @NonNull DialOverlay... overlays) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Dial.createStandard(context(), overlays)
                .draw(new Canvas(bitmap), new Rect(0, 0, sizePx, sizePx), time, ZoneOffset.UTC);
        return bitmap;
    }

    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }

    /** A fixed instant, read in UTC so the host's timezone cannot move the hands. */
    private static Instant at(int hourOfDay, int minute) {
        return LocalDateTime.of(2026, Month.MARCH, 20, hourOfDay, minute).toInstant(ZoneOffset.UTC);
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
