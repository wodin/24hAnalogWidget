package info.staticfree.android.twentyfourhour;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * Dead weight kept alive only until the architecture review's candidate G runs.
 * Every supported device is past Honeycomb, so {@code supportsResize} below is
 * constant, the else-branches are unreachable, and the toggle only ever
 * disables the two fixed-size widgets. The lint and deprecation findings in
 * here are suppressed at their narrowest scope rather than fixed, so that
 * modernising them cannot be mistaken for work that keeps the class alive.
 */
public class AnalogClockApplication extends Application {

    private static final String TAG = AnalogClockApplication.class.getSimpleName();

    @SuppressWarnings("deprecation") // AsyncTask.execute - see class javadoc
    @Override
    public void onCreate() {
        super.onCreate();

        new DisableWidgetTask().execute();
    }

    /**
     * Disables the fixed-sized widgets for Android 3.0+, otherwise disables resizable one.
     */
    @SuppressLint("ObsoleteSdkInt") // constant at minSdk 26 - see class javadoc
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

    @SuppressLint("StaticFieldLeak")
    @SuppressWarnings("deprecation") // AsyncTask - see class javadoc
    private class DisableWidgetTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            disableWidgetSizes();
            return true;
        }
    }
}
