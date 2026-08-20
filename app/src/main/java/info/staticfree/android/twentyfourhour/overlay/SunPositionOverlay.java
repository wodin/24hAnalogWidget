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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import info.staticfree.android.twentyfourhour.R;
import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.sun.SunPath;

/**
 * The Sun position, drawn as shading over the hours the sun is down. A painter:
 * it asks a {@link SunPath} what the sun does and the {@link Dial} where each
 * moment falls, and works out neither itself. The only arithmetic left here is
 * turning a dial angle into the frame {@link Canvas#drawArc} measures in, which
 * is a fact about the drawing primitive rather than about the dial.
 */
public class SunPositionOverlay implements DialOverlay {
    private static final String TAG = SunPositionOverlay.class.getSimpleName();
    private static final float HIGH_NOON_ARC_ANGLE = 2;
    private static final float DEGREE_CIRCLE = 360;

    /**
     * A dial angle is measured clockwise from straight up; {@link Canvas#drawArc}
     * measures clockwise from due east. Three quarters of a turn between them.
     */
    private static final float UP_TO_EAST = 270;

    private final RectF inset = new RectF();

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
    }

    private static float arcAngleFor(@NonNull ZonedDateTime moment) {
        return (Dial.angleFor(moment.toLocalTime()) + UP_TO_EAST) % DEGREE_CIRCLE;
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
            // Not an error, and the Placeholder says so: the sun position is
            // unknown rather than absent.
            drawPlaceholder(canvas);

            return;
        }

        SunPath path = SunPath.on(location.getLatitude(), location.getLongitude(), instant, zone);

        if (path == null) {
            Log.w(TAG, "no sun path at " + location + "; drawing the placeholder instead");
            drawPlaceholder(canvas);

            return;
        }

        float morningSunAngle = arcAngleFor(path.sunrise());
        float eveningSunAngle = arcAngleFor(path.sunset());

        drawInsetArc(canvas, eveningSunAngle, morningSunAngle, OVERLAY_SUNSET);

        if (showTwilight) {
            drawInsetArc(canvas, arcAngleFor(path.eveningCivilTwilight()),
                    arcAngleFor(path.morningCivilTwilight()), OVERLAY_SUNSET);
            drawInsetArc(canvas, arcAngleFor(path.eveningNauticalTwilight()),
                    arcAngleFor(path.morningNauticalTwilight()), OVERLAY_SUNSET);
            drawInsetArc(canvas, arcAngleFor(path.eveningAstronomicalTwilight()),
                    arcAngleFor(path.morningAstronomicalTwilight()), OVERLAY_SUNSET);
        }

        if (showHighNoon && Math.abs(eveningSunAngle - morningSunAngle) > 0) {
            float highNoon = arcAngleFor(path.highNoon());

            canvas.drawArc(inset, highNoon - HIGH_NOON_ARC_ANGLE / 2, HIGH_NOON_ARC_ANGLE, true,
                    OVERLAY_SUN);
        }
    }

    private void drawInsetArc(@NonNull Canvas canvas, float startAngle, float endAngle,
            @NonNull Paint paint) {
        canvas.drawArc(inset, startAngle, (DEGREE_CIRCLE + (endAngle - startAngle)) % DEGREE_CIRCLE,
                true, paint);
    }
}
