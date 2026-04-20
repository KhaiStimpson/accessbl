# Implementation Plan — AccessSwitch

**Solo developer · No fixed deadline · Full v1 scope**
**Test hardware: Phone + Chromebook (no physical switch — simulate via keyboard keys)**
**Distribution: Sideload (testing) → Google Play Store (release)**

---

## Phase 0 — Project Scaffolding (Week 1)

- [ ] Initialize Android project (Kotlin DSL, min SDK 26, target SDK 35)
- [ ] Configure Hilt dependency injection
- [ ] Configure Jetpack Compose, DataStore, Navigation
- [ ] Set up project structure per TDD §8 (`input/`, `scanning/`, `overlay/`, `phone/`, `netflix/`, `nav/`, `settings/`, `switchscreen/`, `di/`)
- [ ] Create `AccessSwitchAccessibilityService` skeleton + XML config
- [ ] Create `AppSettings` data class and `SettingsRepository` with DataStore
- [ ] Set up unit test framework (JUnit 5, MockK, Turbine for Flow testing)
- [ ] Set up instrumentation test framework (Compose testing)
- [ ] Configure CI (GitHub Actions: build + unit tests on push)

**Exit criteria:** Project builds, empty app launches, Hilt compiles, CI green.

---

## Phase 1 — Scanning Engine + HW Switch Input (Weeks 2–4)

Core scanning engine is the foundation everything else builds on.

### 1A — ScanningEngine (Week 2)

- [ ] Implement `ScanItem` data class with id, label, icon, action, children
- [ ] Implement `ScanMode` enum (AUTO, STEP, INVERSE, ROW_COLUMN)
- [ ] Implement `ScanningEngine` state machine (IDLE → SCANNING → SELECTED → IDLE)
- [ ] Implement timer-driven auto-scan with configurable interval (500–8000ms)
- [ ] Implement step-scan (Switch 1 = advance, Switch 2 = select)
- [ ] Implement inverse-scan (hold = scan, release = select)
- [ ] Implement row-column scanning (first scan rows, press enters row, scan columns)
- [ ] Expose `StateFlow<ScanState>` with current highlighted index
- [ ] Implement scan loop count + auto-stop
- [ ] Implement pause/resume (screen off detection)
- [ ] Unit tests: all state transitions, timer accuracy ±50ms, loop exhaustion

### 1B — SwitchInputHub + HwSwitchListener (Week 3)

- [ ] Implement `SwitchEvent`, `SwitchSource`, `SwitchId` data types
- [ ] Implement `SwitchInputHub` — merges all input sources, routes to `ScanningEngine`
- [ ] Implement cross-source deduplication (debounce window)
- [ ] Implement `HwSwitchListener` in `AccessSwitchAccessibilityService.onKeyEvent()`
- [ ] Consume switch key events, route to `SwitchInputHub`
- [ ] Detect switch connection/disconnection via `InputManager`
- [ ] Emit `SwitchSourceDisconnectedEvent` on BT switch loss
- [ ] Store switch keycodes in `SettingsRepository`
- [ ] Unit tests: dedup logic, mixed input routing, keycode mapping

### 1C — ScanningOverlayService (Week 4)

- [ ] Create `ScanningOverlayService` with `TYPE_ACCESSIBILITY_OVERLAY` window
- [ ] Render Compose-based grid of `ScanItem` tiles inside `ComposeView` in `WindowManager`
- [ ] Observe `ScanningEngine.StateFlow` — render highlight ring on current item
- [ ] Implement highlight color/style from settings (BORDER, FILL, BOTH)
- [ ] Implement haptic feedback on selection
- [ ] Implement audio feedback on highlight advance (optional, from settings)
- [ ] Test overlay renders above all apps including lock screen
- [ ] Minimum tile size 120×120dp enforced
- [ ] Instrumentation tests: correct tile count, highlight advances correctly

**Switch simulation:** No physical switch available. Use any USB/BT keyboard key (e.g., Space = Switch 1, Enter = Switch 2) to simulate switch input during all development and testing. The `HwSwitchListener` already captures arbitrary keycodes via `AccessibilityService.onKeyEvent()`, so keyboard keys are functionally identical to a real switch. Acquire a real switch for final validation before release.

**Exit criteria:** Keyboard key presses (simulating switches) drive scanning overlay on real device. Auto-scan and step-scan work end-to-end.

---

## Phase 2 — OS Navigation + Settings UI (Weeks 5–6)

### 2A — NavController (Week 5)

- [ ] Implement `NavController` wrapping `performGlobalAction`
- [ ] Support: Home, Back, Recents, Notifications, Quick Settings
- [ ] Build `ScanItem` list for nav panel
- [ ] Wire nav panel into overlay — navigable via scanning

### 2B — Main Menu + Settings (Weeks 5–6)

