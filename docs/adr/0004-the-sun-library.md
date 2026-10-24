---
status: accepted
amended-by: 0006
---

# The sun library

The dial exists to shade the hours the sun spends below the horizon, so it needs
an almanac. From 2011 until candidate C that was jSunTimes, carried in the
source tree as a jar. It is now `org.shredzone.commons:commons-suncalc:3.11`,
resolved from Maven Central like everything else.

A reader who finds this history will see the astronomy under a shipping widget
replaced wholesale in a single commit, and will reasonably assume that was
riskier than it was. It was not risky, for one reason that is invisible in the
diff.

## The code was dormant, and that is the whole reason it was cheap

`SunPositionOverlay.setLocation` had no callers anywhere in the app. Not a
disabled one, not one behind a preference — none. So `location` was permanently
null, the draw always took the Placeholder early return, and every call into the
sun library was unreachable in production.

That is not an accident of the current tree, and the plan for this candidate
stated it too strongly — it said nothing had ever run jSunTimes, which the
history does not support. The setter arrived in `9eaeda2` (2012) with no caller.
The Wear face called it from `d17ba87` (2014-09), at first with a hardcoded
42.4/-71.1 and then from `674d6ce` (2014-12) with two call sites fed by fused
location, so the library really did run — on watches — until ADR-0001 deleted
Wear from this fork. **No phone code has ever called it**, in fifteen years:
every caller that has ever existed lived in `WearFace.java`. Since Wear left,
the first thing to execute jSunTimes was `SunPathShapeTest`, written in this
candidate.

That is what made the swap cost nothing: there was no behaviour to regress
because there was no behaviour. Nothing a user could see moved, and the emulator
check after the swap confirmed exactly that — the same hatched Placeholder over
18:00 through midnight to 06:00, and a minute hand still sweeping.

It is also why the swap happened here rather than in candidate D — the candidate
that gives the phone widget the Sun position it has never had, which became
ADR-0005 and was not yet planned in detail when this was written. D
closes the window: the moment `setLocation` has a caller, replacing the library
becomes a change to something people are looking at, and would have to be
measured as one. Doing it in D would also have meant porting a 2004 `Calendar`
API into a live feature and then swapping out from under it. Doing it in C means
D is built on the `java.time` API from its first line.

## Why replace rather than vendor or keep

Three options. The jar stays as it is; the source is vendored from
`extra/jsuntimes/` into this tree; or the dependency is replaced.

**The licence question is settled, and it does not decide this.** jSunTimes is
GNU GPL, so vendoring would have been permitted. The provenance is pinned: the
downloaded `jsuntimes-1.0.jar` is md5 `d51dc5126e41c7c40c39af8418ca51e8`, and
`extra/jsuntimes/jsuntimes-1.0.jar` was byte-identical to it. What the app
actually shipped was `app/libs/jsuntimes-1.0.stripped.jar`, md5
`c820d6f792928edd0368f1d127719691` — that same jar with the applet, panel,
standalone-app and JUnit classes removed by `extra/jsuntimes/Makefile`. The
classes are `uk.me.jstott.sun`. The archived homepage gives the licence as GPL
and links the version 2 text, but no *source file* carries a version notice, and
GPLv2 §9 says that where the Program specifies no version "you may choose any
version ever published" — so version 3 may be chosen, and the combination with
this GPLv3-or-later project is sound. The architecture review reached the right
conclusion by the wrong route; the route is written down here so the next person
to ask does not have to walk it again.

So the case for replacing is technical, not legal. jSunTimes speaks `Calendar`
and a raw-offset-plus-DST boolean, which is a 2004 API and the part of it least
worth preserving — carrying it forward would have meant carrying that vocabulary
into every caller D adds. commons-suncalc is Apache 2.0, `java.time`-native, and
its `minSdk` matches this project's 26 exactly. It is stable rather than
actively developed, which is a much weaker worry for closed-form astronomy than
for a network client: the formulae are Meeus and they are not going to move.

**The glob was a separate problem with the same fix.** The jar was reached
through `implementation(fileTree("libs") { include("*.jar") })`, which is an
unpinned dependency by construction — any jar dropped in that directory joins
the build, with no version and no checksum. That is gone too, and it would have
been worth removing even if the library had stayed.

Nothing was lost by deleting the jar. Git holds its bytes at `b5e5b1d^`, and
this ADR holds the version, the licence, the md5s and the origin.

## Two of the library's names collide with the glossary

commons-suncalc calls the meridian transit `getNoon()`, which is our **High
noon**'s place under a name `CONTEXT.md` deliberately declined. And its
`SunPosition` class means azimuth and altitude — a direction in the sky — where
this app's **Sun position** means an angle on the dial.

The two collide differently, and the difference is worth keeping straight.
`getNoon()` is real and in use: it, and `Twilight.VISUAL`, are bound to local
names inside `SunPath.java` and neither leaves that file. `SunPosition` is not
used at all — the only suncalc import in the tree is `SunTimes`. So that
collision is one a future reader could walk into rather than one the code has
already resolved, which is why an avoid list is the right place for it: the
glossary keeps its entry and gains the library's name beneath it.

## High noon changed meaning, which is not a rounding change

Before the swap, high noon was the midpoint of sunrise and sunset, computed in
the painter in angle space. It is now the true meridian transit, which is a
different moment.

