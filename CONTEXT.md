# 24h Analog Clock Widget

A homescreen widget that draws a 24-hour analog clock. The hour hand turns once
per day rather than twice, with noon at the top and midnight at the bottom, so
it traces the sun's arc across the sky. The dial is designed to shade the hours
the sun spends below the horizon at the viewer's location.

## The clock

**Dial**:
The circular 24-hour scale everything is drawn on. One full turn is one day, so
an angle on the dial is a time of day — the identity the sun drawing rests on.
_Avoid_: clock, circle

**Face**:
The fixed artwork of the dial: its numbers and tick marks. Stateless, and
identical at every moment.
_Avoid_: background, dial

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
the hour hand would stand at each.
_Avoid_: sun angle, solar position

**Twilight**:
The graded band between full day and full night, in three widening steps: civil,
nautical, astronomical. Drawn as shades laid over one another, so the dial
darkens by degrees rather than at a single edge.
_Avoid_: dusk, dawn, golden hour

**High noon**:
The moment the sun stands highest, halfway between sunrise and sunset. Not
Noon — noon is fixed at the top of the dial, whereas high noon drifts with the
date and with longitude within the timezone.
_Avoid_: solar noon, midday, zenith

**Placeholder**:
The hatched shading over a nominal night, 18:00 through midnight to 06:00, drawn
when no location is known. It means "the sun position is unknown", not "the sun
is down for these hours".
_Avoid_: empty state, fallback, no-data tile

## The app

**Widget**:
An instance of the clock living on the homescreen. A still picture redrawn on a
schedule rather than a live view, so it cannot animate, and the only gesture it
accepts is a single tap.
_Avoid_: applet, tile, gadget
