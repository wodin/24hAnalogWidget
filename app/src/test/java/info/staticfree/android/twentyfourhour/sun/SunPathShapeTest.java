package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;

/**
 * The shape of a sun's day: what is true of any such day anywhere, whatever the
 * numbers turn out to be. Nothing here is read off what the library returns, so
 * the test is free to disagree with it, and it holds across a change of library.
 *
 * <p>Whether those numbers are <em>right</em> is a separate claim and belongs
 * in a separate test, because the two fail for different reasons: the shape
 * holds however inaccurate the almanac behind it is, so this one cannot notice
 * an hour's error and must not be asked to.
 *
 * <p>{@link Berlin} says which day is under the glass, and why that one.
 */
public class SunPathShapeTest {
    private SunPath path;

    @Before
    public void computeTheDay() {
        path = Berlin.sunPath();
    }

    /**
     * The whole day in order. Sunrise before high noon before sunset is the
     * definition of a day; the twilights nest outward from it, civil inside
     * nautical inside astronomical, because each is defined by the sun being
     * further below the horizon than the last.
     */
    @Test
    public void theDayRunsInOrder() {
        assertInOrder("morning astronomical twilight", path.morningAstronomicalTwilight(),
                "morning nautical twilight", path.morningNauticalTwilight());
        assertInOrder("morning nautical twilight", path.morningNauticalTwilight(),
                "morning civil twilight", path.morningCivilTwilight());
        assertInOrder("morning civil twilight", path.morningCivilTwilight(), "sunrise",
                path.sunrise());
        assertInOrder("sunrise", path.sunrise(), "high noon", path.highNoon());
        assertInOrder("high noon", path.highNoon(), "sunset", path.sunset());
        assertInOrder("sunset", path.sunset(), "evening civil twilight",
                path.eveningCivilTwilight());
        assertInOrder("evening civil twilight", path.eveningCivilTwilight(),
                "evening nautical twilight", path.eveningNauticalTwilight());
        assertInOrder("evening nautical twilight", path.eveningNauticalTwilight(),
                "evening astronomical twilight", path.eveningAstronomicalTwilight());
    }

    /**
     * A path is one place's one day, so every moment on it belongs to the date
     * asked for, read in the zone asked for. This is what says the moments came
     * back in the caller's terms rather than in the library's.
     *
     * <p>The date half of that is true of this place on this day and not of
     * everywhere: see {@code SunPath.at}, which can only assume a date because
     * the almanac behind it reports none. A place whose evening twilight runs
     * past local midnight would fail here, and would be right to.
     */
    @Test
    public void everyMomentBelongsToTheDayAndZoneAskedFor() {
        for (ZonedDateTime moment : allNine()) {
            assertEquals(Berlin.ZONE, moment.getZone());
            assertEquals(Berlin.DATE, moment.toLocalDate());
        }
    }

    private ZonedDateTime[] allNine() {
        return new ZonedDateTime[] {path.morningAstronomicalTwilight(),
                path.morningNauticalTwilight(), path.morningCivilTwilight(), path.sunrise(),
                path.highNoon(), path.sunset(), path.eveningCivilTwilight(),
                path.eveningNauticalTwilight(), path.eveningAstronomicalTwilight()};
    }

    private static void assertInOrder(String earlierName, ZonedDateTime earlier, String laterName,
            ZonedDateTime later) {
        assertTrue(earlierName + " (" + earlier + ") should come before " + laterName + " (" +
                later + ")", earlier.isBefore(later));
    }
}
