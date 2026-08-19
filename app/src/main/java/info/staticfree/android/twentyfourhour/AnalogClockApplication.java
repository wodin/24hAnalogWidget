package info.staticfree.android.twentyfourhour;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * Candidate G deletes this class outright: every supported device is past
 * Honeycomb, so the branch below is constant and the whole toggle is dead.
 * Its lint findings are suppressed rather than fixed, so that fixing them
 * cannot be mistaken for work that keeps the class alive.
 */
@SuppressWarnings("deprecation") // AsyncTask - candidate G deletes this class
public class AnalogClockApplication extends Application {

    private static final String TAG = AnalogClockApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        new DisableWidgetTask().execute();
    }

    /**
     * Disables the fixed-sized widgets for Android 3.0+, otherwise disables resizable one.
     */
    @SuppressLint("ObsoleteSdkInt") // candidate G deletes this class
    private void disableWidgetSizes() {

        final PackageManager pm = getPackageManager();

        // for versions 3.0+ we use the resizable widget instead of having multiple pre-defined
        // sizes.
        final boolean supportsResize = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

        // resizable widget

        pm.setComponentEnabledSetting(new ComponentName(this,
                        TwentyFourHourClockWidgetResizable.class),
                supportsResize ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // fixed size widgets

        pm.setComponentEnabledSetting(new ComponentName(this, TwentyFourHourClockWidget.class),
                supportsResize ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(this, TwentyFourHourClockWidget3x.class),
                supportsResize ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP);

        Log.d(TAG, "Resizable widgets are: " + (supportsResize ? "enabled" : "disabled"));
    }

    @SuppressLint("StaticFieldLeak") // candidate G deletes this class
    private class DisableWidgetTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            disableWidgetSizes();
            return true;
        }
    }
}
