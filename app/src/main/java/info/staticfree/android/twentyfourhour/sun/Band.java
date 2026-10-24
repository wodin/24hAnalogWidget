package info.staticfree.android.twentyfourhour.sun;

/*
 * Copyright (C) 2026 Michael Wood
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.ZonedDateTime;

/**
 * The stretch of one day the sun spends below one horizon: from the evening it
 * sinks through it to the morning it climbs back. The dark stretch, because
 * that is what gets shaded; the four of them nest, each twilight's band inside
 * the one before, and the dial darkens by degrees.
 *
 * <p>Three shapes, and a caller must ask which before asking for a moment. An
 * {@linkplain #between arc} has an evening and a morning. A band that is
 * {@linkplain #never never} has neither: the sun stays above this horizon all
 * day, as it does above 18&deg; below in a London June, and there is nothing to
 * shade. A band that is {@linkplain #allDay all day} has neither either, for
 * the opposite reason: the sun never climbs above this horizon, as it does not
 * above the visible one in a Tromsø December, and the whole dial is shaded.
 * The two are not told apart by their moments — both have none — which is
 * why the shape is carried and not inferred.
 *
 * <p>Immutable, and as free of Android as {@link SunPath}.
 */
public final class Band {

    private enum Shape { ARC, NEVER, ALL_DAY }

    private static final Band NEVER = new Band(Shape.NEVER, null, null);

    private static final Band ALL_DAY = new Band(Shape.ALL_DAY, null, null);

    @NonNull
    private final Shape shape;

    @Nullable
    private final ZonedDateTime evening;

    @Nullable
    private final ZonedDateTime morning;

    private Band(@NonNull Shape shape, @Nullable ZonedDateTime evening,
            @Nullable ZonedDateTime morning) {
        this.shape = shape;
        this.evening = evening;
        this.morning = morning;
    }

    /**
     * The sun sank through this horizon at {@code evening} and climbed back at
     * {@code morning}. Both belong to the day asked about, so the evening is
     * ordinarily the later of the two: the arc runs from it round through
     * midnight to the morning, the way a night does on a 24-hour dial.
     */
    @NonNull
    static Band between(@NonNull ZonedDateTime evening, @NonNull ZonedDateTime morning) {
        return new Band(Shape.ARC, evening, morning);
    }

    /** The sun stays above this horizon all day. */
    @NonNull
    static Band never() {
        return NEVER;
    }

    /** The sun stays below this horizon all day. */
    @NonNull
    static Band allDay() {
        return ALL_DAY;
    }

    public boolean isNever() {
        return shape == Shape.NEVER;
    }

    public boolean isAllDay() {
        return shape == Shape.ALL_DAY;
    }

    /**
     * When the sun sank through this horizon.
     *
     * @throws IllegalStateException if the band is {@linkplain #isNever never}
     *         or {@linkplain #isAllDay all day}, which have no such moment.
     */
    @NonNull
    public ZonedDateTime evening() {
        return require(evening);
    }

    /**
     * When the sun climbed back through this horizon.
     *
     * @throws IllegalStateException if the band is {@linkplain #isNever never}
     *         or {@linkplain #isAllDay all day}, which have no such moment.
     */
    @NonNull
    public ZonedDateTime morning() {
        return require(morning);
    }

    @NonNull
    private ZonedDateTime require(@Nullable ZonedDateTime moment) {
        if (moment == null) {
            throw new IllegalStateException("a band that is " + shape + " has no moments");
        }

        return moment;
    }

    @NonNull
    @Override
    public String toString() {
        return shape == Shape.ARC ? "Band{" + evening + " to " + morning + '}'
                : "Band{" + shape + '}';
    }
}
