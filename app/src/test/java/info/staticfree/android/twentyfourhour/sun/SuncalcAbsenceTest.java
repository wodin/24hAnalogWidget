package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.shredzone.commons.suncalc.SunTimes;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import info.staticfree.android.twentyfourhour.observer.Observer;

/**
 * What the library says when a horizon goes uncrossed. A characterisation
 * test, not a shape test: it reads the library directly and pins the three
 * facts a per-band {@link SunPath} leans on, so that a library bump which
 * moves any of them fails here, by name, rather than somewhere in the painter.
 *
 * <ol>
 * <li>A null crossing comes with exactly one of {@code isAlwaysUp} and
 *     {@code isAlwaysDown} set, and the flag respects the twilight angle asked
 *     for — "always up" at 18&deg; below means the sun never falls that far,
 *     not that it never sets.</li>
 * <li>The two sides of a day are asked separately and can disagree: London
 *     on its transition day has a morning crossing and no evening one.</li>
 * <li>The meridian transit is null only at the poles in their dark half.</li>
 * </ol>
 *
 * <p>This is the only test that names a suncalc type, and it is in this
 * package for the same reason {@code SunPath.Almanac} is: the vocabulary stops
 * at the package boundary.
 */
public class SuncalcAbsenceTest {

    @Test
    public void aMissingCrossingSaysWhichWayTheSunStayed() {
        SunTimes midsummer = evening(Tromso.observer(), Tromso.SOMETIME_MIDSUMMER, Tromso.ZONE,
                SunTimes.Twilight.VISUAL);
        assertNull(midsummer.getSet());
        assertTrue("Tromsø at midsummer: the sun never sets", midsummer.isAlwaysUp());
        assertFalse(midsummer.isAlwaysDown());

        SunTimes midwinter = evening(Tromso.observer(), Tromso.SOMETIME_MIDWINTER, Tromso.ZONE,
                SunTimes.Twilight.VISUAL);
        assertNull(midwinter.getSet());
        assertTrue("Tromsø at midwinter: the sun never rises", midwinter.isAlwaysDown());
        assertFalse(midwinter.isAlwaysUp());
    }

    @Test
    public void theFlagsRespectTheTwilightAskedFor() {
        SunTimes astronomical = evening(London.observer(), London.SOMETIME_THAT_DAY, London.ZONE,
                SunTimes.Twilight.ASTRONOMICAL);
        assertNull(astronomical.getSet());
        assertTrue("London at midsummer never gets 18° below, so at that horizon the sun is " +
                "'always up' even though it sets every evening", astronomical.isAlwaysUp());

        SunTimes visual = evening(London.observer(), London.SOMETIME_THAT_DAY, London.ZONE,
                SunTimes.Twilight.VISUAL);
        assertNotNull(visual.getSet());
        assertFalse(visual.isAlwaysUp());
    }

    @Test
    public void midwinterTromsoStillHasTwilightsAroundAHighNoonItNeverReaches() {
        SunTimes civil = evening(Tromso.observer(), Tromso.SOMETIME_MIDWINTER, Tromso.ZONE,
                SunTimes.Twilight.CIVIL);
        assertNotNull("the sun climbs above 6° below at midday, so civil twilight has an end",
                civil.getSet());
        assertFalse(civil.isAlwaysDown());
    }

    @Test
    public void theTwoSidesOfADayCanDisagree() {
        Observer london = London.observer();
        SunTimes morning = morning(london, London.SOMETIME_TRANSITION_DAY, London.ZONE,
                SunTimes.Twilight.ASTRONOMICAL);
        SunTimes evening = evening(london, London.SOMETIME_TRANSITION_DAY, London.ZONE,
                SunTimes.Twilight.ASTRONOMICAL);

        assertNotNull("the sun still rose through 18° below in the small hours",
                morning.getRise());
        assertNull("but that evening it never falls so far again", evening.getSet());
        assertTrue(evening.isAlwaysUp());
    }

    @Test
    public void onlyThePolesLoseTheMeridianTransit() {
        assertNotNull(highNoon(Tromso.observer(), Tromso.SOMETIME_MIDWINTER, Tromso.ZONE));
        assertNull("the pole in its dark half: the sun never crosses the meridian",
                highNoon(new Observer(90, 0), Tromso.SOMETIME_MIDWINTER, ZoneId.of("UTC")));
    }

    // The same anchoring SunPath.Almanac does: the day's high noon, then one
    // search back from it and one forward.

    private static ZonedDateTime highNoon(@NonNull Observer observer, @NonNull Instant instant,
            @NonNull ZoneId zone) {
        return at(observer).on(instant.atZone(zone).toLocalDate()).timezone(zone).forward()
                .execute().getNoon();
    }

    @NonNull
    private static SunTimes morning(@NonNull Observer observer, @NonNull Instant instant,
            @NonNull ZoneId zone, @NonNull SunTimes.Twilight twilight) {
        return at(observer).on(highNoon(observer, instant, zone)).twilight(twilight).reverse()
                .execute();
    }

    @NonNull
    private static SunTimes evening(@NonNull Observer observer, @NonNull Instant instant,
            @NonNull ZoneId zone, @NonNull SunTimes.Twilight twilight) {
        return at(observer).on(highNoon(observer, instant, zone)).twilight(twilight).forward()
                .execute();
    }

    @NonNull
    private static SunTimes.Parameters at(@NonNull Observer observer) {
        return SunTimes.compute().at(observer.latitude(), observer.longitude()).oneDay();
    }
}
