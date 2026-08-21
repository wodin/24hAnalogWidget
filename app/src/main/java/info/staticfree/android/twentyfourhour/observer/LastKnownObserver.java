package info.staticfree.android.twentyfourhour.observer;

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

import android.app.AppOpsManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Where the phone last saw itself, if it has seen itself at all.
 *
 * <p>The name is the invariant: the last <em>known</em> observer is the only
 * one this can produce. {@link LocationManager#getLastKnownLocation} hands over
 * a cache and cannot power a radio, so "this app never actively asks the phone
 * where it is" is readable at every call site without opening this file — which
 * is the whole reason the class is not called {@code ObserverReader}. The
 * provider is {@link LocationManager#PASSIVE_PROVIDER} alone, and it is passive
 * in the same sense: it mirrors whichever provider last produced a fix for
 * somebody else, so it returns the freshest of them without asking any of them
 * for anything.
 *
 * <p>Nothing here asks how old the fix is, because {@link Observer} cannot
 * carry an age and that is deliberate. A phone flown to another hemisphere
 * overnight may briefly draw yesterday's sun; on a dial with no second hand
 * that is a cost worth not building a staleness policy for.
 *
 * <p><strong>Three failures, and two of them are silent.</strong> A read made
 * with no permission at all throws, and the throw names what is missing. A read
 * whose app-op is refused returns null saying nothing; so does a phone that has
 * never had a fix. The refused op is the trap — {@code checkSelfPermission}
 * answers {@code GRANTED} at the very moment the platform refuses, because a
 * foreground-only grant is enforced by the app-op and not by the permission,
 * and <em>nothing whatsoever</em> reaches {@code logcat}. Measured on both
 * emulators; written up in {@code docs/plans/candidate-d-location-notes.md}. So
 * all three reach the caller as null, who has one thing to draw either way, and
 * are told apart exactly once — in a line for whoever is asking why the sun
 * went missing.
 */
public final class LastKnownObserver {

    private static final String TAG = LastKnownObserver.class.getSimpleName();

    /**
     * The last fix the phone happens to have, or {@code null} — including when
     * the answer is really "you were not allowed to ask".
     *
     * <p>A refused read is caught rather than propagated. This is read from a
     * widget's {@code onUpdate}, where a throw would take down the redraw and
     * with it the tick that arms the next one, so the app would stop for good
     * over a permission it is designed to work without. Having no observer is
     * an ordinary state of this app, and the Placeholder is what it looks like.
     *
     * <p>The catch is no wider than the throw: an absent permission is measured
     * to be the one thing {@link LocationManager#getLastKnownLocation} raises
     * here, and it raises it before it has even validated the provider name.
     * That is also why the loud failure is diagnosed on the spot rather than
     * through {@link #whySilent} — the throw already carries the answer, and
     * asking again could only disagree with it.
     */
    @Nullable
    public static Observer read(@NonNull Context context) {
        Location fix;

        try {
            fix = context.getSystemService(LocationManager.class)
                    .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        } catch (SecurityException refused) {
            Log.w(TAG, "no observer: the read was refused outright, so "
                    + "ACCESS_COARSE_LOCATION is not granted — it is the only "
                    + "location permission this app declares that can carry a read. "
                    + "adb shell pm grant " + context.getPackageName()
                    + " android.permission.ACCESS_COARSE_LOCATION");

            return null;
        }

        if (fix == null) {
            Log.w(TAG, "no observer: " + whySilent(context));

            return null;
        }

        return new Observer(fix.getLatitude(), fix.getLongitude());
    }

    /**
     * Which of the two silent failures it was, in words, for a human reading
     * {@code logcat}.
     *
     * <p>Not a type and not an enum: nothing branches on this and nothing
     * outside this file can see it. It exists because the install ritual behind
     * the first of these answers is forgettable and forgetting it leaves no
     * trace at all, so the day the sun disappears the phone should be able to
     * say why in one line. The day a <em>caller</em> needs to act on the
     * difference is the day this earns a type.
     */
    @NonNull
    private static String whySilent(@NonNull Context context) {
        if (refusedTheOp(context)) {
            return "the COARSE_LOCATION app-op is refused, which from a widget is what a "
                    + "foreground-only grant looks like. adb shell pm grant "
                    + context.getPackageName() + " android.permission.ACCESS_BACKGROUND_LOCATION";
        }

        return "the phone has no passive fix cached, and this app will not ask for one";
    }

    /**
     * The app-op behind the permission, which is the only thing that tells a
     * foreground-only grant apart from a full one at the moment of a read.
     *
     * <p>{@code checkOpNoThrow} and not {@code unsafeCheckOpNoThrow}: Q
     * deprecated the former in favour of the latter and API 36 has deprecated
     * it straight back again, so at this {@code compileSdk} the plain name is
     * the undeprecated one. They are the same call, and the "unsafe" the rename
     * warned about is a race this cannot lose — the mode is read a line after
     * the read it explains, and nothing does anything with it but print it.
     *
     * <p>No API guard, because there is nothing to guard: the method is API 19,
     * and below Q — where a granted permission is granted everywhere — it
     * simply answers {@code MODE_ALLOWED} and this falls through to the other
     * case, which is the true one there.
     *
     * <p>Not one of the {@code Raw} variants, and that is the whole trick. A
     * foreground-only grant is stored as {@code MODE_FOREGROUND}, which is what
     * the raw calls return and which says nothing about whether this read was
     * allowed. The plain call resolves it against the process's importance at
     * the moment of asking, so the same app on the same phone with the same
     * grant reads {@code MODE_ALLOWED} from an activity and
     * {@code MODE_IGNORED} from a widget tick. That is the measurement, both
     * halves of it, and it is the only signal the platform gives.
     */
    private static boolean refusedTheOp(@NonNull Context context) {
        return context.getSystemService(AppOpsManager.class)
                .checkOpNoThrow(AppOpsManager.OPSTR_COARSE_LOCATION, Process.myUid(),
                        context.getPackageName())
                != AppOpsManager.MODE_ALLOWED;
    }

    private LastKnownObserver() {
    }
}
