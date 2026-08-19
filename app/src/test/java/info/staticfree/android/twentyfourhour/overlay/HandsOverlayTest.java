package info.staticfree.android.twentyfourhour.overlay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The dial is a 24-hour face: the hour hand sweeps one full circle per day, not
 * two. Noon is at the top and midnight at the bottom, the inverse of a 12-hour
 * clock. Angles are degrees clockwise from straight up, as passed to
 * {@link android.graphics.Canvas#rotate}.
 *
 * <p>The expected values below come from that specification rather than from
 * the implementation's formula, so this test is able to disagree with the code.
 */
public class HandsOverlayTest {
    /** Degrees. The angle is a float; exact equality would be brittle. */
    private static final float TOLERANCE = 0.001f;

    @Test
    public void noonPointsStraightUp() {
        assertEquals(0f, HandsOverlay.getHourHandAngle(12, 0), TOLERANCE);
    }

    @Test
    public void midnightPointsStraightDown() {
        assertEquals(180f, HandsOverlay.getHourHandAngle(0, 0), TOLERANCE);
    }

    @Test
    public void sixInTheMorningPointsLeft() {
        assertEquals(270f, HandsOverlay.getHourHandAngle(6, 0), TOLERANCE);
    }

    @Test
    public void sixInTheEveningPointsRight() {
        assertEquals(90f, HandsOverlay.getHourHandAngle(18, 0), TOLERANCE);
    }

    /**
     * One circle per day is 15 degrees per hour, so a quarter degree per minute.
     * Without this case the whole-hour tests above pass even if the minute term
     * is doubled.
     */
    @Test
    public void hourHandDriftsAQuarterDegreePerMinute() {
        assertEquals(7.5f, HandsOverlay.getHourHandAngle(12, 30), TOLERANCE);
    }
}
