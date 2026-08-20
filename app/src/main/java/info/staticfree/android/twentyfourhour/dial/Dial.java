package info.staticfree.android.twentyfourhour.dial;
/*
 * Copyright (C) 2011-2014 Steve Pomeroy <steve@staticfree.info>
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
 */

/*
 * Some portions Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import info.staticfree.android.twentyfourhour.R;
import info.staticfree.android.twentyfourhour.overlay.DialOverlay;
import info.staticfree.android.twentyfourhour.overlay.HandsOverlay;

/**
 * The 24-hour dial: a Face with overlays drawn over it, painted onto whatever
 * canvas you hand it at whatever moment you name.
 *
 * <p>A Dial is not a {@link android.view.View} and has no lifecycle, so one
 * built per widget update has nothing to tear down. The time is a parameter of
 * {@link #draw} rather than a field, which is what lets the same Dial be drawn
 * at one instant and then at another without anything in between going stale.
 *
 * <p>Immutable, but not thread-safe: the overlays it draws through keep scratch
 * state and move their Drawables' bounds, so draws must not overlap.
 *
 * <p>The dial is square by definition — a circular scale in the largest centred
 * square its bounds will hold — which is why one {@link #getIntrinsicSize()}
 * answers for both axes.
 */
public class Dial {

    @NonNull
    private final Drawable face;

    @NonNull
    private final DialOverlay[] overlays;

    private final int intrinsicSize;

    /**
     * @param face the Face artwork, drawn beneath everything else
     * @param overlays drawn over the Face in the order given, so the last is on top
     */
    public Dial(@NonNull Drawable face, @NonNull DialOverlay... overlays) {
        this.face = face;
        this.overlays = overlays.clone();
        intrinsicSize = Math.max(face.getIntrinsicWidth(), face.getIntrinsicHeight());
    }

    /**
     * The dial this app draws: its own Face artwork and its own two hands, with
     * the given overlays between the two. The hands belong on top, so an
     * overlay passed here is drawn under them.
     */
    @NonNull
    public static Dial createStandard(@NonNull Context context,
            @NonNull DialOverlay... overlays) {
        DialOverlay[] withHands = Arrays.copyOf(overlays, overlays.length + 1);
        withHands[overlays.length] = new HandsOverlay(context.getDrawable(R.drawable.hour_hand),
                context.getDrawable(R.drawable.minute_hand));

        return new Dial(context.getDrawable(R.drawable.clock_face), withHands);
    }

    /**
     * The side, in pixels, of the square the artwork was drawn for. Both hosts
     * size from this: it is the one place the Face's dimensions are known, so
     * swapping the art cannot silently desync them.
     */
    public int getIntrinsicSize() {
        return intrinsicSize;
    }

    /**
     * Draws the dial into {@code bounds}, showing {@code instant} as read in
     * {@code zone}.
     *
     * <p>The dial occupies the largest square that fits centred in the bounds,
     * growing or shrinking to fill it: the artwork's own size decides how much
     * detail there is to draw, never how large it is drawn. Everything below
     * draws in intrinsic-size coordinates and the canvas transform does the
     * fitting, so an overlay never needs to know how large it is being drawn.
     */
    public void draw(@NonNull Canvas canvas, @NonNull Rect bounds, @NonNull Instant instant,
            @NonNull ZoneId zone) {
        int side = Math.min(bounds.width(), bounds.height());
        float scale = (float) side / intrinsicSize;

        canvas.save();

        // Centred in float, so bounds with an odd spare dimension put the dial
        // on the true centre rather than half a pixel to one side of it, as the
        // View this was extracted from did.
        canvas.translate(bounds.left + (bounds.width() - side) / 2f,
                bounds.top + (bounds.height() - side) / 2f);
        canvas.scale(scale, scale);

        int c = intrinsicSize / 2;
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, zone));

        face.setBounds(0, 0, intrinsicSize, intrinsicSize);
        face.draw(canvas);

        for (DialOverlay overlay : overlays) {
            // Always true: re-bounding three Drawables is unmeasurable beside
            // the drawing itself, and caching it is what forced the old View to
            // expose a lifecycle callback as public API.
            overlay.onDraw(canvas, c, c, intrinsicSize, intrinsicSize, calendar, true);
        }

        canvas.restore();
    }
}
