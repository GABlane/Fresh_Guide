# FreshGuide Agent Notes

## Navigation Rules
- Use NavComponent action IDs for in-app navigation (example: `R.id.action_home_to_roomDetail`).
- Avoid direct `navigate(R.id.someFragment)` calls from Home flows unless the graph explicitly supports that path.
- Wrap critical navigation calls with lightweight error handling so failures are visible during QA.

## Home/Floor Integration
- `HomeFragment` is the active controller for overall map + `map_floor_1..5` floor layouts.
- Floor room placeholders use tags (`room_box_n` and `room_label`) and are bound dynamically from Room DB.

## Admin UX
- Admin logout is available directly in `fragment_admin_dashboard.xml` via `btn_admin_logout`.
