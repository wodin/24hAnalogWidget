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
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
 * <p>The unit is one widget rather than all of them, because the cell is what
 * decides the picture and each widget has a cell of its own. Nothing here
 * enforces it — a caller is free to build one picture and set it on twenty —
 * but the result would be every widget drawn for whichever one's cell was asked
 * about.
 *
 * <p>Nothing is kept between calls. A {@link Dial} has no lifecycle to reuse,
 * and a {@link Bitmap} handed to the host is the host's; caching either would be
 * holding on to something that is either free to rebuild or no longer ours.
 */
public final class WidgetPicture {

    /**
     * Assembles the picture for one widget.
     *
     * @param context any context in this app; the drawables, the display and the package name are
     *         read from it
     * @param options the host's description of this widget's cell, as
     *         {@link AppWidgetManager#getAppWidgetOptions} returns it. Empty is allowed and means
     *         the host has not sized this widget
     * @return {@link RemoteViews} the host can show, complete rather than a patch on an earlier one
     */
    @NonNull
    public static RemoteViews build(@NonNull Context context, @NonNull Bundle options) {
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

        picture.setImageViewBitmap(R.id.clock, render(context, options));

        return picture;
    }

    /**
     * Draws the dial into a bitmap the host can show.
     *
     * <p>The bitmap is a <em>resolution</em>, not a size: the layout gives the
     * dial the whole cell and scales the bitmap to fit it, so this decides only
     * how much detail there is to scale. Too small and the dial is soft; too
     * large and pixels were painted to be thrown away.
     */
    @NonNull
    private static Bitmap render(@NonNull Context context, @NonNull Bundle options) {
        final Dial dial = Dial.createStandard(context, new SunPositionOverlay(context));
        final int size = sideForCell(context, options, dial.getIntrinsicSize());

        final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        dial.draw(new Canvas(bitmap), new Rect(0, 0, size, size), Instant.now(),
                ZoneId.systemDefault());

        return bitmap;
    }

    /**
     * The side, in pixels, of the square to draw for a widget in this cell.
     *
     * <p>The host describes one cell with four numbers, which pair up into two
     * rectangles rather than into a range: {@code MIN_WIDTH}×{@code MAX_HEIGHT}
     * is the cell in portrait and {@code MAX_WIDTH}×{@code MIN_HEIGHT} the same
     * cell in landscape. A dial is square, so each rectangle asks for the
     * largest square inside it, and this takes the larger of the two — one
     * bitmap has to answer for both, because all four numbers arrive in one
     * bundle and rotating the device fires no options change at all. The larger
     * is the safe one: downscaling it in the other orientation costs nothing,
     * where upscaling the smaller costs sharpness.
     *
     * <p>Capped at the display's smaller dimension. The host refuses a
     * {@link RemoteViews} whose bitmaps exceed a budget that scales with the
     * display, and a cell measured on this display cannot on its own approach
     * that — so the cap never binds in practice, and exists so that a bundle
     * which is somehow wrong costs a soft dial rather than a widget the host
     * drops.
     *
     * @param fallbackPx used when the bundle describes no cell, which is what a host that has
     *         never sized this widget sends. Some resolution has to be chosen and the artwork's
     *         own is the only one that is not a guess
     */
    private static int sideForCell(@NonNull Context context, @NonNull Bundle options, int fallbackPx) {
        final int portraitDp = Math.min(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT));
        final int landscapeDp = Math.min(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT));
        final int sideDp = Math.max(portraitDp, landscapeDp);

        if (sideDp <= 0) {
            return fallbackPx;
        }

        final DisplayMetrics display = context.getResources().getDisplayMetrics();
        final int sidePx = Math.round(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sideDp, display));

        return Math.min(sidePx, Math.min(display.widthPixels, display.heightPixels));
    }

    private WidgetPicture() {
    }
}
