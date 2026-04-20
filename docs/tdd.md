# Technical Design Document (TDD)
## AccessSwitch — Android Accessibility App

**Version:** 0.2 (Draft)
**Status:** Brainstorm / Pre-Development
**Last Updated:** 2026-04-20
**Author:** TBD

---

## 1. Architecture Overview

### 1.1 High-Level Design

```
 ┌────────────────────────────────────────────────────────────────┐
 │                     CHROMEBOOK / ANDROID PHONE                 │
 │                                                                 │
 │  ┌───────────────┐   ┌──────────────────────────────────────┐  │
 │  │ External HW   │   │         AccessSwitch App              │  │
 │  │ Switch        │──▶│                                       │  │
 │  │ (BT/USB HID)  │   │  ┌──────────────────┐                │  │
 │  └───────────────┘   │  │  SwitchInputHub   │                │  │
 │                       │  │  ┌─────────────┐ │  ┌──────────┐ │  │
 │  ┌───────────────┐    │  │  │HW Switch    │ │  │ Scanning │ │  │
 │  │ Phone         │    │  │  │Listener     │─┼─▶│ Engine   │ │  │
 │  │ (BT HID /     │───▶│  │  ├─────────────┤ │  │          │ │  │
 │  │  touch local) │    │  │  │PhoneSwitch  │─┼─▶│(StateFlow│ │  │
 │  └───────────────┘    │  │  │TouchListener│ │  │ timer)   │ │  │
 │                        │  │  └─────────────┘ │  └────┬─────┘ │  │
 │                        │  └──────────────────┘       │       │  │
 │                        │                             │       │  │
 │                        │  ┌──────────────────────────▼─────┐ │  │
 │                        │  │      ScanningOverlayService     │ │  │
 │                        │  └────────────────┬───────────────┘ │  │
 │                        │                   │                 │  │
 │                        │  ┌────────────────▼──────────────┐  │  │
 │                        │  │       Module Controllers        │  │  │
 │                        │  │  ┌──────┐ ┌────────┐ ┌──────┐  │  │  │
 │                        │  │  │Phone │ │Netflix │ │ Nav  │  │  │  │
 │                        │  └──┴──────┴─┴────────┴─┴──────┴──┘  │  │
 │                        └───────────────────────────────────────┘  │
 │                                                                    │
 │  ┌─────────────────────────────────────────────────────────────┐  │
 │  │                   Android System Layer                       │  │
 │  │   TelecomManager   AccessibilityService   GlobalActions      │  │
 │  └─────────────────────────────────────────────────────────────┘  │
 └────────────────────────────────────────────────────────────────────┘

 ┌──────────────────────────────────────┐
 │   Android Phone (Remote Switch Mode) │
 │                                      │
 │  ┌────────────────────────────────┐  │
 │  │      SwitchScreenActivity      │  │──BT HID──▶ Chromebook
 │  │  ┌──────────┐  ┌────────────┐  │  │
 │  │  │  Zone 1  │  │  Zone 2    │  │  │
 │  │  │ "NEXT ▶" │  │ "✓ SELECT" │  │  │
 │  │  └──────────┘  └────────────┘  │  │
 │  └────────────────────────────────┘  │
 │  BluetoothHidDeviceService           │
 └──────────────────────────────────────┘
```

### 1.2 Core Services

| Service | Type | Purpose |
|---|---|---|
| `SwitchInputHub` | Coordinator class | Merges events from all input sources (HW switch + phone touch) into a single stream for `ScanningEngine` |
| `HwSwitchListener` | `AccessibilityService` callback | Captures global `KeyEvent` from external BT/USB switches |
| `PhoneSwitchTouchListener` | `View.OnTouchListener` | Captures touch zone presses on the local phone Switch Screen |
| `BluetoothHidDeviceService` | Android `Service` | Advertises phone as a BT HID keyboard; emits keycodes to paired Chromebook |
| `ScanningEngine` | Kotlin object / ViewModel | Manages scan timing, state machine, and selection logic |
| `ScanningOverlayService` | `Service` + `WindowManager` | Draws the floating scanning UI overlay above all apps |
| `SwitchScreenActivity` | `Activity` | Full-screen touch zone UI shown when phone is in Switch Screen mode |
| `PhoneController` | Module class | Manages calling via `TelecomManager` / `InCallService` |
| `NetflixController` | Module class | Injects D-pad events into Netflix via `AccessibilityService` |
| `NavController` | Module class | Issues `performGlobalAction` for Home/Back/Recents |
| `SettingsRepository` | Repository | Reads/writes user configuration via DataStore |

