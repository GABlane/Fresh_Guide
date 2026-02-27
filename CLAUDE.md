# FreshGuide — Project Context

## What it is
Emergency Exit Route Guide System — Android app displaying a single-floor building layout for UCC campus.

## Core Features
- Users manually select their classroom/location
- App highlights nearest emergency exit
- Shows directional arrows toward the exit
- Displays safety reminders and evacuation instructions
- No GPS, no real-time tracking, no sensor integration

## Stack
- Language: Java
- Package: com.example.freshguide
- Min SDK: 24 (Android 7.0)
- Target SDK: 36
- UI: ConstraintLayout + Material3
- Theme: Theme.Material3.DayNight.NoActionBar

## Screen Layout (from design)
- Top: FreshGuide logo
- Search bar + filter icon
- Horizontal chip row: Library, CL1, CL2, Registrar, Accounting...
- Main area: Floor map (canvas/custom view) with labeled buildings
- Bottom nav: Home, Schedule, Settings, Profile
- FAB: Navigation/direction button (bottom right)
- Orange accent bars on left side

## Building Locations on Map
- Registrar
- Court
- Library
- UCC South Main Building
- Entrance
- Exit

## Phases
- [ ] Phase 1: Multiple screen support (current)
- [ ] Phase 2: Floor map rendering + building layout
- [ ] Phase 3: Exit route logic + directional arrows
- [ ] Phase 4: Safety reminders + evacuation instructions
- [ ] Phase 5: Location selection UI
- [ ] Phase 6: Polish + accessibility

## Risk Areas
- Floor plan image scaling/distortion
- Custom canvas drawing for arrows/overlays
- Multi-density drawable management
