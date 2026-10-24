# 24h Analog Clock Widget

A homescreen widget that draws a 24-hour analog clock. The hour hand turns once
per day rather than twice, with noon at the top and midnight at the bottom, so
it traces the sun's arc across the sky. The dial is designed to shade the hours
the sun spends below the horizon where the viewer stands.

## The clock

**Dial**:
The circular 24-hour scale everything is drawn on. One full turn is one day, so
an angle on the dial is a time of day — the identity the sun drawing rests on.
_Avoid_: clock, circle

**Face**:
The fixed artwork of the dial: its numbers and graduations. Stateless, and
identical at every moment.
_Avoid_: background, dial, tick marks

**Hand**:
A pointer that rotates about the centre of the dial to show a time. There are
two — hour and minute, no second — and the hour hand turns at half the rate of a
12-hour clock's.
_Avoid_: pointer, needle, arm

**Overlay**:
Anything drawn on the dial above the face — the hands and the sun position among
them. The set is open, so what a dial shows is decided by which overlays it is
given.
_Avoid_: layer, decoration, plugin

## The sun

**Sun position**:
Where the sun is, as an angle on the dial. Because the dial is a 24-hour scale
that angle is a clock time, so sunrise, sunset and the twilights are drawn where
the hour hand would stand at each. Not the sun library's `SunPosition`, which is
a direction in the sky — azimuth and altitude — and which nothing here imports.
_Avoid_: sun angle, solar position, SunPosition

**Sun path**:
The sun's whole day at one place: high noon, and the four Bands of the night
around it. A Sun position is one point on it. A path needs an Observer, and
only the poles in their dark half of the year have none — there the sun never
crosses the meridian, so there is no high noon to build a day around. With no
path, the dial draws the Placeholder instead.
_Avoid_: sun times, solar events, ephemeris

**Band**:
The stretch of one day the sun spends below one horizon, from the evening it
sinks through it to the morning it climbs back. One for the horizon itself —
the night — and one for each Twilight. A band is an arc with those two moments,
or it never happens (London's astronomical twilight around midsummer), or it
lasts all day (the night beyond the polar circle in December). The dial shades
the bands a place has and leaves out the ones it lacks. A band with one
crossing and not the other runs to midnight, the edge of the day, as an
almanac's table shades it.
_Avoid_: interval, period, night (for the twilight ones)

**Twilight**:
The graded band between full day and full night, in three widening steps: civil,
nautical, astronomical. Drawn as shades laid over one another, so the dial
darkens by degrees rather than at a single edge.
_Avoid_: dusk, dawn, golden hour

**High noon**:
The moment the sun stands highest, crossing the meridian. Not Noon — noon is
fixed at the top of the dial, whereas high noon drifts with the date and with
longitude within the timezone.
_Avoid_: solar noon, midday, zenith

**Observer**:
Where the viewer stands, as a latitude and a longitude. The one thing a Sun path
cannot be computed without, and the only thing about the viewer the dial knows.
Not a Sun position, which is an angle on the dial rather than a place on the
earth. A place and not a reading: it carries no accuracy and no age, so there is
nothing to ask how stale it is.
_Avoid_: location, coordinates, GPS

**Placeholder**:
The hatched shading over a nominal night, 18:00 through midnight to 06:00, drawn
whenever there is no Sun path to draw — either because no Observer is known, or
because the Observer stands at a pole in its dark half of the year. It means
"the sun position is unknown", not "the sun is down for these hours". A place
missing a Band is not this: it has a path, and the dial draws what the path
has.
_Avoid_: empty state, fallback, no-data tile

## The app

**Widget**:
An instance of the clock living on the homescreen. A still picture redrawn on a
schedule rather than a live view, so it cannot animate, and the only gesture it
accepts is a single tap.
_Avoid_: applet, tile, gadget

**Tap**:
The single gesture a Widget accepts, and the device's clock app it opens. Rare
by construction, which is what separates it from the Tick: a tap happens only
when a person makes it happen, so what is worth saying once on a tap would be
said 1440 times a day on a tick. There is always one attached, even where
nothing on the phone answers it — "no tap" is never the design, only a fault.
_Avoid_: click, press, touch

**Cell**:
The patch of homescreen the host has given one widget, and the only thing that
decides how large its picture is drawn. The host describes it as two rectangles
in one breath — the cell in portrait and the same cell in landscape — so a cell
is not one width and one height. Each widget has its own, which is why a picture
answers to one widget rather than to all of them.
_Avoid_: size, bounds, tile, slot

**Tick**:
The schedule the widget's picture is redrawn on: once at the top of every
minute, which is as often as a dial with no second hand can change. A chain
rather than a standing order — each redraw arms the next one — so any redraw at
all restores the schedule.
_Avoid_: refresh, poll, frame
