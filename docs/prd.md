# Product Requirements Document
## AccessSwitch — Android Accessibility App

**Version:** 0.2 (Draft)
**Status:** Brainstorm / Pre-Development
**Last Updated:** 2026-04-20
**Author:** TBD

---

## 1. Overview

### 1.1 Product Summary

AccessSwitch is an Android accessibility application that enables users with motor impairments to independently operate their Android device — including making phone calls, navigating the OS, and controlling apps like Netflix — using an on-screen scanning interface driven by one or more input sources.

The app supports three input source types, which can be used simultaneously:
- **External accessibility switch** (Bluetooth or USB HID hardware button)
- **Phone-as-switch** (an Android phone screen used as a large, soft, touch-zone switch)
- **Combined** (e.g., an external switch for "advance" + phone screen for "select")

The app targets two primary form factors:
- **Chromebook** (running Android apps via the Google Play Store) — recommended primary display
- **Android Phone** (standard Android device) — works standalone, and/or acts as a remote switch for the Chromebook

### 1.2 Problem Statement

Users with limited motor control (e.g., due to ALS, cerebral palsy, spinal cord injury, or similar conditions) cannot reliably use touchscreens or standard keyboards. Existing Android switch access features are fragmented, require significant technical setup, and provide poor support for third-party app control (especially streaming apps like Netflix). There is no single, polished app that combines calling, OS navigation, and media control in a switch-optimized interface.

### 1.3 Goals

- Enable **single-switch or two-switch** operation for all core device functions
- Support **phone-as-switch** — an Android phone screen acting as one or two large touch-zone switches
- Allow **external switch + phone-as-switch simultaneously** (mixed input)
- Provide a clear, high-contrast **scanning UI** tuned for low motor precision
- Support **phone calls** (dial, answer, end, contacts)
- Enable **Android OS navigation** (home, back, app switching, notifications)
- Enable **Netflix control** (browse, play, pause, seek, volume)
- Run reliably on both **Chromebook** and **Android phone**

### 1.4 Non-Goals (v1.0)

- iOS support
- Eye-tracking input (future consideration)
- Support for apps other than Netflix (expandable in later versions)
- Custom voice output / AAC (Augmentative and Alternative Communication)
- Cloud sync of user profiles

---

## 2. Target Users

### 2.1 Primary User

> **"The Switch User"** — A person with a significant motor impairment who relies on one or two physical switches to interact with technology. May have full cognitive capacity. Values independence, reliability, and speed.

**Characteristics:**
- May use 1–2 external accessibility switches (e.g., AbleNet, Zygo, Tecla) and/or their phone as a touch switch
- May have limited head/limb control
- Possibly uses a powered wheelchair mount for both the Chromebook and phone
- Device may be a shared Chromebook (school/care facility) or personal Android phone

### 2.2 Secondary Users

- **Caregivers / Setup Administrators** — Configure the app, set up contacts, customise scanning speed and patterns. Need a simple settings UI accessible via touchscreen.
- **Clinicians / AAC Specialists** — May prescribe or configure the app for clients. Need profile export/import.

---

## 3. Key Concepts

### 3.1 Switch Input

A **switch input source** is anything that sends a discrete signal to trigger a scan advance or selection. The app supports three categories of input source, which can be used together simultaneously:

**Category A — External Hardware Switch**

A physical button that connects to the device. Common connection methods:

| Method | Notes |
|---|---|
| Bluetooth HID (keyboard) | Most common; sends keycode (e.g., Space, Enter) |
| USB HID (via OTG adapter) | Wired; reliable for Chromebook |
| 3.5mm Audio Jack | Some legacy switches; requires audio jack detection |
| BLE Custom Profile | Some modern switches (e.g., Tecla) |

The app must support **remappable switch keycodes** so any switch hardware can be used.

**Category B — Phone-as-Switch**

An Android phone acts as one or two large soft-touch switches. The phone runs a companion "Switch Screen" mode — a full-screen touch UI that sends switch events to the scanning engine. This works in two deployment scenarios:

