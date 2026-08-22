package info.staticfree.android.twentyfourhour;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.widget.TextView;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;

import info.staticfree.android.twentyfourhour.widget.Tick;

/**
 * What the demo says about the clock it is showing.
 *
 * <p>The dial itself is not read here — {@code DialRenderingTest} and the
 * overlay suites do that, off any activity at all. What is here is the one
 * thing the demo knows that the widget cannot report: whether the tick is
 * keeping exact time, asked in the foreground where a person can see the
 * answer.
 *
 * <p>Both states are asserted because both are shown; {@code WidgetDemo} has
 * why "nothing on screen" is not allowed to be one of the answers.
 */
@RunWith(RobolectricTestRunner.class)
public class WidgetDemoTest {

    /**
     * Robolectric narrates its own startup before any test body runs, so the
     * log starts non-empty. Clearing draws the line at the moment the app takes
     * over.
     */
    @Before
    public void startFromASilentLog() {
        ShadowLog.clear();
    }

    /**
     * The grant is there, so the dial turns as the minute turns and the demo
     * says so. Pinned to the resource rather than to a sentence: which of the
     * two strings was chosen is the branch under test, and the wording is free
     * to change without this test's leave.
     */
    @Test
    public void theDemoSaysSoWhenTheTickKeepsExactTime() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true);

        WidgetDemo demo = demo();

        assertEquals("a punctual tick should be reported as one",
                demo.getString(R.string.tick_keeps_exact_time), healthLine(demo));
    }

    /**
     * And the other way, which is the state every install starts in on API 33
     * and above. The tag is formatted into the line rather than written out
     * beside it, so a reader who has just been told to look in {@code logcat}
     * is told in the same breath what to filter for — and an unsubstituted
     * {@code %1$s} fails here rather than reaching the screen.
     */
    @Test
    public void theDemoSaysSoWhenTheTickCannotKeepExactTime() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false);

        WidgetDemo demo = demo();

        assertEquals("a tick that cannot keep time should say so, and name the tag to filter on",
                demo.getString(R.string.tick_cannot_keep_exact_time, Diagnosis.TAG),
                healthLine(demo));
    }

    /**
     * The screen says what is wrong; the log says what to type.
     *
     * <p>Asserted as a <strong>whole logged line</strong> rather than as a
     * substring of one, which is the difference the claim rests on: a command
     * appended to a paragraph of explanation is in the log but is not
     * pasteable, and a substring match cannot tell the two apart. Membership of
     * the list of messages is the check that can.
     *
     * <p>And with the running app's own package name, which is not a constant —
     * the debug build installs under a suffixed one so that it can sit beside
     * upstream's, and a command naming the wrong one fails at the prompt.
     */
    @Test
    public void theDemoLogsThePasteableCommandThatWouldFixIt() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false);

        WidgetDemo demo = demo();

        assertTrue("the remediation command should be a logged line of its own, ready to paste",
                loggedMessages().contains("adb shell cmd appops set " + demo.getPackageName()
                        + " SCHEDULE_EXACT_ALARM allow"));
    }

    /**
     * And says nothing when there is nothing to fix. The demo's trigger is rare
     * — a person opening the app — so the line needs no throttling, but a
     * remediation for a fault that is not present is noise at any rate at all.
     */
    @Test
    public void theDemoLogsNoRemedyWhenThereIsNothingToRemedy() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true);

        demo();

        assertFalse("a punctual tick should leave no remediation in the log",
                mentionedInTheLog("appops"));
    }

    /**
     * The demo, created and shown. {@link Tick} is untouched by this: the demo
     * asks what the schedule would do rather than arming anything, so opening
     * the app does not disturb a widget's tick.
     */
    @NonNull
    private static WidgetDemo demo() {
        return Robolectric.buildActivity(WidgetDemo.class).setup().get();
    }

    @NonNull
    private static String healthLine(@NonNull WidgetDemo demo) {
        return ((TextView) demo.findViewById(R.id.tick_health)).getText().toString();
    }

    /** Every message written to the log so far, one entry per line written. */
    @NonNull
    private static List<String> loggedMessages() {
        List<String> messages = new ArrayList<>();

        for (ShadowLog.LogItem line : ShadowLog.getLogs()) {
            messages.add(line.msg);
        }

        return messages;
    }

    /**
     * Whether any line so far contains the word. Deliberately looser than
     * {@link #loggedMessages} membership, because it is only ever asked in the
     * negative: "no line mentions this" is a stronger claim the less exactly it
     * is worded.
     */
    private static boolean mentionedInTheLog(@NonNull String word) {
        return loggedMessages().stream().anyMatch(message -> message.contains(word));
    }
}
