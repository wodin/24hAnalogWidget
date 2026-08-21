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
 * sunrise-sunset.org been used for all nine, sunrise would be 133 seconds out
 * and this test would fail. The case for MET Norway above is why it is not the
 * wrong call — but a reader deserves to know that the green here rests on it,
 * rather than discovering it by changing one constant.
 *
 * <h2>And the same again, south of the equator</h2>
 *
 * <p>{@link BuenosAires} on 15 August 2026 is checked against the same two
 * services, split the same way and for the same reasons: MET Norway for the two
 * horizon crossings and the transit, sunrise-sunset.org for the six twilights
 * it does not publish. Nothing about that division was reconsidered per place,
 * which is the point of having decided it on grounds other than agreement.
 *
 * <p>It agrees with itself the same way, and the working is worth reading
 * because it is a check a second place can offer that the first cannot: an
 * error in the sign of a latitude would leave Berlin's numbers untouched.
 * MET publishes the disc centre 41.49&deg; up at transit, which pins this day's
 * declination at 90&deg; &minus; 41.49&deg; &minus; 34.61&deg; = &plus;13.90&deg;
 * — a <em>northern</em> declination over a southern observer, which is what
 * mid-August at this latitude means and what a mirrored February in Berlin
 * would not have shown. The same half-day formula on it gives 5h24m54s, so a
 * day of 10h49m47s, against MET's own published crossings 10h50m apart. At
 * &minus;6&deg;, &minus;12&deg; and &minus;18&deg; it puts the twilights
 * 25m57s, 55m38s and 1h25m00s from sunrise; sunrise-sunset.org's published
 * twilights sit 25m52s, 55m33s and 1h24m55s from MET's sunrise, and 25m50s,
 * 55m31s and 1h24m53s from MET's sunset. Within seven seconds on all three,
 * morning and evening.
 *
 * <p>sunrise-sunset.org disagrees with MET Norway about sunrise here as it does
 * at Berlin, and in the same direction: its 07:31:45 local is 75 seconds ahead
 * of MET's 07:33, where at Berlin its 07:20:27 is 93 seconds ahead of 07:22. So
 * its sunrise is not used here either — for the reason given above rather than
 * for a fresh one, since two sources cannot settle which of them is the odd one
 * out and only Berlin was surveyed four ways.
 *
 * <p>This fixture was added expecting to find something, and did not: all nine
 * moments land 11 to 16 seconds from what was published, where Berlin's run 17
 * to 68. So the southern hemisphere is not a degraded case here, and that is
 * the finding rather than a bug — worth stating, because a reader who assumes
 * an untested hemisphere was a broken one would be wrong about every version of
 * this file.
 *
 * <p>Each test below is one claim rather than one place: "sunrise and sunset
 * match MET Norway" is a single fact about this app, and it is checked at both
 * of the places it was measured at. Split per place instead, a reader would
 * have to compare two methods to see that they are asking the same question,
 * and a third place would be a few more lines inside these three rather than
 * three more methods to keep in step.
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
    private static final OffsetDateTime BERLIN_MET_NORWAY_SUNRISE =
            OffsetDateTime.parse("2026-02-15T07:22+01:00");

    private static final OffsetDateTime BERLIN_MET_NORWAY_SUNSET =
            OffsetDateTime.parse("2026-02-15T17:18+01:00");

    /** MET Norway's {@code solarnoon}: the sun's true transit of the meridian. */
    private static final OffsetDateTime BERLIN_MET_NORWAY_SOLAR_TRANSIT =
            OffsetDateTime.parse("2026-02-15T12:20+01:00");

    /**
     * sunrise-sunset.org, verbatim from {@code https://api.sunrise-sunset.org/json
     * ?lat=52.5200&lng=13.4050&date=2026-02-15&formatted=0}. That service answers
     * in UTC, and these are left in UTC rather than converted by hand, so that
     * comparing them against the response is reading and not arithmetic. Berlin
     * is an hour ahead of them on this date.
     */
    private static final OffsetDateTime BERLIN_DOT_ORG_MORNING_ASTRONOMICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T04:27:28+00:00");

    private static final OffsetDateTime BERLIN_DOT_ORG_MORNING_NAUTICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T05:06:57+00:00");

    private static final OffsetDateTime BERLIN_DOT_ORG_MORNING_CIVIL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T05:46:55+00:00");

    private static final OffsetDateTime BERLIN_DOT_ORG_EVENING_CIVIL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T16:54:01+00:00");

    private static final OffsetDateTime BERLIN_DOT_ORG_EVENING_NAUTICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T17:33:58+00:00");

    private static final OffsetDateTime BERLIN_DOT_ORG_EVENING_ASTRONOMICAL_TWILIGHT =
            OffsetDateTime.parse("2026-02-15T18:13:27+00:00");

    /**
     * MET Norway again, verbatim from {@code https://api.met.no/weatherapi/sunrise/3.0/sun
     * ?lat=-34.61&lon=-58.38&date=2026-08-15&offset=-03:00}. The offset is asked
     * for in the response's own terms, so these read as Buenos Aires wall clock.
     */
    private static final OffsetDateTime BUENOS_AIRES_MET_NORWAY_SUNRISE =
            OffsetDateTime.parse("2026-08-15T07:33-03:00");

    private static final OffsetDateTime BUENOS_AIRES_MET_NORWAY_SUNSET =
            OffsetDateTime.parse("2026-08-15T18:23-03:00");

    /** Its {@code solarnoon}, published with the disc centre 41.49&deg; up. */
    private static final OffsetDateTime BUENOS_AIRES_MET_NORWAY_SOLAR_TRANSIT =
            OffsetDateTime.parse("2026-08-15T12:58-03:00");

    /**
     * sunrise-sunset.org, verbatim from {@code https://api.sunrise-sunset.org/json
     * ?lat=-34.61&lng=-58.38&date=2026-08-15&formatted=0}. In UTC as that service
     * answers, for the reason Berlin's are: comparing these against the response
     * should be reading and not arithmetic. Buenos Aires is three hours behind
     * them, so its morning twilights carry the same date in UTC and its evening
     * ones have not yet crossed into the next.
     */
    private static final OffsetDateTime BUENOS_AIRES_DOT_ORG_MORNING_ASTRONOMICAL_TWILIGHT =
            OffsetDateTime.parse("2026-08-15T09:08:05+00:00");

    private static final OffsetDateTime BUENOS_AIRES_DOT_ORG_MORNING_NAUTICAL_TWILIGHT =
            OffsetDateTime.parse("2026-08-15T09:37:27+00:00");

    private static final OffsetDateTime BUENOS_AIRES_DOT_ORG_MORNING_CIVIL_TWILIGHT =
            OffsetDateTime.parse("2026-08-15T10:07:08+00:00");

    private static final OffsetDateTime BUENOS_AIRES_DOT_ORG_EVENING_CIVIL_TWILIGHT =
            OffsetDateTime.parse("2026-08-15T21:48:50+00:00");

    private static final OffsetDateTime BUENOS_AIRES_DOT_ORG_EVENING_NAUTICAL_TWILIGHT =
            OffsetDateTime.parse("2026-08-15T22:18:31+00:00");

    private static final OffsetDateTime BUENOS_AIRES_DOT_ORG_EVENING_ASTRONOMICAL_TWILIGHT =
            OffsetDateTime.parse("2026-08-15T22:47:53+00:00");

    /**
     * See <em>Why two minutes</em> above. One tolerance for all nine moments,
     * high noon included: it used to carry a third minute for the gap between
     * a sunrise/sunset midpoint and a true transit, and it no longer computes
     * the former.
     */
    private static final Duration TOLERANCE = Duration.ofMinutes(2);

    private SunPath berlin;

    private SunPath buenosAires;

    @Before
    public void computeTheDays() {
        berlin = Berlin.sunPath();
        buenosAires = BuenosAires.sunPath();
    }

    @Test
    public void sunriseAndSunsetMatchMetNorway() {
        assertPublished("Berlin sunrise", BERLIN_MET_NORWAY_SUNRISE, berlin.sunrise(), TOLERANCE);
        assertPublished("Berlin sunset", BERLIN_MET_NORWAY_SUNSET, berlin.sunset(), TOLERANCE);

        assertPublished("Buenos Aires sunrise", BUENOS_AIRES_MET_NORWAY_SUNRISE,
                buenosAires.sunrise(), TOLERANCE);
        assertPublished("Buenos Aires sunset", BUENOS_AIRES_MET_NORWAY_SUNSET,
                buenosAires.sunset(), TOLERANCE);
    }

    @Test
    public void theTwilightsMatchSunriseSunsetOrg() {
        assertTwilightsPublished("Berlin", berlin, BERLIN_DOT_ORG_MORNING_ASTRONOMICAL_TWILIGHT,
                BERLIN_DOT_ORG_MORNING_NAUTICAL_TWILIGHT, BERLIN_DOT_ORG_MORNING_CIVIL_TWILIGHT,
                BERLIN_DOT_ORG_EVENING_CIVIL_TWILIGHT, BERLIN_DOT_ORG_EVENING_NAUTICAL_TWILIGHT,
                BERLIN_DOT_ORG_EVENING_ASTRONOMICAL_TWILIGHT);

        assertTwilightsPublished("Buenos Aires", buenosAires,
                BUENOS_AIRES_DOT_ORG_MORNING_ASTRONOMICAL_TWILIGHT,
                BUENOS_AIRES_DOT_ORG_MORNING_NAUTICAL_TWILIGHT,
                BUENOS_AIRES_DOT_ORG_MORNING_CIVIL_TWILIGHT,
                BUENOS_AIRES_DOT_ORG_EVENING_CIVIL_TWILIGHT,
                BUENOS_AIRES_DOT_ORG_EVENING_NAUTICAL_TWILIGHT,
                BUENOS_AIRES_DOT_ORG_EVENING_ASTRONOMICAL_TWILIGHT);
    }

    /**
     * High noon is the solar transit, and is measured against a published one.
     *
     * <p>It has not always been. Until commons-suncalc replaced jsuntimes this
     * app took high noon to be the midpoint of sunrise and sunset, which is a
     * near neighbour of the transit rather than the same moment: the sun's
     * declination moves between one horizon crossing and the other, so the two
     * crossings are not symmetric about the transit. That was worth some 20
     * seconds at Berlin — and it is <em>not</em> the equation of time, which
     * shifts the transit and both crossings alike and so cancels out of a
     * midpoint exactly. The midpoint measured 68 seconds after MET Norway's
     * transit; the transit itself measures 50, of a piece with the other eight
     * moments.
     *
     * <p>So this test is still separate, but for a smaller reason: it is the
     * one moment MET Norway publishes that the six twilights cannot corroborate,
     * and it is where a change in what the code <em>means</em> would show up
     * rather than a change in how accurately it means it.
     */
    @Test
    public void highNoonIsTheSolarTransit() {
        assertPublished("Berlin high noon", BERLIN_MET_NORWAY_SOLAR_TRANSIT, berlin.highNoon(),
                TOLERANCE);
        assertPublished("Buenos Aires high noon", BUENOS_AIRES_MET_NORWAY_SOLAR_TRANSIT,
                buenosAires.highNoon(), TOLERANCE);
    }

    /** The six twilights of one place, in the order they happen. */
    private static void assertTwilightsPublished(String place, SunPath path,
            OffsetDateTime morningAstronomical, OffsetDateTime morningNautical,
            OffsetDateTime morningCivil, OffsetDateTime eveningCivil,
            OffsetDateTime eveningNautical, OffsetDateTime eveningAstronomical) {
        assertPublished(place + " morning astronomical twilight", morningAstronomical,
                path.morningAstronomicalTwilight(), TOLERANCE);
        assertPublished(place + " morning nautical twilight", morningNautical,
                path.morningNauticalTwilight(), TOLERANCE);
        assertPublished(place + " morning civil twilight", morningCivil,
                path.morningCivilTwilight(), TOLERANCE);
        assertPublished(place + " evening civil twilight", eveningCivil,
                path.eveningCivilTwilight(), TOLERANCE);
        assertPublished(place + " evening nautical twilight", eveningNautical,
                path.eveningNauticalTwilight(), TOLERANCE);
        assertPublished(place + " evening astronomical twilight", eveningAstronomical,
                path.eveningAstronomicalTwilight(), TOLERANCE);
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
