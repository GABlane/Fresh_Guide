# Feature Issue: Full Dark Mode Support

## Overview
Implement comprehensive Dark Mode support across the entire FreshGuide application. The app currently uses `DayNight` themes but lacks a specific dark color palette, resulting in sub-optimal readability and aesthetics when the system theme is switched.

**Priority:** Medium
**Status:** Backlog

---

## 1. Technical Requirements

### A. Color Palette (Dark)
Create a new `app/src/main/res/values-night/colors.xml` file with appropriate dark-surface colors.
```xml
<resources>
    <color name="surface_white">#121212</color> <!-- Dark background -->
    <color name="text_primary">#E1E1E1</color>  <!-- Light text -->
    <color name="green_pill">#1B3A1B</color>     <!-- Darker green for pills -->
    <color name="input_background">#1E1E1E</color>
</resources>
```

### B. Theme Refactoring
- **Base Theme:** Ensure `Base.Theme.FreshGuide` in `res/values/themes.xml` correctly references semantic colors (e.g., using `@color/surface_white` which will now vary by mode).
- **Hardcoded Colors:** Identify and replace any hardcoded `#FFFFFF` or `#000000` in layout XML files with semantic color references from `colors.xml`.

### C. Asset Optimization
- [ ] **Vectors:** Ensure `ic_logo_mark.xml` and other icons use `?attr/colorControlNormal` or specific semantic colors instead of hardcoded hex values.
- [ ] **Illustrations:** If onboarding images have white backgrounds, create dark-mode variants or use transparent backgrounds.

---

## 2. Implementation Steps

### Step 1: Resource Setup
- [ ] Create `res/values-night/colors.xml`.
- [ ] Define the dark mode equivalents for all primary surface, background, and text colors.

### Step 2: Layout Cleanup
- [ ] Audit `fragment_home.xml`, `fragment_room_detail.xml`, and `activity_login.xml`.
- [ ] Replace `android:textColor="#1C1B1F"` with `android:textColor="@color/text_primary"`.
- [ ] Replace `android:background="#FFFFFF"` with `android:background="@color/surface_white"`.

### Step 3: Settings Integration (Optional but Recommended)
- [ ] Add a "Theme" toggle in the **Settings** tab.
- [ ] Options: "Follow System", "Light", "Dark".
- [ ] Use `AppCompatDelegate.setDefaultNightMode()` to persist the user's choice.

### Step 4: Component Polish
- [ ] **Cards:** Update `MaterialCardView` backgrounds to ensure they remain distinct from the dark background (e.g., using a slightly lighter gray `#1E1E1E` for cards).
- [ ] **Dividers:** Ensure dividers are visible but subtle in dark mode.

---

## 3. Success Criteria
- [ ] The app is fully readable and aesthetically pleasing when system dark mode is enabled.
- [ ] No "white flashes" occur during activity transitions (especially the Splash screen).
- [ ] Icons and logos remain high-contrast and visible.
- [ ] Text maintains an accessibility ratio of at least 4.5:1 against dark backgrounds.
