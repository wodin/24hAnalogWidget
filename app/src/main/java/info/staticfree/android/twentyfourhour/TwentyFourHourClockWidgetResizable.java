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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Calendar;

import info.staticfree.android.twentyfourhour.widget.WidgetPicture;

public class TwentyFourHourClockWidgetResizable extends AppWidgetProvider {

    /**
     * Sending this broadcast intent will cause the clock widget to update.
     */
    public static String ACTION_CLOCK_UPDATE = "info.staticfree.android.twentyfourhour.ACTION_CLOCK_UPDATE";

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        final AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(createUpdate(context));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        final String action = intent.getAction();

        if (ACTION_CLOCK_UPDATE.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)
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
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        startTicking(context);

        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId,
                    WidgetPicture.build(context, appWidgetManager.getAppWidgetOptions(appWidgetId)));
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
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        startTicking(context);

        appWidgetManager.updateAppWidget(appWidgetId, WidgetPicture.build(context, newOptions));
    }

    /**
     * Schedules an alarm to update the clock every minute, at the top of the minute.
     *
     * <p>Called on every update, and safe to be: the {@link PendingIntent} it
     * schedules is the same one each time, so {@link AlarmManager} replaces the
     * outstanding alarm rather than adding to it. See {@link #createUpdate}.
     *
     * @param context application context
     */
    private void startTicking(Context context) {
        final AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);

        // schedules updates so they occur on the top of the minute
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, 1);
        alarmManager.setRepeating(AlarmManager.RTC, c.getTimeInMillis(), 1000 * 60,
                createUpdate(context));
    }

    /**
     * Creates the {@link PendingIntent} that broadcasts an update to the widget.
     *
     * <p>Immutable because one built with neither {@code FLAG_IMMUTABLE} nor
     * {@code FLAG_MUTABLE} is fatal when targeting S ≥ 31, and a process that
     * dies scheduling its tick never gets as far as drawing anything. Immutable
     * rather than mutable because the intent says only <em>redraw now</em>:
     * there is nothing in it for a receiver to fill in.
     *
     * <p>What holds this to one alarm is not a flag but identity — two
     * {@link PendingIntent}s equal on request code and
     * {@link Intent#filterEquals} are the same one, so {@link AlarmManager}
     * replaces rather than accumulates, and {@link #onDisabled} can cancel the
     * alarm {@link #startTicking} set. {@code FLAG_UPDATE_CURRENT} does not
     * provide that; it only refreshes an existing instance's extras, which is a
     * no-op while this intent carries none and the right flag if it ever does.
     *
     * @param context application context
     * @return the {@link PendingIntent} whose delivery triggers a redraw
     */
    private PendingIntent createUpdate(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(ACTION_CLOCK_UPDATE),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