They differ by the sun's declination moving between the two crossings — worth
about 20 seconds at Berlin in February. Not by the equation of time, which is
the explanation the code used to carry: the equation of time shifts the transit
and both horizon crossings alike, so it cancels out of a midpoint exactly.

Measured against MET Norway's published transit, the old midpoint was 68 seconds
late and the real transit is 50. The almanac test had been carrying three
minutes of tolerance for high noon where the other eight moments carry two, the
extra minute being owed to the definitional gap. It went back with the swap,
because leaving it would have kept buying slack the change had already paid for.

## What the swap actually bought, measured

Berlin, 52.5200 N 13.4050 E, `Europe/Berlin`, 2026-02-15, against published
values:

| Moment | jSunTimes | commons-suncalc | published | source |
|---|---|---|---|---|
| morning astronomical twilight | +50s | **+28s** | 05:27:28 | .org |
| morning nautical twilight | +47s | **+24s** | 06:06:57 | .org |
| morning civil twilight | +45s | **+17s** | 06:46:55 | .org |
| sunrise | +63s | **+40s** | 07:22 | MET |
| high noon | +68s | **+50s** | 12:20 | MET |
| sunset | +74s | **+68s** | 17:18 | MET |
| evening civil twilight | +40s | **+39s** | 17:54:01 | .org |
| evening nautical twilight | +43s | **+37s** | 18:33:58 | .org |
| evening astronomical twilight | +45s | **+39s** | 19:13:27 | .org |

The surprise of this candidate is that jSunTimes was *not* wrong. Nothing on a
phone had ever executed it, and the plan expected fifteen years of quiet error;
every one of its nine moments was inside two minutes. What it had was a phase
lag — the whole day uniformly 40 to 74 seconds late, never early, with the day
*length* exact to the second. commons-suncalc shortens that lag by about a third
and does not remove it: across the six moments published to the second, the mean
residual falls from +45s to +31s, and every one is still positive. Sunset barely
moves at all, 74s to 68s. The plan for this candidate called the lag "roughly
halved", which its own table does not support.

So accuracy is a bonus here, not the justification. The justification is the
API, the licence-free provenance, and the glob.

## A literal port would have swapped one dating bug for a subtler one

jSunTimes returned times with no date, so the old code pinned each onto the day
that was asked about — which is wrong wherever an evening twilight runs past
local midnight. The plan assumed that guess would simply evaporate once the
library reported dates.

It does not. commons-suncalc does not look a day up; it *searches* from an
anchor instant for the next horizon crossing. The obvious translation —
`.on(date).timezone(zone).oneDay()` — therefore asks for the first setting event
within 24 hours of local midnight, which at Kashgar on the solstice is *last
night's* astronomical twilight ending at 00:27. Correct-looking, and a day
wrong.

So a sun path is now built outward from high noon rather than from local
midnight: the crossings before the transit are the morning, the crossings after
it are the evening. That is symmetric where midnight is not — it holds equally
for a place whose clock runs ahead of its sun and one whose clock runs behind —
and it dates every moment correctly as a consequence rather than as a special
case. It costs nine `execute()` calls: one for the transit, then reverse and
forward from it at each of the four horizons.

Kashgar, 39.4704 N 75.9898 E on Beijing's clock, is the fixture that holds this.
It was written before the swap and failed against jSunTimes exactly as
predicted, with the last moment of the day dated twenty-four hours early.

## Absence became a value, so the catch went

The old painter wrapped the whole draw in `catch (IllegalArgumentException)`,
because jSunTimes threw when a place had no such sunrise. commons-suncalc
reports that by returning null from `getRise()`/`getSet()`, so there is nothing
to catch and the catch was deleted rather than translated. What the new library
*does* throw — `IllegalArgumentException` on a latitude outside ±90 — is a
caller's bug rather than a place without a sunrise, and swallowing it would hide
one.

The all-or-nothing rule survived the change: a path was nine moments or it was
null, and a caller with eight had nothing to draw. That fires somewhere far more
ordinary than "polar circle" suggests — from about 48° the sun stops falling a
full 18° below the horizon near midsummer, so **London has no astronomical
twilight from about mid-May to late July** and, under this rule, no sun path
either. It drew the Placeholder, which was the honest answer for a seam with no
way to say "these eight". ADR-0006 gives it one: a path is now four bands, each
able to be absent on its own, and London keeps its other three.

## Consequences

The sun's astronomy now lives in `sun/`, has no `android.` import at all, and is
asserted on a plain JVM to the second rather than to the pixel. `Calendar`,
`GregorianCalendar` and `java.util.TimeZone` are absent from `dial/`, `overlay/`
and `sun/`, and there is no `fileTree` in the build.

The two nets that hold this are deliberately separate, because they fail for
different reasons. `SunPathShapeTest` says a day is shaped like a day — sunrise
before high noon before sunset, every moment on the date and in the zone asked
for — and that claim must survive a change of library. `SunPathAlmanacTest` says
the day is in the right *place*, against times somebody else published, and
exists precisely to be free to disagree with a library. A phase lag slides a
whole day sideways without disturbing a single ordering, so the shape test
cannot see the thing the swap improved; that is the concrete reason for two.

The almanac test asserts against MET Norway for sunrise, sunset and the transit
and against sunrise-sunset.org for the six twilights, at two minutes' tolerance,
with its reasons and its own discomfort about the split written out in its
javadoc. That is what makes the next swap measurable the same way this one was:
the values it checks come from neither library, so no library's mistakes are
enshrined in them.