| Scenario | How it works |
|---|---|
| **Phone is the device** | The phone runs AccessSwitch; full-screen transparent overlay captures taps and feeds them directly into the `ScanningEngine` |
| **Phone as remote switch for Chromebook** | Phone uses `BluetoothHidDevice` API (Android 9+) to present itself as a BT keyboard to the Chromebook; taps emit configured keycodes that the Chromebook's `SwitchInputService` receives |

**Phone Switch Screen layout (configurable):**

| Layout | Zone 1 | Zone 2 |
|---|---|---|
| Full screen (1-switch) | Entire screen = Switch 1 | — |
| Left / Right split | Left half = Switch 1 (Advance) | Right half = Switch 2 (Select) |
| Top / Bottom split | Top half = Switch 1 | Bottom half = Switch 2 |
| Large / Small (asymmetric) | 80% screen = Switch 1 | 20% strip = Switch 2 |

The phone Switch Screen displays large, high-contrast labelled zones (e.g., "NEXT ▶" / "✓ SELECT") and provides vibration + visual flash feedback on each press, confirming the action registered.

**Category C — Combined (External + Phone simultaneously)**

Both input sources are active at the same time and feed into the same `ScanningEngine`. Example use cases:

- External switch mounted on wheelchair arm = Switch 1 (advance)
- Phone screen on lap tray = Switch 2 (select)
- External switch as sole input; phone screen as backup / emergency select

The scanning engine treats all active input sources as equivalent — a press from any source fires the configured action for that switch number.

### 3.2 Scanning

**Scanning** is the process of highlighting elements on screen one-by-one (or in groups) until the user activates their switch to select the highlighted item.

**Scanning modes to support:**

| Mode | Description |
|---|---|
| **Auto-scan** | App cycles through items automatically; switch press = select |
| **Step scan** | Switch 1 = advance; Switch 2 = select |
| **Inverse scan** | Switch held = scan; switch released = select |
| **Two-switch row-column** | Switch 1 scans rows; Switch 2 scans columns |

**Scanning parameters (configurable):**
- Scan speed (delay between highlights, e.g., 0.5s – 5s)
- Number of loops before stopping
- Auto-restart after selection
- Auditory and/or visual feedback on selection

### 3.3 Scanning Groups

Items are organised into logical **groups** (cells in a grid) that can be scanned as a group first (row/column scanning), then individually within the group.

---

## 4. Features & Requirements

### 4.1 Core Scanning Engine

| ID | Requirement | Priority |
|---|---|---|
| SCN-01 | App must implement auto-scan, step-scan, and inverse-scan modes | Must Have |
| SCN-02 | Scan speed must be configurable from 0.5s to 8s in 0.1s increments | Must Have |
| SCN-03 | Visual highlight must be high-contrast (configurable colour, e.g., yellow, red, blue) | Must Have |
| SCN-04 | Scanning must restart from the beginning after a selection | Must Have |
| SCN-05 | App must support row-column scanning for grid layouts | Should Have |
| SCN-06 | Optional auditory feedback (click or spoken label) on each highlighted item | Should Have |
| SCN-07 | Optional switch vibration/haptic feedback on selection | Should Have |
| SCN-08 | App must pause scanning when the device screen is off | Must Have |

### 4.2 Switch Input — External Hardware

| ID | Requirement | Priority |
|---|---|---|
| SWI-01 | App must detect key events from Bluetooth HID devices | Must Have |
| SWI-02 | App must support USB HID switches via OTG | Must Have |
| SWI-03 | Switch keycode must be remappable in settings | Must Have |
| SWI-04 | Support for 1-switch and 2-switch configurations | Must Have |
| SWI-05 | Switch debounce setting to prevent accidental double-presses | Must Have |
| SWI-06 | Support 3.5mm audio jack switches (detect plug-in event) | Nice to Have |
| SWI-07 | App must detect external switch BT disconnection and alert the user with an overlay warning | Must Have |
| SWI-08 | On BT switch disconnection, scanning must pause and resume automatically when reconnected | Must Have |

