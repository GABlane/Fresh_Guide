<p align="center">
  <img src="logo.png" alt="FreshGuide Logo" width="200"/>
</p>

<h1 align="center">FreshGuide</h1>
<h3 align="center">A Friendly Guide To UCC</h3>

<p align="center">
  An Android app that helps users quickly locate the nearest emergency exit in a building — no GPS, no internet required.
</p>

---

## Overview

FreshGuide displays a single-floor building layout of the UCC campus and guides users to the nearest emergency exit based on their manually selected location. Designed for use during fire drills, evacuations, and emergency situations.

## Features

- Select your current classroom or location manually
- Highlights the nearest emergency exit on the floor map
- Directional arrows guiding you toward the exit
- Safety reminders and evacuation instructions
- Works fully offline — no GPS, no sensors, no internet

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| UI | ConstraintLayout + Material3 |
| Min SDK | Android 7.0 (API 24) |
| Target SDK | Android 15 (API 36) |
| Build | Gradle 9.0.1 |

## Project Status

| Phase | Description | Status |
|---|---|---|
| Phase 1 | Multiple screen support | ✅ Done |
| Phase 2 | Floor map rendering + custom view | 🔄 In Progress |
| Phase 3 | Exit route logic + directional arrows | ⏳ Pending |
| Phase 4 | Safety reminders + evacuation instructions | ⏳ Pending |
| Phase 5 | Location selection UI | ⏳ Pending |

## Getting Started

### Prerequisites
- Android Studio (Ladybug or newer)
- Android SDK API 24+
- Java 11

### Setup
```bash
# Clone the repository
git clone https://github.com/your-username/FreshGuide.git

# Open in Android Studio
File → Open → select the FreshGuide folder

# Let Gradle sync, then run on emulator or device
```

### Recommended Emulators
| AVD | Screen | Density |
|---|---|---|
| Pixel 6 | 6.4" | xxhdpi |
| Pixel 4a | 5.8" | xxhdpi |
| Pixel Tablet | 10.95" | xhdpi |

## Project Structure

```
app/src/main/
  java/com/example/freshguide/
    SplashActivity.java       # Animated splash screen
    LoginActivity.java        # Login / account creation
    MainActivity.java         # Main map screen
  res/
    layout/                   # Phone layouts
    layout-sw600dp/           # Tablet layouts
    drawable/                 # Vector icons + logo mark
    drawable-*/               # Logo PNGs at each density
    values/                   # Colors, strings, dimens, themes
    values-sw600dp/           # Tablet dimen overrides
```

## Team

| Name | Role |
|---|---|
| **Gab** | Team Lead Developer |
| **Angela** | Frontend Developer |
| **Jovylyn** | Frontend Developer |
| **Joyce** | Frontend Developer |
| **Bryan** | Backend Developer |
| **Trisha** | Backend Developer |

---

<p align="center">
  University of the Cebu City — BSCS 3A, Group 2
</p>
