package info.staticfree.android.twentyfourhour.widget;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.content.Context;

import androidx.annotation.NonNull;

import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowPendingIntent;

import java.util.List;

/**
 * Reading the alarm table back, for tests on either side of the seam: what
 * {@code Tick} puts there and what the receiver causes to be put there are the
 * same table, asked about the same way.
 *
 * <p>Shared rather than copied because the awkward part is not the reading but
 * the {@code @SuppressWarnings} and the reason for it, and a reason explained in
 * two places is a reason that will be true in one of them.
 */
public final class Alarms {

    @NonNull
    public static List<ShadowAlarmManager.ScheduledAlarm> scheduled(@NonNull Context context) {
        return shadowOf(context.getSystemService(AlarmManager.class)).getScheduledAlarms();
    }

    /**
     * The one alarm there should be. Fails rather than returning null when there
     * is a second, because two alarms and one alarm are the two outcomes the
     * scheduling rule exists to tell apart.
     */
    @NonNull
    public static ShadowAlarmManager.ScheduledAlarm only(@NonNull Context context) {
        List<ShadowAlarmManager.ScheduledAlarm> alarms = scheduled(context);
        assertEquals("expected exactly one scheduled alarm", 1, alarms.size());
        return alarms.get(0);
    }

    /**
     * The {@code PendingIntent} an alarm would send. Robolectric deprecated the
     * field without putting an accessor in its place, so the one use of it lives
     * here rather than at every call site.
     */
    @NonNull
    @SuppressWarnings("deprecation")
    public static ShadowPendingIntent operationOf(
            @NonNull ShadowAlarmManager.ScheduledAlarm alarm) {
        return shadowOf(alarm.operation);
    }

    private Alarms() {
    }
}
