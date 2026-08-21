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
import android.os.Bundle;

import info.staticfree.android.twentyfourhour.dial.Dial;
import info.staticfree.android.twentyfourhour.dial.DialView;
import info.staticfree.android.twentyfourhour.observer.LastKnownObserver;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;

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
    }
}
