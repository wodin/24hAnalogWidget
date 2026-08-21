---
status: accepted
---

# The observer's position

The dial shades the hours the sun spends below the horizon, so it has to know
where the observer is standing. From this candidate it finds out by reading the
fix the phone already has cached, once per tick, on a permission that has to be
granted from a shell.

A reader who finds this will find two surprising things at once: an app whose
README says it never asks the phone where it is, holding
`ACCESS_BACKGROUND_LOCATION` — which a real user can only reach through a
Settings screen, having granted the foreground permission first, and which Play
policy treats as high-risk — and an install procedure with three `adb`
incantations in it that nothing in the app will ever prompt for. Both are
deliberate, and neither is obvious from the code.

## The feature was there in 2013 and this fork lost it

`SunPositionOverlay` has had a `setLocation` since 2012 and, on a phone, has
never had a caller — ADR-0004 traces that. What it had *before* the setter was
its own `LocationManager`: at tag `0.4.2` (`511f9ff`, April 2013) the overlay
called `getLastKnownLocation("passive")` from inside its own draw.
`1c9c896` (February 2017, *"Refactor SunPositionOverlay; break out location
loading"*, 147 deletions) took that out, only the Wear face picked it back up,
and `a710c6d` deleted Wear from this fork. The line that made the sun appear was
never re-attached to the phone.

That is not a story about old code. The phone this fork is built for was running
upstream's 0.4.2 Play release — `versionCode=8 versionName=0.4.2 targetSdk=17`,
installed July 2023 — and it draws a **real sun path**. Pixel-probing a
screencap of it (median luminance over radii 150–235, chosen to miss the hands
and the numerals) put the shading at ~18:40 → ~07:02 with its midpoint at
~00:51, against the Placeholder's fixed 18:00 → 06:00 centred on 00:00. The
evening edge forty minutes out and the morning one an hour, and the band centred
on a real solar midnight rather than on the clock's.

So this candidate is not a new feature. It is a **regression being repaid**, and
the README used to say the opposite — that the missing sun was true of upstream
too and not a regression in this fork. It was false about the very build the
phone was running.

What made the 2013 code work is exactly what makes this expensive now:
`targetSdk=17` gets install-time grants and is exempt from auto-reset. This
candidate re-buys both at `targetSdk 36`, by hand, and the rest of this document
is the price.

## The gate is process importance, not the receiver

The belief this candidate started from was "a background read returns null on
foreground-only permission". That is correct, and the boundary is not where the
sentence implies.

Measured 2026-08-21 on an API 31 emulator, from a throwaway probe called in
`onUpdate` and in `WidgetDemo.onCreate`, logging the outcome of every provider
alongside the permission states, the app-op modes and
`ActivityManager.getMyMemoryState().importance`:

- A genuine alarm tick fired while `WidgetDemo` was on screen reported
  `importance=100`, `opCoarse=ALLOWED`, and **got a fix** — on coarse permission
  alone.
- HOME was pressed. The next tick, **same process, same pid, same permissions,
  same populated cache**, reported `importance=300`, `opCoarse=IGNORED`, and
  four nulls.

One variable, and the answer flipped. So the widget's read succeeds exactly
while the app's own UI is in front of it, which is precisely when nobody is
looking at the widget. `IMPORTANCE_SERVICE` — 300 — is what a manifest
receiver's process gets while `onReceive` runs, and every background reading
taken in that session was at 300.

**The refusal is silent in every channel a developer has.** No exception. No
stack. `isProviderEnabled` answers `true`; the location master switch is on; a
fix is sitting in the cache. The whole unfiltered log buffer for the window was
searched for `location`, `appop`, `denied` and `permission`, and on API 34 there
were no hits at all. An app that gets this wrong is indistinguishable from an
app on a phone that has never had a fix.

There is exactly one signal, and the app has to ask for it itself:
`AppOpsManager` reports `MODE_IGNORED` for `OPSTR_COARSE_LOCATION` at the moment
of the denied read while `checkSelfPermission(ACCESS_COARSE_LOCATION)` returns
`GRANTED`. The two disagree and the app-op is the one telling the truth — a
foreground-only grant is stored as `MODE_FOREGROUND` and resolved against the
process's importance at the moment of asking. From outside,
`adb shell cmd appops get` shows the same thing as `COARSE_LOCATION: foreground`
with a `rejectTime`. Both are visible in a dump; neither is visible in a log.

That measurement is what buys the diagnosis in `LastKnownObserver.whySilent` and
the sentence about it in the README. Without it, "the sun is broken again" has
no cheap answer.

## Auto-reset will take the grant back, and a pinned widget does not stop it

The obvious hope is that a widget on the homescreen counts as using the app. It
does not.

Measured on the API 31 emulator with the widget bound to the Nexus Launcher host
and ticking, and the app never opened: after the PermissionController job ran,
`ACCESS_COARSE_LOCATION: granted=false, flags=[…|AUTO_REVOKED]` and
`app_hibernation get-state` returned `true`. With
`appops set … AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore` set beforehand, the same
job left the grant intact and the app unhibernated.

The target phone — a Samsung Galaxy A34, the one device this fork is built for —
runs the default policy: a 90-day threshold, checked every 15 days, hibernation
enabled. It already whitelists another app that way, so the mechanism is live
there rather than an emulator artefact.

So the install ritual is three commands, not two, and the third is the one
nobody will remember in three months. It is documented in the README beside the
grants, and its failure is silent: one day the sun quietly stops. The log
diagnosis above is what makes that recoverable in one line rather than an
afternoon.

Runtime detection was considered and rejected. The app would have nowhere to say
it: this fork has no notification, no settings screen and no UI other than a
demo dial, and building one to report a permission would be a larger change than
the feature.

## Why not a configured position

A build constant or a settings screen would need no permission, no app-op, no
install ritual and no adb at all. It is by far the simplest thing that could
work, and it loses on three counts.

The repo is **public**, so a constant commits real coordinates to GitHub. A
settings screen needs preference infrastructure this app does not have — there
is no `Preference` anywhere in the tree, no settings activity, and no persisted
state at all. And a fixed position can never correct itself: the dial would go
on drawing the observer's home sun after they had flown somewhere else.

Reading the cache loses none of those and costs a permission. That is the trade,
and it is the reason the honest-looking cheap option was not taken.

**Foreground-only, with the activity reading and persisting**, was also
measured, and it works — an activity reads the cache on coarse permission alone.
It was rejected because the value would then be exactly as stale as the last
time the user opened an app whose only purpose is to demonstrate a dial, and it
would need the persistence layer that does not exist in order to deliver a worse
answer than the passive read gives for free.

## The read cannot power a radio, and the name says so

`LastKnownObserver.read` calls `getLastKnownLocation` on `PASSIVE_PROVIDER` and
nothing else. That is the whole implementation, and the two choices in it are
the substance of the "never actively asks" claim.

`getLastKnownLocation` hands over a cache. It has no way to start a fix, so the
claim holds **by construction** rather than by discipline, and the class is named
for the invariant so that a call site reads as a promise without anyone opening
the file. `PASSIVE_PROVIDER` is passive in the same sense: it mirrors whichever
provider last produced a fix for somebody else, so it returns the freshest of
them without asking any of them for anything.

What was measured on the phone is the cache behind that, read-only, with nothing
installed: `dumpsys location` against `/proc/uptime` put fused at 2m14s, network
at 6m48s and gps at 40m43s. Passive itself was not read — it is the inference
from those three that it would have handed over the 2m14s fix rather than the
40m43s one. The same reading weakens how much that matters: fused and network
reported *identical* coordinates, so on this phone the choice of provider barely
signifies and only age does. 0.4.2 chose passive too; its multi-provider scan
was a pre-Froyo fallback and is not reproduced.

Nothing asks how old the fix is, and `Observer` cannot express an age. Turning a
phone off, flying to another hemisphere and turning it on again may briefly draw
yesterday's sun. On a dial with no second hand that is cheaper than any
staleness policy, and the type is what keeps it that way: a max-age check cannot
be added without first changing `Observer`, which is the moment its javadoc gets
re-read.

**Null is an ordinary state, not an error.** Even with every permission granted
there may be no cached fix — `getLastKnownLocation` returns null until something
else on the phone has recently asked for a location. A real phone has Maps,
weather and GMS doing that constantly, but that is someone else's behaviour and
not a guarantee, and it is not something this widget can arrange for itself
without doing the one thing it says it will not do. So the Placeholder path is
the normal path and stays.

## The value is pushed, and it is not Android's Location

Two shapes were available. The overlay pulls a position when it draws, which is
what 0.4.2 did; or something upstream reads one and hands it over.

Pull was rejected. It puts a `LocationManager` call inside a canvas draw, which
is exactly why the 2013 code could never be tested, and `DialView` draws the same
overlay inside `WidgetDemo`, where a system read per frame would be plainly
wrong. Candidate C had just finished making the overlay a painter with
everything settled before `draw`; a pull would have undone that in the first
commit that used it.

What crosses the seam is an `Observer` — a latitude and a longitude — and not
`android.location.Location`, which cannot be constructed on a plain JVM and
carries twenty fields of which the sun uses two. That is the same argument that
shaped `SunPath` in candidate C, and it is what makes `SunPositionOverlay`
assertable without an emulator for the first time.

The read happens in the **provider**, in `onUpdate` and
`onAppWidgetOptionsChanged`, which puts the one Android-flavoured call where
`Tick` and `AppWidgetManager` already live, leaves `WidgetPicture` the assembler
its javadoc claims it is, and reads once per tick rather than once per widget.
Both call sites are covered, and separately: they share no code, so a claim
proved on one says nothing about the other.

`WidgetDemo` performs its own read, and that is not duplication for its own
sake — it is the differential diagnostic. The demo runs in the foreground where
a coarse grant alone always succeeds; the widget runs at importance 300 where it
needs the background op. A sun in the demo and a hatched widget means the
background grant is gone; both hatched means no fix is cached. The two nulls the
platform refuses to tell apart, told apart by opening an app.

There is no interface behind any of this. One class, one static read; the
"second implementation" a seam would exist for is a value, not a class. An
interface arrives the day a second *real* source does.

## What is verified where

Three levels, and each can see something the others cannot.

**On the JVM.** The sun's arithmetic is pinned to the second by
`SunPathAlmanacTest` against published times, now at Buenos Aires as well as
Berlin — an opposite-hemisphere fixture six months offset, so a sign error makes
the two disagree legibly rather than quietly. `SunPositionOverlayTest` probes
pixels for the wedge the observer produces, `WidgetPictureTest` asserts that an
observer handed in produces something other than the Placeholder, and two tests
on the receiver assert that the observer reaches the picture from each of its
call sites. Mutating either receiver read to null fails exactly one of them.

**On the emulator (API 31, 2026-08-22).** The full path, end to end, and it
proved more than it was expected to. The plan predicted the Placeholder there,
the emulator's location cache having been measured empty the day before; it had
since been warmed at Berlin and the fix survived in `system_server`, so the
widget drew a real day. After a genuine alarm tick with the app in the
background — `LastKnownObserver` silent, which is what a successful read at
`importance=300` looks like — the screencap was probed the way the 0.4.2 build
was, and puts the day's edges at **20:16** and **06:01** against MET Norway's
published Berlin sunset **20:17** and sunrise **06:00** for that date, the
emulator's clock running at the same +02:00 offset. The sun glyph sits at
**13:08** against a published transit of **13:09**. Nowhere near the
Placeholder's flat 18:00 and 06:00.

The two failure paths were walked on the same device, one permission at a time.
The differential diagnostic reproduced exactly as designed above, all three log
diagnoses were produced in turn, and the `SecurityException` from an unpermitted
read was caught rather than taking the redraw and with it the tick down: the
alarm re-armed and the next minute redrew.

**On the phone.** The only place with a real cache, a real grant and One UI's
own policies on top of the platform's, and therefore the only place the feature
can honestly be called demonstrated. That check is the exit criterion of this
candidate's last slice and is not carried by this ADR.

## The title collides with the glossary, and the code does not

`CONTEXT.md` defines **Sun position** as an angle on the dial, and its
**Observer** entry says in as many words that an observer is "not a Sun
position, which is an angle on the dial rather than a place on the earth". This
document is nonetheless called *The observer's position*, and says "position"
for a place several times below.

That is a licence taken by the prose and refused by the code. The review that
opened this candidate proposed the type name `ObserverPosition`; it was rejected
for exactly the collision above, and what shipped is `Observer` — the word
`SunPath.Almanac`'s javadoc was already using. So the glossary's rule holds
where it can be enforced. A reader who meets "position" in this file should read
it as English rather than as the glossary term, and should not carry it into a
name.

## Consequences

Location comes back into a codebase that had been free of it since 2017, in one
package of two classes: a value with two doubles and a reader with one call. The
`sun` package stays Android-free, and `SunPositionOverlay` — which used to hold
an `android.location.Location` — no longer imports it. `LastKnownObserver` is
the only file in `src/main` that names `android.location` at all, which is the
shape the seam was drawn for.

The cost is an install ritual. Three shell commands after every fresh install,
one of which prevents a silent revocation ninety days later, and none of which
the app will ever ask for. That is affordable only because the audience is one
phone, sideloaded. If this fork ever ships to anyone else it needs a permission
request flow, and the seam does not move for it: `LastKnownObserver.read` is
still the call, and the only thing that changes is who has granted what before
it runs.

The manifest lost two entries on the way in. `ACCESS_FINE_LOCATION` is gone, for
the reason its own comment there now gives: fine asks for precision the dial
cannot show. `com.google.android.permission.PROVIDE_BACKGROUND` is gone too:
`pm list permissions` does not define it on either emulator, so nothing can ever
have granted it,
and its name is actively misleading in a block being edited precisely to add
background location. Both were deleted here rather than left for a later
candidate, because dead weight in a block nobody is rewriting is dead weight
forever.