---

## 2. Module Breakdown

### 2.1 SwitchInputHub

`SwitchInputHub` is the central coordinator that normalises events from all input sources into a single `SwitchEvent` stream consumed by `ScanningEngine`. This is the key architectural addition that enables mixed input.

```kotlin
enum class SwitchSource { HW_SWITCH, PHONE_TOUCH_LOCAL, PHONE_BT_HID }

data class SwitchEvent(
    val switchId: SwitchId,        // SWITCH_1 or SWITCH_2
    val source: SwitchSource,
    val timestampMs: Long
)

class SwitchInputHub @Inject constructor(
    private val settings: SettingsRepository,
    private val scanningEngine: ScanningEngine
) {
    // Both listeners call this; engine doesn't care about source
    fun onRawEvent(switchId: SwitchId, source: SwitchSource) {
        val now = SystemClock.elapsedRealtime()
        if (isDuplicate(switchId, now)) return   // cross-source dedup within debounce window
        scanningEngine.onSwitchPressed(switchId)
    }
}
```

**Cross-source deduplication:** If both an external switch AND the phone screen fire Switch 1 within the debounce window (e.g., user presses external switch and accidentally also touches phone screen), only the first event is processed.

---

### 2.2 HwSwitchListener (`AccessibilityService`)

**Responsibilities:**
- Register as an `AccessibilityService` to receive global key events
- Intercept switch keypresses regardless of which app is in focus
- Route events to `SwitchInputHub`
- Detect and report switch connection/disconnection via `InputManager`

**Key implementation notes:**
- Must declare `android:accessibilityFlags="flagRequestFilterKeyEvents"` in the accessibility service XML config
- Override `onKeyEvent(KeyEvent)` — return `true` to consume switch presses (prevents them reaching the active app)
- Switch keycode is stored in `SettingsRepository` and loaded at service start
- On `InputDevice` removal: emit `SwitchSourceDisconnectedEvent` → `ScanningOverlayService` shows reconnect banner

```kotlin
// Pseudocode — onKeyEvent handler
override fun onKeyEvent(event: KeyEvent): Boolean {
    val switchId = when (event.keyCode) {
        settings.switch1Keycode -> SwitchId.SWITCH_1
        settings.switch2Keycode -> SwitchId.SWITCH_2
        else -> return false
    }
    if (event.action != KeyEvent.ACTION_DOWN) return false
    switchInputHub.onRawEvent(switchId, SwitchSource.HW_SWITCH)
    return true // consume the event
}
```

**Chrome OS note:** Chrome OS may route some key events through its own layer before reaching ARC++. USB HID switches are more reliable than BT on Chrome OS. Test thoroughly.

---

### 2.3 PhoneSwitchTouchListener (Local Touch Mode)

Used when the phone itself is the primary device (not remoting to a Chromebook).

**Responsibilities:**
- Display `SwitchScreenActivity` as a full-screen, always-on overlay
- Map touch zones to Switch 1 / Switch 2 based on configured layout
- Forward events to `SwitchInputHub.onRawEvent(..., SwitchSource.PHONE_TOUCH_LOCAL)`
- Provide immediate haptic + visual feedback (< 16ms, on UI thread)

```kotlin
// Zone layout enum
enum class SwitchZoneLayout {
    FULL_SCREEN,          // whole screen = Switch 1
    LEFT_RIGHT,           // left = Switch 1, right = Switch 2
    TOP_BOTTOM,           // top = Switch 1, bottom = Switch 2
    ASYMMETRIC_80_20      // 80% left = Switch 1, 20% right = Switch 2
}
```

**Touch handling:** Use `onTouchListener` with `ACTION_DOWN` only (ignore `ACTION_MOVE`, `ACTION_UP`) to prevent drag-to-select. Zone boundaries are calculated once on layout and cached.

**Screen-on:** Apply `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` to the Switch Screen window. Do not rely on the power manager WakeLock — prefer window flags for simplicity.

---

### 2.4 BluetoothHidDeviceService (Remote Switch Mode)

Used when the phone acts as a remote BT switch for the Chromebook.

**API:** `BluetoothHidDevice` (Android API 28+). The phone registers as a HID keyboard descriptor and emits keystroke reports to the paired Chromebook.

