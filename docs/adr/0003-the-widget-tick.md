---
status: accepted
---

# The widget tick

A widget is a still picture, so a clock drawn as one is wrong the moment it is
drawn. Something has to redraw it, and the choice of what is a policy trade
between punctuality, battery and the number of moving parts. None of that trade
is visible in the code it produces — a single `set()` call — so it is recorded
here rather than left to be re-derived.

The tick is a non-wakeup one-shot alarm, anchored at the top of the next minute
and re-armed by whatever the last one caused. Once a minute, because a dial with
no second hand cannot change more often than that. `AlarmManager.RTC` rather
than `RTC_WAKEUP`, exact where the platform permits it and inexact where it
does not, and armed by every path that redraws the widget rather than by a
lifecycle hook.

## Why not a repeat

The obvious shape is one `setRepeating` at a minute's interval, and the reason
this plan gave for rejecting it was that a repeat accumulates drift. That reason
is false, and measuring it was the first step of the slice that replaced it. On
the API 34 emulator on 2026-08-20, `dumpsys alarm`'s `origWhen` walked 15:41:00
→ 15:42:00 → … → 15:46:00 across six fires, every one exactly on the boundary:
`AlarmManagerService` computes each period from the original anchor, not from
when the last one actually fired, so lateness never carries forward.

What a repeat does cost is its batching window. The framework gives an inexact
alarm a window of roughly 0.75 × its futurity, and for a repeat "futurity" is
the whole interval — so a minutely repeat asks to be run any time in the
following 45 seconds, and reported exactly `window=+45s0ms`. Measured, it spent
most of them: the dial changed some 45 seconds into the minute it was already
showing.

A one-shot's futurity is instead the distance to its own trigger, which after
the first link is under a minute and shrinks the later the previous redraw was.
Measured on the current code the window oscillates rather than settling —
`+8s718ms`, `+38s460ms`, `+43s530ms`, `+36s91ms` — because a late fire re-arms
closer to the next boundary and an early one further from it. Average lateness
is lower. The worst case is not.

That is the honest summary of the one-shot against the repeat, and for a while
it left the dial most of a minute behind — the largest user-visible defect the
widget had. That defect is closed now, but nothing about the shape closed it:
what did is two sections down.

## Why 0.4.2 kept time and this did not

Upstream's 0.4.2 is on the same phone, and for a while its dial turned on the
second while this one did not. That was worth chasing, because the section above
predicts the opposite: 0.4.2 uses `setRepeating`, and a repeat is the shape
rejected here for asking to be run any time in the following 45 seconds.

Measured on the A34, Android 16, on 2026-08-22, with both builds installed side
by side and a widget of each pinned. From `dumpsys alarm`, ten consecutive
history rows per app:

- 0.4.2 — `window=+45s0ms repeatInterval=60000 flags=0x1`, and every row landing
  on `…:00.000`, `…:00.005`, `…:00.001`. It has the window and does not spend it.
- This build — one-shot, no repeat interval, rows at 3, 9, 12, 14, 15 and 18
  seconds past the minute, and one minute with no row at all: the tick anchored
  at 08:32:00 arrived at 08:33:09, 69 seconds late, so the dial went straight
  from 08:32 to 08:34 and was a whole minute wrong for most of two.

Both apps were in standby bucket 10, so buckets are not the difference. The
difference is `targetSdk`: 0.4.2 declares 17, and `AlarmManager` documents the
inexact delivery introduced in API 19 as applying only to apps that target 19 or
above — "applications whose targetSdkVersion is earlier than API 19 will
continue to see the previous behavior in which all alarms are delivered exactly
when requested". Its `flags=0x1` is `FLAG_STANDALONE`, the alarm held out of the
batching it is exempt from.

So 0.4.2's punctuality was never a design this fork failed to copy. It is a
compatibility exemption for code written before the rule existed, and one no
build targeting 36 can ask for. There is no version of this code that performs
like it without an exact alarm, which is what the next section is about.

## Why an exact alarm after all

This was decided twice and the first answer was no. Recorded then:
`SCHEDULE_EXACT_ALARM` is denied by default from API 33 and sends the user to a
settings screen to grant it; `USE_EXACT_ALARM` is install-granted but declared
to be for alarm clocks and calendar reminders, which a minute hand is not. A
dial occasionally a minute stale in someone's pocket does not justify either.

Half of that survives. `USE_EXACT_ALARM` is still not ours to claim. But "sends
the user to a settings screen" is a cost for an app with users, and this fork
has one phone whose install already runs two `pm grant` lines and an `appops`
command; the op is set there the same way the others are and reads back
`SCHEDULE_EXACT_ALARM: allow`. What actually tipped it is the section above: the
defect is not "occasionally a minute stale", it is a dial that skips minutes and
sits visibly behind the phone's own clock, next to a fifteen-year-old build that
does not.

