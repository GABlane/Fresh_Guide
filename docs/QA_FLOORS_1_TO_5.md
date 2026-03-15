# QA Issue: Campus Navigation & Data Integrity (Floors 1-5)

## Overview
This issue tracks the full Quality Assurance (QA) pass for the **FreshGuide** Android application, specifically focusing on the recent backend seed update covering all 5 floors of the UCC South Main Building.

**Assigned to:** @partner
**Priority:** High
**Status:** Ready for Testing

---

## 1. Test Environment Setup
- [ ] Pull latest `master` on Android.
- [ ] Pull latest `main` on Laravel Backend.
- [ ] Run `php artisan migrate:fresh --seed` to ensure the 5-floor data is active.
- [ ] Update `local.properties` with the current `ngrok` URL.

---

## 2. Data Verification (Floors 1-5)
Navigate through the **Admin > Floors** or **Home > Floor Chips** to verify:
- [ ] **Floor 1:** Verify Main Lobby, Registrar, and Finance offices exist.
- [ ] **Floor 2:** Verify Classrooms 201, 202, 203, and Lecture Room 221.
- [ ] **Floor 3:** Verify Computer Lab, MIS, and CS department offices.
- [ ] **Floor 4:** Verify LabTech facility and Law faculty offices/classrooms.
- [ ] **Floor 5:** Verify Auditorium Court and Psychology classrooms (501, 502).

---

## 3. Functional QA Checklist

### A. Authentication
- [ ] **Student Login:** Enter a valid Student ID (e.g., `20230054-S`). Verify it bypasses the password field.
- [ ] **Admin Login:** Use `admin@freshguide.com` / `password`. Verify the Admin Dashboard is accessible.
- [ ] **Logout:** Ensure session is cleared and redirects back to Login.

### B. Navigation & Routing
- [ ] **Search:** Search for "Computer Laboratory". Does it appear correctly?
- [ ] **Directions:** Trigger "Main Gate to Registrar" route. Verify all steps (Gate → Lobby → Office) display.
- [ ] **Floor Switching:** Tap floor chips 1 through 5. Does the room list/map update instantly?

### C. Admin CRUD (Sanity Check)
- [ ] **Create Room:** Add a test room on Floor 3.
  - *Note: Room Type must be lowercase (classroom, lab, office, restroom, other).*
- [ ] **Update Room:** Change the description of your test room.
- [ ] **Delete Room:** Remove the test room and verify it's gone from the user-side list.

---

## 4. Known Edge Cases to Test
- [ ] **Case Sensitivity:** Try creating a room with "LAB" or "Office" (Android should now auto-fix this to lowercase).
- [ ] **Empty States:** Check a building that has no rooms. Does it show a "No rooms found" message?
- [ ] **Offline Sync:** Tap the sync button in the Profile/Home. Does it successfully fetch the latest bootstrap data?

---

## 5. Bug Reporting Template
If a bug is found, please comment below with:
1. **Screen:** (e.g., RoomDetailFragment)
2. **Action:** (e.g., "Tapped Save button")
3. **Result:** (e.g., "App crashed" or "422 Error")
4. **Expected:** (e.g., "Room should be saved")
