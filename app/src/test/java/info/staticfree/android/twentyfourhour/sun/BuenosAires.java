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
 *
 * <p>The only fixture visible outside this package, because it is the only one
 * with readers outside it. The pixel probes want this place's day, and two of
 * them want the moments of it as well, so that a probe asks where the sun
 * actually was rather than where it was on the day this was written; the reader
 * that produces an {@link Observer} from the phone wants nothing of the day at
 * all, only a latitude and a longitude that are safe to publish and are written
 * down once — a second copy of them would be a second answer about where Buenos
 * Aires is. Named by role rather than by class here, because the list of
 * readers has grown twice already.
 */
public final class BuenosAires {
    static final double LATITUDE = -34.61;

    static final double LONGITUDE = -58.38;

    /** Argentina has kept a fixed UTC&minus;3 with no daylight saving since 2009. */
    public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    static final LocalDate DATE = LocalDate.of(2026, Month.AUGUST, 15);

    /** Local noon, for the reason {@link Berlin} gives. */
    public static final Instant SOMETIME_THAT_DAY =
            LocalDateTime.of(DATE, LocalTime.NOON).atZone(ZONE).toInstant();

    /** Where this fixture stands, for anything that wants the place and not the day. */
    @NonNull
    public static Observer observer() {
        return new Observer(LATITUDE, LONGITUDE);
    }

    /**
     * This place's whole sun day. Public for the same reason {@link #observer()}
     * is: a caller that built its own would be a second answer, and would have
     * to repeat the assertion below to get a non-null one.
     */
    @NonNull
    public static SunPath sunPath() {
        SunPath path = SunPath.on(observer(), SOMETIME_THAT_DAY, ZONE);

        assertNotNull("Buenos Aires in August has a sunrise, a sunset and all three twilights",
                path);

        return path;
    }

    private BuenosAires() {
    }
}