The middle option stays dead. A throwaway build calling
`setWindow(RTC, topOfNextMinute, 5000, …)` reported `window=+40s425ms`:
precisely the 0.75 × futurity a plain `set()` produces. The framework replaces a
too-short window with its own inexact heuristic rather than clamping to what was
asked. Punctuality costs an exact alarm or it costs nothing and buys nothing.

Measured after the change, same phone, ten consecutive deliveries:

    08:56:00.012  08:57:00.017  08:58:00.015  08:59:00.009  09:00:00.012
    09:01:00.009  09:02:00.003  09:03:00.010  09:04:00.005  09:05:00.016

Every one inside 17 ms of its minute, none skipped, and the pending alarm now
reads `window=0 exactAllowReason=permission` — the same standalone row 0.4.2
gets by exemption, this one by grant.

The grant can be taken away again while the app runs, and `setExact` throws when
it has been. That throw would end the chain, and nothing re-arms a tick that was
never armed, so the dial would freeze on its last minute and go on looking like
a clock. `Tick` therefore checks `canScheduleExactAlarms` and *also* catches, the
catch being for the gap between the two. Falling back costs the punctuality and
nothing else.

### Why the fallback is reported after all

This section used to end "which is why the fallback is silent and not a logged
failure". Half of that was right, and the half that was wrong cost a session.

The right half is that there is nothing here to interrupt a *user* with. The
inexact schedule is the one this widget kept for its whole life before the grant
existed, and a dial some seconds late is a dial.

The wrong half is the step from there to "so there is nothing to write down".
What changed between the two decisions is not the schedule but this fork's own
README, which now publishes `cmd appops set … SCHEDULE_EXACT_ALARM allow` as one
of its install steps. That turns the fallback from an accepted mode into **a
step somebody skipped** — and a skipped install step is exactly what is worth
reporting, because its symptom is a clock. Measured on the A34: 3 to 69 seconds
late with a minute skipped outright, which at a glance is a working dial.
Finding out otherwise took a side-by-side install of upstream's build and two
`dumpsys alarm` sessions, and nothing on the phone said a word.

So it is reported, and neither report is on the redraw path:

- `WidgetDemo` states on screen whether the tick keeps exact time, **in both
  states**. Always shown rather than only when refused, because a line that
  appears only when something is wrong cannot be told apart from a line that has
  stopped being drawn — and silence that reads as working is this app's failure
  mode three times over.
- The same launch writes the `appops` command to `logcat` under `24hWidget`, as
  a line of its own after the line explaining it, and built from the running
  app's own package name because the debug build installs under a suffixed one.
  The screen answers the question; the log carries the thing that has to be
  typed exactly, which is worthless anywhere but at a prompt — and worth little
  there if the reader has to pick it out of a paragraph.

`Tick` itself still says nothing, and that constraint is what put the words in
the demo rather than beside the check. Every caller inside `Tick` is on the
redraw path, where `onUpdate` runs once a minute, so a line written there would
be 1440 a day. Throttling one with a `static` memo does not rescue it: a
widget's process is started to deliver a broadcast and is a kill candidate the
moment it returns, so whether the memo survives to the next tick is up to memory
pressure — one line a day on an idle phone, 1440 on a busy one,
non-deterministically, and the log could not tell you which. Opening the app is
the rare trigger this needs, so `mayBeExact` was promoted to
`Tick.keepsExactTime` for the demo to ask.

What the screen reports is the grant, and not the punctuality of any particular
redraw — the two are the same thing only while nothing changes underneath them.
`schedule` branches on the very method the demo calls, so the two cannot read
the platform differently; but the `SecurityException` catch above exists because
the grant can be withdrawn between the check and the call, and a tick armed
through that catch is inexact while the screen last said otherwise. The window
is small, the direction is the safe one — the screen is optimistic, never
falsely alarming — and the widest true statement available is the one the
method's javadoc makes rather than a promise about the next minute.

### What it costs

Nothing measurable, because the tick is `RTC` and non-wakeup. A non-wakeup alarm
cannot bring the device out of suspend — it is delivered at the next moment the
device is awake regardless — so exactness only moves *where inside an
already-awake period* a redraw lands. It cannot add a wakeup, there being none
to add to, and what batching normally buys, coalescing alarms so the CPU leaves
idle fewer times, is not available to spend when the CPU is not idle.