### 4.3 Switch Input — Phone as Switch

| ID | Requirement | Priority |
|---|---|---|
| PAS-01 | Phone must support a "Switch Screen" mode: a full-screen touch UI that fires switch events | Must Have |
| PAS-02 | Switch Screen must support configurable zone layouts: full-screen (1-switch), left/right split, top/bottom split, asymmetric 80/20 | Must Have |
| PAS-03 | Each zone must display a large, high-contrast label (e.g., "NEXT ▶", "✓ SELECT") | Must Have |
| PAS-04 | Phone must provide vibration feedback on each zone tap | Must Have |
| PAS-05 | Phone must provide visual flash feedback (zone briefly highlights) on each tap | Must Have |
| PAS-06 | When used as remote switch for Chromebook: phone must advertise itself as a Bluetooth HID keyboard and emit configured keycodes on tap | Must Have |
| PAS-07 | Chromebook pairing with phone-as-switch must be achievable by a caregiver in < 5 minutes | Must Have |
| PAS-08 | Switch Screen must remain active when the phone's own screen auto-dim would apply; disable screen timeout in this mode | Must Have |
| PAS-09 | If the phone is also making a call (phone-as-device scenario), Switch Screen must remain accessible — show a persistent switch overlay above the in-call screen | Must Have |
| PAS-10 | Caregiver can configure which zone maps to Switch 1 vs Switch 2 | Must Have |

### 4.4 Switch Input — Combined (External + Phone Simultaneously)

| ID | Requirement | Priority |
|---|---|---|
| CMB-01 | External hardware switch and phone Switch Screen must be able to operate at the same time | Must Have |
| CMB-02 | A press from either source fires the correct switch event (no conflicts, no priority ordering needed) | Must Have |
| CMB-03 | Settings must allow mixed assignment: e.g., external switch = Switch 1, phone zone = Switch 2 | Must Have |
| CMB-04 | Status indicator in caregiver settings shows which input sources are currently active/connected | Should Have |

### 4.5 Home / Main Menu

The main screen is the user's launchpad. It must be scannable and contain tiles for each major function.

| ID | Requirement | Priority |
|---|---|---|
| HME-01 | Main menu must display large, icon+text tiles for: Phone, Netflix, Back/Home, Notifications, Settings | Must Have |
| HME-02 | Tiles must be usable at a minimum 120×120dp touch target | Must Have |
| HME-03 | Main menu must auto-focus and begin scanning on launch | Must Have |
| HME-04 | App must be launchable as a full-screen overlay on top of other apps | Must Have |

### 4.6 Phone / Calling

| ID | Requirement | Priority |
|---|---|---|
| PHN-01 | User can select a contact from a scannable contact list | Must Have |
| PHN-02 | Contact list is filterable by "Favourites" (configurable by caregiver) | Must Have |
| PHN-03 | User can initiate a call by scanning to a contact and selecting | Must Have |
| PHN-04 | In-call screen shows: End Call, Speaker On/Off, Mute, Volume Up/Down | Must Have |
| PHN-05 | Incoming call screen shows: Answer, Decline — both scannable | Must Have |
| PHN-06 | App automatically surfaces the in-call scanning overlay when a call is active | Must Have |
| PHN-07 | Support for a numeric dialpad (scannable) for non-contact calls | Should Have |
| PHN-08 | Recent calls list, scannable | Should Have |

### 4.7 Netflix Control

Netflix does not expose a standard accessibility API. Control must be achieved via Android's `AccessibilityService` to inject key events and interact with focusable UI elements.

