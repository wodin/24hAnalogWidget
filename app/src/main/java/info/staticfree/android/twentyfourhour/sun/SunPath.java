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

import org.shredzone.commons.suncalc.SunTimes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import info.staticfree.android.twentyfourhour.observer.Observer;

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
     * The sun's day where {@code observer} stands, for whichever day contains
     * {@code instant} when read in {@code zone}. Every moment returned is read
     * in that same zone, so a path cannot be asked for one place and read in
     * another's terms.
     *
     * <p><strong>The sun's day, not the calendar's.</strong> It is built
     * outward from high noon — the crossings before it are the morning, the
     * crossings after it are the evening — so a place whose sun runs late
     * against its clock gets a day that is still in order, with its last
     * moments dated to the day after the one asked about. Kashgar on the
     * solstice is such a place; {@code SunPathShapeTest} holds an example.
     *
     * @return {@code null} if this place has no such day, meaning the sun does
     *         not cross one of the four horizons a path is defined by — or, at
     *         the poles in their dark half of the year, does not cross the
     *         meridian either. A missing horizon needs nothing as exotic as a
     *         polar circle: from about 48&deg; north or south the sun stops
     *         falling a full 18&deg; below it around the summer solstice, so
     *         London has no astronomical twilight in late June and no path here
     *         either. All of it is one answer, because a caller has nothing to
     *         draw either way.
     */
    @Nullable
    public static SunPath on(@NonNull Observer observer, @NonNull Instant instant,
            @NonNull ZoneId zone) {
        Almanac almanac = Almanac.at(observer, instant, zone);

        if (almanac == null) {
            return null;
        }

        ZonedDateTime morningAstronomicalTwilight = almanac.morning(SunTimes.Twilight.ASTRONOMICAL);
        ZonedDateTime morningNauticalTwilight = almanac.morning(SunTimes.Twilight.NAUTICAL);
        ZonedDateTime morningCivilTwilight = almanac.morning(SunTimes.Twilight.CIVIL);
        ZonedDateTime sunrise = almanac.morning(SunTimes.Twilight.VISUAL);
        ZonedDateTime sunset = almanac.evening(SunTimes.Twilight.VISUAL);
        ZonedDateTime eveningCivilTwilight = almanac.evening(SunTimes.Twilight.CIVIL);
        ZonedDateTime eveningNauticalTwilight = almanac.evening(SunTimes.Twilight.NAUTICAL);
        ZonedDateTime eveningAstronomicalTwilight = almanac.evening(SunTimes.Twilight.ASTRONOMICAL);

        // All nine or none. A latitude with a sunrise and a sunset but no
        // astronomical twilight is a real place, and it draws the Placeholder
        // rather than most of a day: this seam has no way to say "these eight",
        // and inventing one would be answering a question nothing has asked.
        if (anyMissing(morningAstronomicalTwilight, morningNauticalTwilight, morningCivilTwilight,
                sunrise, sunset, eveningCivilTwilight, eveningNauticalTwilight,
                eveningAstronomicalTwilight)) {
            return null;
        }

        return new SunPath(morningAstronomicalTwilight, morningNauticalTwilight,
                morningCivilTwilight, sunrise, almanac.highNoon(), sunset, eveningCivilTwilight,
                eveningNauticalTwilight, eveningAstronomicalTwilight);
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

    private static boolean anyMissing(@Nullable ZonedDateTime... moments) {
        for (ZonedDateTime moment : moments) {
            if (moment == null) {
                return true;
            }
        }

        return false;
    }

    /**
     * One observer's one day, with everything that does not vary between
     * questions bound once: where they stand, and the high noon their day is
     * built outward from. Two questions remain — what the morning did, and what
     * the evening did — each asked at whichever of the four horizons the caller
     * names.
     *
     * <p>This is the boundary the library's vocabulary is translated at. Its
     * {@code getNoon()} is the meridian transit, which is what this app calls
     * <em>high noon</em>; its {@code Twilight.VISUAL} is the horizon crossing
     * this app calls sunrise and sunset, refraction and the sun's disc
     * included. Neither name gets out of this file.
     */
    static final class Almanac {

        @NonNull
        private final Observer observer;

        @NonNull
        private final ZonedDateTime highNoon;

        /** The day asked about, as the stretch of time it is: midnight to midnight in its zone. */
        @NonNull
        private final ZonedDateTime startOfDay;

        @NonNull
        private final ZonedDateTime endOfDay;

        /**
         * The almanac for this observer on the day containing {@code instant}
         * read in {@code zone}, anchored on the first meridian crossing at or
         * after local midnight.
         *
         * <p>Local midnight is the anchor because a day has to start somewhere
         * and that is where the calendar says it does. Every other moment is
         * then measured from the crossing rather than from midnight, which is
         * what frees the rest of the day from the calendar's opinion.
         *
         * @return {@code null} where the sun does not cross the meridian at
         *         all, which is the poles in their dark half of the year. There
         *         is no day here to be the morning and evening of.
         */
        @Nullable
        static Almanac at(@NonNull Observer observer, @NonNull Instant instant,
                @NonNull ZoneId zone) {
            LocalDate date = instant.atZone(zone).toLocalDate();
            ZonedDateTime highNoon = observedFrom(observer)
                    .on(date)
                    .timezone(zone)
                    .forward()
                    .execute()
                    .getNoon();

            return highNoon == null ? null
                    : new Almanac(observer, highNoon, date.atStartOfDay(zone));
        }

        private Almanac(@NonNull Observer observer, @NonNull ZonedDateTime highNoon,
                @NonNull ZonedDateTime startOfDay) {
            this.observer = observer;
            this.highNoon = highNoon;
            this.startOfDay = startOfDay;
            this.endOfDay = startOfDay.plusDays(1);
        }

        @NonNull
        ZonedDateTime highNoon() {
            return highNoon;
        }

        /** The last time the sun rose through {@code twilight} before high noon. */
        @Nullable
        ZonedDateTime morning(@NonNull SunTimes.Twilight twilight) {
            return search(twilight).reverse().execute().getRise();
        }

        /** The first time the sun set through {@code twilight} after high noon. */
        @Nullable
        ZonedDateTime evening(@NonNull SunTimes.Twilight twilight) {
            return search(twilight).forward().execute().getSet();
        }

        /**
         * The stretch of this day the sun spends below {@code twilight}'s
         * horizon, whatever shape that turns out to be. The one place a missing
         * crossing is resolved: the library answers each side of the day
         * separately, with a null crossing and a flag saying which way the sun
         * stayed, and the two sides can disagree — London on 22 May has a
         * morning astronomical crossing and no evening one.
         *
         * <p>A missing side is what an almanac prints as a dash: the darkness
         * did not begin, or did not end, within the date. So the band runs to
         * the edge of the day, midnight, which is what the published tables
         * shade to and is already a fixed point on the dial. Upstream's
         * {@code noon ± 12h} is the same idea approximated.
         *
         * <p>With one side missing, the crossing on the other must itself fall
         * within the day, or there is no darkness today to shade: at the margin
         * of polar day the last rise can sit just before yesterday's midnight,
         * and running a band from midnight to it would shade the whole dial.
         * A day with both crossings is not clipped, because its moments are
         * allowed to spill — see <em>the sun's day, not the calendar's</em> on
         * {@link SunPath#on} — and the dial reads them by time alone.
         */
        @NonNull
        Band band(@NonNull SunTimes.Twilight twilight) {
            SunTimes morning = search(twilight).reverse().execute();
            SunTimes evening = search(twilight).forward().execute();

            if (morning.isAlwaysDown() || evening.isAlwaysDown()) {
                return Band.allDay();
            }

            ZonedDateTime rise = morning.getRise();
            ZonedDateTime set = evening.getSet();

            if (rise == null && set == null) {
                return Band.never();
            }

            if (rise == null) {
                return withinToday(set) ? Band.between(set, endOfDay) : Band.never();
            }

            if (set == null) {
                return withinToday(rise) ? Band.between(startOfDay, rise) : Band.never();
            }

            return Band.between(set, rise);
        }

        private boolean withinToday(@NonNull ZonedDateTime moment) {
            return !moment.isBefore(startOfDay) && moment.isBefore(endOfDay);
        }

        /**
         * A fresh search of the day either side of high noon. Fresh because the
         * builder carries the query's state, so one shared between two questions
         * would answer the second with the first's direction.
         */
        @NonNull
        private SunTimes.Parameters search(@NonNull SunTimes.Twilight twilight) {
            return observedFrom(observer).on(highNoon).twilight(twilight);
        }

        /**
         * The half of a query that is the observer rather than the question:
         * where they stand, and that they are asking about a day rather than
         * about whenever the sun next obliges.
         */
        @NonNull
        private static SunTimes.Parameters observedFrom(@NonNull Observer observer) {
            return SunTimes.compute().at(observer.latitude(), observer.longitude()).oneDay();
        }
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
