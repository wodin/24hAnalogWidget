package info.staticfree.android.twentyfourhour.widget;

import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isHand;
import static info.staticfree.android.twentyfourhour.dial.Artwork.probe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
     * The bitmap is square at the size of the Face artwork, whatever the host's
     * cell measures — the dial is drawn at the resolution it was painted for and
     * the cell is not consulted. Sizing it from the cell is the next slice, at
     * which point this assertion is meant to fail.
     */
    @Test
    public void theBitmapIsSquareAtTheSizeOfTheFaceArtwork() {
        Bitmap drawn = bitmapOf(inflate());

        assertEquals("the bitmap should be the size of the Face artwork", INTRINSIC_PX,
                drawn.getWidth());
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

    /** The picture, as the host would inflate it. */
    @NonNull
    private static ImageView inflate() {
        Context context = context();
        return WidgetPicture.build(context).apply(context, new FrameLayout(context))
                .findViewById(R.id.clock);
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
