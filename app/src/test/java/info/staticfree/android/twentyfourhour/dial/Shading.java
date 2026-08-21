package info.staticfree.android.twentyfourhour.dial;

import static info.staticfree.android.twentyfourhour.dial.Artwork.probeAlong;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;

import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;

import java.util.TimeZone;

/**
 * Where a dial drawn for Buenos Aires and a dial drawn for nobody disagree, and
 * how much of that disagreement a render actually shows.
 *
 * <p>Two test classes now ask the same question of different objects — whether
 * an Observer reached the overlay — and the answer is read off pixels every
 * time. {@code WidgetPictureTest} asks it of two pictures it assembles itself;
 * the receiver's test asks it of the pictures the host was handed after an
 * update and after a resize. The wedges below are measured rather than chosen,
 * so a copy of them in each test is a measurement that goes stale in one: this
 * is {@link Artwork}'s reason for existing, applied to angles rather than to
 * radii.
 *
 * <p>As there, nothing here says what ought to be found — only where to look,
 * how much was different, and how much of a difference is more than noise.
 * <em>Which way</em> each wedge should flip is the claim, and that stays in the
 * tests: the same two numbers below support "this wedge went darker" and "that
 * one went lighter", and only a test knows which it is owed.
 *
 * <p><strong>Every constant here is void unless the dial is drawn in UTC</strong>,
 * which is what {@link #inTheDialsZone()} is for and why it lives in the same
 * file. A test that took the wedges without the rule would be probing angles
 * chosen for a different dial.
 */
public final class Shading {

    /**
     * The zone the picture must be drawn in for the wedges below to mean
     * anything, and the reason they hold on any date.
     *
     * <p>Every call site draws at {@link java.time.Instant#now()} and none has a
     * seam to hand it a fixed one, so a probe angle has to be daylight — or
     * night — in Buenos Aires every day of the year. Read in the city's own zone
     * it could not be: its sunset wanders 2h21m across the seasons, which is
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
    public static final double AFTERNOON_FROM = 95;

    public static final double AFTERNOON_TO = 125;

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
    public static final double SMALL_HOURS_FROM = 275;

    public static final double SMALL_HOURS_TO = 300;

    /**
     * The share of a wedge that has to flip for it to count as flipped, rather
     * than any single pixel doing. Loose against what was measured when this
     * landed — 70% and 71% of the afternoon over two runs, and 91% to 100% of
     * the small hours as the hands moved between them — and still tight enough
     * that shading which merely clipped the edge of a wedge would fail.
     * It cannot be raised much: the Placeholder inks about three-quarters of
     * what it covers, and two hands can hide half a wedge between them.
     */
    public static final int A_QUARTER_OF_THE_WEDGE = 25;

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
     *
     * <p>Every caller draws 392px square — the artwork's intrinsic size, which
     * is also what a 196dp cell comes to at {@code xhdpi}. That is what lets one
     * ring serve them all: a picture of any other size would put its inset edge
     * somewhere else entirely, and every radius here with it.
     */
    private static final int RING_FROM_PX = 80;

    private static final int RING_TO_PX = 96;

    private static final int RING_STEP_PX = 4;

    /**
     * Pins the dial's zone for the length of a test, and puts it back
     * afterwards.
     *
     * <p>A rule rather than a pair of methods copied into each test class, for
     * the reason the wedges are here at all — and because it is the wedges'
     * precondition, so the two cannot drift apart while they share a file.
     */
    @NonNull
    public static TestRule inTheDialsZone() {
        return new ExternalResource() {
            private TimeZone deviceZone;

            @Override
            protected void before() {
                deviceZone = TimeZone.getDefault();
                TimeZone.setDefault(DIAL_ZONE);
            }

            @Override
            protected void after() {
                TimeZone.setDefault(deviceZone);
            }
        };
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
    public static int percentageDarkerIn(@NonNull Bitmap first, @NonNull Bitmap second,
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

    private Shading() {
    }
}
