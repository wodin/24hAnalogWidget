---
status: accepted
amends: 0004
---

# A sun path is bands, not moments

A sun path used to be nine moments or null. It is now high noon and four
bands — the stretches of the day the sun spends below the horizon and below
each of the three twilight horizons — and it is null only at the poles in
their dark half of the year, where there is no meridian crossing to build a
day around.

ADR-0004 recorded the all-or-nothing rule and the surprise under it: from
about 48° the sun stops falling a full 18° below the horizon near midsummer,
so London had no astronomical twilight from mid-May to late July and, with no
path, drew the Placeholder for ten weeks. The Placeholder means "the sun
position is unknown". In a London June it is not unknown; one band is absent
and the other three are ordinary. This ADR retires the rule.

## Where the idea came from

Upstream came back to life in September 2026 with a Wear OS rewrite, and its
new `sun/` companion app computes each twilight as its own interval —
`{start, noon, end}` with `isAlwaysAbove()` and `isAlwaysBelow()` — rather
than as a set of moments that must all exist (xxv/24hAnalogWidget `d53ab4a`).
Nothing of upstream's code was taken: it is Wear-only, uses its own NOAA
solver where this fork keeps commons-suncalc, and expresses hours as doubles
where this fork keeps `ZonedDateTime`. What was taken is the shape — a band
per horizon, each able to say it never happened or lasted all day.

## A band can be half there

The thing the plan did not expect, and the reason the resolution is per side
rather than per band: the library answers the morning and the evening of a
day separately, and on the transition days either side of the gap they
disagree. London on 22 May 2026 rose through 18° below at 01:17 and that
evening never fell so far again — a morning crossing and no evening one.
`SuncalcAbsenceTest` pins this against the library directly, along with the
two facts beside it: a null crossing comes with exactly one of `isAlwaysUp`
and `isAlwaysDown`, and the flag respects the twilight angle asked for.

## A missing side runs to midnight

This is the one design decision in the change, and it has a conventional
answer. An almanac table prints a twilight that did not begin or end within
the date as a dash, and its graph shades to the edge of the day. So a band
with one crossing runs from that crossing to midnight: London on 22 May is
shaded for astronomical twilight from 00:00 to 01:17. Midnight is also a
point the dial already has, at its bottom.

Two alternatives were considered and are worse. Upstream's `noon ± 12h` and
this fork's own first draft, "the sun's low point", are the same shortcut
under two names; both claim London was not fully dark between midnight and
00:57 on that morning, which is false, and both drift from midnight by the
whole clock-to-sun offset — some three hours at Kashgar.

One guard goes with it. With one side missing, the crossing on the other must
itself fall within the day asked about, or there is no darkness today to
shade and the band is never. At the margin of polar day the last rise can sit
just before yesterday's midnight; a band run from midnight to it would shade
the whole dial. A day with both crossings is not clipped, because its moments
are allowed to spill into the next date — ADR-0004's *the sun's day, not the
calendar's* — and the dial reads them by time alone.

## The shape is carried, not inferred

A band that never happens and one that lasts all day both have no moments,
and on the dial both begin and end at one point. The painter's sweep is
`(360 + end − start) % 360`, which is 0 for both, so a painter working from
angles alone would draw nothing for polar night. `Band` therefore carries its
shape, and the painter asks for it before asking for a moment: never is
skipped, all day is a full-circle coat, an arc is drawn as before.

## What changed on the dial

- London, mid-May to late July: three bands instead of the Placeholder.
- Above the polar circle in winter: a fully shaded dial, the twilights nested
  on it, and the high noon mark on a sun that never clears the horizon — it
  marks the meridian crossing, not the middle of a day.
- Above the polar circle in summer: an unshaded dial with only the mark. A cue
  that says "the sun is up all day" in words is a possible later improvement.
- The Placeholder now means only what the glossary always said it meant: no
  observer, or an observer at a pole in the dark.

## What did not change

commons-suncalc stays, for ADR-0004's reasons. The almanac test still pins
the same nine published moments to the same two minutes; they are reached
through `night().morning()` rather than `sunrise()`, and nothing else. A day
with all its crossings produces the same arcs it did.

## Consequences

`SunPath` exposes `highNoon()` and four `Band`s. Nine accessors are gone; so
is `anyMissing`. `Almanac.band()` is the single place a missing crossing is
resolved, and the only place that still names a suncalc type apart from the
two tests that read the library on purpose.

ADR-0004's section *Absence became a value, so the catch went* is amended by
this one: the catch is still gone and null is still how the library reports a
place without a crossing, but a path no longer answers eight moments with
nothing. Its claim that London "draws the Placeholder, which is the honest
answer" was honest for the seam it had; this ADR gives the seam a way to say
"these three".
