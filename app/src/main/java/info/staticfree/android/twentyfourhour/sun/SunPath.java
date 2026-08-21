package info.staticfree.android.twentyfourhour.sun;

/*
 * Copyright (C) 2011-2017 Steve Pomeroy <steve@staticfree.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * 20130315 - modified to add Civil, Nautical, Astronomical twilight
 * times by Rob Prior <android@b4.ca>
 *
 */

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.function.Function;

import uk.me.jstott.coordconv.LatitudeLongitude;
import uk.me.jstott.sun.Sun;
import uk.me.jstott.sun.Time;

/**
 * The sun's whole day at one place: sunrise, sunset, the three twilights either
 * side, and high noon. A Sun position is one point on it.
 *
 * <p>Moments, not angles. What an almanac publishes is times, so times are what
 * an outside source can contradict; mapping one to a place on the dial is the
 * Dial's job and happens as it is drawn. Nothing here draws, and nothing here
 * touches Android, so a day can be asserted on a plain JVM to the second rather
 * than to the pixel.
 *
 * <p>Immutable and computed once: the nine moments are settled by the time you
 * hold one.
 */
public final class SunPath {

    @NonNull
    private final ZonedDateTime morningAstronomicalTwilight;

    @NonNull
    private final ZonedDateTime morningNauticalTwilight;

    @NonNull
    private final ZonedDateTime morningCivilTwilight;

    @NonNull
    private final ZonedDateTime sunrise;

    @NonNull
    private final ZonedDateTime highNoon;

    @NonNull
    private final ZonedDateTime sunset;

    @NonNull
    private final ZonedDateTime eveningCivilTwilight;

    @NonNull
    private final ZonedDateTime eveningNauticalTwilight;

    @NonNull
    private final ZonedDateTime eveningAstronomicalTwilight;

    /**
     * The sun's day at {@code latitude}/{@code longitude}, for whichever day
     * contains {@code instant} when read in {@code zone}. Every moment returned
     * is read in that same zone, so a path cannot be asked for one place and
     * read in another's terms.
     *
     * @return {@code null} if this place has no such day — the sun never
     *         crosses one of the horizons the path is defined by, or the
     *         coordinates and the zone disagree badly enough to put a moment
     *         outside the day. Both mean the same thing to a caller: there is
     *         nothing to draw.
     */
    @Nullable
    public static SunPath on(double latitude, double longitude, @NonNull Instant instant,
            @NonNull ZoneId zone) {
        ZonedDateTime moment = instant.atZone(zone);
        LocalDate date = moment.toLocalDate();
        LatitudeLongitude place = new LatitudeLongitude(latitude, longitude);

        // jsuntimes is a 2004 API: it takes a Calendar for the date, and then
        // the zone and whether daylight saving applies again, separately. This
        // is the only Calendar left in the app, and it leaves with the library.
        Calendar calendar = GregorianCalendar.from(moment);
        TimeZone timeZone = calendar.getTimeZone();
        boolean daylightSaving = calendar.get(Calendar.DST_OFFSET) != 0;

        // Four of Sun's five arguments never vary within a day, so they are
        // bound once here and each call site below names only its event.
        Function<SunEvent, ZonedDateTime> when =
                event -> at(event.time(calendar, place, timeZone, daylightSaving), date, zone);

        try {
            ZonedDateTime sunrise = when.apply(Sun::sunriseTime);
            ZonedDateTime sunset = when.apply(Sun::sunsetTime);

            return new SunPath(when.apply(Sun::morningAstronomicalTwilightTime),
                    when.apply(Sun::morningNauticalTwilightTime),
                    when.apply(Sun::morningCivilTwilightTime), sunrise,
                    highNoonBetween(sunrise, sunset), sunset,
                    when.apply(Sun::eveningCivilTwilightTime),
                    when.apply(Sun::eveningNauticalTwilightTime),
                    when.apply(Sun::eveningAstronomicalTwilightTime));

            // Not swallowed in error: throwing is how Sun says "no such day
            // here", and a Time it does return but cannot place in one fails
            // in LocalTime.of for the same reason. Neither is a fault to
            // report, and the exception says nothing a caller could act on
            // beyond the null. Whether that absence is worth mentioning is the
            // caller's to decide, which is what keeps Android's Log out of a
            // class that is otherwise assertable on a plain JVM.
        } catch (IllegalArgumentException | DateTimeException e) {
            return null;
        }
    }