- [ ] Implement main menu panel: 2×3 grid (Phone, Netflix, Nav, Notifications, Settings, Back)
- [ ] Main menu auto-focuses and begins scanning on launch
- [ ] Implement settings UI (Jetpack Compose + Navigation):
  - Scan mode selection
  - Scan speed slider
  - Switch 1/2 keycode configuration (key capture dialog)
  - Debounce duration
  - Highlight color/style
  - Audio/haptic feedback toggles
  - Scan loop count
  - Favourite contacts management
  - Phone Switch Screen zone layout selector
  - Zone-to-switch mapping
  - BT HID mode enable/disable
  - BT HID keycode configuration
  - Live input source status indicator
- [ ] Implement PIN-lock for settings screen
- [ ] Settings export/import (JSON) — low priority, do last

**Exit criteria:** User can navigate Android OS entirely via scanning + external switch. Caregiver can configure all settings.

---

## Phase 3 — Phone Calling (Weeks 7–9)

### 3A — PhoneController (Weeks 7–8)

- [ ] Query `ContactsContract` for contacts + favourites
- [ ] Build scannable contact list (`ScanItem` list)
- [ ] Filter by favourites (from settings)
- [ ] Initiate outgoing call via `TelecomManager.placeCall()`
- [ ] Implement `InCallService` for incoming call handling (answer/decline)
- [ ] Expose `CallState` as `StateFlow` (Idle, Ringing, Active, Ended)
- [ ] Build in-call overlay: End Call, Mute, Speaker, Volume Up/Down
- [ ] Auto-surface in-call overlay when call is active
- [ ] Runtime permission requests: `READ_CONTACTS`, `CALL_PHONE`, `READ_PHONE_STATE`

### 3B — Phone Panel UI (Week 9)

- [ ] Build phone panel: 3×3 contacts grid + call controls row
- [ ] Incoming call overlay: Answer / Decline (scannable)
- [ ] Scannable numeric dialpad (Should Have)
- [ ] Recent calls list (Should Have)
- [ ] Test end-to-end: select contact → call → end call

**Exit criteria:** User can make and receive phone calls entirely via scanning.

---

## Phase 4 — Netflix Control (Weeks 10–12)

### 4A — NetflixController (Weeks 10–11)

- [ ] Implement mode detection: subscribe to `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`
- [ ] Detect Netflix package (`com.netflix.mediaclient`)
- [ ] Distinguish browse vs. playback activity via `className` heuristics
- [ ] Implement fallback: manual mode toggle scan tile
- [ ] Browse mode: inject D-pad key events (Up/Down/Left/Right/Center/Back)
- [ ] Playback mode: Play/Pause, FF, Rewind, Volume Up/Down, Stop
- [ ] Volume control via `AudioManager.adjustStreamVolume`
- [ ] Launch Netflix via package intent

### 4B — Netflix Panel UI (Week 12)

- [ ] Build browse panel: 3×3 D-pad grid
- [ ] Build playback panel: media controls row
- [ ] Auto-switch panels based on mode detection
- [ ] "Back to Menu" tile always present
- [ ] Ensure overlay doesn't block subtitles (positioning)
- [ ] Test on real Netflix app — browse → select → play → controls → exit

**Exit criteria:** User can browse, select, and control Netflix playback entirely via scanning.

---

## Phase 5 — Phone-as-Switch (Weeks 13–16)

### 5A — Local Touch Mode (Week 13)

- [ ] Implement `SwitchScreenActivity` — full-screen touch UI
- [ ] Implement all 4 zone layouts: `FULL_SCREEN`, `LEFT_RIGHT`, `TOP_BOTTOM`, `ASYMMETRIC_80_20`
- [ ] Zone boundary calculation + hit testing
- [ ] Large, high-contrast zone labels ("NEXT ▶", "✓ SELECT")
- [ ] Implement `PhoneSwitchTouchListener` — `ACTION_DOWN` only, ignore move/up
- [ ] Route events to `SwitchInputHub.onRawEvent(..., PHONE_TOUCH_LOCAL)`
- [ ] Haptic feedback (< 16ms, UI thread)
- [ ] Visual flash feedback on tap
- [ ] `FLAG_KEEP_SCREEN_ON` on Switch Screen window
- [ ] Foreground service to keep Switch Screen alive
- [ ] Test: all 4 layouts, zone boundaries, feedback

### 5B — BT HID Remote Mode (Weeks 14–15)

- [ ] Implement `BluetoothHidDeviceService`
- [ ] Register HID keyboard descriptor with BT stack
- [ ] Implement `sendKeyTap()` — key-down + immediate key-up report
- [ ] Expose `StateFlow<BtHidState>` (disconnected, connecting, connected)
- [ ] Implement pairing flow (caregiver-facing):
  1. Enable BT discoverability
  2. Register HID profile
  3. Chromebook pairs (appears as "AccessSwitch Keyboard")
  4. Store pairing; auto-reconnect on subsequent sessions
- [ ] Configurable keycodes per zone (from settings)
- [ ] Runtime permissions: `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` (API 31+) or legacy BT permissions (API 28–30)
- [ ] Conditional `ACCESS_FINE_LOCATION` for API 28–30
- [ ] Test: pair phone → Chromebook, verify keycode received

### 5C — Combined Input + Edge Cases (Week 16)

