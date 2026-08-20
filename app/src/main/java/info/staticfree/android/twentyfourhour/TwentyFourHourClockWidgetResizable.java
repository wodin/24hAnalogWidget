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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;

import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;

public class TwentyFourHourClockWidgetResizable extends AppWidgetProvider {

    /**
     * Sending this broadcast intent will cause the clock widgets to update.
     */
    public static String ACTION_CLOCK_UPDATE = "info.staticfree.android.twentyfourhour.ACTION_CLOCK_UPDATE";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);

        startTicking(context);
    }

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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        // Every update is a whole one, so the click intent is attached every
        // time rather than once: there is no earlier update left underneath for
        // it to survive in.
        final PendingIntent intent = ClockUtil.getClockIntent(context);
        if (intent != null) {
            rv.setOnClickPendingIntent(R.id.clock, intent);
        }

        rv.setImageViewBitmap(R.id.clock, render(context));

        appWidgetManager.updateAppWidget(appWidgetIds, rv);
    }

    /**
     * Draws the dial into a bitmap the host can show.
     *
     * <p>The bitmap is the dial's intrinsic size, which is the artwork's own, so
     * it carries no margin the dial could never reach into. What size the
     * widget's cell actually is, the receiver has no way to ask here; sizing the
     * bitmap to the cell is the architecture review's candidate E.
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

    /**
     * Schedules an alarm to update the clock every minute, at the top of the minute.
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
     * Creates an intent to update the clock(s).
     *
     * @param context application context
     * @return the intent to update the clock
     */
    private PendingIntent createUpdate(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(ACTION_CLOCK_UPDATE),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
