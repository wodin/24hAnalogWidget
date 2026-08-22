package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
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
 * <p>Three places, for three different reasons. {@link Berlin} is the day the
 * sun tests ask their questions of, temperate and unremarkable.
 * {@link BuenosAires} is the same kind of day in the other hemisphere, because
 * a shape that only ever held north of the equator was never shown to be a
 * shape. {@link Kashgar} is here only because its day overruns local midnight,
 * which is the one thing the other two cannot show.
 *
 * <p>And {@link London}, which is not a fourth day of the same kind but the
 * absence of one: the place whose sun keeps every appointment but the last, so
 * that there is no whole day here to have a shape at all.
 */
public class SunPathShapeTest {

    /**
     * The whole day in order. Sunrise before high noon before sunset is the
     * definition of a day; the twilights nest outward from it, civil inside
     * nautical inside astronomical, because each is defined by the sun being
     * further below the horizon than the last.
     *
     * <p>Asked of both hemispheres, because the arithmetic behind it is not
     * symmetric in the sign of a latitude: a day shown to run in order only
     * north of the equator was never shown to be a day.
     */
    @Test
    public void theDayRunsInOrder() {
        assertRunsInOrder(Berlin.sunPath());
        assertRunsInOrder(BuenosAires.sunPath());
    }

    /**
     * A path is one place's one day, so every moment on it belongs to the date
     * asked for, read in the zone asked for. This is what says the moments came
     * back in the caller's terms rather than in the library's.
     *
     * <p>Buenos Aires runs three hours <em>behind</em> UTC where Berlin runs an
     * hour ahead, so an offset applied with the wrong sign lands six hours out
     * there — far enough to carry the evening's last twilight over into the
     * next date, where this notices it.
     *
     * <p>The date half of that is true of these two days and not of every day
     * everywhere — see {@link #aDayMayReachPastLocalMidnight()}.
     */
    @Test
    public void everyMomentBelongsToTheDayAndZoneAskedFor() {
        assertEveryMomentBelongsTo(Berlin.ZONE, Berlin.DATE, Berlin.sunPath());
        assertEveryMomentBelongsTo(BuenosAires.ZONE, BuenosAires.DATE, BuenosAires.sunPath());
    }

    /**
     * A day is the sun's, not the calendar's: where the two disagree, the
     * ordering above still holds. Kashgar's solstice runs some three hours late
     * against its clock, so the last of its evening twilight falls after local
     * midnight — and a path that dated its moments by the day it was asked
     * about, rather than by the day the sun actually has, would put that moment
     * twenty-four hours early and break the order.
     */
    @Test
    public void aDayThatOverrunsMidnightStillRunsInOrder() {
        assertRunsInOrder(Kashgar.sunPath());
    }

    /**
     * The same fact stated directly, so that a failure says which half is
     * wrong. Every moment is still read in the zone that was asked for; what
     * moves is the date, and only for the moment that has actually crossed
     * into the next one.
     */
    @Test
    public void aDayMayReachPastLocalMidnight() {
        SunPath path = Kashgar.sunPath();

        for (ZonedDateTime moment : allNine(path)) {
            assertEquals(Kashgar.ZONE, moment.getZone());
        }

        assertEquals("sunrise belongs to the day asked about", Kashgar.DATE,
                path.sunrise().toLocalDate());
        assertEquals("sunset belongs to the day asked about", Kashgar.DATE,
                path.sunset().toLocalDate());
        assertEquals("the evening's astronomical twilight ends after local midnight",
                Kashgar.DATE.plusDays(1), path.eveningAstronomicalTwilight().toLocalDate());
    }

    /**
     * All nine moments or none. A place can have a sunrise and a sunset and
     * still be missing a twilight: from about 48&deg; north the sun stops
     * falling a full 18&deg; below the horizon around the June solstice, so
     * London has no astronomical twilight from roughly mid-May to late July
     * while its days are otherwise ordinary.
     *
     * <p>The path is null there rather than partial, because this seam has no
     * way to say "these eight" and the painter has nothing to do with the
     * answer but draw the Placeholder. That is a real behaviour and a
     * surprising one — London is not the Arctic — so it is asserted rather
     * than left to be discovered by whoever first lights this feature up.
     */
    @Test
    public void aPlaceMissingOneTwilightHasNoPathAtAll() {
        assertNull("London in late June has a sunrise and a sunset but never a night dark " +
                        "enough for astronomical twilight, so there is no whole day to draw",
                SunPath.on(London.observer(), London.SOMETIME_THAT_DAY, London.ZONE));
    }

    private static void assertEveryMomentBelongsTo(@NonNull ZoneId zone, @NonNull LocalDate date,
            @NonNull SunPath path) {
        for (ZonedDateTime moment : allNine(path)) {
            assertEquals("every moment is read in the zone asked for", zone, moment.getZone());
            assertEquals(moment + " should belong to " + date, date, moment.toLocalDate());
        }
    }

    private static void assertRunsInOrder(@NonNull SunPath path) {
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

    @NonNull
    private static ZonedDateTime[] allNine(@NonNull SunPath path) {
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