- [ ] Test external switch + phone touch simultaneously — both fire correctly
- [ ] Test cross-source dedup — accidental double-tap from both sources
- [ ] Test phone calling while Switch Screen active — overlay coexistence
- [ ] Test BT HID + phone call simultaneously — audio routing, no HID dropout
- [ ] Test BT switch disconnect → phone switch fallback
- [ ] Measure BT HID latency (target < 150ms, 95th percentile)
- [ ] Battery drain measurement (BT HID foreground service, 4-hour session, target < 5%/hour)

**Exit criteria:** Phone works as a local or remote switch. Mixed input works. Latency and battery targets met.

---

## Phase 6 — Polish, Testing & Hardening (Weeks 17–19)

### 6A — Comprehensive Testing

- [ ] Full manual test matrix (TDD §7.4 — adapted: use keyboard keys to simulate switch input; defer real-switch-specific tests to when hardware is available)
- [ ] Unit test coverage: ScanningEngine, SwitchInputHub, SwitchZoneLayout, SettingsRepository, PhoneController
- [ ] Instrumentation tests: overlay rendering, scanning highlight, Switch Screen zones, PIN lock/unlock
- [ ] Integration tests: end-to-end call flow, Netflix D-pad injection, BT HID pairing
- [ ] Chrome OS specific: test `performGlobalAction` availability in ARC++
- [ ] Verify Netflix package name + mode detection on current Netflix version

### 6B — UX Polish

- [ ] WCAG AA compliance check: 4.5:1 contrast for text/icons, 7:1 for highlight
- [ ] Error states: BT disconnect banner, missing permissions guidance, no SIM/calling unavailable
- [ ] Graceful degradation: hide phone tile on Chromebook if calling unavailable
- [ ] Hide BT HID option if `BluetoothHidDevice` API unavailable on device
- [ ] Optional Switch Screen lock toggle (prevent accidental mode change)
- [ ] Auto-brightness override for Switch Screen zones (visible in dark)

### 6C — Performance & Stability

- [ ] Profile scanning timer accuracy under load
- [ ] Memory leak testing (long-running overlay service)
- [ ] ANR prevention: ensure all BT and system API calls are off main thread
- [ ] Crash reporting integration (Firebase Crashlytics or similar)
- [ ] Target: > 99.5% crash-free sessions

**Exit criteria:** All manual test scenarios pass. No P0 bugs. Performance targets met.

---

## Phase 7 — Release (Week 20+)

### 7A — Pre-Release

- [ ] Write accessibility service description string (shown in Android Settings)
- [ ] Create app listing assets (icon, screenshots, feature graphic)
- [ ] Write Play Store description emphasizing accessibility use case
- [ ] Privacy policy (contacts read, no data leaves device)
- [ ] Internal testing track on Google Play Console
- [ ] Distribute test APK to 2–3 real users / caregivers for feedback

### 7B — Release

- [ ] Address feedback from internal testers
- [ ] Closed testing track → Open testing (if appropriate)
- [ ] Production release on Google Play Store
- [ ] Sideload APK available on project website/GitHub

### 7C — Post-Release Monitoring

- [ ] Monitor crash reports
- [ ] Monitor Netflix compatibility (check on each Netflix update)
- [ ] Respond to accessibility-specific feedback

---

## Dependency Graph

```
Phase 0 (Scaffold)
  └── Phase 1 (Scanning + HW Switch) ← CRITICAL PATH
        ├── Phase 2 (Nav + Settings + Main Menu)
        │     ├── Phase 3 (Phone Calling)
        │     └── Phase 4 (Netflix Control)
        └── Phase 5 (Phone-as-Switch)
              └── Phase 5C (Combined Input)
  Phase 6 (Polish) ← after Phases 2–5
    └── Phase 7 (Release)
```

Phases 3, 4, and 5 can be developed in parallel after Phase 2 if desired. Phase 1 is the critical path — everything depends on the scanning engine and input infrastructure.

---

## Key Risks to Monitor Throughout

| Risk | Checkpoint |
|---|---|
| Compose-in-WindowManager stability | Phase 1C — evaluate early; fall back to View-based overlay if issues |
| Chrome OS AccessibilityService limitations | Phase 2A — test on real Chromebook immediately |
| Netflix UI changes break detection | Phase 4A — implement manual toggle fallback from day one |
| BT HID OEM compatibility | Phase 5B — test on 3+ phone models |
| BT HID latency | Phase 5C — measure; investigate BLE HID if > 200ms |
| No physical switch | All phases — simulate with keyboard keys; acquire real switch before Phase 7 release |

---

## Open Questions (Resolve During Development)

1. **`InCallService` vs `TelecomManager`** — evaluate in Phase 3A; prefer `InCallService` for richer control, fall back to `Intent.ACTION_CALL` if permissions problematic
2. **Chromebook calling** — detect and hide phone tile if no SIM/Phone Hub available (Phase 6B)
3. **Tecla BLE switch** — standard BLE HID should suffice; test in Phase 1B, bundle SDK only if needed
4. **SOS tile** — defer to post-v1 unless user testing reveals demand
5. **Settings profile export** — implement last (Phase 2B), low priority
