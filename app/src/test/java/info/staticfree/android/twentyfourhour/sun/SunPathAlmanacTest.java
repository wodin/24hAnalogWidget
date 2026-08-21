package info.staticfree.android.twentyfourhour.sun;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * Whether the sun's day is in the <em>right place</em>, measured against times
 * somebody else published.
 *
 * <p>Its companion {@link SunPathShapeTest} says a day is shaped like a day —
 * true however wrong the almanac behind it is. This one is the other half, and
 * the two are separate because they fail for different reasons: the shape must
 * survive a change of library, and this must be free to disagree with one.
 *
 * <p>Nothing here is read off what the code returns. Every constant is the
 * verbatim response of a public service, with the URL beside it, so a reader
 * can re-fetch it and check without taking this test's word — or the library's.
 *
 * <h2>Why two sources, and which is trusted for what</h2>
 *
 * <p>NOAA's own calculator runs entirely in the browser and cannot be asked a
 * question from a script, so four others were, for sunrise here on this date:
 *
 * <ul>
 * <li>MET Norway, {@code api.met.no/weatherapi/sunrise/3.0} — 07:22</li>
 * <li>Open-Meteo, {@code archive-api.open-meteo.com/v1/archive} — 07:22</li>
 * <li>sunrisesunset.io, {@code api.sunrisesunset.io/json} — 07:21:15</li>
 * <li>sunrise-sunset.org, {@code api.sunrise-sunset.org/json} — 07:20:27</li>
 * </ul>
 *
 * <p>Nearly three minutes apart, and that spread is convention rather than
 * error: sunrisesunset.io looks up Berlin's 39 m of terrain, and the horizon
 * dips about 0.18&deg; for an observer that high, which is worth some 75
 * seconds. The other three assume sea level, as this app does. So a source had
 * to be chosen per moment rather than overall:
 *
 * <ul>
 * <li><strong>Sunrise, sunset and the solar transit come from MET Norway</strong>,
 *     the Norwegian Meteorological Institute — a national weather service rather
 *     than a hobby endpoint, and sea level like this app. It also publishes the
 *     sun's elevation at transit, 24.89&deg;, which pins this day's solar
 *     declination at 90&deg; &minus; 52.52&deg; &minus; 24.89&deg; =
 *     &minus;12.59&deg;. The ordinary half-day formula
 *     {@code cos H = (sin h - sin phi sin delta) / (cos phi cos delta)} on that
 *     declination gives 4h58m05s, so a day of 9h56m10s — MET's own published
 *     sunrise and sunset are 9h56m apart. It agrees with itself, rather than
 *     merely being quoted.</li>
 * <li><strong>The six twilights come from sunrise-sunset.org</strong>, because
 *     MET Norway does not publish them. Its sunrise and sunset are the pair that
 *     disagree with everybody, so they are deliberately not used here — but the
 *     same formula at &minus;6&deg;, &minus;12&deg; and &minus;18&deg; puts each
 *     twilight 35m25s, 1h15m23s and 1h54m52s from sunrise, and its published
 *     twilights sit that far from MET's sunrise and sunset to within about half
 *     a minute, which is the resolution MET publishes those to. Every number in
 *     this paragraph is re-derivable from the constants below.</li>
 * </ul>
 *
 * <p>Splitting the sources per moment is the uncomfortable part, so it is stated
 * plainly rather than buried: each moment is checked against the source with an
 * independent reason to be right about that moment, decided before this test was
 * run and not by seeing which one the code agreed with.
 *
 * <p><strong>It is also load-bearing, and that must be said too.</strong> Had
 * sunrise-sunset.org been used for all nine, sunrise would be 156 seconds out
 * and this test would fail. The case for MET Norway above is why it is not the
 * wrong call — but a reader deserves to know that the green here rests on it,
 * rather than discovering it by changing one constant.
 *
 * <h2>Why two minutes</h2>
 *
 * <p>A tolerance cannot be finer than its sources agree, and these span about
 * two minutes. MET Norway also publishes only to the minute, which is up to
 * another 60 seconds on its own. Two minutes is half a degree of a
 * twenty-four-hour dial, and the face artwork is 196 to 392 pixels square, so
 * that is one to two pixels of arc at its outer edge and less further in.
 * A tighter bound would therefore be arbitrating between almanacs rather than
 * testing this app — and it could not be drawn either way. A looser one would
 * stop catching what actually goes wrong here: an hour of daylight saving, a
 * dropped zone offset, a day to either side.
 */
public class SunPathAlmanacTest {

    /**
     * MET Norway, the Norwegian Meteorological Institute, verbatim from
     * {@code https://api.met.no/weatherapi/sunrise/3.0/sun
     * ?lat=52.5200&lon=13.4050&date=2026-02-15&offset=+01:00}. Published to the
     * minute, which is part of why the tolerance is what it is.
     */
    private static final OffsetDateTime MET_NORWAY_SUNRISE =
            OffsetDateTime.parse("2026-02-15T07:22+01:00");

    private static final OffsetDateTime MET_NORWAY_SUNSET =
            OffsetDateTime.parse("2026-02-15T17:18+01:00");