**Responsibilities:**
- Register a HID keyboard descriptor with the Bluetooth stack
- Accept connection from paired Chromebook
- On zone tap: send a `HID keyboard report` for the configured keycode (Switch 1 keycode / Switch 2 keycode)
- Immediately send a key-up report after key-down (simulate a tap, not a hold)
- Expose connection state as `StateFlow<BtHidState>` for the settings UI status indicator

```kotlin
// HID keyboard descriptor (standard boot keyboard)
private val HID_DESCRIPTOR = byteArrayOf(
    0x05.toByte(), 0x01.toByte(), // Usage Page: Generic Desktop
    0x09.toByte(), 0x06.toByte(), // Usage: Keyboard
    // ... (full 45-byte standard keyboard descriptor)
)

// Send a keycode tap
fun sendKeyTap(keycode: Byte) {
    val keyDown = ByteArray(8).also { it[2] = keycode }
    val keyUp   = ByteArray(8) // all zeros
    hidDevice.sendReport(hostDevice, HID_REPORT_ID, keyDown)
    hidDevice.sendReport(hostDevice, HID_REPORT_ID, keyUp)
}
```

**Pairing flow (caregiver setup):**
1. Caregiver opens AccessSwitch settings on phone → taps "Pair as Switch for Chromebook"
2. Phone enables BT discoverability + registers HID profile
3. Caregiver opens Chromebook BT settings → pairs with phone (appears as "AccessSwitch Keyboard")
4. Pairing stored; phone auto-reconnects on subsequent sessions

**Conflict — phone also making a call:** The BT HID connection uses the BT Classic stack (HID profile). Phone calls use either the cellular radio or BT HSP/HFP. These are different BT profiles and operate independently — no conflict expected, but must be tested.

**Latency target:** < 150ms from screen tap to Chromebook `onKeyEvent`. Main sources of latency: touch event delivery (~16ms), BT HID report transmission (~50–80ms on typical Android BT stack), ARC++ key event dispatch (~20–30ms).

---

### 2.5 ScanningEngine

*(Unchanged from v0.1 — now receives events from `SwitchInputHub` rather than directly from `SwitchInputService`)*

**State Machine:**

```
        ┌────────────┐
        │   IDLE     │◀──────────────────────────┐
        └─────┬──────┘                           │
              │ start()                           │ stop() / loops exhausted
              ▼                                   │
        ┌─────────────┐  switch press (auto-scan) │
        │  SCANNING   │──────────────────────────▶│
        │  (RUNNING)  │                           │
        └─────┬───────┘                           │
              │ item highlighted                  │
              │ → notify observers                │
              ▼                                   │
        ┌─────────────┐                           │
        │  SELECTED   │───────────────────────────┘
        │  (ACTION)   │  action dispatched → restart
        └─────────────┘
```

```kotlin
data class ScanItem(
    val id: String,
    val label: String,
    val iconRes: Int?,
    val action: () -> Unit,
    val children: List<ScanItem> = emptyList() // for row-column groups
)
```

