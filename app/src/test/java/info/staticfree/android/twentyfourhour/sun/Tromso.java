package info.staticfree.android.twentyfourhour.sun;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;

import info.staticfree.android.twentyfourhour.observer.Observer;

/**
 * The place where a horizon goes uncrossed in both directions: at 69&deg;
 * north the sun neither rises nor sets around either solstice. In June it
 * stays above every one of the four horizons, so no band is ever shaded; in
 * December it stays below the visible one all day, so the day band is the
 * whole dial, while the three twilights still come and go around a high noon
 * the sun never quite reaches.
 *
 * <p>{@link London} loses one twilight on one side at a time. This fixture is
 * here for the other two shapes a band can take — nothing at all, and all
 * day — which no temperate place can show.
 *
 * <p>A public city centre, never the device's own fix: this repo is public.
 */
public final class Tromso {
    static final double LATITUDE = 69.6492;

    static final double LONGITUDE = 18.9553;

    public static final ZoneId ZONE = ZoneId.of("Europe/Oslo");

    /** The June solstice: the sun above every horizon all day. */
    static final LocalDate MIDSUMMER = LocalDate.of(2026, Month.JUNE, 21);

    /** The December solstice: the sun below the visible horizon all day. */
    static final LocalDate MIDWINTER = LocalDate.of(2026, Month.DECEMBER, 21);

    /** Local noon, for the reason {@link Berlin} gives. */
    public static final Instant SOMETIME_MIDSUMMER =
            LocalDateTime.of(MIDSUMMER, LocalTime.NOON).atZone(ZONE).toInstant();

    public static final Instant SOMETIME_MIDWINTER =
            LocalDateTime.of(MIDWINTER, LocalTime.NOON).atZone(ZONE).toInstant();

    @NonNull
    public static Observer observer() {
        return new Observer(LATITUDE, LONGITUDE);
    }

    private Tromso() {
    }
}