    /**
     * MET Norway's {@code solarnoon}: the sun's true transit of the meridian.
     * See {@link #highNoonIsNearTheSolarTransitButIsNotIt()} — the code does not
     * currently compute this quantity.
     */
    private static final OffsetDateTime MET_NORWAY_SOLAR_TRANSIT =
            OffsetDateTime.parse("2026-02-15T12:20+01:00");

    /**
     * sunrise-sunset.org, verbatim from {@code https://api.sunrise-sunset.org/json
     * ?lat=52.5200&lng=13.4050&date=2026-02-15&formatted=0}. That service answers
     * in UTC, and these are left in UTC rather than converted by hand, so that
     * comparing them against the response is reading and not arithmetic. Berlin
     * is an hour ahead of them on this date.
     */
    private static final OffsetDateTime DOT_ORG_MORNING_ASTRONOMICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T04:27:28+00:00");

    private static final OffsetDateTime DOT_ORG_MORNING_NAUTICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T05:06:57+00:00");

    private static final OffsetDateTime DOT_ORG_MORNING_CIVIL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T05:46:55+00:00");

    private static final OffsetDateTime DOT_ORG_EVENING_CIVIL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T16:54:01+00:00");

    private static final OffsetDateTime DOT_ORG_EVENING_NAUTICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T17:33:58+00:00");

    private static final OffsetDateTime DOT_ORG_EVENING_ASTRONOMICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T18:13:27+00:00");

    /** See <em>Why two minutes</em> above. */
    private static final Duration TOLERANCE = Duration.ofMinutes(2);

    /**
     * High noon's own, and looser, because it is not being compared against
     * itself. See {@link #highNoonIsNearTheSolarTransitButIsNotIt()}.
     */
    private static final Duration HIGH_NOON_TOLERANCE = Duration.ofMinutes(3);

    private SunPath path;

    @Before
    public void computeTheDay() {
        path = Berlin.sunPath();
    }

    @Test
    public void sunriseAndSunsetMatchMetNorway() {
        assertPublished("sunrise", MET_NORWAY_SUNRISE, path.sunrise(), TOLERANCE);
        assertPublished("sunset", MET_NORWAY_SUNSET, path.sunset(), TOLERANCE);
    }

    @Test
    public void theTwilightsMatchSunriseSunsetOrg() {
        assertPublished("morning astronomical twilight", DOT_ORG_MORNING_ASTRONOMICAL_TWILIGHT,
                path.morningAstronomicalTwilight(), TOLERANCE);
        assertPublished("morning nautical twilight", DOT_ORG_MORNING_NAUTICAL_TWILIGHT,
                path.morningNauticalTwilight(), TOLERANCE);
        assertPublished("morning civil twilight", DOT_ORG_MORNING_CIVIL_TWILIGHT,
                path.morningCivilTwilight(), TOLERANCE);
        assertPublished("evening civil twilight", DOT_ORG_EVENING_CIVIL_TWILIGHT,
                path.eveningCivilTwilight(), TOLERANCE);
        assertPublished("evening nautical twilight", DOT_ORG_EVENING_NAUTICAL_TWILIGHT,
                path.eveningNauticalTwilight(), TOLERANCE);
        assertPublished("evening astronomical twilight", DOT_ORG_EVENING_ASTRONOMICAL_TWILIGHT,
                path.eveningAstronomicalTwilight(), TOLERANCE);
    }

    /**
     * High noon is measured against the solar transit, which is <em>not the same
     * moment</em> and is why this assertion is separate and its tolerance looser.
     *
     * <p>This app has always meant by high noon the midpoint of sunrise and
     * sunset. The transit is when the sun actually crosses the meridian. They
     * drift apart because the sun's declination moves between one horizon
     * crossing and the other, so the two crossings are not symmetric about the
     * transit — worth some 20 seconds here. Note that this is <em>not</em> the
     * equation of time, which shifts the transit and both crossings alike and so
     * cancels out of a midpoint exactly.
     *
     * <p>The extra minute over {@link #TOLERANCE} is that gap, rounded up to a
     * whole minute; the other two are the same measurement spread as everything
     * else. Measured, high noon lands 68 seconds after MET Norway's transit —
     * about 20 of definition and the rest the same lag the eight other moments
     * show. Recorded so the third minute cannot outlive the reason for it.
     *
     * <p>Replacing the almanac behind this replaces the midpoint with a genuine
     * transit — a change in what the code <em>means</em>, not in how accurately
     * it means it. At that point the definitional minute is no longer owed and
     * this tolerance should come back down to {@link #TOLERANCE}.
     */
    @Test
    public void highNoonIsNearTheSolarTransitButIsNotIt() {
        assertPublished("high noon", MET_NORWAY_SOLAR_TRANSIT, path.highNoon(),
                HIGH_NOON_TOLERANCE);
    }

    private static void assertPublished(String moment, OffsetDateTime published,
            ZonedDateTime computed, Duration tolerance) {
        Duration off = Duration.between(published.toInstant(), computed.toInstant());

        assertTrue(moment + " was computed as " + computed + ", which is " + off.toSeconds() +
                        "s from the published " + published + " — more than the stated tolerance " +
                        "of " + tolerance.toSeconds() + "s",
                off.abs().compareTo(tolerance) <= 0);
    }
}
