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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import info.staticfree.android.twentyfourhour.TwentyFourHourClockWidgetResizable;

/**
 * The schedule the widget's picture is redrawn on: one redraw at the top of
 * every minute, which is as often as a dial without a second hand can change.
 *
 * <p>Armed a minute at a time rather than as a repeat, and re-armed by whatever
 * the last one caused. That makes the schedule a chain rather than a standing
 * order — every link asks for exactly one more — which is what lets the same
 * call serve arming, re-arming and recovery, and it is the reason
 * {@link #schedule} may be called by anything that redraws the widget without
 * anyone counting the calls.
 *
 * <p>Not a repeat and not a wakeup. In short: a repeat does not drift, but the
 * framework derives its batching window from the repeat interval, so a minutely
 * one asks to be run any time in the following 45 seconds and takes most of
 * them; and {@link AlarmManager#RTC} is <em>defined</em> as delivered at the
 * next moment the device is awake, which is already "redraw when someone starts
 * looking".
 *
 * <p>Exact when the platform allows it, inexact when it does not — see
 * {@link #schedule}, and {@link #keepsExactTime} for the question anything
 * outside this class asks to find out which. Every one of those is a policy
 * trade rather than a fact about this code, and each was measured before it was
 * chosen. The measurements live in {@code docs/adr/0003-the-widget-tick.md},
 * which is also where to look before changing any of them.
 */
public final class Tick {

    /**
     * What the tick says. Private, because what a tick is made of is nobody
     * else's business: this used to be a public constant on a receiver with a
     * matching {@code <intent-filter>}, so "redraw now" was an action anything
     * on the device could broadcast without even knowing what it was talking
     * to. The string survives at all only because {@link #isTick} has to pick
     * the alarm's own delivery out of the other broadcasts the receiver is
     * sent.
     *
     * <p>Its <em>value</em> is now arbitrary. It used to be worth preserving
     * across an upgrade, because {@link AlarmManager} matches an outstanding
     * {@link PendingIntent} by {@link Intent#filterEquals} and a changed action
     * would strand the tick the previous version armed. Naming the component
     * broke that match anyway — <strong>measured</strong> across the upgrade
     * that introduced it, the old alarm survived {@code install -r} and sat
     * beside the new one, beyond the reach of {@link #cancel}. It is harmless
     * (its implicit action now matches no filter, so it is delivered to
     * nothing) and a reboot clears it, alarms being memory-only. The string is
     * kept because changing it would buy nothing, not because keeping it buys
     * anything.
     */
    private static final String ACTION_TICK =
            "info.staticfree.android.twentyfourhour.ACTION_CLOCK_UPDATE";

    /**
     * Arms the next redraw, replacing any already armed.
     *
     * <p>Exact if the platform will have it. That costs a permission, and the
     * permission is why this is not simply {@code setExact}: it is denied by
     * default from API 33, so the call would throw on any phone whose owner had
     * not granted it. What buys the exactness is the audience — one sideloaded
     * phone whose install already runs {@code pm grant} — and what makes it
     * safe to ask for is that being refused costs only the punctuality.
     *
     * <p>Without it the tick is delivered at the framework's convenience within
     * roughly the next 45 seconds, which is not a rounding error: measured
     * against upstream's build on the same phone, this dial ran 3 to 69 seconds
     * behind and skipped a minute outright. Exactness is the only cure — a
     * short {@code setWindow} was measured and the framework substitutes its own
     * heuristic for the window asked. Both measurements are in ADR-0003.
     *
     * <p>Idempotent, and that is load-bearing rather than incidental — it is
     * what lets every path that redraws the widget arm the next redraw without
     * anyone tracking whether one is outstanding. It holds by identity, not by
     * a flag: {@link #pendingRedraw} builds the same {@link PendingIntent}
     * every time, so {@link AlarmManager} replaces rather than accumulates.
     */
    public static void schedule(@NonNull Context context) {
        final AlarmManager alarms = alarms(context);
        final long topOfNextMinute = topOfNextMinute();
        final PendingIntent redraw = pendingRedraw(context);

        if (keepsExactTime(alarms)) {
            try {
                alarms.setExact(AlarmManager.RTC, topOfNextMinute, redraw);
                return;
            } catch (SecurityException revokedSinceTheCheck) {
                // Between the check and the call is a window the user can
                // revoke the grant in, and an uncaught throw here would end the
                // chain: nothing re-arms a tick that never got armed, so the
                // dial would freeze on the minute it last drew and read as a
                // working clock. The inexact tick below is what this build did
                // before exactness was asked for, so falling to it costs
                // punctuality and nothing else.
            }
        }

        alarms.set(AlarmManager.RTC, topOfNextMinute, redraw);
    }

