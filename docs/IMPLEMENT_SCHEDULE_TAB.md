# Feature Issue: Implement Student Schedule Tab

## Overview
Implement the "Schedule" tab to allow students to view their class schedules (Subject, Time, Room, Instructor) directly in the app. This feature should support offline viewing via the local Room database.

**Priority:** High
**Status:** Backlog

---

## 1. Technical Requirements

### A. Data Model (Android)
Create a `Schedule` entity for the Room database and a corresponding DTO for API responses.
```java
// Fields:
// int id
// String student_id
// String subject_code
// String subject_name
// String instructor
// String schedule_time (e.g., "MW 9:00 AM - 10:30 AM")
// String room_code
// int room_id (nullable, for linking to map)
```

### B. API Integration
- **Endpoint:** `GET /api/schedule` (requires Student token)
- **ApiService:** Add `Call<ApiResponse<List<ScheduleDto>>> getStudentSchedule();`
- **Sync:** Update the `BootstrapResponse` or add a new sync step to fetch and cache the schedule locally.

### C. UI Components
- **Fragment:** `ScheduleFragment.java`
- **Layout:** `fragment_schedule.xml` (RecyclerView with SwipeRefreshLayout)
- **Item Layout:** `item_schedule.xml` (CardView with subject info, room link, and time)
- **Empty State:** Show a "No classes scheduled" message if the list is empty.

---

## 2. Implementation Steps

### Step 1: Database & Network (Backend/Android)
- [ ] (Backend) Create `schedules` table and seed some data for student `20230054-S`.
- [ ] (Backend) Add `ScheduleController` with `index` method filtered by the authenticated student's ID.
- [ ] (Android) Create `ScheduleDao`, `Schedule` entity, and update `AppDatabase`.
- [ ] (Android) Create `ScheduleDto` and update `ApiService`.

### Step 2: Repository & ViewModel
- [ ] Create `ScheduleRepository` to handle local caching (Single Source of Truth).
- [ ] Create `ScheduleViewModel` to expose `LiveData<List<Schedule>>` to the UI.

### Step 3: UI Development
- [ ] Build `fragment_schedule.xml` with a clean, modern list design.
- [ ] Implement `ScheduleAdapter` (using `ListAdapter` and `DiffUtil`).
- [ ] Add a "View Room" button in each schedule card that navigates to the `RoomDetailFragment` using the `room_id`.

### Step 4: Navigation
- [ ] Replace the "Coming soon" Toast in the bottom navigation with an actual navigation action to `ScheduleFragment`.
- [ ] Update `nav_graph.xml` with the new fragment destination.

---

## 3. Success Criteria
- [ ] Student can log in and see their specific class schedule.
- [ ] Schedule persists offline (works without internet after first sync).
- [ ] Tapping on a room in the schedule takes the user to that room's detail page.
- [ ] Swipe-to-refresh correctly updates the schedule from the server.
