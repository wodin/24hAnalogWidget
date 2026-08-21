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
 * {@link Berlin}'s mirror: the same kind of day, on the other side of the
 * equator and the other side of Greenwich.
 *
 * <p>Deliberately the same sort of place — temperate, mid-winter, no daylight
 * saving transition near, all three twilights plainly distinct — six months
 * offset, so that the two fixtures are asking one question of two hemispheres
 * rather than two questions. A sign lost off a latitude moves this day into the
 * hemisphere Berlin already covers, where it stops being a second question at
 * all — and it moves loudly: tried once, it put sunrise here 79 minutes early.
 * That is an error a northern fixture alone cannot see, because at 52&deg;
 * north there is no sign to lose.
 *
 * <p>It is also the repo's first western longitude and first negative UTC
 * offset. Berlin and {@link Kashgar} both run ahead of UTC, so until now an
 * offset applied with the wrong sign could only ever have been wrong in one
 * direction, and both of the two places that could catch it were on the same
 * side of zero.
 *
 * <p>A public city centre, never the device's own fix: this repo is public.
 */
final class BuenosAires {
    static final double LATITUDE = -34.61;

    static final double LONGITUDE = -58.38;

    /** Argentina has kept a fixed UTC&minus;3 with no daylight saving since 2009. */
    static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    static final LocalDate DATE = LocalDate.of(2026, Month.AUGUST, 15);

    /** Local noon, for the reason {@link Berlin} gives. */
    private static final Instant SOMETIME_THAT_DAY =
            LocalDateTime.of(DATE, LocalTime.NOON).atZone(ZONE).toInstant();

    @NonNull
    static SunPath sunPath() {
        SunPath path = SunPath.on(new Observer(LATITUDE, LONGITUDE), SOMETIME_THAT_DAY, ZONE);

        assertNotNull("Buenos Aires in August has a sunrise, a sunset and all three twilights",
                path);

        return path;
    }

    private BuenosAires() {
    }
}
