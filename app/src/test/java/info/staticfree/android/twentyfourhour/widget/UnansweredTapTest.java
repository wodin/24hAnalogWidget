package info.staticfree.android.twentyfourhour.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.Arrays;
import java.util.List;

/**
 * The receiver a tap reaches when there was nothing to open, and the two things
 * it owes: that it says something, and that it is reachable at all.
 *
 * <p>That the picture addresses this receiver is {@code WidgetPictureTest}'s
 * claim, and that the line arrives under the published tag is
 * {@code DiagnosisTest}'s. What is left here is the receiver's own end of the
 * arrangement.
 */
@RunWith(RobolectricTestRunner.class)
public class UnansweredTapTest {

    /**
     * Robolectric narrates its own startup, so the log is not empty when a test
     * body begins. Clearing draws the line at the moment the code under test
     * takes over — the same reason and the same {@code @Before} as
     * {@code DiagnosisTest}.
     */
    @Before
    public void startFromASilentLog() {
        ShadowLog.clear();
    }

    /**
     * One line, and one that can be acted on.
     *
     * <p>The wording is not asserted and should not be — everywhere else in
     * this suite a diagnosis is a sentence nobody pins. The exception is the
     * one token that makes the line worth writing: {@code <queries>} is the
     * fault that has actually happened here, and a line that reported the
     * symptom without naming the likeliest cause would be a second silence in a
     * louder voice. So what is pinned is not the sentence but the presence of
     * the pointer inside it.
     *
     * <p>Exactly one line, because the value of a rare diagnosis is that it is
     * rare. Two would be the beginning of noise a reader learns to scroll past.
     */
    @Test
    public void theTapThatOpenedNothingNamesWhatToCheck() {
        new UnansweredTap().onReceive(context(), new Intent());

        List<ShadowLog.LogItem> written = ShadowLog.getLogs();

        assertEquals("a tap with nothing to open should write one line and no more", 1,
                written.size());
        assertTrue("and should name <queries>, which is how the tap went missing before: "
                        + written.get(0).msg,
                written.get(0).msg.contains("<queries>"));
    }

    /**
     * Declared in the manifest, and closed to everyone else.
     *
     * <p>Asserted against the manifest because nothing in this suite can
     * otherwise see it: Robolectric records a broadcast whether or not anything
     * is registered to receive it, so an undeclared receiver would leave every
     * other test here green while the tap on the phone reached nobody. That is
     * the same shape as the {@code <queries>} fault this receiver exists to
     * report, which makes leaving it unasserted the one thing this slice cannot
     * do.
     *
     * <p>{@code exported="false"} is asserted beside it rather than taken on
     * trust, because it is the half a future edit is likeliest to "fix": a
     * receiver whose sender lives in the launcher's process does look like one
     * that has to be exported. Why it does not have to be is written where the
     * attribute is, in the manifest, and is deliberately not repeated here.
     */
    @Test
    public void theReceiverIsDeclaredAndOpenToNobodyElse() throws NameNotFoundException {
        ActivityInfo declared = Arrays.stream(receiversInTheManifest())
                .filter(receiver -> UnansweredTap.class.getName().equals(receiver.name))
                .findFirst()
                .orElse(null);

        assertNotNull("the manifest should declare the receiver a tap is addressed to", declared);
        assertFalse("nothing outside this app should be able to send to it", declared.exported);
    }

    @NonNull
    private static ActivityInfo[] receiversInTheManifest() throws NameNotFoundException {
        PackageInfo manifest = context().getPackageManager()
                .getPackageInfo(context().getPackageName(), PackageManager.GET_RECEIVERS);

        return manifest.receivers;
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
