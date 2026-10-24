package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/** A band is one of three shapes, and only the arc has moments. */
public class BandTest {

    private static final ZonedDateTime EVENING =
            ZonedDateTime.of(2026, 2, 15, 17, 18, 0, 0, ZoneId.of("Europe/Berlin"));

    /** This day's morning, which comes before this day's evening: the arc wraps. */
    private static final ZonedDateTime MORNING = EVENING.minusHours(10);

    @Test
    public void anArcHasItsTwoMoments() {
        Band band = Band.between(EVENING, MORNING);

        assertFalse(band.isNever());
        assertFalse(band.isAllDay());
        assertEquals(EVENING, band.evening());
        assertEquals(MORNING, band.morning());
    }

    @Test
    public void neverAndAllDayAreToldApartByShapeNotMoments() {
        assertTrue(Band.never().isNever());
        assertFalse(Band.never().isAllDay());
        assertTrue(Band.allDay().isAllDay());
        assertFalse(Band.allDay().isNever());

        assertThrows(IllegalStateException.class, () -> Band.never().evening());
        assertThrows(IllegalStateException.class, () -> Band.never().morning());
        assertThrows(IllegalStateException.class, () -> Band.allDay().evening());
        assertThrows(IllegalStateException.class, () -> Band.allDay().morning());
    }
}
