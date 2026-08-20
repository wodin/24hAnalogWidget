---
status: accepted
---

# The Dial is not a View

Drawing the dial used to mean owning a `View`. `Analog24HClock` extended `View`
and kept the time, the overlays and the scale as fields, so the only way to get
pixels out of it was to hold one and drive it. The homescreen widget, which has
no layout and no window, did that by hand: construct the View, call the
*protected* `onSizeChanged` from outside to arm an internal cache, `measure()`
it with a raw `int` that is not a `MeasureSpec`, `layout()` it, then ask for its
drawing cache. Every one of those steps is a lie told to a lifecycle that is not
running.

The dial is now a plain object. `Dial.draw(Canvas, Rect, Instant, ZoneId)`
paints one moment into one rectangle, and `getIntrinsicSize()` reports the
square the artwork was drawn for. A `Dial` is immutable, holds no lifecycle and
keeps no cache, so one built per widget update has nothing to tear down and
nowhere for staleness to live. Time is a parameter rather than a field, which
is what lets the same Dial be drawn at one instant and then at another with
nothing in between to invalidate.

## Why the View survives

`DialView` is thin, and getting thinner was the point. It holds a `Dial`, a
`Rect` and an optional pinned `Instant`, sizes itself from
`getIntrinsicSize()`, and forwards `onDraw` to `Dial.draw` with its own bounds
and either that instant or the present moment. A reader looking
for code smells will find a Middle Man and reach for the delete key.

The reason to keep it is not that it does enough work to earn its file. It is
that a second host is what keeps `draw` shaped like a *dial*. An interface with
one caller cannot be distinguished from that caller's convenience, and the
pressure to fold the caller's assumptions back into the callee is invisible
while there is only one. With the receiver alone, nothing would object if the
bounds became a `Bitmap`, or the size an `int` because the receiver's target is
always square, or the instant vanished because the receiver always draws *now*.
Each of those is a step back toward an object that can only be used the one way
it is already used.

The two hosts disagree in exactly the places that matter. The View learns its
bounds late, from a layout it does not control, and redraws the same instance
many times at whatever moment each draw happens; the receiver picks its own
size, draws once, and throws the result at a host process. What survives both
disagreements is the parameter list `draw` now has.

So `DialView` is a design check that costs one small class, and deleting it is a
decision about the interface, not a tidy-up. Should the demo activity ever go,
the honest move is to notice that `Dial` has one caller again and say so here.

## Consequences

The widget's bitmap is now `getIntrinsicSize()` square rather than the
hand-written `453f * density`. The Face is 196dp, which the 440dpi emulator the
check ran on resolves to 539px, against the 1245px that constant asked for: a
dial occupying 43% of its own bitmap and transparent everywhere else. Before and
after, in the same pinned 2×2 cell 360px wide, the dial went from 148px across
to 342px. The 5% of the width it still does not cover is the margin the artwork
carries around itself — its painted circle ends at 187px of its 196dp. That is
width only: the cell was 360×606 and a dial is square, so nearly half its
height stays empty. No sizing decision closes that — a round dial in a cell
taller than it is wide leaves the difference empty whatever bitmap it is drawn
into. What the receiver's ignorance of the cell does cost is resolution: the
host had the cell as 130×220dp and was never asked, so the dial is drawn at
539px to be shown at 342px.

Rendering became testable off-device, which it had never been. Robolectric with
native graphics draws real pixels on the JVM, and the tests probe them at fixed
instants — a pixel on the hour hand's axis at one time, bare Face at another.
That net was built before the extraction started and is what makes the claim
"the pixels did not change" checkable rather than hopeful.

The before/after check could not use the ordinary path of adding the widget from
the picker. Adding it fires `onEnabled`, which schedules the tick through a
`PendingIntent` built with neither `FLAG_IMMUTABLE` nor `FLAG_MUTABLE` — fatal
when targeting S ≥ 31 — so the receiver's process dies before `onUpdate` renders
anything and the host is left showing the static `initialLayout` artwork. The
flag is untouched here, because the tick is not this decision's to rewrite, so
both builds do it and both were driven instead through `ACTION_CLOCK_UPDATE`,
which reaches `onUpdate` without passing through `onEnabled`.

Both halves of that obstacle have since gone, in candidate E: `b816892` gave the
`PendingIntent` `FLAG_IMMUTABLE`, so adding the widget renders it, and S3 moved
the arming into `onUpdate` and deleted the `onEnabled` override altogether. The
paragraph above records why B's check took the route it did, not how the
receiver behaves now.
