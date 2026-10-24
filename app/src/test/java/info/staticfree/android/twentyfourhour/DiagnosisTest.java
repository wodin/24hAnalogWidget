package info.staticfree.android.twentyfourhour;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowLog;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import info.staticfree.android.twentyfourhour.observer.LastKnownObserver;
import info.staticfree.android.twentyfourhour.observer.Observer;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;
import info.staticfree.android.twentyfourhour.sun.Tromso;
import info.staticfree.android.twentyfourhour.widget.UnansweredTap;

/**
 * That every diagnosis this app writes arrives where the README says to look.
 *
 * <p>The one test in this suite that reads the log, and the only one that
 * should: everywhere else the log is a side effect nobody asserts on, because
 * nothing branches on it and pinning a sentence would test the sentence. The
 * tag is the exception, because it is not a sentence — it is the argument to
 * the {@code adb logcat -s} the README publishes, so a line under any other
 * name is a line nobody will be filtering for on the day it matters.
 *
 * <p><strong>Which is why each case asserts the whole set of tags written and
 * not merely that its own is among them.</strong> A site that keeps a private
 * tag of its own passes an existence check and fails this one. That is the
 * regression worth catching, since the failure it causes is silence, and
 * silence is what this suite exists to make impossible.
 *
 * <p>One class rather than an assertion added to each site's own test: the
 * claim is that the sites agree, and a claim about several files does not live
 * in any one of them.
 *
 * <p><strong>What it does not reach.</strong> One diagnosis has no case here —
 * the refused read in {@code LastKnownObserver} — and it is left out because the
 * shadow that raises a {@link SecurityException} is
 * {@code LastKnownObserverTest}'s and {@code Fixes} says in as many words that
 * it stays there, so reaching it from here would mean a second copy of what the
 * platform does to an unpermitted read. The cost is named rather than hidden —
 * that one line's tag could be changed with the suite staying green.
 */
@RunWith(RobolectricTestRunner.class)
public class DiagnosisTest {

    /** Any side at all: nothing here reads a pixel, only the log. */
    private static final int SIDE_PX = 100;

    /**
     * The one tag written here that is not this app's.
     *
     * <p>Robolectric narrates lifecycle transitions under it, from inside
     * {@code create()} — so unlike the startup narration, which {@link #startFromASilentLog}
     * gets ahead of by clearing, this one arrives interleaved with the lines
     * under test and cannot be cleared in advance.
     *
     * <p>Dropped by name rather than by relaxing the assertion to "contains".
     * The strictness is the whole value of this class, and "every tag but this
     * one, which the harness wrote" keeps it: the exclusion is a single literal
     * a reader can check, and it says what it is excluding and why. What it
     * costs is that a site of ours which adopted this exact tag would pass —
     * named here rather than left to be discovered.
     *
     * <p>Dropped at one call site and not in {@link #tagsWritten}, so that the
     * cases which build no activity keep the unconditional reading they were
     * written with. An exclusion in the shared helper would quietly pay for
     * this case out of the other four.
     */
    private static final String ROBOLECTRIC_NARRATION = "LifecycleMonitor";

    /**
     * Robolectric brings up an Android it has to narrate — {@code OverlayConfig}
     * and {@code PackageParser} both write before any test body runs — so the
     * log starts non-empty and the set below would never be this app's alone.
     * Clearing here draws the line at the moment the app takes over: everything
     * afterwards was written by the code under test.
     */
    @Before
    public void startFromASilentLog() {
        ShadowLog.clear();
    }

    /**
     * The literal, pinned.
     *
     * <p>Asserting a constant against its own value is normally a tautology
     * worth deleting, and this one earns its place for a reason outside the
     * compiler: the same string is written down again in the README, as the
     * filter a person is told to type, and nothing links the two. So renaming
     * the constant is a silent break of a published command, and the compiler
     * cannot say so. This can, and the failure message is the reminder of where
     * the other copy lives.
     */
    @Test
    public void theTagIsTheOneTheReadmePublishes() {
        assertEquals("the README publishes this literal; change both or neither", "24hWidget",
                Diagnosis.TAG);
    }

