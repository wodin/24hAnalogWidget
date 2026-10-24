package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.shredzone.commons.suncalc.SunTimes;

import java.time.Instant;
import java.time.ZoneId;

import info.staticfree.android.twentyfourhour.observer.Observer;
import info.staticfree.android.twentyfourhour.sun.SunPath.Almanac;

/**
 * How a missing crossing is resolved into a band. The facts about the library
 * this leans on are pinned in {@link SuncalcAbsenceTest}; this asks what
 * {@code Almanac} makes of them. Names a suncalc type for the same reason that
 * test does.
 */
public class AlmanacBandTest {

    @Test
    public void aDayWithBothCrossingsIsAnArc() {
        Band day = band(Berlin.observer(), Berlin.SOMETIME_THAT_DAY, Berlin.ZONE,
                SunTimes.Twilight.VISUAL);

        assertFalse(day.isNever());
        assertFalse(day.isAllDay());
        assertTrue("this day's morning comes before this day's evening",
                day.morning().isBefore(day.evening()));
    }

    @Test
    public void aHorizonTheSunNeverSinksBelowIsNever() {
        assertTrue("London at midsummer never gets 18° below",
                band(London.observer(), London.SOMETIME_THAT_DAY, London.ZONE,
                        SunTimes.Twilight.ASTRONOMICAL).isNever());
        assertTrue("Tromsø at midsummer: the sun never sets at all",
                band(Tromso.observer(), Tromso.SOMETIME_MIDSUMMER, Tromso.ZONE,
                        SunTimes.Twilight.VISUAL).isNever());
    }

    @Test
    public void aHorizonTheSunNeverClimbsAboveIsAllDay() {
        assertTrue("Tromsø at midwinter: the sun never rises",
                band(Tromso.observer(), Tromso.SOMETIME_MIDWINTER, Tromso.ZONE,
                        SunTimes.Twilight.VISUAL).isAllDay());
        assertFalse("but it does climb above 6° below at midday",
                band(Tromso.observer(), Tromso.SOMETIME_MIDWINTER, Tromso.ZONE,
                        SunTimes.Twilight.CIVIL).isAllDay());
    }

    /**
     * London, 22 May: the sun rose through 18&deg; below at 01:17 and never
     * falls so far again. The band has a morning and no evening, so it runs
     * from the edge of the day — midnight — to 01:17, as an almanac's table
     * shades it.
     */
    @Test
    public void aMissingSideRunsToMidnight() {
        Band astronomical = band(London.observer(), London.SOMETIME_TRANSITION_DAY, London.ZONE,
                SunTimes.Twilight.ASTRONOMICAL);

        assertFalse(astronomical.isNever());
        assertFalse(astronomical.isAllDay());
        assertEquals(London.TRANSITION_DATE.atStartOfDay(London.ZONE), astronomical.evening());
        assertEquals(London.TRANSITION_DATE, astronomical.morning().toLocalDate());
        assertEquals(1, astronomical.morning().getHour());
    }

    /**
     * A day with both crossings keeps them as the sun's day dates them, spill
     * and all: Kashgar's evening astronomical twilight is dated tomorrow and
     * is not clipped to today, because the dial reads it by time alone.
     */
    @Test
    public void aWholeDayIsNotClippedToTheDate() {
        Band astronomical = band(Kashgar.observer(), Kashgar.SOMETIME_THAT_DAY, Kashgar.ZONE,
                SunTimes.Twilight.ASTRONOMICAL);

        assertEquals(Kashgar.DATE.plusDays(1), astronomical.evening().toLocalDate());
        assertEquals(Kashgar.DATE, astronomical.morning().toLocalDate());
    }

    @NonNull
    private static Band band(@NonNull Observer observer, @NonNull Instant instant,
            @NonNull ZoneId zone, @NonNull SunTimes.Twilight twilight) {
        return almanac(observer, instant, zone).band(twilight);
    }

    @NonNull
    private static Almanac almanac(@NonNull Observer observer, @NonNull Instant instant,
            @NonNull ZoneId zone) {
        Almanac almanac = SunPath.Almanac.at(observer, instant, zone);
        assertNotNull(almanac);

        return almanac;
    }
}
