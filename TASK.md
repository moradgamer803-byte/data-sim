# TASK — Data SIM app (com.zyvo.datasim)

GOAL: one-tap QS tile + tiny app that switches the phone's DEFAULT DATA SIM
between Grameenphone (subId 2, slot 0) and Robi (subId 1, slot 1) headlessly
via root (`settings put global multi_sim_data_call` + `svc data disable/enable`).
Delivered as an APK through GitHub Actions.

## Steps
- [x] Confirm the headless switch command works and is readable back (`dumpsys isub`)
- [x] Rule out CLI alternatives (cmd phone / uni_phone / isub / statusbar tile)
- [x] Save the dual-SIM findings to AGENTS.md
- [x] Scaffold `~/datasim` (gradle, manifest, Java, res, CI workflow)
- [x] Static verification: XML parse + R refs + findViewById ids (all pass)
- [x] javac type-check against generated Android stubs (0 errors)
- [x] UI preview page with 3 directions → user picked **3 Warm Guard** (already the coded palette)
- [x] Apply pick / re-run checks (palette matched; no res change needed)
- [x] git init + push to GitHub → repo `moradgamer803-byte/data-sim`
- [x] CI run #1 failed: `TileService.onLongClick()` is API 35+ → override removed (phone is Android 12 anyway)
- [ ] Actions build green → deliver repo link + APK release URL

## Key facts
- SIM map: subId 2 = Grameenphone slot 0 (default data) · subId 1 = Robi slot 1.
- isub lists stale subs 3–7 with simSlotIndex=-1 → filter `slot >= 0`.
- Robi is 2G-only at home → switch succeeds but Robi may not attach data (coverage).
- Tile: tap = data off? turn on : switch to the other SIM; long-press = open picker.
- Build template copied from ~/routercontrol (AGP 8.5.2, Java 17, Material 1.12.0,
  workflow softprops/action-gh-release tag `apk-${{ github.run_number }}`).

NOW: fix pushed → waiting on CI run #2 → deliver repo + APK links.

## Lessons
- `TileService.onLongClick()` does NOT exist in compileSdk 34 (added API 35) → never
  `@Override` it; Android 12 tiles have no long-press at all. Picker opens from the
  app icon (tile opens it too when root is missing).
