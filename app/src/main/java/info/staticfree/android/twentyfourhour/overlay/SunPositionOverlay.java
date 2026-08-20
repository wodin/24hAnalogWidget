package info.staticfree.android.twentyfourhour.overlay;

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

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import info.staticfree.android.twentyfourhour.R;
import info.staticfree.android.twentyfourhour.dial.Dial;
import uk.me.jstott.coordconv.LatitudeLongitude;
import uk.me.jstott.sun.Sun;
import uk.me.jstott.sun.Time;

import static uk.me.jstott.sun.Sun.eveningAstronomicalTwilightTime;
import static uk.me.jstott.sun.Sun.eveningCivilTwilightTime;
import static uk.me.jstott.sun.Sun.eveningNauticalTwilightTime;
import static uk.me.jstott.sun.Sun.morningAstronomicalTwilightTime;
import static uk.me.jstott.sun.Sun.morningCivilTwilightTime;
import static uk.me.jstott.sun.Sun.morningNauticalTwilightTime;

public class SunPositionOverlay implements DialOverlay {
    private static final String TAG = SunPositionOverlay.class.getSimpleName();
    private static final float HIGH_NOON_ARC_ANGLE = 2;
    private static final float DEGREE_CIRCLE = 360;

    private final RectF inset = new RectF();
    private final LatitudeLongitude latLon = new LatitudeLongitude(0, 0);

    @Nullable
    private Location location;

    private static final Paint OVERLAY_NO_INFO_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final Paint OVERLAY_SUN = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint OVERLAY_SUNSET = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        OVERLAY_SUN.setARGB(127, 255, 201, 14); // Orange for Sun
        OVERLAY_SUN.setStyle(Paint.Style.FILL);

        OVERLAY_SUNSET.setARGB(20, 0, 0, 0); // Sunrise/Sunset
        OVERLAY_SUNSET.setStyle(Paint.Style.FILL);
    }

    private float scale = 0.5f;
    private boolean showTwilight = true;
    private boolean showHighNoon = true;

    public SunPositionOverlay(@NonNull Context context) {
        OVERLAY_NO_INFO_PAINT.setShader(new BitmapShader(BitmapFactory
                .decodeResource(context.getResources(), R.drawable.no_sunrise_sunset_tile),
                Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setShadeAlpha(int alpha) {
        OVERLAY_SUNSET.setAlpha(alpha);
    }

    public void setShowTwilight(boolean showTwilight) {
        this.showTwilight = showTwilight;
    }

    public void setShowHighNoon(boolean showHighNoon) {
        this.showHighNoon = showHighNoon;
    }

    public void setLocation(@Nullable Location location) {
        this.location = location;

        if (location != null) {
            latLon.setLatitude(location.getLatitude());
            latLon.setLongitude(location.getLongitude());
        }
    }

    private float getHourArcAngle(@NonNull Time time) {
        return (Dial.angleFor(LocalTime.of(time.getHours(), time.getMinutes())) + 270) %
                DEGREE_CIRCLE;
    }

    private void drawPlaceholder(@NonNull Canvas canvas) {
        canvas.drawArc(inset, 0, DEGREE_CIRCLE / 2, true, OVERLAY_NO_INFO_PAINT);
    }

    @Override
    public void draw(@NonNull Canvas canvas, int side, @NonNull Instant instant,
            @NonNull ZoneId zone) {
        int centre = side / 2;
        int insetRadius = (int) (side / 2.0f * scale);
        inset.set(centre - insetRadius, centre - insetRadius, centre + insetRadius,
                centre + insetRadius);

        if (location == null) {
            // not much we can do if we don't have a location
            drawPlaceholder(canvas);

            return;
        }

        // jsuntimes takes a Calendar and a TimeZone, so this is where the moment
        // is converted back to one. The last Calendar in the module, and it goes
        // when the library does.
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, zone));

        TimeZone tz = calendar.getTimeZone();

        boolean dst = calendar.get(Calendar.DST_OFFSET) != 0;

        try {
            float morningSunAngle = getHourArcAngle(Sun.sunriseTime(calendar, latLon, tz, dst));

            float eveningSunAngle = getHourArcAngle(Sun.sunsetTime(calendar, latLon, tz, dst));

            float highNoon = (DEGREE_CIRCLE + morningSunAngle +
                    ((DEGREE_CIRCLE + (eveningSunAngle - morningSunAngle)) % DEGREE_CIRCLE) / 2) %
                    DEGREE_CIRCLE;

            drawInsetArc(canvas, eveningSunAngle, morningSunAngle, OVERLAY_SUNSET);

            if (showTwilight) {
                drawInsetArc(canvas,
                        getHourArcAngle(eveningCivilTwilightTime(calendar, latLon, tz, dst)),
                        getHourArcAngle(morningCivilTwilightTime(calendar, latLon, tz, dst)),
                        OVERLAY_SUNSET);
                drawInsetArc(canvas,
                        getHourArcAngle(eveningNauticalTwilightTime(calendar, latLon, tz, dst)),
                        getHourArcAngle(morningNauticalTwilightTime(calendar, latLon, tz, dst)),
                        OVERLAY_SUNSET);
                drawInsetArc(canvas,
                        getHourArcAngle(eveningAstronomicalTwilightTime(calendar, latLon, tz, dst)),
                        getHourArcAngle(morningAstronomicalTwilightTime(calendar, latLon, tz, dst)),
                        OVERLAY_SUNSET);
            }

            if (showHighNoon) {
                if (Math.abs(eveningSunAngle - morningSunAngle) > 0) {
                    canvas.drawArc(inset, highNoon - HIGH_NOON_ARC_ANGLE / 2, HIGH_NOON_ARC_ANGLE,
                            true, OVERLAY_SUN);
                }
            }

            // this can happen when lat/lon and the timezone are out of sync, causing impossible
            // sunrise/sunset times to be calculated. Sun reports those by throwing; a Time it
            // does return, but out of range, fails later in LocalTime.of. Same cause, same
            // answer.
        } catch (IllegalArgumentException | DateTimeException e) {
            Log.e(TAG, "Error computing sunrise / sunset time", e);
            drawPlaceholder(canvas);
        }
    }

    private void drawInsetArc(@NonNull Canvas canvas, float startAngle, float endAngle,
            @NonNull Paint paint) {
        canvas.drawArc(inset, startAngle, (DEGREE_CIRCLE + (endAngle - startAngle)) % DEGREE_CIRCLE,
                true, paint);
    }
}
