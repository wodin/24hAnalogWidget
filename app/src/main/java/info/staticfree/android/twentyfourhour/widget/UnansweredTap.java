package info.staticfree.android.twentyfourhour.widget;

/*
 * Copyright (C) 2026 Michael Wood
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.util.Log;

import androidx.annotation.NonNull;

import info.staticfree.android.twentyfourhour.Diagnosis;

/**
 * Where the widget's tap goes when the picture found nothing to open, and the
 * one line of {@code logcat} that says so.
 *
 * <p><strong>Why a tap that only complains, rather than no tap at all.</strong>
 * This app has lost its tap twice on the phone it is built for — once to the
 * missing {@code <queries>} element, once to the missing {@code SET_ALARM}
 * permission — and both times the widget was indistinguishable from one that
 * had never had a tap: a picture that ignores a finger. Attaching nothing is
 * therefore not the honest option it looks like, because "nothing happened" is
 * exactly what a broken tap already says.
 *
 * <p><strong>Only the first of those two faults reaches here</strong>, and the
 * two are worth keeping apart. A missing {@code <queries>} leaves the alarm
 * list unresolvable, so {@link WidgetPicture} hangs this receiver on the
 * picture and a tap arrives to be complained about. A missing
 * {@code SET_ALARM} does not: the intent resolves, the direct tap is attached,
 * and the activity manager turns it away afterwards without any code of ours
 * running. Nothing here can see that one, and nothing at runtime can — which
 * is why it has a detector of a different kind, the manifest assertion in
 * {@code WidgetPictureTest}.
 *
 * <p><strong>Why the working tap does not come through here.</strong> The
 * obvious shape is one receiver for every tap, deciding on arrival whether to
 * open the clock app or to complain. It was rejected: this receiver would then
 * have to start an activity from the background, which API 29 restricted and
 * API 34 restricted again, so a tap that works today would rest on a rule that
 * keeps being narrowed. {@link WidgetPicture} attaches the direct intent when
 * the alarm list resolves and this receiver only when it does not, so a working
 * phone keeps the tap it always had — no hop, no latency, and nothing of ours
 * standing between the finger and the clock app.
 *
 * <p><strong>Nothing appears on screen.</strong> A {@code Toast} would serve
 * the person holding the phone better and is not this: the fault is a build
 * defect rather than something a user can act on, and the reader who can act on
 * it is someone with {@code logcat} in front of them. Adding
 * user-facing behaviour is its own decision, not a side effect of fixing a
 * silence.
 *
 * <p>Nothing throttles the line, and nothing needs to. The trigger is a person
 * putting a finger on the widget, which is rare by construction — unlike the
 * redraw path, where {@code onUpdate} runs every minute.
 */
public final class UnansweredTap extends BroadcastReceiver {

    /**
     * The {@link PendingIntent} whose delivery is one complaint, for
     * {@link WidgetPicture} to hang on a picture that has nothing to open.
     *
     * <p>Built by the class it addresses, so that the two halves which have to
     * agree — what is sent, and what answers it — cannot drift apart. That is
     * the mirror image of {@code Tick}, which builds a redraw addressed to a
     * receiver that is not {@code Tick} at all. The rule both follow is the
     * same one: the intent lives with whichever end knows why it exists, and
     * here that is this end, whose whole reason for being is this one message.
     *
     * <p>Addressed by class, carrying no action and no extras. There is nothing
     * to say beyond the fact of the tap — the receiver knows why it exists —
     * and an explicit intent needs no {@code <intent-filter>} to be delivered,
     * which is what lets the manifest entry stay closed.
     *
     * <p>Immutable, because a {@link PendingIntent} built with neither
     * {@code FLAG_IMMUTABLE} nor {@code FLAG_MUTABLE} is fatal when targeting
     * API 31 or above, and because there is nothing in an empty intent for a
     * sender to usefully fill in.
     */
    @NonNull
    static PendingIntent pendingComplaint(@NonNull Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(context, UnansweredTap.class),
                PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Says what the tap could not do, and names the first thing to check.
     *
     * <p>The {@code <queries>} element is named first because it is the one
     * that has actually failed here, and because its failure is invisible from
     * inside the app: the package manager answers "nobody handles that" in the
     * same words whether the phone has no clock app or this app is merely not
     * allowed to see the one it has. The second is overwhelmingly the likelier
     * of the two on any real phone.
     *
     * <p>Neither the context nor the intent is read. The receiver's identity is
     * the whole message, which is why the intent carries nothing.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(Diagnosis.TAG, "the tap had nothing to open: when this picture was drawn, no "
                + "activity answered " + AlarmClock.ACTION_SHOW_ALARMS + ". Check the <queries> "
                + "element in the manifest first — without it this app cannot see the clock app "
                + "whatever is installed, and that is how the tap went missing before. Failing "
                + "that, this phone genuinely has no clock app.");
    }
}
