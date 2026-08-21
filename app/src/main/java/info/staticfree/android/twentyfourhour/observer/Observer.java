package info.staticfree.android.twentyfourhour.observer;

/*
 * Copyright (C) 2011-2012 Steve Pomeroy <steve@staticfree.info>
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

/**
 * Where the viewer stands: a latitude and a longitude, and nothing else.
 *
 * <p>Deliberately not Android's {@code Location}, which cannot be constructed
 * on a plain JVM and carries twenty fields of which the sun uses two. The same
 * argument that shaped {@code SunPath}: what crosses a seam should be what the
 * far side needs.
 *
 * <p>Nothing here knows what an observer is <em>for</em>. The sun reads one; a
 * reader produces one; neither is visible from inside this package, and the
 * dependency runs that way round on purpose.
 *
 * <p><strong>No age.</strong> This is a place, not a reading, so there is
 * nothing to ask how old it is and no max-age check anyone can add without
 * first changing the type — which is the moment they would have to re-read why
 * there isn't one.
 *
 * <p>Nothing validates the two numbers, because the almanac already does: a
 * latitude outside &plusmn;90&deg; fails inside {@code SunTimes} with a message
 * naming the range, and a second check here would only be able to say the same
 * thing earlier.
 */
public final class Observer {

    private final double latitude;

    private final double longitude;

    public Observer(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Degrees north of the equator; negative south of it. */
    public double latitude() {
        return latitude;
    }

    /** Degrees east of Greenwich; negative west of it. */
    public double longitude() {
        return longitude;
    }

    /**
     * The two numbers comma-separated, latitude first, which is the order every
     * mapping service reads them in — so a log line carrying one can be pasted
     * straight into a map.
     *
     * <p>Here because a painter that cannot find a sun path says so to the log,
     * and that line is only worth writing if it names the place it failed at.
     * There is deliberately no {@code equals} to go with it: nothing compares
     * two observers, and a value that is only ever printed needs only this.
     */
    @Override
    @NonNull
    public String toString() {
        return latitude + ", " + longitude;
    }
}
