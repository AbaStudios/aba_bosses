# Audio Flagger

A zero-dependency web app for marking beat timestamps on a song so lasers / boss
attacks can be synced to them. Just open `index.html` in a browser — no build,
no server.

## Workflow

1. **Open audio** (or drag & drop a file, e.g. `Project-DOWNFALL-Soundtrack.mp3`).
2. Play the track (Space) and tap **F** on each beat to drop a flag. Slow the
   playback speed down for tighter accuracy.
3. Drag flags (or use `,` / `.` for ±10 ms nudges, Shift for ±1 ms) to fine-tune.
   The "selected ms" box lets you type an exact millisecond value.
4. **Export CSV** or **Export JSON** to get the list of beat times in ms.

Flags are also autosaved to `localStorage` per audio-file name, so reopening the
same song restores your work.

## Flags

- Created with **F** at the playhead; deleted with **Delete** while selected.
- Each flag has a unique id (random UUID by default) and one of four colors:
  red, yellow, green, blue (pick with **1–4** or the toolbar swatches;
  double-click a flag to cycle its color). Use colors to separate tracks of
  events, e.g. red = boss attack, yellow = laser.
- During playback, crossing a flag fires a screen-edge pulse vignette in that
  flag's color so you can eyeball sync accuracy.

## Import / export formats

**CSV** — `ID,Time (ms),Time (formatted),Color Label` (header optional):

```csv
ID,Time (ms),Time (formatted),Color Label
4474c8a1-2d6f-48d4-8497-e678413f0fd9,359.34,00:00.359,yellow
```

Import also accepts a bare one-column list of ms values.

**JSON** — `{ "flags": [ { "id", "ms", "color" } ] }`, a bare array of those
objects, or a bare array of ms numbers:

```json
[359.34, 719, 1079]
```

## Shortcuts

| Key | Action |
| --- | --- |
| Space | play / pause |
| F | add flag at playhead |
| Delete / Backspace | delete selected flag |
| 1–4 | set color (selected flag + new-flag default) |
| , / . | nudge selected flag ±10 ms (Shift: ±1 ms) |
| ← / → | seek 1 s (Shift: 5 s) |
| Home | jump to start |
| wheel | zoom (Shift+wheel: pan) |
| Ctrl+Z | undo flag changes |
| Esc | deselect |
