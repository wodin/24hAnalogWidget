package info.staticfree.android.twentyfourhour.dial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import info.staticfree.android.twentyfourhour.overlay.DialOverlay;

/**
 * What a Dial hands its overlays. {@link DialRenderingTest} watches the pixels;
 * this watches the seam itself, which pixels can only show indirectly.
 *
 * <p>The claim is that an overlay is told the dial's <em>intrinsic</em> side and
 * not the on-screen one — the canvas transform does the fitting, so an overlay
 * that sized itself from the bounds would be scaled twice.
 */
@RunWith(RobolectricTestRunner.class)
public class DialOverlaySeamTest {
    private static final int INTRINSIC_PX = 64;
    private static final ZoneId ZONE = ZoneId.of("Australia/Adelaide");

    private static final Instant MOMENT =
            LocalDateTime.of(2026, Month.MARCH, 20, 9, 41, 30).toInstant(ZoneOffset.UTC);

    @Test
    public void tellsTheOverlayTheIntrinsicSideNotTheOnScreenOne() {
        Recorder recorder = drawInto(new Rect(0, 0, 500, 300));

        assertEquals("the artwork's own side, not the 300px square it was fitted into",
                INTRINSIC_PX, recorder.side);
    }

    @Test
    public void theSideDoesNotChangeWithTheBounds() {
        assertEquals(drawInto(new Rect(0, 0, 500, 300)).side,
                drawInto(new Rect(0, 0, 17, 17)).side);
    }

    @Test
    public void passesTheMomentAndTheZoneThroughUntouched() {
        Recorder recorder = drawInto(new Rect(0, 0, 128, 128));

        assertEquals(MOMENT, recorder.instant);
        assertSame(ZONE, recorder.zone);
    }

    @Test
    public void drawsEveryOverlayOnceInTheOrderGiven() {
        Recorder first = new Recorder();
        Recorder second = new Recorder();
        List<Recorder> order = new ArrayList<>();
        first.log = order;
        second.log = order;

        render(new Rect(0, 0, 128, 128), first, second);

        assertEquals(List.of(first, second), order);
    }

    @NonNull
    private static Recorder drawInto(@NonNull Rect bounds) {
        Recorder recorder = new Recorder();
        render(bounds, recorder);
        return recorder;
    }

    private static void render(@NonNull Rect bounds, @NonNull DialOverlay... overlays) {
        Bitmap bitmap = Bitmap.createBitmap(Math.max(bounds.width(), 1),
                Math.max(bounds.height(), 1), Bitmap.Config.ARGB_8888);
        new Dial(face(), overlays).draw(new Canvas(bitmap), bounds, MOMENT, ZONE);
    }

    /** A Face of a known intrinsic size and nothing else; the artwork is not what is under test. */
    @NonNull
    private static Drawable face() {
        return new ColorDrawable() {
            @Override
            public int getIntrinsicWidth() {
                return INTRINSIC_PX;
            }

            @Override
            public int getIntrinsicHeight() {
                return INTRINSIC_PX;
            }
        };
    }

    /** An overlay that draws nothing and remembers what it was asked to draw. */
    private static class Recorder implements DialOverlay {
        private int side;
        private Instant instant;
        private ZoneId zone;
        private List<Recorder> log;

        @Override
        public void draw(@NonNull Canvas canvas, int side, @NonNull Instant instant,
                @NonNull ZoneId zone) {
            this.side = side;
            this.instant = instant;
            this.zone = zone;

            if (log != null) {
                log.add(this);
            }
        }
    }
}
