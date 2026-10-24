package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import info.staticfree.android.twentyfourhour.observer.Observer;

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
 * <p>And {@link London} and {@link Tromso}, which are not further days of the
 * same kind but days with a band missing: London's sun keeps every appointment
 * but the last, and Tromsø's keeps none in June and only the twilights' in
 * December. A path still has a shape there; it is the bands that change theirs.
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
                path.night().morning().toLocalDate());
        assertEquals("sunset belongs to the day asked about", Kashgar.DATE,
                path.night().evening().toLocalDate());
        assertEquals("the evening's astronomical twilight ends after local midnight",
                Kashgar.DATE.plusDays(1), path.astronomicalTwilight().evening().toLocalDate());
    }

    /**
     * A place can have a sunrise and a sunset and still be missing a twilight:
     * from about 48&deg; north the sun stops falling a full 18&deg; below the
     * horizon around the June solstice, so London has no astronomical twilight
     * from roughly mid-May to late July while its days are otherwise ordinary.
     *
     * <p>The path is there and says so, band by band: three arcs and one
     * that never happens. It used to be null here instead, all nine moments or
     * none, and the painter fell back to the Placeholder for a city that is not
     * the Arctic. That was the surprise this test was written to record; this
     * is its correction.
     */
    @Test
    public void aPlaceMissingOneTwilightStillHasAPath() {
        SunPath path = SunPath.on(London.observer(), London.SOMETIME_THAT_DAY, London.ZONE);

        assertNotNull("London in late June has a sunrise and a sunset", path);
        assertIsArc("night", path.night());
        assertIsArc("civil twilight", path.civilTwilight());
        assertIsArc("nautical twilight", path.nauticalTwilight());
        assertTrue("but never a night dark enough for astronomical twilight",
                path.astronomicalTwilight().isNever());
    }

    /**
     * Beyond the polar circle the same thing happens to every horizon at once
     * in June, and to the visible one in December — when the three twilights
     * still come and go around a high noon the sun never quite reaches. The
     * two shapes a band can take besides an arc, each where it actually occurs.
     */
    @Test
    public void polarDayAndPolarNightAreBandsOfTheirOwnShape() {
        SunPath midsummer =
                SunPath.on(Tromso.observer(), Tromso.SOMETIME_MIDSUMMER, Tromso.ZONE);
        assertNotNull(midsummer);

        for (Band band : allFour(midsummer)) {
            assertTrue("Tromsø at midsummer: " + band, band.isNever());
        }

        SunPath midwinter =
                SunPath.on(Tromso.observer(), Tromso.SOMETIME_MIDWINTER, Tromso.ZONE);
        assertNotNull(midwinter);

        assertTrue("Tromsø at midwinter: the sun never rises", midwinter.night().isAllDay());
        assertIsArc("civil twilight", midwinter.civilTwilight());
        assertIsArc("nautical twilight", midwinter.nauticalTwilight());
        assertIsArc("astronomical twilight", midwinter.astronomicalTwilight());
        assertRunsInOrder(midwinter.civilTwilight(), midwinter.nauticalTwilight(),
                midwinter.astronomicalTwilight());
    }

    /**
     * Only the poles have no path at all: in their dark half of the year the
     * sun never crosses the meridian, so there is no high noon to build a day
     * around.
     */
    @Test
    public void thePoleInItsDarkHalfHasNoPath() {
        assertNull(SunPath.on(new Observer(90, 0), Tromso.SOMETIME_MIDWINTER, ZoneId.of("UTC")));
    }

    private static void assertEveryMomentBelongsTo(@NonNull ZoneId zone, @NonNull LocalDate date,
            @NonNull SunPath path) {
        for (ZonedDateTime moment : allNine(path)) {
            assertEquals("every moment is read in the zone asked for", zone, moment.getZone());
            assertEquals(moment + " should belong to " + date, date, moment.toLocalDate());
        }
    }

    /**
     * Sunrise before high noon before sunset, and the twilights nesting outward
     * from it: each band's morning before the shallower one's, each evening
     * after. Which is to say the bands nest, each inside the last.
     */
    private static void assertRunsInOrder(@NonNull SunPath path) {
        assertInOrder("sunrise", path.night().morning(), "high noon", path.highNoon());
        assertInOrder("high noon", path.highNoon(), "sunset", path.night().evening());
        assertRunsInOrder(path.night(), path.civilTwilight(), path.nauticalTwilight(),
                path.astronomicalTwilight());
    }

    /** Each band an arc nested inside the one before it, shallowest first. */
    private static void assertRunsInOrder(@NonNull Band... shallowestFirst) {
        for (int i = 1; i < shallowestFirst.length; i++) {
            Band shallower = shallowestFirst[i - 1];
            Band deeper = shallowestFirst[i];

            assertInOrder("the deeper band's morning", deeper.morning(),
                    "the shallower band's morning", shallower.morning());
            assertInOrder("the shallower band's evening", shallower.evening(),
                    "the deeper band's evening", deeper.evening());
        }
    }

    private static void assertIsArc(@NonNull String name, @NonNull Band band) {
        assertFalse(name + " never happens", band.isNever());
        assertFalse(name + " lasts all day", band.isAllDay());
    }

    @NonNull
    private static Band[] allFour(@NonNull SunPath path) {
        return new Band[] {path.night(), path.civilTwilight(), path.nauticalTwilight(),
                path.astronomicalTwilight()};
    }

    @NonNull
    private static ZonedDateTime[] allNine(@NonNull SunPath path) {
        return new ZonedDateTime[] {path.astronomicalTwilight().morning(),
                path.nauticalTwilight().morning(), path.civilTwilight().morning(),
                path.night().morning(), path.highNoon(), path.night().evening(),
                path.civilTwilight().evening(), path.nauticalTwilight().evening(),
                path.astronomicalTwilight().evening()};
    }

    private static void assertInOrder(String earlierName, ZonedDateTime earlier, String laterName,
            ZonedDateTime later) {
        assertTrue(earlierName + " (" + earlier + ") should come before " + laterName + " (" +
                later + ")", earlier.isBefore(later));
    }
}
