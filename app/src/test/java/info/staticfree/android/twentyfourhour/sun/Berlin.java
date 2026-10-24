package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;

import info.staticfree.android.twentyfourhour.observer.Observer;

/**
 * The place and the day the sun tests start from.
 *
 * <p>Berlin in mid-February: a temperate latitude far enough from an equinox
 * for the nine moments to be plainly distinct, and far enough from a solstice
 * for all three twilights to actually occur — a June solstice in London has no
 * astronomical twilight at all, which would have made a test of the day's shape
 * a lie. Its winter offset is a fixed UTC+1 with no daylight saving transition
 * near, so the zone is a whole hour off UTC without being zero: enough to catch
 * an offset dropped on the floor, not enough to drag in an ambiguous hour.
 *
 * <p>{@link BuenosAires} is deliberately its mirror, six months and a
 * hemisphere away, and everything above is why that is what a mirror of this
 * had to be.
 *
 * <p>Here rather than in either test because which day the sun tests examine is
 * one decision, and the two tests must be looking at the same one for their
 * findings to be about the same thing.
 */
final class Berlin {
    static final double LATITUDE = 52.5200;

    static final double LONGITUDE = 13.4050;

    static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    static final LocalDate DATE = LocalDate.of(2026, Month.FEBRUARY, 15);

    /**
     * Any moment inside the day, since a path is the whole day containing one.
     * Twelve o'clock, so that a zone mishandled by up to twelve hours still
     * lands on the date asked for and the failure shows up as a wrong time
     * rather than as a wrong day.
     */
    static final Instant SOMETIME_THAT_DAY =
            LocalDateTime.of(DATE, LocalTime.NOON).atZone(ZONE).toInstant();

    /** Where this fixture stands, for anything that wants the place and not the day. */
    @NonNull
    static Observer observer() {
        return new Observer(LATITUDE, LONGITUDE);
    }

    /**
     * The sun's day here. {@link SunPath#on} may return null for a place that
     * has no such day, but this is not one — so that is asserted once, here,
     * rather than left for every test to re-check before it can begin.
     */
    @NonNull
    static SunPath sunPath() {
        SunPath path = SunPath.on(observer(), SOMETIME_THAT_DAY, ZONE);

        assertNotNull("Berlin in February has a sunrise, a sunset and all three twilights", path);

        return path;
    }

    private Berlin() {
    }
}