Measured over the 8m41s after the grant, screen on, both widgets pinned and
ticking, from `dumpsys batterystats`:

| | CPU | wakeups |
| --- | --- | --- |
| 0.4.2, exact by exemption | 1.257 s | 0 |
| this build, exact by grant | 1.106 s | 0 |

The fork is the cheaper of the two while keeping the same time, and neither app
appears in the *Wakeup alarms* section at all.

### And when nobody is looking

Whether the exact tick should be suspended while the phone is locked, or while
the widget is off screen, is the obvious next question. It already is, by the
alarm type rather than by anything this app arranges.

Measured over 3 days 9 hours of uptime: 0.4.2's tick was delivered 1410 times
against the 4911 minutes that elapsed. Some 71% of its ticks never happened at
all, the device having been suspended and a non-wakeup alarm not being delivered
to a suspended device — across three nights, with `0 wakeups` throughout.
Exactness does not touch that. It applies only once the device is up.

The other half of the question has no mechanism behind it: a widget on a
homescreen page nobody is looking at is not distinguishable from one being read.
`AppWidgetProvider` has no visibility callback and no launcher reports one.
Reacting to the screen going on and off explicitly would cost more than it saves,
for the reason the next section gives.

## Why nothing wakes the device

`RTC` is non-wakeup, which is defined as delivered at the next moment the device
is awake. That is not a compromise, it is the requirement stated back: nobody is
looking at a homescreen widget while the screen is off, and a redraw that
happens the instant someone starts looking is what a wakeup would have bought at
a wakeup's price. Measured: screen off at 15:55:44, on at 16:00:44 — five
minutes of deferred ticks — and the dial was correct within about 13 seconds.

This is also why there is no screen-on receiver, which is the shape most widgets
of this kind reach for. `ACTION_SCREEN_ON` cannot be declared in a manifest at
all, so it needs a runtime `registerReceiver` from a process that is alive to
make the call. This app has no long-lived process: it is a receiver plus a demo
activity nobody has reason to leave running. Arranging one — a Service, or an
`Application` kept alive — would be a substantial new moving part to reproduce a
guarantee the alarm type already makes.

## Why no `BOOT_COMPLETED`

Alarms are memory-only, so a reboot clears the table and something has to arm
the first tick of the new boot. The plan assumed that had to be a
`BOOT_COMPLETED` receiver. Measuring first showed it did not: the host already
re-sends `APPWIDGET_ENABLED`, `APPWIDGET_UPDATE` and `APPWIDGET_UPDATE_OPTIONS`
to a provider with a pinned widget on every boot, and since every update arms
the tick, that update is the re-arm. Measured by polling `dumpsys activity
broadcasts` from the moment `sys.boot_completed` flipped, and again after the
tick was extracted: the tick was back as a single one-shot within about 30
seconds of boot.

The mechanism is `AppWidgetServiceImpl.sendEnableAndUpdateIntentLocked`, reached
from `ensureGroupStateLoadedLocked` when the service reloads persisted widget
state — which happens on user unlock, and therefore on every boot. It is named
here so a future reader can re-check it against AOSP instead of rebooting an
emulator and inferring.

It is an implementation detail, not a contract. `AppWidgetProvider` documents
`onEnabled` as sent when a widget is added for the first time and says nothing
about re-sending it on load. If a release ever stops, the failure mode is quiet:
the widget keeps showing the picture it had at shutdown and never changes again,
with nothing in a log to say so. A frozen dial reads as a working clock at a
glance, which is what makes it worth writing down. The re-check, when that day
comes, is a reboot and a look at `dumpsys alarm`.

Weighed against that, a `BOOT_COMPLETED` receiver costs a permission, a filter
and a process start on every boot of every device — including the boots where no
widget is pinned — to buy a re-arm that already happens. The cost is certain and
the benefit contingent, so it was not taken. `updatePeriodMillis` stays `0` for
a related reason: the platform will not deliver a periodic update more often
than once every 30 minutes, which is far too slow to be a tick, and as a safety
net it would spend a wakeup every half hour against a failure never observed.

Force-stop stays unrecoverable. `FLAG_STOPPED` blocks every broadcast to a
force-stopped app until the user launches it, so no mechanism — alarm, boot
receiver or host update — survives it. That is not a defect to fix.

## One rule, not two hooks

Every path that redraws the widget arms the next redraw: `onUpdate` and
`onAppWidgetOptionsChanged` both call `Tick.schedule`, `onDisabled` cancels, and
`onEnabled` has no override at all.

