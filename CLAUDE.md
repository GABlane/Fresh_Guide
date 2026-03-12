# FreshGuide Android — Project Context

## What it is
Dual-role campus navigation app for UCC students and admins.
Students find rooms, get directions, and sync offline data.
Admins manage buildings, floors, rooms, facilities, origins, routes, and publish data versions.

## Stack
- Language: Java
- Package: com.example.freshguide
- Min SDK: 24 (Android 7.0), Target SDK: 36
- Architecture: MVVM + Retrofit + Room + NavComponent
- UI: ConstraintLayout + Material3
- Theme: Theme.Material3.DayNight.NoActionBar
- Auth: Sanctum tokens stored in EncryptedSharedPreferences

## Backend
- Laravel 11 + Sanctum API
- Path: `/home/john/projects/AndroidStudioProjects/Fresh_Guide_BackEnd/laravel/`
- API base URL: set `api.base.url` in `local.properties` (ngrok required — ApiClient enforces HTTPS)

## Auth Roles
| Role | Login method |
|------|-------------|
| Student | Student ID only (no password) |
| Admin | Email + password |

## Key Screens
- LoginActivity — student ID / admin login, register dialog
- MainActivity — NavController + BottomNav + options menu
- HomeFragment — sync status, stats, quick actions
- RoomListFragment — search + RecyclerView
- RoomDetailFragment — facilities, Get Directions button
- OriginPickerFragment — setFragmentResult return flow
- DirectionsFragment — route steps RecyclerView
- ProfileFragment — student info + sync version + logout
- AdminDashboardFragment — counts + nav to CRUD screens
- Admin CRUD: Buildings, Floors, Rooms, Facilities, Origins, Routes, Publish

## Architecture Notes
- All admin list screens use GenericListAdapter + AdminViewModel
- All admin form screens use fragment_admin_form.xml (3 EditText fields + Save button)
- Room list uses RoomAdapter (ListAdapter + DiffUtil)
- Directions use RouteStepAdapter
- Offline data lives in Room DB, synced via /api/sync/bootstrap
- Admin actions are online-only (no offline admin)

## Risk Areas
- API_BASE_URL must be set before build — app throws at startup if blank
- ngrok URL changes every session — rebuild required after URL change
- EncryptedSharedPreferences — key rotation issues on device wipe

## Agent Routing Defaults
- Building agent: gpt-5.3-codex (high)
- Planner agent: claude-sonnet-3-5v2
