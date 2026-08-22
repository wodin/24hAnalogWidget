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
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import info.staticfree.android.twentyfourhour.R;
import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.observer.Observer;
import info.staticfree.android.twentyfourhour.sun.SunPath;

/**
 * The Sun position, drawn as shading over the hours the sun is down. A painter:
 * it asks a {@link SunPath} what the sun does and the {@link Dial} where each
 * moment falls, and works out neither itself. The only arithmetic left here is
 * turning a dial angle into the frame {@link Canvas#drawArc} measures in, which
 * is a fact about the drawing primitive rather than about the dial.
 *
 * <p><strong>Nothing tunes what this draws.</strong> Four setters once did — an
 * inset scale, a shade alpha and a switch each for the Twilight bands and the
 * High noon mark — all of them configuring upstream's Wear watch face, which
 * {@code docs/adr/0001-drop-wear-support.md} dropped. The switches went, not what
 * they switched: every arc they guarded is still drawn, unconditionally.
 * {@link #setObserver} is not a knob; it pushes in what to draw rather than how.
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

    /** How far out the overlay reaches, as a fraction of the dial's radius. */
    private static final float INSET_RADIUS_FRACTION = 0.5f;

    private final RectF inset = new RectF();

    @Nullable
    private Observer observer;

    private static final Paint OVERLAY_NO_INFO_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final Paint OVERLAY_SUN = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint OVERLAY_SUNSET = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        OVERLAY_SUN.setARGB(127, 255, 201, 14); // Orange for Sun
        OVERLAY_SUN.setStyle(Paint.Style.FILL);

        OVERLAY_SUNSET.setARGB(20, 0, 0, 0); // Sunrise/Sunset
        OVERLAY_SUNSET.setStyle(Paint.Style.FILL);
    }

    public SunPositionOverlay(@NonNull Context context) {
        OVERLAY_NO_INFO_PAINT.setShader(new BitmapShader(BitmapFactory
                .decodeResource(context.getResources(), R.drawable.no_sunrise_sunset_tile),
                Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    /**
     * Where the dial is being looked at from, or {@code null} if nobody knows.
     * Pushed in rather than read here: an overlay is a painter, and a painter
     * that went looking for the answer would be doing it inside a draw.
     */
    public void setObserver(@Nullable Observer observer) {
        this.observer = observer;
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
        int insetRadius = (int) (side / 2.0f * INSET_RADIUS_FRACTION);
        inset.set(centre - insetRadius, centre - insetRadius, centre + insetRadius,
                centre + insetRadius);

        if (observer == null) {
            // Not an error, and the Placeholder says so: the sun position is
            // unknown rather than absent.
            drawPlaceholder(canvas);

            return;
        }

        SunPath path = SunPath.on(observer, instant, zone);

        if (path == null) {
            Log.w(TAG, "no sun path at " + observer + "; drawing the placeholder instead");
            drawPlaceholder(canvas);

            return;
        }

        drawInsetArc(canvas, arcAngleFor(path.sunset()), arcAngleFor(path.sunrise()),
                OVERLAY_SUNSET);
        drawInsetArc(canvas, arcAngleFor(path.eveningCivilTwilight()),
                arcAngleFor(path.morningCivilTwilight()), OVERLAY_SUNSET);
        drawInsetArc(canvas, arcAngleFor(path.eveningNauticalTwilight()),
                arcAngleFor(path.morningNauticalTwilight()), OVERLAY_SUNSET);
        drawInsetArc(canvas, arcAngleFor(path.eveningAstronomicalTwilight()),
                arcAngleFor(path.morningAstronomicalTwilight()), OVERLAY_SUNSET);

        // Marked without asking whether there was a day to mark the middle of.
        // A SunPath searches back from High noon for its sunrise and forward for
        // its sunset, so any path in hand has the one strictly before the other.
        float highNoonAngle = arcAngleFor(path.highNoon());

        canvas.drawArc(inset, highNoonAngle - HIGH_NOON_ARC_ANGLE / 2, HIGH_NOON_ARC_ANGLE, true,
                OVERLAY_SUN);
    }

    private void drawInsetArc(@NonNull Canvas canvas, float startAngle, float endAngle,
            @NonNull Paint paint) {
        canvas.drawArc(inset, startAngle, (DEGREE_CIRCLE + (endAngle - startAngle)) % DEGREE_CIRCLE,
                true, paint);
    }
}
