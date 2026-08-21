24h Analog Clock Widget
=======================

This is a homescreen widget that displays a 12-at-the-top 24 hour analog clock.
Noon is at the top, so that the hour hand mimics the path of the sun.

![screenshot of widget](extra/sshot02.png)

The dial also shows when the sun rises and sets where you are standing. It works
out where that is from the last fix the phone happens to have cached, so the two
permissions below have to be granted before it can draw a real sun path. Without
them it draws the hatched Placeholder instead, which means "the sun position is
unknown" rather than "the sun is down". It draws that too on the days a place
has no whole sun path to show — London, for one, loses its astronomical twilight
around midsummer.

See [CONTEXT.md](CONTEXT.md) for the vocabulary the code uses.

About this fork
---------------

This is a personal, divergent fork of
[xxv/24hAnalogWidget](https://github.com/xxv/24hAnalogWidget), which has been
unmaintained since 2017. It is built to be sideloaded onto one phone; it is not
published anywhere, and there are no plans to send changes upstream.

It differs from upstream in two ways that matter:

- **The Android Wear watch face is gone.** See
  [ADR-0001](docs/adr/0001-drop-wear-support.md). Upstream's copy remains the
  place to look for it.
- **The build has been modernised** to Gradle 9, AGP 9, AndroidX and SDK 36,
  and the `lib` module has been folded into `app`.

For the Play Store listings, the watch face, and any support for either, go
upstream.

Building
--------

    ./gradlew assembleDebug

Requires an Android SDK with platform 36, and a JDK 17 installed somewhere
Gradle can find it. The build pins the Java toolchain to 17, so the JDK on your
`PATH` need not be 17 — but no toolchain resolver is configured, so one must
already be present locally.

Permissions
-----------

The manifest requests `ACCESS_COARSE_LOCATION` and `ACCESS_BACKGROUND_LOCATION`,
and the sun path needs both. A widget redraws at a process importance the
platform treats as "nobody is looking", where a foreground-only grant is refused
without a word — so coarse alone leaves the dial hatched exactly when it is on
screen and nobody has opened the app.

Neither permission is ever used to ask the phone where it is. The only call the
app makes is `getLastKnownLocation` on the passive provider, which reads a fix
some other app has already paid for and cannot power a radio, so this costs no
battery. Coarse rather than fine because coarse is enough: a ~2 km grid moves
sunrise by seconds.

This fork is sideloaded, so both are granted by hand rather than asked for. If
the sun goes missing, `logcat` says which of them is the one that is absent.

Source
------

The sun's times come from
[commons-suncalc](https://shredzone.org/maven/commons-suncalc/), resolved from
Maven Central like every other dependency. It replaced jSunTimes, which used to
be carried in the source tree as a jar.

License
-------

24h Analog Clock Widget  
Copyright (C) 2011-2014 [Steve Pomeroy](mailto:steve@staticfree.info)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
