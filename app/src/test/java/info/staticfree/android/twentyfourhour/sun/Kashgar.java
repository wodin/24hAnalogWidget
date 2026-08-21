package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;

/**
 * A place whose sun does not fit inside one local date.
 *
 * <p>Kashgar sits at 76&deg; east and keeps the clock of Beijing, some 3,400 km
 * away — so its whole solar day runs about three hours late against the wall
 * clock (high noon lands near 14:58), and on the June solstice the last of the
 * evening's astronomical twilight falls after local midnight. That makes it the
 * counterexample to {@link Berlin}: everything a temperate mid-February day
 * hides about dating, this one shows.
 *
 * <p>Only for the dating claim, and so the one fixture with no published times
 * recorded against it: what it is here to show is where a day <em>sits</em>
 * against a clock, which {@link SunPathShapeTest} can see for itself. Being
 * right to the minute is {@link Berlin}'s question and {@link BuenosAires}'s,
 * and it is answered where the almanac is.
 */
final class Kashgar {
    static final double LATITUDE = 39.4704;

    static final double LONGITUDE = 75.9898;

    /** China keeps one timezone from end to end, and observes no daylight saving. */
    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    static final LocalDate DATE = LocalDate.of(2026, Month.JUNE, 21);

    private static final Instant SOMETIME_THAT_DAY =
            LocalDateTime.of(DATE, LocalTime.NOON).atZone(ZONE).toInstant();

    @NonNull
    static SunPath sunPath() {
        SunPath path = SunPath.on(LATITUDE, LONGITUDE, SOMETIME_THAT_DAY, ZONE);

        assertNotNull("Kashgar on the solstice has a sunrise, a sunset and all three twilights",
                path);

        return path;
    }

    private Kashgar() {
    }
}
