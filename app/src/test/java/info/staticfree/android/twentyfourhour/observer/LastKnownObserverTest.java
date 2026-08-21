package info.staticfree.android.twentyfourhour.observer;

import static info.staticfree.android.twentyfourhour.observer.Fixes.cacheAPassiveFixAt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLocationManager;

import info.staticfree.android.twentyfourhour.sun.BuenosAires;

/**
 * The one Android-flavoured read this feature makes, and the three answers it
 * can get.
 *
 * <p>Two of the three are the same answer — {@code null} — arrived at for
 * different reasons, and the platform will not tell them apart: a refused read
 * returns null with nothing in the log, and so does a phone that has simply
 * never had a fix. That is measured, not assumed, and the measurement is in
 * {@code docs/plans/candidate-d-location-notes.md}. What is asserted here is
 * only that both reach the caller as null rather than as an exception or as a
 * wrong place; the log line that distinguishes them for a human is deliberately
 * not asserted, because nothing branches on it and pinning its wording would
 * test the sentence rather than the behaviour.
 *
 * <p>The exception case needs a shadow of its own. Robolectric's
 * {@link ShadowLocationManager} does not model permissions at all — it will
 * hand a cached fix to a caller that holds no grant — so the one behaviour this
 * class exists to absorb is the one behaviour the stock shadow cannot
 * produce.
 *
 * <p>The refused app-op has no case here at all, and could not have one. It is
 * a function of the process's importance, and a JVM has no importance:
 * Robolectric's {@code ShadowAppOpsManager} resolves a foreground-only grant to
 * {@code MODE_ALLOWED} unconditionally, which on a device is the answer for a
 * visible activity and the opposite of the answer for a widget tick. So the
 * failure that motivated the diagnosis is exactly the failure only a phone can
 * show.
 */
@RunWith(RobolectricTestRunner.class)
public class LastKnownObserverTest {

    /**
     * A cached fix comes back as the place it names.
     *
     * <p>The latitude and longitude are {@link BuenosAires}' rather than a
     * fresh pair, because a place written down twice is a place that can
     * disagree with itself, and because that fixture already carries the reason
     * they are a public city centre and never a real device's own fix: this
     * repo is public.
     *
     * <p>Both numbers are negative and they differ in magnitude, so a latitude
     * arriving in longitude's place fails here rather than passing by symmetry.
     */
    @Test
    public void aCachedPassiveFixIsWhereTheObserverStands() {
        Observer expected = BuenosAires.observer();
        cacheAPassiveFixAt(context(), expected);

        Observer observed = LastKnownObserver.read(context());

        assertNotNull("a cached passive fix should yield an observer", observed);
        assertEquals("the observer should stand at the fix's latitude", expected.latitude(),
                observed.latitude(), 0);
        assertEquals("the observer should stand at the fix's longitude", expected.longitude(),
                observed.longitude(), 0);
    }

    /**
     * No cached fix is nobody, and that is the ordinary case rather than an
     * error: the passive provider only ever mirrors a fix some other app has
     * already caused, so a phone that nothing has asked to place itself has
     * none to hand over. The dial draws its Placeholder and says so.
     */
    @Test
    public void noCachedFixIsNoObserver() {
        assertNull("a phone with no cached fix should yield no observer",
                LastKnownObserver.read(context()));
    }

    /**
     * A read with no permission is nobody too, rather than a crash.
     *
     * <p>{@link SecurityException} is what an ungranted read throws — measured
     * on both emulators, and thrown before the provider name is even validated,
     * so it is the permission check talking rather than the provider. It would
     * surface inside a widget's {@code onUpdate}, where an uncaught throw takes
     * the whole redraw down and with it the tick that would have armed the next
     * one. So the loudest of the three failures has to become the quietest.
     *
     * <p>A fix is cached first, so that returning null cannot be the stock
     * shadow's empty cache answering by accident.
     */
    @Test
    @Config(shadows = ShadowRefusedLocationManager.class)
    public void aRefusedReadIsNoObserver() {
        cacheAPassiveFixAt(context(), BuenosAires.observer());

        assertNull("a refused read should yield no observer rather than throw",
                LastKnownObserver.read(context()));
    }

    /**
     * What {@link LocationManager#getLastKnownLocation} does when the caller
     * holds no location permission. Robolectric's own shadow has no notion of
     * permissions, so this is the platform's behaviour written down rather than
     * simulated: the message is the API 31 emulator's, verbatim.
     */
    @Implements(LocationManager.class)
    public static class ShadowRefusedLocationManager extends ShadowLocationManager {

        @Implementation
        @Override
        protected Location getLastKnownLocation(@NonNull String provider) {
            throw new SecurityException("uid 10150 does not have "
                    + "android.permission.ACCESS_COARSE_LOCATION or "
                    + "android.permission.ACCESS_FINE_LOCATION.");
        }
    }

    @NonNull
    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }
}
