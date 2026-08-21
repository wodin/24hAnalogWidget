package info.staticfree.android.twentyfourhour.observer;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;

/**
 * Giving the phone something to have last seen.
 *
 * <p>One home because two test classes need it and the provider is the whole of
 * what they are agreeing about: a fix cached under any other name is a fix
 * {@link LastKnownObserver} never looks at, so a test that named its own would
 * go on passing while measuring nothing. Same argument as {@code Artwork} and
 * {@code Shading}: the fact that could go stale lives once.
 *
 * <p>Robolectric's shadow models no permissions, so this leaves a fix any caller
 * can read. Refusing one is {@code LastKnownObserverTest}'s own shadow, and
 * stays there.
 */
public final class Fixes {

    /**
     * Leaves a passive fix at the given place, as another app's read would have.
     *
     * <p>Takes an {@link Observer} rather than a latitude and a longitude so
     * that a caller cannot hand them over the wrong way round — the reason the
     * value exists at all — and gives back nothing, because what a test wants
     * next is not the {@link Location} but whatever read it.
     */
    public static void cacheAPassiveFixAt(@NonNull Context context, @NonNull Observer observer) {
        Location fix = new Location(LocationManager.PASSIVE_PROVIDER);
        fix.setLatitude(observer.latitude());
        fix.setLongitude(observer.longitude());

        shadowOf(context.getSystemService(LocationManager.class))
                .simulateLocation(LocationManager.PASSIVE_PROVIDER, fix);
    }

    private Fixes() {
    }
}