| ID | Requirement | Priority |
|---|---|---|
| NFX-01 | App can launch the Netflix Android app | Must Have |
| NFX-02 | Scanning overlay maps to D-pad navigation (up/down/left/right/select) for browsing | Must Have |
| NFX-03 | During playback, scanning overlay shows: Play/Pause, Forward 10s, Back 10s, Volume Up, Volume Down, Stop/Exit | Must Have |
| NFX-04 | App automatically detects when Netflix is in browse vs. playback mode and switches overlay | Must Have |
| NFX-05 | The scanning overlay must not block Netflix's own subtitle/CC display | Should Have |
| NFX-06 | "Back to Menu" tile must always be available in the Netflix overlay | Must Have |

> **Technical Note:** NFX control will use `AccessibilityService` + `dispatchGesture` or `performGlobalAction`. Netflix UI detection uses `AccessibilityEvent` window state changes to differentiate browse/playback.

### 4.8 Android OS Navigation

| ID | Requirement | Priority |
|---|---|---|
| NAV-01 | User can trigger Home, Back, Recents from the scanning overlay | Must Have |
| NAV-02 | User can expand the notification shade via scanning | Should Have |
| NAV-03 | User can dismiss notifications via scanning | Nice to Have |
| NAV-04 | App switcher (recent apps) accessible via scanning | Should Have |
| NAV-05 | Quick settings tiles (Wi-Fi, Bluetooth, Volume) accessible via scanning | Nice to Have |

### 4.9 Settings (Caregiver / Admin UI)

The settings screen is designed for touchscreen use by a caregiver, not for switch scanning (though switch access remains possible).

| ID | Requirement | Priority |
|---|---|---|
| SET-01 | Configure scan mode (auto, step, inverse) | Must Have |
| SET-02 | Configure scan speed | Must Have |
| SET-03 | Configure switch 1 and switch 2 keycodes (external hardware) | Must Have |
| SET-04 | Configure switch debounce duration | Must Have |
| SET-05 | Set favourite contacts (shown first in phone panel) | Must Have |
| SET-06 | Configure highlight colour and style | Should Have |
| SET-07 | Enable/disable auditory feedback | Should Have |
| SET-08 | Configure number of scan loops before auto-stop | Should Have |
| SET-09 | PIN-lock settings screen to prevent accidental changes by user | Should Have |
| SET-10 | Export/import settings profile (JSON file) | Nice to Have |
| SET-11 | Configure phone Switch Screen zone layout (full / left-right / top-bottom / asymmetric) | Must Have |
| SET-12 | Configure which Switch Screen zone maps to Switch 1 vs Switch 2 | Must Have |
| SET-13 | Enable/disable phone-as-switch BT HID mode (Chromebook remote) | Must Have |
| SET-14 | Show live input source status: which external switches and phone zones are currently active | Should Have |
| SET-15 | Configure keycode emitted by phone Switch Screen zones (for BT HID remote mode) | Must Have |

---

## 5. Platform Requirements

### 5.1 Android Phone — Standalone Mode (app runs on phone)

| Requirement | Detail |
|---|---|
| Minimum OS | Android 8.0 (API 26) |
| Permissions required | `BIND_ACCESSIBILITY_SERVICE`, `READ_CONTACTS`, `CALL_PHONE`, `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `SYSTEM_ALERT_WINDOW` |
| Accessibility Service | Required — must be enabled by user in Accessibility Settings |
| App runs as | Foreground service with persistent notification |
| Phone-as-switch | Touch overlay active on same device; no BT HID needed |

### 5.2 Android Phone — Remote Switch Mode (switch for Chromebook)

| Requirement | Detail |
|---|---|
| Minimum OS | Android 9.0 (API 28) — required for `BluetoothHidDevice` API |
| Permissions required | `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, `FOREGROUND_SERVICE`, `WAKE_LOCK` |
| BT HID profile | Phone registers as a HID keyboard; emits keycodes on screen tap |
| Screen timeout | `FLAG_KEEP_SCREEN_ON` applied to Switch Screen activity to prevent dimming |
| Conflict — phone also making calls | In-call overlay and Switch Screen co-exist; tested explicitly (see Section 9) |

### 5.3 Chromebook

