# AccessSwitch — User Guide

This guide explains how to set up and use AccessSwitch on a **Chromebook** (primary) and on an **Android phone** (standalone or as a remote switch for the Chromebook).

---

## Table of Contents

1. [What AccessSwitch Does](#1-what-accessswitch-does)
2. [Before You Start — What You Will Need](#2-before-you-start--what-you-will-need)
3. [Setting Up on a Chromebook](#3-setting-up-on-a-chromebook)
   - 3.1 [Install the App](#31-install-the-app)
   - 3.2 [Enable the Accessibility Service (required)](#32-enable-the-accessibility-service-required)
   - 3.3 [Grant the Overlay Permission (if prompted)](#33-grant-the-overlay-permission-if-prompted)
   - 3.4 [Open AccessSwitch and Confirm It Is Running](#34-open-accessswitch-and-confirm-it-is-running)
4. [Setting Up on an Android Phone](#4-setting-up-on-an-android-phone)
5. [Using the Scanning Overlay](#5-using-the-scanning-overlay)
   - 5.1 [What the Overlay Looks Like](#51-what-the-overlay-looks-like)
   - 5.2 [The Main Menu Tiles](#52-the-main-menu-tiles)
6. [Opening Netflix Through AccessSwitch](#6-opening-netflix-through-accessswitch)
7. [Connecting a Physical Switch](#7-connecting-a-physical-switch)
8. [Using Your Phone as a Switch for the Chromebook](#8-using-your-phone-as-a-switch-for-the-chromebook)
9. [Settings Reference](#9-settings-reference)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. What AccessSwitch Does

AccessSwitch places a **scanning overlay** on top of your screen. The overlay cycles through large tiles one at a time, highlighting each one. When a tile is highlighted, you press your switch (a physical button, an external device, or your phone screen) to select it.

From the overlay you can:

- **Navigate** — press Home, Back, see recent apps, or open notifications
- **Netflix** — browse and control Netflix playback without touching the screen
- **Phone** — call contacts, answer calls, and manage in-call controls *(Android phone only)*
- **Settings** — adjust scan speed, highlight colour, switch keycodes, and more

The overlay runs as a persistent layer on top of every other app, so you do not need to return to the AccessSwitch app window to use it.

---

## 2. Before You Start — What You Will Need

| Item | Notes |
|------|-------|
| Chromebook running ChromeOS M108 or later | Any modern Chromebook sold after 2021 should qualify |
| Google Play Store enabled on the Chromebook | Most consumer Chromebooks have it on by default |
| AccessSwitch app installed from the Play Store | See Section 3.1 |
| An input source | Physical switch (Bluetooth or USB), phone running AccessSwitch in switch mode, or keyboard keys for testing (Space = Switch 1, Enter = Switch 2 by default) |
| Netflix app installed (for Netflix control) | Install it from the Play Store on the Chromebook |

---

## 3. Setting Up on a Chromebook

### 3.1 Install the App

1. Open the **Google Play Store** on your Chromebook.
2. Search for **AccessSwitch** and install it.
3. Once installed, open it from the launcher. You will see the AccessSwitch home screen with the message:
   > *"Enable the Accessibility Service in Settings > Accessibility to start scanning."*

   Do not close the app yet — follow Section 3.2 to complete the setup.

---

### 3.2 Enable the Accessibility Service (required)

> **Why is this needed?** AccessSwitch uses Android's Accessibility Service to draw its scanning overlay on top of other apps, detect which app is in the foreground (e.g., Netflix browse vs. playback), and send navigation commands (Home, Back, etc.) on your behalf. Without this permission the overlay cannot appear.

**Step-by-step for ChromeOS:**

1. Click the **clock** in the bottom-right corner of the screen to open the system tray.
2. Click the **gear icon (⚙)** to open ChromeOS Settings.
3. In the left-hand menu, click **Apps**.
4. Click **Google Play Store**.
5. Click **Manage Android preferences**. A separate Android Settings window will open.
6. In that Android Settings window, scroll down and tap **Accessibility**.
7. In the list of installed services, find and tap **AccessSwitch**.
8. Tap the toggle at the top of the screen to turn it **ON**.
9. Read the permission dialogue that appears — it explains that AccessSwitch can observe and interact with apps on your behalf. Tap **Allow** (or **OK**).
10. The Android Settings window will close or return to the Accessibility list. AccessSwitch is now enabled.

> **Tip:** The exact wording of menus can vary slightly depending on your ChromeOS version. If you cannot find "Google Play Store" under Apps, look for **"Linux and Android"** or try searching "Accessibility" in the ChromeOS Settings search bar, then look for the Android section.

Once the service is enabled:

- Return to the AccessSwitch app (open it from the launcher if needed).
- The scanning overlay will appear automatically on top of the screen.
- The status message on the home screen will no longer prompt you to enable the service.

---

### 3.3 Grant the Overlay Permission (if prompted)

On some ChromeOS versions the system may also ask you to allow AccessSwitch to **"display over other apps"** (the `SYSTEM_ALERT_WINDOW` permission).

If you see this prompt:

1. Tap **Allow** in the dialogue, or
2. If redirected to a list, find **AccessSwitch** and toggle **Allow display over other apps** to ON.

---

### 3.4 Open AccessSwitch and Confirm It Is Running

1. Open AccessSwitch from the launcher (if not already open).
2. The scanning overlay (a dark semi-transparent grid of tiles) should now appear on top of the screen.
3. You should see at least three tiles: **Navigate**, **Netflix**, and **Settings**.
4. The first tile will be highlighted with a coloured border — this means scanning has started.

If the overlay does not appear, see [Section 10 — Troubleshooting](#10-troubleshooting).

---

## 4. Setting Up on an Android Phone

The steps are the same as for Chromebook, but the Accessibility Service is found in a different place:

1. Open the Android **Settings** app.
2. Tap **Accessibility**.
3. Under **Downloaded apps** (or **Installed services**), tap **AccessSwitch**.
4. Tap the toggle to turn it **ON** and confirm the permission dialogue.

Additionally, if you want the scanning overlay to appear on top of other apps, grant the **"Display over other apps"** permission:

1. Go to **Settings > Apps > AccessSwitch**.
2. Tap **Advanced** (or the three-dot menu).
3. Tap **Display over other apps** and toggle it ON.

---

## 5. Using the Scanning Overlay

### 5.1 What the Overlay Looks Like

The overlay is a dark, semi-transparent layer covering the screen. It shows a grid of large tiles (minimum 120 × 120 dp each). One tile at a time is highlighted with a coloured border or fill — this is the currently focused tile.

- In **auto-scan mode** (default), the highlight moves to the next tile automatically after a set delay (default 2 seconds).
- In **step-scan mode**, pressing Switch 1 moves to the next tile; pressing Switch 2 selects it.
- Press your switch when the tile you want is highlighted to activate it.

### 5.2 The Main Menu Tiles

When the overlay first appears you see the **main menu**:

| Tile | What it does |
|------|-------------|
| **Navigate** | Opens the navigation sub-menu (Home, Back, Recent Apps, Notifications) |
| **Phone** | Opens the phone/calling panel — calls contacts or shows in-call controls *(Android phone only; hidden on Chromebook)* |
| **Netflix** | Launches Netflix and activates Netflix browse/playback controls |
| **Settings** | Opens the AccessSwitch settings screen (for caregiver/admin configuration) |

---

## 6. Opening Netflix Through AccessSwitch

1. Make sure the **Netflix app** is installed on your Chromebook from the Google Play Store.
2. From the AccessSwitch overlay main menu, wait for the **Netflix** tile to be highlighted, then press your switch.
3. AccessSwitch will launch Netflix automatically.
4. The overlay will switch to **Netflix browse mode**: tiles for Up, Down, Left, Right, Select, and Back to Menu.
5. Use your switch to navigate Netflix's home screen and select a show.
6. Once a title starts playing, the overlay automatically switches to **playback mode**: tiles for Play/Pause, Forward 10s, Back 10s, Volume Up, Volume Down, and Back to Menu.
7. To return to the AccessSwitch main menu at any time, select the **Back to Menu** tile.

> **Note:** If Netflix does not detect its mode automatically (for example, after an app update), a manual fallback overlay is shown. Use the **Back to Menu** tile and try again.

---

## 7. Connecting a Physical Switch

AccessSwitch works with any physical switch that presents as a **Bluetooth HID keyboard** or is connected via **USB HID** (with a USB-A OTG adapter for Chromebook).

**Bluetooth switch (e.g., AbleNet Blue2, Zygo, Tecla):**

1. Put your switch into pairing mode (see your switch's manual).
2. On the Chromebook, open ChromeOS Settings > Bluetooth and pair the switch as you would any Bluetooth keyboard.
3. Once paired, the switch will send a keycode when pressed. By default, AccessSwitch maps **Space** to Switch 1 and **Enter** to Switch 2.
4. To remap the keycode: from the overlay select **Settings > Switch Configuration**, then tap **Capture Switch 1** (or Switch 2) and press your switch button to record the keycode.

**USB switch (via OTG adapter):**

1. Plug your USB switch into the Chromebook's USB-A port (or use a USB-C to USB-A adapter).
2. The switch should be recognised automatically as a keyboard device — no extra setup required.
3. Remap the keycode in Settings if needed (same steps as above).

---

## 8. Using Your Phone as a Switch for the Chromebook

Your Android phone can act as one or two large touch-zone buttons that control the Chromebook remotely over Bluetooth.

**One-time caregiver setup (approx. 5 minutes):**

1. On the Android phone, install AccessSwitch from the Play Store and enable its Accessibility Service (see Section 4).
2. Open AccessSwitch on the phone, go to **Settings > Phone Switch** and enable **BT HID Remote Mode**.
3. On the Chromebook, open ChromeOS Settings > Bluetooth and scan for new devices.
4. The phone should appear as **"AccessSwitch Switch"** — pair it.
5. Once paired, tapping the phone's Switch Screen will send key events to the Chromebook, which AccessSwitch receives and treats as switch presses.

**Daily use:**

- Open AccessSwitch on the phone and tap the large zone(s) to control the Chromebook overlay.
- Zone labels (e.g., **NEXT ▶** / **✓ SELECT**) are shown on the phone screen.
- The phone will vibrate and flash on each press to confirm it registered.

---

## 9. Settings Reference

Open Settings from the overlay (**Settings** tile) or from the AccessSwitch app home screen (**Open Settings** button). Settings are designed to be configured by a caregiver using touch.

| Setting | What it does |
|---------|-------------|
| **Scan Mode** | Auto-scan, Step-scan, or Inverse-scan |
| **Scan Speed** | Delay between highlights (0.5 s – 8 s) |
| **Switch Configuration** | Remap keycodes for Switch 1 and Switch 2; set debounce duration |
| **Phone Switch** | Configure touch-zone layout (full screen, left/right split, top/bottom split, asymmetric) and zone assignments |
| **BT HID** | Enable phone-as-remote-switch mode and configure emitted keycodes |
| **Appearance** | Highlight colour and style (border, fill, or both) |
| **Feedback** | Enable/disable auditory click and haptic feedback |
| **PIN Lock** | Set a PIN to prevent accidental changes to settings |

---

## 10. Troubleshooting

### The scanning overlay does not appear after enabling the Accessibility Service

- Close AccessSwitch completely (remove it from recent apps) and reopen it.
- Check that the Accessibility Service is still on: go back through ChromeOS Settings > Apps > Google Play Store > Manage Android preferences > Accessibility and confirm the AccessSwitch toggle is ON.
- On some Chromebook models, switching between windowed and full-screen mode for the AccessSwitch app can cause the overlay to reappear.

### The overlay appears but nothing is highlighted / scanning does not start

- Make sure at least one input source is active. Connect your switch or open the Phone Switch Screen on your phone.
- In Settings > Scan Mode, confirm a scan mode is selected and scan speed is not set too slowly.

### Netflix tile launches Netflix but the overlay disappears

- The Netflix overlay only appears once Netflix's window is in the foreground. Bring the Netflix window to full screen.
- If mode detection fails, the overlay will show a fallback set of controls. Select **Back to Menu** and tap **Netflix** again.

### My physical switch is not being detected

- Confirm the switch is paired/connected in Chromebook Bluetooth settings or plugged in via USB.
- In AccessSwitch Settings > Switch Configuration, use **Capture Switch 1** to record the exact keycode your switch sends.
- If the switch sends a keycode that AccessSwitch already uses for another function, it may be consumed before AccessSwitch sees it. Try remapping to a less common key.

### Scanning stops and shows a reconnection warning

- Your Bluetooth switch has disconnected. Scanning pauses automatically.
- Reconnect the switch via ChromeOS Bluetooth settings. Scanning resumes automatically once the device is detected again.

### I cannot find "Manage Android preferences" on my Chromebook

- Ensure the Play Store is enabled: ChromeOS Settings > Apps > Google Play Store > Turn on.
- On some enterprise or school-managed Chromebooks the Play Store (and Android apps) may be restricted by a system administrator. Contact your IT administrator if this is the case.