    /**
     * A phone with no cached fix has no observer, and does not pass that off in
     * silence. Which of the reasons it was is the line's own business — the
     * wording is deliberately not asserted anywhere — so what is pinned here is
     * only that a line was written, and written where it will be read.
     */
    @Test
    public void theMissingObserverIsDiagnosedUnderTheOneTag() {
        LastKnownObserver.read(context());

        assertEquals("a phone with no fix should say so under the published tag",
                singletonList(Diagnosis.TAG), tagsWritten());
    }

    /**
     * An observer whose place has no sun path that day gets the Placeholder, and
     * says so. The drawing cannot make that distinction — an unknown observer
     * and a known one with no path hatch the same nominal night — so the log is
     * the only place the two come apart.
     *
     * <p>Only the poles in their dark half have no path now that a missing
     * horizon is a band of its own shape rather than a missing path; London,
     * which used to stand here, has one.
     */
    @Test
    public void theMissingSunPathIsDiagnosedUnderTheOneTag() {
        SunPositionOverlay overlay = new SunPositionOverlay(context());
        overlay.setObserver(new Observer(90, 0));

        overlay.draw(new Canvas(blankBitmap()), SIDE_PX, Tromso.SOMETIME_MIDWINTER,
                ZoneId.of("UTC"));

        assertEquals("a place with no sun path should say so under the published tag",
                singletonList(Diagnosis.TAG), tagsWritten());
    }

    /**
     * A tap that found nothing to open says so under the same tag, which is the
     * whole reason the receiver it reaches exists. This is the newest of the
     * sites and so the likeliest to have been written with a tag of its own.
     */
    @Test
    public void theUnansweredTapIsDiagnosedUnderTheOneTag() {
        new UnansweredTap().onReceive(context(), new Intent());

        assertEquals("a tap with nothing to open should say so under the published tag",
                singletonList(Diagnosis.TAG), tagsWritten());
    }

    /**
     * A minute hand that cannot be punctual says so under the same tag, from
     * the demo rather than from the tick — {@code Tick.keepsExactTime} has why.
     *
     * <p>Three lines land here rather than one: the demo reads the observer as
     * well, and a Robolectric phone has no fix cached, and the tick's own
     * report is a sentence followed by the command. That is what the set
     * assertion is for — all three are this app's, so all three must agree, and
     * a second tag turning up fails whichever of them brought it.
     */
    @Test
    public void theInexactTickIsDiagnosedUnderTheOneTag() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false);

        Robolectric.buildActivity(WidgetDemo.class).setup();

        assertEquals("a tick that cannot keep exact time should say so under the published tag",
                singletonList(Diagnosis.TAG), tagsWrittenIgnoring(ROBOLECTRIC_NARRATION));
    }

    /**
     * Every tag written so far, in the order first seen and without repeats.
     * The set and not the count, so a failure names the tag that turned up
     * rather than only that one did.
     */
    @NonNull
    private static List<String> tagsWritten() {
        return ShadowLog.getLogs().stream()
                .map(line -> line.tag)
                .distinct()
                .collect(toList());
    }

    /**
     * The same, less one tag the harness wrote rather than the app. Takes the
     * tag to drop as an argument so that a reader of the case can see which
     * leniency it is buying, instead of inheriting an exclusion from a helper.
     */
    @NonNull
    private static List<String> tagsWrittenIgnoring(@NonNull String harnessTag) {
        List<String> tags = new ArrayList<>(tagsWritten());
        tags.remove(harnessTag);

        return tags;
    }

    @NonNull
    private static Bitmap blankBitmap() {
        return Bitmap.createBitmap(SIDE_PX, SIDE_PX, Bitmap.Config.ARGB_8888);
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
