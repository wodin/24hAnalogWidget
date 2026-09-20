package info.staticfree.android.twentyfourhour;

/*
 * Copyright (C) 2011-2013 Steve Pomeroy <steve@staticfree.info>
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.dial.DialView;
import info.staticfree.android.twentyfourhour.observer.LastKnownObserver;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;
import info.staticfree.android.twentyfourhour.widget.Tick;
import info.staticfree.android.twentyfourhour.widget.WidgetPicture;

/**
 * The dial on a screen of its own, showing what the widget shows.
 *
 * <p>It reads the observer the same way the widget does, which makes it the
 * differential diagnostic for the two silent failures. Both reach a caller as
 * null: the phone has never had a fix, or the app-op refused the read. This
 * runs in the foreground, where a coarse grant alone is always allowed, so the
 * same call made from here answers what the widget's own answer cannot —
 * <em>widget hatched and this showing a sun</em> means the background op is
 * denied, <em>both hatched</em> means there is no fix to be had.
 *
 * <p>{@code LastKnownObserver} names the two in {@code logcat} as well, and
 * more exactly. This is the reading that needs no cable: it tells them apart on
 * the screen already in your hand, which is where somebody wondering why the
 * sun went missing is standing.
 *
 * <p>The third silent failure is answered here too, though not by that
 * comparison — nothing about a punctual dial and a late one looks different.
 * Measured on the A34, a dial that had fallen 3 to 69 seconds behind and
 * skipped a minute still read as a working clock, and finding out otherwise
 * cost a side-by-side install of upstream's build and two {@code dumpsys alarm}
 * sessions. So this one is answered in words rather than by looking.
 *
 * <p><strong>Why here and not in the widget.</strong> Every reading on this
 * screen needs a person to trigger it, and this activity is the only trigger of
 * that kind the app has; {@code Tick.keepsExactTime} has why the tick cannot
 * report on itself.
 */
public class WidgetDemo extends Activity {

    /**
     * Reads the observer here rather than leaving the overlay to fetch its own,
     * for the reason the widget reads in its receiver: the overlay is a painter,
     * and a system call inside a draw would run once per redraw — which is what
     * the 0.4.2 build did, and why it could not be tested.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final DialView dialView = findViewById(R.id.dial);

        final SunPositionOverlay sun = new SunPositionOverlay(this);
        sun.setObserver(LastKnownObserver.read(this));

        dialView.setDial(Dial.createStandard(this, sun));

        reportWhetherTheTickKeepsTime();
        reportWhatTheTapOpens();
    }

    /**
     * Says on screen which of its three shapes the widget's tap has here, and
     * names the component in {@code logcat}.
     *
     * <p>The tap can open a clock app by its front door, open the bare alarm
     * list because no clock app has one, or reach {@code UnansweredTap}
     * because nothing answers at all. From the homescreen the first two look
     * identical until tapped and the third says so only in the log, and the
     * picture that chooses is rebuilt once a minute, so it cannot be the one
     * to say — {@code CONTEXT.md} has why a tick may not speak. This is the
     * app's one person-triggered reading, so it says here, once per launch.
     *
     * <p>All three states are shown, for the reason the tick's line shows
     * both of its own. The log line names the package rather than the app's
     * label, because it is read beside a {@code Permission Denial} that names
     * the package too.
     */
    private void reportWhatTheTapOpens() {
        final TextView health = findViewById(R.id.tap_health);
        final Intent opens = WidgetPicture.clockApp(this);

        if (opens == null) {
            health.setText(getString(R.string.tap_opens_nothing, Diagnosis.TAG));
            Log.w(Diagnosis.TAG, "the tap opens nothing: no activity answers SHOW_ALARMS, "
                    + "or none is visible to this app");

            return;
        }

        if (opens.getComponent() == null) {
            health.setText(R.string.tap_opens_alarm_list_only);
            Log.w(Diagnosis.TAG, "the tap sends " + opens.getAction()
                    + " itself: no app that answers it has a launcher activity");

            return;
        }

        final String clockApp = opens.getComponent().getPackageName();

        health.setText(getString(R.string.tap_opens_clock_app, clockApp));
        Log.i(Diagnosis.TAG, "the tap opens " + opens.getComponent().flattenToShortString());
    }

    /**
     * Says on screen whether the minute hand is punctual, and in
     * {@code logcat} what to type if it is not.
     *
     * <p><strong>Both states are shown.</strong> A line that appears only when
     * something is wrong is indistinguishable from a line that has stopped
     * being drawn, and every defect this app has had was a silence that read as
     * working. So the healthy case gets words too, and their real job is to
     * prove the unhealthy one would have appeared.
     *
     * <p>The screen and the log carry different halves on purpose. The screen
     * is read on a phone with no cable attached and answers one question; the
     * command that fixes it has to be typed exactly and is worthless anywhere
     * but at a prompt, so it goes where a prompt already is.
     *
     * <p><strong>The command is a line of its own</strong>, after the line that
     * explains it. Appending it to the explanation would leave a reader
     * selecting a fragment out of a paragraph, which is the difference between
     * a command that is pasteable and one that merely appears in the log.
     * {@code LastKnownObserver} appends, and is the worse of the two for it.
     *
     * <p>Built with {@link #getPackageName} rather than written out, because it
     * is not the same on both builds: the debug one installs under a suffixed
     * applicationId so that it can sit beside upstream's, and a command naming
     * the wrong one fails with a message about an unknown package rather than
     * doing nothing visible.
     */
    private void reportWhetherTheTickKeepsTime() {
        final TextView health = findViewById(R.id.tick_health);

        if (Tick.keepsExactTime(this)) {
            health.setText(R.string.tick_keeps_exact_time);

            return;
        }

        health.setText(getString(R.string.tick_cannot_keep_exact_time, Diagnosis.TAG));

        Log.w(Diagnosis.TAG, "the tick cannot keep exact time: SCHEDULE_EXACT_ALARM is declared "
                + "but not granted, which is the default at API 33 and above. Measured on the "
                + "A34, the dial then ran 3 to 69 seconds behind the phone's own clock and "
                + "skipped a minute outright. The next line grants it.");
        Log.w(Diagnosis.TAG,
                "adb shell cmd appops set " + getPackageName() + " SCHEDULE_EXACT_ALARM allow");
    }
}
