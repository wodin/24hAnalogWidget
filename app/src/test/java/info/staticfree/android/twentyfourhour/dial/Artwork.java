package info.staticfree.android.twentyfourhour.dial;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;

/**
 * Facts read off the drawables, the colour tests that recognise them, and the
 * way a render is probed against them. The artwork is the only authority on its
 * own dimensions, so these say where a probe should look and what it would be
 * looking at — never what ought to be found there. Expectations about how the
 * dial is arranged belong in the tests.
 *
 * <p>One home for them because two tests probe the same pixels, and a radius
 * copied into both is a radius that goes stale in one.
 */
final class Artwork {

    /** The Face is 196dp square; the {@code xhdpi} qualifier makes that 392px. */
    static final int INTRINSIC_PX = 392;

    /**
     * Radii in pixels from the centre. The hour hand's artwork reaches 98px and
     * the minute hand's 137px, so the radius of a probe alone decides which
     * hands could possibly be under it.
     */
    static final int WITHIN_HOUR_HAND = 80;

    static final int BEYOND_HOUR_HAND = 120;

    /** Just inside the Face's painted circle, whose artwork ends at 187px. */
    static final int INSIDE_FACE_EDGE = 180;

    /** Just outside that circle once halved, and still inside a half-sized canvas. */
    static final int OUTSIDE_HALVED_FACE_EDGE = 96;

    /**
     * Outside the 392px box the artwork occupies at intrinsic size, so nothing
     * drawn at intrinsic size can reach here however it is positioned.
     */
    static final int OUTSIDE_INTRINSIC_BOX = 220;

    /**
     * The pixel that many pixels right of, and below, the centre of a render.
     * Every radius here is measured from the dial's centre, so probing from
     * anywhere else would mean restating the arithmetic at each call.
     */
    static int probe(@NonNull Bitmap render, int dx, int dy) {
        return render.getPixel(render.getWidth() / 2 + dx, render.getHeight() / 2 + dy);
    }

    /** The hands are painted a near-black #141414. */
    static boolean isHand(int pixel) {
        return Color.alpha(pixel) == 255 && Color.red(pixel) < 0x40;
    }

    /**
     * Bare Face rather than a hand over it. Everywhere the tests probe, the Face
     * is a light grey above #A0, so the two never blur together. Stronger than
     * "not a hand", which an unpainted pixel would satisfy just as well.
     */
    static boolean isFace(int pixel) {
        return Color.alpha(pixel) == 255 && Color.red(pixel) > 0x80;
    }

    private Artwork() {
    }
}
