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
 * The place with no whole sun day, on a day that is otherwise unremarkable.
 *
 * <p>The other three fixtures are here for days that <em>work</em>. This one is
 * here for the day that does not: from about 48&deg; north the sun stops falling
 * a full 18&deg; below the horizon around the June solstice, so London has no
 * astronomical twilight from roughly mid-May to late July while its sunrise and
 * its sunset carry on as usual. {@link SunPath} is all nine moments or none, so
 * there is no path here at all — and the surprise is the point: London is not
 * the Arctic, and this is the ordinary city the failure turns up in.
 *
 * <p>Two readers, which is why it is a fixture rather than four lines inside
 * one: {@link SunPathShapeTest} asks that the path is null, and the overlay's
 * diagnosis test asks that a painter handed this place says so rather than
 * going quiet. Same argument as {@link BuenosAires} and as {@code Fixes} — the
 * fact that could go stale lives once, and a second copy of where London is
 * would be a second answer.
 *
 * <p>A public city centre, never the device's own fix: this repo is public.
 */
public final class London {
    static final double LATITUDE = 51.5074;

    static final double LONGITUDE = -0.1278;

    /**
     * Britain is on summer time in June, and the date below is chosen inside
     * it, so this fixture exercises an offset rather than assuming UTC.
     */
    public static final ZoneId ZONE = ZoneId.of("Europe/London");

    /** The June solstice, the middle of the stretch with no astronomical twilight. */
    static final LocalDate DATE = LocalDate.of(2026, Month.JUNE, 21);

    /**
     * The edge of that stretch. The gap does not open all at once: on this day
     * the sun still rose through 18&deg; below in the small hours, but that
     * evening it never falls so far again. One crossing and not the other.
     */
    static final LocalDate TRANSITION_DATE = LocalDate.of(2026, Month.MAY, 22);

    /** Local noon, for the reason {@link Berlin} gives. */
    public static final Instant SOMETIME_THAT_DAY =
            LocalDateTime.of(DATE, LocalTime.NOON).atZone(ZONE).toInstant();

    public static final Instant SOMETIME_TRANSITION_DAY =
            LocalDateTime.of(TRANSITION_DATE, LocalTime.NOON).atZone(ZONE).toInstant();

    /** Where this fixture stands, for anything that wants the place and not the day. */
    @NonNull
    public static Observer observer() {
        return new Observer(LATITUDE, LONGITUDE);
    }

    private London() {
    }
}
