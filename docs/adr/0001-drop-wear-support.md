---
status: accepted
---

# Drop Android Wear support

Upstream shipped a watch face alongside the homescreen widget. This fork deletes
the `wear` and `wearphone` modules and keeps only the phone widget, because
nobody working on this fork owns a watch — which makes the face both unmotivated
and unverifiable.

Keeping it was not free. The face was the only code touching `FusedLocationApi`,
which no longer exists in `play-services-location:21.4.0`; only
`FusedLocationProviderClient` does. Retaining Wear therefore meant rewriting its
location handling — 3 `FusedLocationApi` call sites, 13 location-handling lines,
all in `WearFace.java` — inside a modernisation whose exit criterion is merely
that the project assembles, and with no device on which to tell a correct
rewrite from a merely plausible one. Wear was also the only thing forcing
Kotlin, via the AndroidX watch face API. Deleting it collapsed the module graph
from four modules to one.

## Consequences

The source is not lost: `upstream/main` holds xxv's copy permanently, and this
fork's own history retains the modules up to their removal. Restoring the face
means reviving a module rather than rewriting one — but it also means doing the
location migration deferred here.

The watch face was the only caller of `SunPositionOverlay.setLocation`. The
phone widget never called it even upstream, so it has always drawn the
Placeholder; what this deletion removed is the only code that ever supplied a
real location. The manifest's two location permissions are consequently unused.
`DateOverlay` and `DebugOverlay` likewise lost their only callers. Both were
left in the tree here and deleted in the lint phase that followed, once the
triage confirmed no later candidate wanted them.
