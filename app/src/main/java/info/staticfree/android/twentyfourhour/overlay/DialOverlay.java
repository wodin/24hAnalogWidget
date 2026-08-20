package info.staticfree.android.twentyfourhour.overlay;

import android.graphics.Canvas;
import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Something drawn on the dial, over the Face.
 *
 * @author <a href="mailto:steve@staticfree.info">Steve Pomeroy</a>
 */
public interface DialOverlay {

    /**
     * Draws this overlay onto {@code canvas}, showing {@code instant} as read in
     * {@code zone}.
     *
     * <p>The dial is square, so one {@code side} answers for both axes and the
     * centre is {@code side / 2}. The canvas is already translated and scaled to
     * fit, so an overlay draws in the dial's own coordinates and never learns
     * how large it is really being drawn.
     *
     * @param canvas the canvas to draw onto
     * @param side the side, in pixels, of the square the dial occupies
     * @param instant the moment to show
     * @param zone the zone that moment is read in
     */
    void draw(@NonNull Canvas canvas, int side, @NonNull Instant instant, @NonNull ZoneId zone);
}