    private SunPath(@NonNull ZonedDateTime morningAstronomicalTwilight,
            @NonNull ZonedDateTime morningNauticalTwilight,
            @NonNull ZonedDateTime morningCivilTwilight, @NonNull ZonedDateTime sunrise,
            @NonNull ZonedDateTime highNoon, @NonNull ZonedDateTime sunset,
            @NonNull ZonedDateTime eveningCivilTwilight,
            @NonNull ZonedDateTime eveningNauticalTwilight,
            @NonNull ZonedDateTime eveningAstronomicalTwilight) {
        this.morningAstronomicalTwilight = morningAstronomicalTwilight;
        this.morningNauticalTwilight = morningNauticalTwilight;
        this.morningCivilTwilight = morningCivilTwilight;
        this.sunrise = sunrise;
        this.highNoon = highNoon;
        this.sunset = sunset;
        this.eveningCivilTwilight = eveningCivilTwilight;
        this.eveningNauticalTwilight = eveningNauticalTwilight;
        this.eveningAstronomicalTwilight = eveningAstronomicalTwilight;
    }

    /**
     * High noon as this app has always meant it: the midpoint of sunrise and
     * sunset. That is close to the sun's true transit but not the same moment.
     *
     * <p>The difference is <em>not</em> the equation of time, which this comment
     * claimed until a test went looking: the equation of time moves the transit
     * and both horizon crossings alike, so it cancels out of a midpoint exactly.
     * What is left is that the sun's declination shifts between one crossing and
     * the other, so the two are not symmetric about the transit. That is worth
     * some 20 seconds at a temperate latitude away from the solstices, where
     * declination moves fastest; {@code SunPathAlmanacTest} measures it.
     */
    @NonNull
    private static ZonedDateTime highNoonBetween(@NonNull ZonedDateTime sunrise,
            @NonNull ZonedDateTime sunset) {
        return sunrise.plus(Duration.between(sunrise, sunset).dividedBy(2));
    }

    /**
     * Places one of jsuntimes' wall-clock times into the day that was asked
     * about.
     *
     * <p><strong>Asked about, not the day it falls on.</strong> A {@code Time}
     * is hours, minutes and seconds with no date attached, so this can only
     * assume. The assumption holds wherever the sun's whole day fits inside one
     * local date, and fails where an evening twilight runs past local midnight
     * — which a timezone wide for its longitude, or daylight saving, can do
     * even at a temperate latitude. Such a moment is dated a day early.
     *
     * <p>Nothing is drawn wrongly by it: the painter reads only the wall-clock
     * time and the dial wraps at midnight of its own accord. But this seam
     * publishes dates, and for such a place the date is wrong. It is an
     * artefact of the almanac behind it rather than of the idea; one that
     * returns already-dated moments has nothing to assume.
     *
     * <p>Seconds are truncated, not rounded. jsuntimes reports them as a
     * fraction and the sub-second part is a hundred-thousandth of a degree on
     * the dial.
     */
    @NonNull
    private static ZonedDateTime at(@NonNull Time time, @NonNull LocalDate date,
            @NonNull ZoneId zone) {
        return date.atTime(
                        LocalTime.of(time.getHours(), time.getMinutes(), (int) time.getSeconds()))
                .atZone(zone);
    }

    /**
     * One of jsuntimes' eight event methods, which all share a shape the
     * library never named. Goes when the library does.
     */
    @FunctionalInterface
    private interface SunEvent {
        @NonNull
        Time time(@NonNull Calendar calendar, @NonNull LatitudeLongitude place,
                @NonNull TimeZone zone, boolean daylightSaving);
    }

    @NonNull
    public ZonedDateTime morningAstronomicalTwilight() {
        return morningAstronomicalTwilight;
    }

    @NonNull
    public ZonedDateTime morningNauticalTwilight() {
        return morningNauticalTwilight;
    }

    @NonNull
    public ZonedDateTime morningCivilTwilight() {
        return morningCivilTwilight;
    }

    @NonNull
    public ZonedDateTime sunrise() {
        return sunrise;
    }

    @NonNull
    public ZonedDateTime highNoon() {
        return highNoon;
    }

    @NonNull
    public ZonedDateTime sunset() {
        return sunset;
    }

    @NonNull
    public ZonedDateTime eveningCivilTwilight() {
        return eveningCivilTwilight;
    }

    @NonNull
    public ZonedDateTime eveningNauticalTwilight() {
        return eveningNauticalTwilight;
    }

    @NonNull
    public ZonedDateTime eveningAstronomicalTwilight() {
        return eveningAstronomicalTwilight;
    }
}
