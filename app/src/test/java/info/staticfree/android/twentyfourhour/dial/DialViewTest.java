package info.staticfree.android.twentyfourhour.dial;

import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.WITHIN_HOUR_HAND;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isHand;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

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
import java.time.ZoneId;

/**
 * The adapter's own two jobs, which {@link DialRenderingTest} deliberately does
 * not cover: telling the layout how large the dial wants to be, and handing the
 * dial the bounds the layout settled on.
 *
 * <p>Not covered: that an unpinned view shows the present moment. There is no
 * clock to control at this seam — {@link Instant#now()} does not go through
 * anything Robolectric shadows — and a test that renders twice and hopes the
 * minute did not turn between them is a flake waiting to happen.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class DialViewTest {

    @Test
    public void theViewAsksForTheDialsIntrinsicSize() {
        DialView view = new DialView(RuntimeEnvironment.getApplication());

        int unbounded = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(unbounded, unbounded);

        assertEquals(INTRINSIC_PX, view.getMeasuredWidth());
        assertEquals(INTRINSIC_PX, view.getMeasuredHeight());
    }

    /**
     * Laid out larger than it asked for, the view still hands the dial the whole
     * of itself — the bounds are the view, and what the dial does inside them is
     * the dial's business, asserted in {@link DialRenderingTest}.
     */
    @Test
    public void theViewDrawsTheDialAcrossItsWholeSelf() {
        int sizePx = 2 * INTRINSIC_PX;
        DialView view = new DialView(RuntimeEnvironment.getApplication());
        view.setTime(localNoon());

        int spec = View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY);
        view.measure(spec, spec);
        view.layout(0, 0, sizePx, sizePx);

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));

        assertTrue("a hand should cover the top of the centred dial",
                isHand(bitmap.getPixel(sizePx / 2, sizePx / 2 - WITHIN_HOUR_HAND)));
    }

    /**
     * The instant that is noon in the zone the view reads, so the hands land at
     * the top of the dial. The view uses the system zone and offers no way to
     * change it, so the test has to meet it there.
     */
    @NonNull
    private static Instant localNoon() {
        return LocalDateTime.of(2026, Month.MARCH, 20, 12, 0).atZone(ZoneId.systemDefault())
                .toInstant();
    }
}