The alternative is to name one hook that arms the tick and then reason about
whether every path reaches it. That is what the code did before, arming in
`onEnabled` — and the reasoning was wrong in a way worth being precise about.
The tick was not in fact lost across reboots; the host's undocumented re-send
above kept arming it. So the fault was never a broken clock. It was an invariant
resting on a hook the platform documents as first-instance-only, held up by
behaviour the platform does not document at all: it worked by courtesy. One rule
that holds on every path is cheaper to keep true than an invariant whose
exceptions a reader must re-derive at each call site, and it does not depend on
the courtesy.

What makes the one rule safe is that arming is idempotent by identity rather
than by a flag: `Tick` builds the same `PendingIntent` every time, so
`AlarmManager` replaces the outstanding alarm instead of accumulating one per
call. The mechanics belong on `Tick.schedule`; what belongs here is that the
idempotence is load-bearing, and that anything changing the tick's `Intent`
breaks it. `AlarmManager` matches by `Intent.filterEquals`, so a changed intent
strands the alarm it meant to replace rather than replacing it — measured across
the upgrade that named the receiver, as two alarms in the table with the older
one beyond the reach of `Tick.cancel` until the next reboot.

## The two bitmap ceilings

There are two, they are easy to confuse, and only one of them is ours.

The host's is a budget on the whole `RemoteViews`.
`AppWidgetServiceImpl.computeMaximumWidgetBitmapMemory()` reads the real display
size and sets the limit to `6 * width * height` bytes, under the comment *"Cap
memory usage at 1.5 times the size of the display — 1.5 \* 4 bytes/pixel \* w \*
h ==> 6 \* w \* h"*. Checked against AOSP `main` on 2026-08-20, where this plan
had been carrying the figure as a recollection; it is exact. Nothing in this app
enforces it and nothing needs to, because the limit scales with the display and
a bitmap sized from a cell on that display cannot approach it. The hand-written
`453f * density` this widget used before candidate B could, which is the part
worth recording.

Ours is the clamp in `WidgetPicture.sideForCell`, holding the bitmap's side to
the display's smaller dimension. It exists because of the budget rather than
independently of it, but it is not what keeps the budget: an options bundle that
somehow reported an absurd cell would otherwise become a bitmap the host
refuses, and the clamp makes that a soft dial instead. It never binds in
practice, and never binding is the intended state.

## The receiver's name is not ours to change

`Tick` lives in the `widget` package and names the receiver, which lives in the
root package, so `widget` — previously a leaf — now points back at its parent.
Moving the receiver in beside `WidgetPicture` and `Tick` would make the mutual
reference intra-package for no change in design, and is the obvious tidy-up to
reach for.

Measured before deciding, on 2026-08-20. With a widget pinned and rendering, a
build that moved the class to
`…twentyfourhour.widget.TwentyFourHourClockWidgetResizable` was installed over
it with `install -r`. `dumpsys appwidget` then listed the provider under its new
name and listed no widgets at all; the homescreen showed bare wallpaper.
Reinstalling the original build did not bring it back — the instance is gone
from the host's database, and the user's only recovery is to add the widget
again by hand.

So the receiver's fully-qualified name is not an implementation detail. It is
the `ComponentName` the host has persisted for every pinned widget, and the
component every outstanding `PendingIntent` names. That makes it published API
with an installed base, and a rename a silent, unrecoverable deletion of every
user's widget. The package cycle is a real cost and it is much the smaller one.
The receiver stays where it is.

Note what made this worth measuring rather than reasoning about: the compiler
follows every reference to that name in the source, and none of the references
that matter are in the source.

## Consequences

The receiver keeps four overrides and no fields. Everything about when the
widget redraws is in `Tick`, a plain object with no lifecycle and nothing to
construct, so the schedule is assertable on the JVM: Robolectric's
`ShadowAlarmManager` exposes the type, trigger time, interval and operation, and
`ShadowPendingIntent` exposes the mutability flag. `TickTest` holds all five.
Three of them were wrong at some point in this candidate — the interval, the
target and the mutability flag — and each was wrong in a way no compiler could
see.

Exactness is assertable there too, and by luck rather than design.
`ShadowAlarmManager` records a window per alarm with `WINDOW_EXACT` and
`WINDOW_HEURISTIC` sentinels, and lets a test set what `canScheduleExactAlarms`
answers — so both branches are readable on the JVM without a device or a grant.
The tests ask which window was recorded rather than which method was called,
because it is the promise that matters and not the call that made it.

One thing those tests cannot hold: `ShadowPendingIntent` does not implement the
S+ mutability rule, so the missing-flag crash that made this widget unaddable
cannot be reproduced on the JVM. The flag is asserted; the throw is not. That
gap is why every slice that changed the tick was also run on a device.