    /**
     * Whether the platform will presently allow an exact tick.
     *
     * <p>A question about the grant, and deliberately not a promise about any
     * particular redraw. The two come apart on the path {@link #schedule}
     * catches: the grant can be withdrawn between this answer and the next
     * arming, and that tick is then inexact while this method last said
     * otherwise. So "yes" means the next tick will be armed exactly if nothing
     * changes under it, which is the most anything can truthfully say here.
     *
     * <p>{@code SCHEDULE_EXACT_ALARM} is granted on install below API 33 and
     * denied by default at or above it, so this is a runtime question rather
     * than a manifest one, and the answer can change under a running app —
     * hence the catch in {@link #schedule} as well as the check here. Below API
     * 31 there is nothing to ask: exactness needed no permission before
     * {@code S}, and {@code canScheduleExactAlarms} did not exist to answer.
     *
     * <p><strong>Public because the fallback turned out to be worth
     * reporting</strong>, which ADR-0003 recorded the opposite of and now
     * records the correction to. What changed is not the schedule but this
     * fork's README, which publishes the {@code appops} line that grants the op
     * as an install step — so the fallback stopped being an accepted mode and
     * became a step somebody skipped. Measured on the A34, what that looks like
     * is a dial running 3 to 69 seconds behind the phone's own clock with a
     * minute skipped outright: a working clock at a glance, which is the
     * failure mode this app keeps having.
     *
     * <p><strong>Asked here, reported elsewhere.</strong> This answers and says
     * nothing itself, because every caller inside this class is on the redraw
     * path and {@code onUpdate} runs minutely — a line written here would be
     * 1440 a day. {@code WidgetDemo} asks it once per launch, which is a person
     * opening the app, and that is where the answer is turned into words.
     */
    public static boolean keepsExactTime(@NonNull Context context) {
        return keepsExactTime(alarms(context));
    }

    /**
     * The same question of an {@link AlarmManager} already in hand, which is
     * how {@link #schedule} asks it: the two callers differ only in whether
     * they have the service yet, so the alternative is the caller that does
     * fetching it a second time to satisfy a signature.
     */
    private static boolean keepsExactTime(@NonNull AlarmManager alarms) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms();
    }

    /**
     * Ends the chain, for when the last widget has gone. Nothing is left to
     * re-arm it, and an alarm outliving the widget it drew would be a process
     * started every minute to render nothing.
     */
    public static void cancel(@NonNull Context context) {
        alarms(context).cancel(pendingRedraw(context));
    }

    /**
     * Whether this broadcast is the tick asking for a redraw.
     *
     * <p>The receiver is sent several kinds of broadcast and this is the one it
     * cannot recognise from anything the framework names. Asking here rather
     * than comparing a string at the call site is what keeps the action private:
     * what the tick is made of stays with the thing that makes it.
     */
    public static boolean isTick(@NonNull Intent intent) {
        return ACTION_TICK.equals(intent.getAction());
    }

    /**
     * The next minute boundary, as a wall-clock instant.
     *
     * <p>Boundaries are found in UTC and read in local time, which is the same
     * boundary: every real timezone is a whole number of minutes from UTC, so a
     * minute begins at the same instant in all of them. Anchoring to the
     * boundary rather than a minute from now is what keeps the dial in step with
     * the clock the user reads it against — the hand moves as the minute turns,
     * not at whatever offset into the minute the widget was last added.
     */
    private static long topOfNextMinute() {
        return Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES)
                .toEpochMilli();
    }

    /**
     * The {@link PendingIntent} whose delivery is one redraw.
     *
     * <p>Addressed to the receiver by class. An implicit broadcast has not
     * reached a manifest receiver since API 26 except under an exemption this
     * action has no claim to; the tick arrived all the same only because an app
     * may still broadcast implicitly to itself, which is an exemption to rest a
     * clock on only if you have read that it exists. Naming the receiver is
     * also what lets the {@code <intent-filter>} go, since an explicit intent
     * needs no filter to be delivered.
     *
     * <p>The action is carried as well as the component, because
     * {@link #isTick} is how the receiver tells this delivery from the
     * {@code TIME_CHANGED} and widget broadcasts that arrive at the same class.
     *
     * <p>Immutable because a {@link PendingIntent} built with neither
     * {@code FLAG_IMMUTABLE} nor {@code FLAG_MUTABLE} is fatal when targeting
     * API 31 or above, and a process that dies arming its tick never gets as far
     * as drawing anything. Immutable rather than mutable because the intent says
     * only <em>redraw now</em>: there is nothing in it for a receiver to fill in.
     *
     * <p>{@code FLAG_UPDATE_CURRENT} refreshes an existing instance's extras,
     * which is a no-op while this intent carries none and the right flag if it
     * ever does. It is not what makes {@link #schedule} idempotent — identity
     * is; see there.
     */
    @NonNull
    private static PendingIntent pendingRedraw(@NonNull Context context) {
        final Intent tick = new Intent(context, TwentyFourHourClockWidgetResizable.class)
                .setAction(ACTION_TICK);

        return PendingIntent.getBroadcast(context, 0, tick,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @NonNull
    private static AlarmManager alarms(@NonNull Context context) {
        return context.getSystemService(AlarmManager.class);
    }

    private Tick() {
    }
}