| Requirement | Detail |
|---|---|
| Chrome OS version | M108+ (stable Android app support) |
| Android subsystem | ARC++ or ARCVM |
| External switch connection | USB HID preferred; BT HID supported |
| Phone-as-switch connection | Pair phone via BT HID once (caregiver setup); phone then acts as a keyboard |
| Known limitations | Accessibility Services have reduced permissions on some Chrome OS versions; must document and test |
| Calling | Requires connected Android phone via Phone Hub, or the user's phone running AccessSwitch in standalone mode |
| Window mode | Run in full-screen Android app window |

---

## 6. UX & Design Principles

1. **Large, unambiguous targets** — All interactive elements ≥ 120×120dp
2. **High contrast** — Minimum 4.5:1 contrast ratio (WCAG AA) for all text/icons; scanning highlight at 7:1
3. **No time pressure** — No dialogs or popups that auto-dismiss without user action (except scan loops, which are configurable)
4. **Predictable layout** — The position of tiles on the scanning grid never changes unexpectedly
5. **Error tolerance** — A single accidental switch press must be recoverable (e.g., "Back" tile always present)
6. **Caregiver-first setup** — Complex configuration hidden behind a PIN-protected settings screen

---

## 7. Success Metrics

| Metric | Target |
|---|---|
| Time to make a call (from app launch) | < 30 seconds for a trained user |
| Time to start playing a Netflix title | < 60 seconds from app launch |
| External switch input latency | < 100ms from keypress to UI response |
| Phone-as-switch tap latency (local) | < 50ms tap to scanning action |
| Phone-as-switch BT HID latency (remote) | < 150ms tap on phone to Chromebook response |
| Caregiver setup time (external switch) | < 15 minutes from install to first call |
| Caregiver setup time (phone-as-switch, remote) | < 5 minutes BT pairing + configuration |
| Crash-free sessions | > 99.5% |

---

## 8. Out of Scope (Future Versions)

- Word prediction / AAC communication boards
- Eye tracking input
- Smart home control (lights, thermostat)
- Multi-user profiles
- Cloud backup of settings
- iOS version

---

## 9. Open Questions

1. Can `AccessibilityService` on Chrome OS reliably inject D-pad events into Netflix's ARC++ instance?
2. Does the Tecla BLE switch SDK need to be bundled, or is standard BLE HID sufficient?
3. What is the minimum Chromebook hardware spec we should target?
4. Should the app include an emergency "SOS" call tile on the home screen?
5. How should the app handle BT switch disconnection mid-session? (Currently: pause scanning + reconnect overlay)
6. If the user's phone is making a call AND being used as a remote switch for the Chromebook simultaneously, does audio routing cause any issues?
7. Should the phone Switch Screen have a "lock" to prevent accidental mode-change if the user bumps the phone?
8. What is the real-world BT HID latency on a range of Android phones → Chromebook pairings?
9. Should phone Switch Screen zones be visible in the dark (i.e., auto-brightness override)?

---

## 10. Glossary

| Term | Definition |
|---|---|
| Switch Access | Using a physical button (switch) instead of touch to operate a device |
| Scanning | Cycling through UI elements automatically or manually until the user selects one |
| Phone-as-Switch | Using an Android phone's touchscreen as one or two large soft switch buttons |
| Switch Screen | The full-screen touch UI on a phone configured as a switch; shows large labelled zones |
| BT HID | Bluetooth Human Interface Device — allows a phone to emulate a keyboard/switch to another device |
| ARC++ | Android Runtime for Chrome — allows Android apps to run on Chromebook |
| AccessibilityService | Android system service that can observe and interact with the UI of other apps |
| HID | Human Interface Device — USB/Bluetooth profile for keyboards, mice, and switches |
| D-pad | Directional pad — up/down/left/right/centre navigation used by Android TV and apps |
| Highlight | The visual indicator showing which element is currently being scanned |
| Mixed Input | Using an external switch and phone-as-switch simultaneously as different switch channels |
