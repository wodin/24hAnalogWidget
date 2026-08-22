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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneId;

import info.staticfree.android.twentyfourhour.R;
import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.observer.Observer;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;

/**
 * What one widget shows: a dial drawn at this moment, and a tap — which opens
 * the device's alarm list, or, on a device where nothing answers, says as much.
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
     * @param observer where the dial is being looked at from, or {@code null} if nobody knows —
     *         which is the ordinary case and draws the Placeholder
     * @return {@link RemoteViews} the host can show, complete rather than a patch on an earlier one
     */
    @NonNull
    public static RemoteViews build(@NonNull Context context, @NonNull Bundle options,
            @Nullable Observer observer) {
        final RemoteViews picture = new RemoteViews(context.getPackageName(), R.layout.widget);

        // Every picture is a whole one, so the tap is attached every time rather
        // than once: there is no earlier picture left underneath for it to
        // survive in.
        picture.setOnClickPendingIntent(R.id.clock, tap(context));

        picture.setImageViewBitmap(R.id.clock, render(context, options, observer));

        return picture;
    }

    /**
     * A tap that opens whatever shows alarms on this device, or — where nothing
     * does — one that reaches {@link UnansweredTap} and says so. The alarm list
     * because that is what a clock widget has always opened here, and because
     * the widget itself cannot be tapped into anything else: it shows a time
     * and takes no input.
     *
     * <p>There is always a tap. Attaching none where nothing resolves was the
     * earlier behaviour and looked like the honest one, but the two ways this
     * app has really lost its tap both presented as a widget that ignores a
     * finger — so an absent tap is a fault's disguise rather than a statement.
     * {@link UnansweredTap} says why nothing opened; that it is our receiver
     * and not our activity is explained there.
     *
     * <p>{@link AlarmClock#ACTION_SHOW_ALARMS} rather than a named component.
     * This used to walk a table of six vendor clock packages asking the package
     * manager whether each was installed, which was the only way to do it in
     * 2011 and stopped working at API 30: another package is invisible to that
     * question unless the manifest declares a {@code <queries>} for it, so all
     * six answered "not installed" and the widget quietly lost its tap. The
     * action has been platform since API 19, is what every clock app registers
     * for, and needs no table to keep up to date — only the one {@code
     * <queries>} entry that makes its handler visible to ask about.
     *
     * <p>Resolved here, as the picture is built, because this is where one of
     * the two intents has to be chosen. The choice could be deferred instead —
     * hang the receiver on every tap and let it resolve on arrival — and that
     * shape is workable rather than impossible; {@link UnansweredTap} is where
     * the reason for rejecting it is written down. {@link
     * PackageManager#MATCH_DEFAULT_ONLY} asks the question the launcher will
     * later ask when it sends this: not "does any filter mention the action"
     * but "would starting it find someone".
     *
     * <p>Immutable, because nothing that receives this is meant to add to it.
     */
    @NonNull
    private static PendingIntent tap(@NonNull Context context) {
        final Intent alarms = new Intent(AlarmClock.ACTION_SHOW_ALARMS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (context.getPackageManager().resolveActivity(alarms, PackageManager.MATCH_DEFAULT_ONLY)
                == null) {
            return UnansweredTap.pendingComplaint(context);
        }

        return PendingIntent.getActivity(context, 0, alarms, PendingIntent.FLAG_IMMUTABLE);
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
    private static Bitmap render(@NonNull Context context, @NonNull Bundle options,
            @Nullable Observer observer) {
        final SunPositionOverlay sun = new SunPositionOverlay(context);
        sun.setObserver(observer);

        final Dial dial = Dial.createStandard(context, sun);
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
