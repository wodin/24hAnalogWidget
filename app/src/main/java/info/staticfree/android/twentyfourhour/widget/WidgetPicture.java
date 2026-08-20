package info.staticfree.android.twentyfourhour.widget;

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

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneId;

import info.staticfree.android.twentyfourhour.ClockUtil;
import info.staticfree.android.twentyfourhour.R;
import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;

/**
 * What one widget shows: a dial drawn at this moment, and the tap that opens the
 * device's clock app.
 *
 * <p>The unit is one widget rather than all of them. Two widgets on a homescreen
 * are two cells of their own sizes, so a picture built once and shown on every
 * id is only correct while they happen to match. Nothing here enforces that —
 * a caller is free to build one and set it on twenty — but building per widget
 * is what a caller has to do anyway once the cell decides what is drawn, and
 * this is the signature that lets it.
 *
 * <p>Nothing is kept between calls. A {@link Dial} has no lifecycle to reuse,
 * and a {@link Bitmap} handed to the host is the host's; caching either would be
 * holding on to something that is either free to rebuild or no longer ours.
 */
public final class WidgetPicture {

    /**
     * Assembles the picture for one widget.
     *
     * @param context any context in this app; the drawables and the package name are read from it
     * @return {@link RemoteViews} the host can show, complete rather than a patch on an earlier one
     */
    @NonNull
    public static RemoteViews build(@NonNull Context context) {
        final RemoteViews picture = new RemoteViews(context.getPackageName(), R.layout.widget);

        // Every picture is a whole one, so the tap is attached every time rather
        // than once: there is no earlier picture left underneath for it to
        // survive in. Null on most modern devices, where ClockUtil finds no
        // clock app at all: from API 30 another package's components are
        // invisible without a <queries> entry, and this app declares none.
        final PendingIntent tap = ClockUtil.getClockIntent(context);
        if (tap != null) {
            picture.setOnClickPendingIntent(R.id.clock, tap);
        }

        picture.setImageViewBitmap(R.id.clock, render(context));

        return picture;
    }

    /**
     * Draws the dial into a bitmap the host can show.
     *
     * <p>The bitmap is the dial's intrinsic size, which is the artwork's own, so
     * the dial is drawn at the resolution it was painted for. The dial fills
     * whatever bounds it is given, so a larger bitmap would not be a larger
     * dial, only the same art upscaled. What size the widget's cell actually is,
     * this does not ask — so a cell larger than the artwork shows it upscaled,
     * and a smaller one shows detail that was drawn and then thrown away.
     */
    @NonNull
    private static Bitmap render(@NonNull Context context) {
        final Dial dial = Dial.createStandard(context, new SunPositionOverlay(context));
        final int size = dial.getIntrinsicSize();

        final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        dial.draw(new Canvas(bitmap), new Rect(0, 0, size, size), Instant.now(),
                ZoneId.systemDefault());

        return bitmap;
    }

    private WidgetPicture() {
    }
}