**Key responsibilities:**
- Maintains an ordered list of `ScanItem` objects (the current overlay's focusable items)
- Drives a `Handler` / `CoroutineScope` timer that advances the highlighted index every `scanIntervalMs`
- On switch press: executes the selected item's `action` lambda
- Exposes `StateFlow<ScanState>` for the overlay to observe and render highlights
- Supports row-column mode: first scan cycles through rows; a press enters the row and scans columns

---

### 2.6 ScanningOverlayService

**Responsibilities:**
- Create a `WindowManager.LayoutParams` window with type `TYPE_ACCESSIBILITY_OVERLAY`
- Inflate a Compose or View-based grid of `ScanItem` tiles
- Observe `ScanningEngine.StateFlow` and update highlight ring on the current item
- Pass item lists from each module controller (Phone, Netflix, Nav) to `ScanningEngine`

**Window parameters:**
```kotlin
WindowManager.LayoutParams(
    WRAP_CONTENT, WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
    PixelFormat.TRANSLUCENT
)
```

> `TYPE_ACCESSIBILITY_OVERLAY` is only available to `AccessibilityService` — ensures the overlay appears above all apps including lock screen and system dialogs.

**Layout strategy:**
- Main menu: 2×3 grid (Phone, Netflix, Nav, Notifications, Settings, Back)
- Phone panel: 3×3 contacts grid + call controls row
- Netflix panel: 3×3 D-pad grid + playback controls row (context-switching based on mode)
- All grids use `dp` units; minimum tile size 120×120dp

---

### 2.7 PhoneController

**Responsibilities:**
- Query `ContactsContract` for contacts and favourites
- Initiate outgoing calls via `TelecomManager.placeCall()`
- Handle incoming calls via `InCallService` (or `android.intent.action.ANSWER` broadcast for pre-API 28)
- Expose `CallState` (IDLE, RINGING, ACTIVE, ENDED) as `StateFlow`
- Build `ScanItem` list for the overlay: contacts, call controls, dialpad

**Key implementation notes:**
- `READ_CONTACTS` and `CALL_PHONE` permissions required (runtime-request on first launch)
- For Chromebook: phone calling requires the Android phone to be connected via "Phone Hub" or the app must use the device's own SIM if running on a phone. **Document this constraint clearly in setup.**
- In-call overlay shows: End Call, Mute/Unmute, Speaker On/Off, Volume Up, Volume Down

```kotlin
// Simplified call state
sealed class CallState {
    object Idle : CallState()
    data class Ringing(val callerName: String) : CallState()
    data class Active(val callerName: String, val durationSeconds: Int) : CallState()
    object Ended : CallState()
}
```

---

### 2.8 NetflixController

Netflix does not implement Android TV's `TvView` or expose standard accessibility actions for media control. Control is achieved by injecting **key events** via `AccessibilityService.dispatchGesture` and `performAction`.

**Mode detection:**
- Subscribe to `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`
- Inspect `event.packageName` (should be `com.netflix.mediaclient`) and `event.className`
- Determine if Netflix is showing a browse activity or a player activity by checking `className` patterns (e.g., `...VideoPlayerActivity` vs `...HomeActivity`)

**Browse mode — D-pad navigation:**

| Scan Tile | Action |
|---|---|
| ▲ Up | `KeyEvent.KEYCODE_DPAD_UP` |
| ▼ Down | `KeyEvent.KEYCODE_DPAD_DOWN` |
| ◀ Left | `KeyEvent.KEYCODE_DPAD_LEFT` |
| ▶ Right | `KeyEvent.KEYCODE_DPAD_RIGHT` |
| ✓ Select | `KeyEvent.KEYCODE_DPAD_CENTER` |
| ← Back | `performGlobalAction(GLOBAL_ACTION_BACK)` |

**Playback mode — media controls:**

| Scan Tile | Action |
|---|---|
| ⏯ Play/Pause | `KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE` |
| ⏩ +10s | `KeyEvent.KEYCODE_MEDIA_FAST_FORWARD` or gesture swipe right |
| ⏪ -10s | `KeyEvent.KEYCODE_MEDIA_REWIND` or gesture swipe left |
| 🔊 Vol+ | `AudioManager.adjustStreamVolume(STREAM_MUSIC, ADJUST_RAISE)` |
| 🔇 Vol- | `AudioManager.adjustStreamVolume(STREAM_MUSIC, ADJUST_LOWER)` |
| ✕ Stop | `performGlobalAction(GLOBAL_ACTION_BACK)` |

> **Risk:** Netflix may update its UI hierarchy, breaking `className`-based mode detection. Mitigation: implement a version-check heuristic and fallback to a "manual toggle" where the user switches between browse/playback modes via a scan tile.

---

### 2.9 NavController

Simple wrapper around `AccessibilityService.performGlobalAction`.

| Action | API call |
|---|---|
| Home | `performGlobalAction(GLOBAL_ACTION_HOME)` |
| Back | `performGlobalAction(GLOBAL_ACTION_BACK)` |
| Recents | `performGlobalAction(GLOBAL_ACTION_RECENTS)` |
| Notifications | `performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)` |
| Quick Settings | `performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)` |

---

## 3. Data Layer

### 3.1 Settings (DataStore / SharedPreferences)

```kotlin
data class AppSettings(
    // Scanning
    val scanMode: ScanMode,               // AUTO, STEP, INVERSE, ROW_COLUMN
    val scanIntervalMs: Long,             // 500–8000
    val scanLoops: Int,                   // 1–5, or INFINITE
    val highlightColor: Int,              // ARGB
    val highlightStyle: HighlightStyle,   // BORDER, FILL, BOTH
    val audioFeedback: Boolean,
    val hapticFeedback: Boolean,

    // External HW switch
    val switch1Keycode: Int,              // KeyEvent keycode for Switch 1
    val switch2Keycode: Int,              // KeyEvent keycode for Switch 2
    val debounceMs: Long,                 // 100–1000ms

    // Phone-as-switch (local touch mode)
    val phoneLocalSwitchEnabled: Boolean,
    val phoneLocalZoneLayout: SwitchZoneLayout, // FULL_SCREEN, LEFT_RIGHT, TOP_BOTTOM, ASYMMETRIC_80_20
    val phoneLocalZone1SwitchId: SwitchId,      // which switch zone 1 fires (SWITCH_1 or SWITCH_2)
    val phoneLocalZone2SwitchId: SwitchId,

    // Phone-as-switch (BT HID remote mode)
    val phoneBtHidEnabled: Boolean,
    val phoneBtHidSwitch1Keycode: Int,    // keycode emitted via BT HID for Switch 1 zone
    val phoneBtHidSwitch2Keycode: Int,
    val phoneBtHidPairedDeviceAddress: String?, // MAC of paired Chromebook; null = not yet paired

    // Contacts / calling
    val settingsPinHash: String?,         // SHA-256 of 4-digit PIN; null = unlocked
    val favouriteContactIds: Set<Long>
)
```

### 3.2 Contact Data

Contacts are read from `ContactsContract` at runtime. Favourites are stored as a `Set<Long>` of contact IDs in `AppSettings`. No contacts are stored locally — always read from the system contacts database.

---

## 4. Permissions

### 4.1 Chromebook / Phone — Core App

| Permission | Reason | When Requested |
|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | HW switch key event capture + overlay + Netflix control | Must be granted by user in Accessibility Settings |
| `READ_CONTACTS` | Contact list for calling | Runtime request at first launch |
| `CALL_PHONE` | Initiate calls | Runtime request at first launch |
| `READ_PHONE_STATE` | Detect incoming calls | Runtime request at first launch |
| `FOREGROUND_SERVICE` | Keep overlay and switch services alive | Declared in manifest |
| `SYSTEM_ALERT_WINDOW` | Draw overlay above other apps | Directed Settings intent (special permission) |
| `VIBRATE` | Haptic feedback on switch press | Normal permission (manifest only) |
| `MODIFY_AUDIO_SETTINGS` | Volume control via scanning | Normal permission |
| `WAKE_LOCK` | Keep CPU awake during active scanning session | Normal permission |

### 4.2 Android Phone — Phone-as-Switch Additional Permissions

| Permission | Reason | Min API | When Requested |
|---|---|---|---|
| `BLUETOOTH_ADVERTISE` | Advertise phone as BT HID device to Chromebook | API 31 (Android 12) | Runtime request when BT HID mode enabled |
| `BLUETOOTH_CONNECT` | Connect to paired Chromebook and send HID reports | API 31 | Runtime request when BT HID mode enabled |
| `BLUETOOTH` + `BLUETOOTH_ADMIN` | Legacy BT permissions for API < 31 | API 28 | Runtime (API 28–30) or manifest (older) |
| `ACCESS_FINE_LOCATION` | Required by Android for BT scanning on API 28–30 | API 28 | Runtime request (API 28–30 only) |

> **Note:** `ACCESS_FINE_LOCATION` is only required on API 28–30 for BT device discovery. From API 31 onwards it is replaced by `BLUETOOTH_SCAN`. Conditionally request based on runtime API level.

---

## 5. Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI (overlay) | Jetpack Compose (inside a `ComposeView` added to `WindowManager`) |
| UI (Switch Screen) | Jetpack Compose full-screen `Activity` with `FLAG_KEEP_SCREEN_ON` |
| UI (settings) | Jetpack Compose + Navigation |
| Architecture | MVVM + clean architecture (Use Cases) |
| State management | Kotlin `StateFlow` / `SharedFlow` |
| DI | Hilt |
| Settings persistence | Jetpack DataStore (Preferences) |
| BT HID (phone remote switch) | Android `BluetoothHidDevice` API (API 28+) |
| Build | Gradle (Kotlin DSL) |
| Min SDK (core app) | API 26 (Android 8.0) |
| Min SDK (BT HID remote mode) | API 28 (Android 9.0) — `BluetoothHidDevice` required |
| Target SDK | API 35 |

---

## 6. Key Technical Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Netflix UI changes break mode detection | High | Medium | Fallback to manual mode toggle scan tile; add version-pinned tests |
| `AccessibilityService` permissions restricted on Chrome OS ARC | Medium | High | Test on real Chromebook early; document workaround (prefer phone form factor) |
| HW Bluetooth switch dropout mid-session | Medium | High | Detect disconnection via `InputManager`; pause scanning; show reconnect overlay; phone-as-switch remains active as fallback |
| `TYPE_ACCESSIBILITY_OVERLAY` blocked by some OEM launchers | Low | Medium | Fall back to `TYPE_APPLICATION_OVERLAY` with `SYSTEM_ALERT_WINDOW` |
| Calling unavailable on Chromebook (no SIM) | High | High | Document clearly: calling requires an Android phone; Chromebook is nav/media only in v1 |
| `BluetoothHidDevice` profile unavailable on some Android OEMs | Medium | High | Check at runtime; disable BT HID remote mode gracefully with a user-facing explanation if unsupported |
| BT HID latency too high for comfortable scanning (> 200ms) | Medium | Medium | Measure on 3+ real devices; if latency is unacceptable, investigate BLE HID as alternative |
| Phone BT HID + phone call audio conflict | Low | High | Test explicitly: BT HID uses Classic HID profile; phone calls use HFP — different profiles, expected to coexist; document any OEM-specific exceptions |
| Phone Switch Screen dismissed by OS (battery optimisation) | Medium | Medium | Use a `FOREGROUND_SERVICE` to keep the Switch Screen process alive; acquire `WAKE_LOCK` during active sessions |
| User accidentally triggers phone Switch Screen while it is in their pocket / bag | Low | Low | Add optional "lock" toggle in caregiver settings to disable Switch Screen without closing the app |

---

## 7. Testing Strategy

### 7.1 Unit Tests

- `ScanningEngine` state machine transitions (all modes)
- `ScanningEngine` timer: verify interval accuracy within ±50ms
- `SwitchInputHub` cross-source deduplication: two simultaneous events within debounce window → only one `onSwitchPressed` call
- `SwitchInputHub` mixed input: HW switch fires Switch 1, phone touch fires Switch 2 → both dispatched independently
- `HwSwitchListener.onKeyEvent` debounce logic
- `SettingsRepository` read/write round-trips (all new phone-as-switch fields)
- `PhoneController.CallState` transitions
- `SwitchZoneLayout` boundary calculations: verify zone hit-testing at boundary edges

### 7.2 Instrumentation / UI Tests

- Overlay renders correct number of tiles for each panel
- Scanning highlight advances through all items in correct order
- HW switch press triggers correct action for each panel
- Phone Switch Screen zone tap triggers correct switch event
- Settings PIN lock/unlock flow
- Switch Screen `FLAG_KEEP_SCREEN_ON` remains active after 60s idle

### 7.3 Integration Tests

- End-to-end call flow: select contact → initiate call → end call (using a test SIM or loopback)
- Netflix D-pad injection: verify focus moves in browse mode
- Netflix playback injection: verify play/pause toggles
- Mixed input: external switch press + phone zone tap in same session — both trigger correctly, no conflicts
- BT HID pairing flow: phone pairs with test Chromebook; keycode received correctly on Chromebook side

### 7.4 Manual / Device Tests

| Test | Device(s) | Notes |
|---|---|---|
| USB HID switch input | Chromebook | OTG adapter required |
| BT HID switch input | Android phone | Pair switch before testing |
| Phone local touch — all 4 zone layouts | Android phone | Test each layout option |
| Phone remote switch via BT HID | Phone + Chromebook | Measure round-trip latency with timestamps |
| Phone calling while phone-as-switch active | Android phone | Verify call overlay + switch zones coexist |
| Phone BT HID + phone call simultaneously | Phone + Chromebook | Test audio routing; verify no HID dropout |
| HW switch BT disconnect → phone switch fallback | Phone | Disconnect BT switch mid-session; verify phone zones take over seamlessly |
| Incoming call overlay | Android phone | Call from another device |
| Netflix mode detection | Both | Browse → playback transition |
| Full end-to-end: caregiver setup → user operation | Both | Measure setup time against < 15 min target |

---

## 8. Project Structure

```
accessswitch/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/accessswitch/
│   │   │   │   ├── input/
│   │   │   │   │   ├── SwitchInputHub.kt          ← merges all sources
│   │   │   │   │   ├── HwSwitchListener.kt        ← AccessibilityService key events
│   │   │   │   │   ├── PhoneSwitchTouchListener.kt ← local touch zones
│   │   │   │   │   ├── BluetoothHidDeviceService.kt ← BT HID remote mode
│   │   │   │   │   ├── SwitchEvent.kt
│   │   │   │   │   └── SwitchSource.kt
│   │   │   │   ├── accessibility/
│   │   │   │   │   └── AccessSwitchAccessibilityService.kt
│   │   │   │   ├── scanning/
│   │   │   │   │   ├── ScanningEngine.kt
│   │   │   │   │   ├── ScanItem.kt
│   │   │   │   │   └── ScanMode.kt
│   │   │   │   ├── overlay/
│   │   │   │   │   ├── ScanningOverlayService.kt
│   │   │   │   │   └── ui/
│   │   │   │   │       ├── MainMenuPanel.kt
│   │   │   │   │       ├── PhonePanel.kt
│   │   │   │   │       ├── NetflixPanel.kt
│   │   │   │   │       └── NavPanel.kt
│   │   │   │   ├── switchscreen/
│   │   │   │   │   ├── SwitchScreenActivity.kt    ← full-screen phone touch UI
│   │   │   │   │   ├── SwitchScreenViewModel.kt
│   │   │   │   │   └── ui/
│   │   │   │   │       ├── FullScreenZone.kt
│   │   │   │   │       ├── SplitZone.kt
│   │   │   │   │       └── AsymmetricZone.kt
│   │   │   │   ├── phone/
│   │   │   │   │   └── PhoneController.kt
│   │   │   │   ├── netflix/
│   │   │   │   │   └── NetflixController.kt
│   │   │   │   ├── nav/
│   │   │   │   │   └── NavController.kt
│   │   │   │   ├── settings/
│   │   │   │   │   ├── AppSettings.kt
│   │   │   │   │   └── SettingsRepository.kt
│   │   │   │   └── di/
│   │   │   │       └── AppModule.kt
│   │   │   └── res/
│   │   │       └── xml/
│   │   │           └── accessibility_service_config.xml
│   │   └── test/
│   │       ├── ScanningEngineTest.kt
│   │       ├── SwitchInputHubTest.kt
│   │       └── SwitchZoneLayoutTest.kt
│   └── build.gradle.kts
└── README.md
```

---

## 9. Accessibility Service Config

```xml
<!-- res/xml/accessibility_service_config.xml -->
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeViewFocusChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagRequestFilterKeyEvents|flagRetrieveInteractiveWindows"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:packageNames="com.netflix.mediaclient" />
```

> **Note:** `packageNames` can be omitted if global nav features are needed. Restricting to Netflix reduces battery and event noise when Netflix is active.

---

## 10. Open Technical Questions

1. **Chrome OS `performGlobalAction`:** Which global actions are available in the ARC++ environment? Needs device testing.
2. **Netflix package name stability:** `com.netflix.mediaclient` is the known package name but should be verified and version-pinned in CI.
3. **Compose in WindowManager:** Jetpack Compose inside a `ComposeView` added via `WindowManager` works on API 26+ but has some known issues with focus and recomposition. Evaluate stability vs. a traditional View-based overlay.
4. **`InCallService` vs. `TelecomManager`:** `InCallService` gives richer call control (answer/decline) but requires `MANAGE_OWN_CALLS` or carrier privilege. Fallback: use `Intent.ACTION_CALL` and detect state via `TelephonyManager`.
5. **Chromebook SIM / Phone Hub:** If the user runs the app on a Chromebook with no paired Android phone, calling is unavailable. Should the app detect this and hide the phone tile gracefully?
6. **`BluetoothHidDevice` OEM support:** Which Android OEMs restrict or omit this API? Samsung, Google Pixel, and OnePlus are expected to support it; others need explicit testing.
7. **BT HID real-world latency:** What is the 95th-percentile round-trip latency from phone tap → Chromebook key event across 3–5 real device pairings? If > 200ms, investigate BLE HID (`BluetoothGattServer` with HID-over-GATT profile) as a lower-latency alternative.
8. **Phone Switch Screen and Active Call coexistence:** When the phone is making a call and the Switch Screen is active, does the in-call UI obscure the zone labels? Needs UI layout testing. Likely fix: render Switch Screen as a semi-transparent overlay on top of the in-call screen.
9. **Battery impact of BT HID foreground service:** A persistent BT HID connection with a foreground service will consume battery. Measure drain over a 4-hour session and optimise if > 5% per hour.
