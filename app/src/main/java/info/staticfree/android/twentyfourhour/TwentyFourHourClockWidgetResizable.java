package info.staticfree.android.twentyfourhour;

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

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import info.staticfree.android.twentyfourhour.observer.LastKnownObserver;
import info.staticfree.android.twentyfourhour.observer.Observer;
import info.staticfree.android.twentyfourhour.widget.Tick;
import info.staticfree.android.twentyfourhour.widget.WidgetPicture;

public class TwentyFourHourClockWidgetResizable extends AppWidgetProvider {

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        Tick.cancel(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final String action = intent.getAction();

        if (Tick.isTick(intent) || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            final ComponentName appWidgets = new ComponentName(context.getPackageName(), getClass()
                    .getName());
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            final int ids[] = appWidgetManager.getAppWidgetIds(appWidgets);
            if (ids.length > 0) {
                onUpdate(context, appWidgetManager, ids);
            }
        }
    }

    /**
     * Arms the tick that will redraw the widgets, then gives each of them a
     * freshly built picture.
     *
     * <p>One rule rather than two hooks: everything that redraws the widget also
     * schedules the next redraw, so <em>a widget on screen has a tick behind
     * it</em> is restored by any update at all — including the one the host
     * sends after a reboot, which is what carries the clock across one.
     * {@code onEnabled} cannot make that promise: the platform documents its
     * broadcast as sent when a widget is added <em>for the first time</em>, and
     * an invariant resting on a hook that says "first time" is resting on
     * whatever the host happens to do afterwards.
     *
     * <p>Armed before the render rather than after, so a render that throws —
     * an allocation failure under memory pressure, say — costs one redraw rather
     * than the clock.
     *
     * <p>A picture each, rather than one built and shown on every id: the ids in
     * one update are separate widgets in cells of their own sizes, and a picture
     * can only answer to one of them.
     *
     * <p>The observer, though, is read once and shared. It is a property of the
     * phone rather than of a cell, so a read per id would ask the same question
     * as many times as the user happens to have pinned widgets — and would let
     * two dials on one screen disagree about where they are being looked at
     * from, if a fix landed between them. This is also the whole of the read:
     * {@link LastKnownObserver} is the only Android-flavoured call the sun
     * needs, and it lives here beside {@code Tick} and {@code AppWidgetManager}
     * rather than inside the assembler or, worse, inside a draw.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Tick.schedule(context);

        final Observer observer = LastKnownObserver.read(context);

        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId,
                    WidgetPicture.build(context, appWidgetManager.getAppWidgetOptions(appWidgetId),
                            observer));
        }
    }

    /**
     * Redraws the one widget the user resized, for the cell it now occupies.
     *
     * <p>Without this the host keeps showing the picture it was last given,
     * scaled to the new cell — a dial that grows blurred as it grows. The host
     * does not follow a resize with an update, so this is the only notice there
     * is.
     *
     * <p>The new cell is read from {@code newOptions} rather than from
     * {@code appWidgetManager}: the framework hands over the fresh bundle, and
     * asking the manager would be putting a second source between this and
     * something the first one has just said.
     *
     * <p>Arms the tick as well, for the reason {@link #onUpdate} gives. A resize
     * cannot in practice be the first thing a widget receives, so this buys no
     * case an update does not already cover — it is here so that <em>every path
     * that redraws the widget schedules the next redraw</em> holds without an
     * exception a reader would have to check.
     *
     * <p>And reads the observer, because a redraw that skipped the read would
     * hand the picture a null and paint the Placeholder over a sun the user
     * could see a moment ago, purely because they dragged a corner. One read
     * rather than {@link #onUpdate}'s one-for-many, there being one widget
     * here.
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        Tick.schedule(context);

        final Observer observer = LastKnownObserver.read(context);

        appWidgetManager.updateAppWidget(appWidgetId,
                WidgetPicture.build(context, newOptions, observer));
    }
}
