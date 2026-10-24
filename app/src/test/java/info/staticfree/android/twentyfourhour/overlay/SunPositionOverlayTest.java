package info.staticfree.android.twentyfourhour.overlay;

import static info.staticfree.android.twentyfourhour.dial.Artwork.INTRINSIC_PX;
import static info.staticfree.android.twentyfourhour.dial.Artwork.isSunOrange;
import static info.staticfree.android.twentyfourhour.dial.Artwork.probeAlong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.time.ZonedDateTime;
import java.util.function.IntPredicate;

import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.observer.Observer;
import info.staticfree.android.twentyfourhour.sun.Band;
import info.staticfree.android.twentyfourhour.sun.BuenosAires;
import info.staticfree.android.twentyfourhour.sun.SunPath;
/**
 * Whether the shading lands where the observer's own sun puts it, or on a
 * nominal night that belongs to nobody.
 *
 * <p>The times themselves are not under test here — {@code SunPathAlmanacTest}
 * already pins all nine to within two minutes of what two services published,
 * and says at length why two minutes is as fine as its sources go. What this
 * owes is the two steps after that: that an {@link Observer} handed in
 * reaches the {@link info.staticfree.android.twentyfourhour.sun.SunPath} at all,
 * and that a moment on that path is drawn at the dial angle it belongs to. Both
 * are only visible in pixels, which is why this is the one overlay test that
 * renders.
 *
 * <p><strong>Two angles that flip in opposite directions.</strong> The
 * Placeholder covers 18:00 through midnight to 06:00; Buenos Aires on this day
 * covers 18:23 through midnight to 07:33. So 18:08 is inside the first and
 * outside the second, and 06:40 is outside the first and inside the second. One
 * probe alone would be satisfied by an overlay that drew nothing at all, and
 * the pair cannot be: they rule out a path that never arrived, a path that came
 * back null, an offset dial, and a wedge drawn from its end to its start.
 *
 * <p><strong>And a third at noon, which those two cannot replace.</strong> A
 * night is very nearly symmetric about the dial's vertical axis, so a mapping
 * that ran <em>anticlockwise</em> would move both of the probes above onto the
 * other night edge and satisfy them both — 18:08 would land just outside a
 * sunset now standing at 264.25&deg;, and 06:40 just inside a sunrise now at
 * 66.75&deg;. What such a dial cannot hide is that it shades the middle of the
 * day: the wedge would then run from 264.25&deg; through straight up to
 * 66.75&deg;, which is 05:37 through noon to 16:27. Noon is the one angle a
 * mirror moves from outside the night to well inside it, so it is probed too.
 *
 * <p><strong>And the twilights and the high noon mark, which those three are
 * blind to.</strong> The twilight arcs are nested <em>inside</em> the
 * sunrise-to-sunset wedge — each evening twilight is later than sunset and each
 * morning one earlier than sunrise — so they decide how dark a night pixel is
 * and never whether it is painted at all, and a probe that counts coverage
 * cannot see one. The high noon mark is two degrees wide and stands at this
 * place's own high noon, which is nearly fifteen degrees round the dial from
 * the dial's noon, so every probe above misses it too. Both are measured below
 * instead, so that deleting either from {@code draw} is caught here rather than
 * on a phone.
 *
 * <p>The overlay is drawn by itself onto an empty bitmap rather than through a
 * {@link info.staticfree.android.twentyfourhour.dial.Dial}, so every painted
 * pixel here is the sun's and there are no hands or Face to tell it apart from.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
public class SunPositionOverlayTest {

    /**
     * 18:08 as an angle on the dial, clockwise from straight up: midnight is at
     * the bottom, so {@code 180 + 18h08m x 15 degrees per hour} modulo a turn.
     * Eight minutes into the Placeholder's night, and fifteen minutes short of
     * Buenos Aires' sunset at 18:23, which stands at 95.75&deg;.
     *
     * <p>Stated as a number rather than computed from {@code Dial.angleFor},
     * deliberately: the mapping is half of what this test is here to check, and
     * a probe that used it could not see it move.
     */
    private static final double EARLY_EVENING = 92;

    /**
     * 06:40 the same way. Forty minutes after the Placeholder's night ends, and
     * fifty-three minutes short of Buenos Aires' sunrise at 07:33, which stands
     * at 293.25&deg;.
     */
    private static final double EARLY_MORNING = 280;

    /**
     * Straight up, where the dial fixes noon. Broad daylight in Buenos Aires
     * and outside the Placeholder's night as well, so this probe discriminates
     * between neither of those — it is here for the anticlockwise dial above,
     * which is the one error the other two probes are blind to.
     *
     * <p>Thirteen and a half degrees clear of the high noon marker, which
     * stands at 14.5&deg; on this day and is two degrees wide.
     */
    private static final double NOON = 0;

    /**
     * The radii, in pixels from the centre, sampled along one dial angle. The
     * shading is a wedge drawn from the centre out to the inset edge, which the
     * overlay's own default scale of one half puts at 98px of the 196px that is
     * half of 392. Every pixel on one radius of a wedge is inside
     * it or outside it together.
     *
     * <p>A whole radius rather than a single pixel because the Placeholder is a
     * hatch of diagonal lines with transparent gaps between them, and one probe
     * can land in a gap and read as unshaded. Sixty-one samples cannot all be
     * gaps, and none of them can be ink where nothing was drawn.
     *
     * <p><strong>The inner bound is load-bearing.</strong> A degree is worth
     * fewer pixels the nearer the centre you stand, so a sample rounded to the
     * pixel grid there sits at a coarser angle than the one asked for: below
     * about 15px the 92&deg; probe drifts within antialiasing reach of the
     * 95.75&deg; edge, and by 14px it rounds onto 90.000&deg; exactly, which is
     * the Placeholder's own boundary. At 30px the worst sample is 2.0px clear of
     * any edge, against the 0.71px an antialiased edge can reach, so the margin
     * is nearly threefold rather than nearly nothing.
     */
    private static final int INNERMOST_PROBE_PX = 30;

    private static final int OUTERMOST_PROBE_PX = 90;

    /** The one radius a depth probe reads at; {@link #shadeDepthAt} says why 60. */
    private static final int SHADE_PROBE_PX = 60;

    /**
     * Spelt as {@code Dial} and {@code HandsOverlay} both spell it. All three
     * are private to their own file, so this is a fourth copy of one number
     * rather than a fourth name for it.
     */
    private static final double DEGREES_PER_TURN = 360;

    /**
     * The shading follows the observer's own sunrise and sunset, at both of the
     * angles where those disagree with a nominal night.
     */
    @Test
    public void theSunIsDrawnWhereTheObserverStands() {
        Bitmap drawn = render(BuenosAires.observer());

        assertEquals("18:08 is still daylight in Buenos Aires, whose sun sets at 18:23", 0,
                paintedAlong(drawn, EARLY_EVENING));
        assertTrue("06:40 is still night in Buenos Aires, whose sun rises at 07:33",
                paintedAlong(drawn, EARLY_MORNING) > 0);
        assertEquals("noon is daylight everywhere the sun rises at all", 0,
                paintedAlong(drawn, NOON));
    }

    /**
     * And with no observer the same two angles come out the other way round,
     * which is what makes the pair above a measurement rather than a coincidence.
     */
    @Test
    public void thePlaceholderShadesANominalNightInstead() {
        Bitmap drawn = render(null);

        assertTrue("18:08 is inside the nominal night, which begins at 18:00",
                paintedAlong(drawn, EARLY_EVENING) > 0);
        assertEquals("06:40 is outside it, because that night ends at 06:00", 0,
                paintedAlong(drawn, EARLY_MORNING));
    }

    /**
     * Each of the three twilights paints a narrower wedge over the night the
     * one before it left, so a night pixel gets darker with every horizon the
     * sun has already crossed. Four probes, one in each band, and the claim is
     * the staircase rather than any single reading: it takes a probe per
     * {@code drawInsetArc} call to fail when that one call goes.
     *
     * <p>Depth is read as alpha because every coat is the same black at
     * {@code alpha 20} on a bitmap that started transparent, so more coats can
     * only mean more alpha — 20, 38, 55 and 71 for one through four of them.
     * Composing them over a background first would say the same thing in
     * darkness and would need a background to be true of.
     */
    @Test
    public void eachTwilightDeepensTheNightBeneathIt() {
        SunPath path = BuenosAires.sunPath();
        Bitmap drawn = render(BuenosAires.observer());

        Band night = path.night();
        Band civil = path.civilTwilight();
        Band nautical = path.nauticalTwilight();
        Band astronomical = path.astronomicalTwilight();

        int nightDepth = shadeDepthAt(drawn, midwayAngle(night.evening(), civil.evening()));
        int civilDepth = shadeDepthAt(drawn, midwayAngle(civil.evening(), nautical.evening()));
        int nauticalDepth =
                shadeDepthAt(drawn, midwayAngle(nautical.evening(), astronomical.evening()));
        int astronomicalDepth =
                shadeDepthAt(drawn, midwayAngle(astronomical.evening(), astronomical.morning()));

        assertTrue("the hour after sunset is shaded at all", nightDepth > 0);
        assertTrue("civil twilight is darker than the sunset wedge alone",
                civilDepth > nightDepth);
        assertTrue("nautical twilight is darker than civil", nauticalDepth > civilDepth);
        assertTrue("astronomical twilight is darker than nautical",
                astronomicalDepth > nauticalDepth);
    }

    /**
     * The high noon mark stands where this observer's sun actually crosses the
     * meridian, which is not where the dial puts noon. Buenos Aires runs nearly
     * an hour behind its own meridian, so the two are about fifteen degrees
     * apart — far enough that the mark being at one of them rules out the other,
     * and the only reason a probe at {@link #NOON} never sees orange.
     *
     * <p>The angle is computed from the path rather than written down, so that
     * moving the fixture's date moves the probe with it instead of quietly
     * pointing it at empty dial. That costs the mapping check the other probes
     * make — this one would follow {@code Dial.angleFor} wherever it went — and
     * those three already own that question.
     */
    @Test
    public void theHighNoonMarkStandsAtTheObserversOwnHighNoon() {
        SunPath path = BuenosAires.sunPath();
        Bitmap drawn = render(BuenosAires.observer());

        assertTrue("the sun is marked at high noon, " + path.highNoon().toLocalTime(),
                sunOrangeAlong(drawn, Dial.angleFor(path.highNoon().toLocalTime())) > 0);
        assertEquals("and not at the dial's noon, which high noon is not", 0,
                sunOrangeAlong(drawn, NOON));
    }

    /**
     * The overlay alone on a transparent bitmap, for Buenos Aires' 15 August
     * whether or not it is told that is where it stands: the day asked about is
     * the same either way, so the observer is the only difference between the
     * two renders.
     */
    @NonNull
    private static Bitmap render(@Nullable Observer observer) {
        Bitmap bitmap =
                Bitmap.createBitmap(INTRINSIC_PX, INTRINSIC_PX, Bitmap.Config.ARGB_8888);
        SunPositionOverlay overlay =
                new SunPositionOverlay(RuntimeEnvironment.getApplication());

        overlay.setObserver(observer);
        overlay.draw(new Canvas(bitmap), INTRINSIC_PX, BuenosAires.SOMETIME_THAT_DAY,
                BuenosAires.ZONE);

        return bitmap;
    }

    /**
     * How many of the sampled radii along {@code dialAngle} hold a pixel
     * {@code wanted} accepts. The count rather than a yes or no, so that a near
     * miss reaches the failure message reading differently from a wholesale one.
     */
    private static int countAlong(@NonNull Bitmap drawn, double dialAngle,
            @NonNull IntPredicate wanted) {
        int found = 0;

        for (int radius = INNERMOST_PROBE_PX; radius <= OUTERMOST_PROBE_PX; radius++) {
            if (wanted.test(probeAlong(drawn, dialAngle, radius))) {
                found++;
            }
        }

        return found;
    }

    /**
     * How many of those radii carry any paint at all. Zero means the angle was
     * left alone; anything else means it was shaded.
     */
    private static int paintedAlong(@NonNull Bitmap drawn, double dialAngle) {
        return countAlong(drawn, dialAngle, pixel -> Color.alpha(pixel) > 0);
    }

    /**
     * The angle halfway between two moments, going the way the clock does. The
     * arcs are swept clockwise from the earlier moment to the later one, so a
     * midpoint taken the other way round would leave the band it is meant to be
     * inside.
     */
    private static double midwayAngle(@NonNull ZonedDateTime from, @NonNull ZonedDateTime to) {
        double start = Dial.angleFor(from.toLocalTime());
        double sweep = (Dial.angleFor(to.toLocalTime()) - start + DEGREES_PER_TURN) % DEGREES_PER_TURN;

        return (start + sweep / 2) % DEGREES_PER_TURN;
    }

    /**
     * How much shading has accumulated at one angle, as alpha. A single pixel
     * rather than the radius {@link #paintedAlong} sweeps, because the arcs it
     * reads are solid wedges rather than the Placeholder's hatch, so every
     * sample along one radius would carry the same answer.
     *
     * <p><strong>60px is chosen against the narrowest band.</strong> That is the
     * one between sunset and civil twilight, 6.46&deg; on this day, which at
     * 60px is 6.8px across — so a probe at its midpoint stands 3.4px from
     * either edge, against the 0.71px an antialiased edge can reach and the
     * 0.71px more that rounding to the pixel grid can cost. Twice the margin
     * needed, and 38px inside the 98px the inset edge stands at.
     */
    private static int shadeDepthAt(@NonNull Bitmap drawn, double dialAngle) {
        return Color.alpha(probeAlong(drawn, dialAngle, SHADE_PROBE_PX));
    }

    /**
     * How many of those radii carry the sun's orange, which only the high noon
     * mark is painted in.
     *
     * <p>The whole radius here even though the mark is a solid wedge, unlike
     * {@link #shadeDepthAt}'s single pixel, because the mark is only two degrees
     * wide: at 30px that is one pixel across, which the rounding to the pixel
     * grid can step over. The outer samples cannot all miss a wedge three pixels
     * wide.
     */
    private static int sunOrangeAlong(@NonNull Bitmap drawn, double dialAngle) {
        return countAlong(drawn, dialAngle, pixel -> isSunOrange(pixel));
    }
}
