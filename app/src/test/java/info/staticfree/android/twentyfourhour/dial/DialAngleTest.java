package info.staticfree.android.twentyfourhour.dial;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalTime;

/**
 * The dial is a 24-hour face: one full turn is one day, so a time maps to one
 * angle, not two. Noon is at the top and midnight at the bottom, the inverse of
 * a 12-hour clock. Angles are degrees clockwise from straight up, as passed to
 * {@link android.graphics.Canvas#rotate}.
 *
 * <p>The expected values below come from that specification rather than from
 * the implementation's formula, so this test is able to disagree with the code.
 */
public class DialAngleTest {
    /** Degrees. The angle is a float; exact equality would be brittle. */
    private static final float TOLERANCE = 0.001f;

    @Test
    public void noonPointsStraightUp() {
        assertEquals(0f, Dial.angleFor(LocalTime.of(12, 0)), TOLERANCE);
    }

    @Test
    public void midnightPointsStraightDown() {
        assertEquals(180f, Dial.angleFor(LocalTime.of(0, 0)), TOLERANCE);
    }

    @Test
    public void sixInTheMorningPointsLeft() {
        assertEquals(270f, Dial.angleFor(LocalTime.of(6, 0)), TOLERANCE);
    }

    @Test
    public void sixInTheEveningPointsRight() {
        assertEquals(90f, Dial.angleFor(LocalTime.of(18, 0)), TOLERANCE);
    }

    /**
     * One circle per day is 15 degrees per hour, so a quarter degree per minute.
     * Without this case the whole-hour tests above pass even if the minute term
     * is doubled.
     */
    @Test
    public void theAngleDriftsAQuarterDegreePerMinute() {
        assertEquals(7.5f, Dial.angleFor(LocalTime.of(12, 30)), TOLERANCE);
    }
}
