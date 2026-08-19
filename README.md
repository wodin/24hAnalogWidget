24h Analog Clock Widget
=======================

This is a homescreen widget that displays a 12-at-the-top 24 hour analog clock.
Noon is at the top, so that the hour hand mimics the path of the sun.

![screenshot of widget](extra/sshot02.png)

The dial is also designed to show when the sun rises and sets at your location.

**That part does not work.** Nothing in the phone widget ever supplies a
location, so the dial always draws the hatched "no location" placeholder instead
of a real sun position. This is true of upstream too — it is not a regression in
this fork — and wiring it up is later work.

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

The manifest requests the two location permissions, but nothing reads location —
see above. They are vestigial until the sunrise/sunset feature is wired up. Even
then it will not impact battery life, as it never actively requests location; it
only uses cached locations acquired by other apps.

Source
------

This code depends on [jSunTimes](http://www.jstott.me.uk/jsuntimes/), which is
included in the source tree in both byte-compiled form as well as source.

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
