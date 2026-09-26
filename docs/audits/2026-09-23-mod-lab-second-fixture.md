# Mod Lab Second Live Fixture

Pinned toolkit commit: `360ace2`. Scenario: `vanilla-written-book`.
The disposable copy was created with the toolkit fixture command, then validated
by its doctor command against the actual Java PID, loopback bridge listener,
game directory, fixture marker, world, survival mode, framebuffer and launch log.
The world default mode was explicitly aligned to survival before doctor passed.

This is a vanilla-only scenario in the existing Aura-installed development
runtime, not a zero-mod installation or a packaged-client compatibility claim.
Startup log and title identify Minecraft 1.21.1 and Fabric; the runtime includes
Aura, Fabric API, Patchouli and the locally hardened development bridge.

## Failure and Correction

- `build/qa-audit/mod-lab-vanilla-client/baseline`: empty selected hand; using it
  leaves no GUI. Capture correctly reports a screen-class assertion failure.
- `build/qa-audit/mod-lab-vanilla-client/corrected-book`: a vanilla written book
  is supplied, and the same use-item scenario opens `BookViewScreen`. Capture
  exits successfully with `captured`, not an automatic semantic pass.
- The intervening `corrected` directory is an invalid setup attempt: its item
  command was rejected because the preceding capture had exited control mode.
  Its unchanged-input failure is preserved and excluded from the corrected pair.
- Both valid reports acknowledge control-mode exit and contain 1280x720 images.
- Independent Astra image review confirms bare hand/no book in both baseline
  images, a held book in the corrected before image, and readable "Control.
  Vanilla written-book fixture." text, "Page 1 of 1" and Done without clipping
  in the corrected after image. Blank/duplicate-widget telemetry does not mean
  visible duplicate controls; the screenshot is authoritative for appearance.

## Resource and Shutdown Evidence

Client wrapper `mod-lab-vanilla-client` records normal save/quit and exit 0.
Peak working set: 1318.8 MiB. Peak private allocation: 1851.2 MiB. The client
guard was 3200 MiB; it did not trip. One-second sampling is not an OS hard cap.
Codex/ChatGPT and other agent hosts are excluded from the user's tooling budget.
The heavier combined desktop metric in the raw report is informational only.

Raw identity, local paths, world copy, screenshots and bridge data remain ignored
local QA artifacts. Only sanitized findings belong in the public toolkit repo.
