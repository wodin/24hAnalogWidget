package info.staticfree.android.twentyfourhour;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.List;

import info.staticfree.android.twentyfourhour.observer.LastKnownObserver;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;
import info.staticfree.android.twentyfourhour.sun.London;

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
 * <p><strong>What it does not reach.</strong> There is a third diagnosis, on
 * the refused read in {@code LastKnownObserver}, and no case here drives it: the
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
     */
    @Test
    public void theMissingSunPathIsDiagnosedUnderTheOneTag() {
        SunPositionOverlay overlay = new SunPositionOverlay(context());
        overlay.setObserver(London.observer());

        overlay.draw(new Canvas(blankBitmap()), SIDE_PX, London.SOMETIME_THAT_DAY, London.ZONE);

        assertEquals("a place with no sun path should say so under the published tag",
                singletonList(Diagnosis.TAG), tagsWritten());
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

    @NonNull
    private static Bitmap blankBitmap() {
        return Bitmap.createBitmap(SIDE_PX, SIDE_PX, Bitmap.Config.ARGB_8888);
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
