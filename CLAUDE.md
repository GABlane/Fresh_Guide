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
- Room images: POST/DELETE api/admin/rooms/{id}/image endpoints

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
- AdminRoomFormFragment — room CRUD with image upload/preview
- Admin CRUD: Buildings, Floors, Rooms, Facilities, Origins, Routes, Publish

## Architecture Notes
- All admin list screens use GenericListAdapter + AdminViewModel
- All admin form screens use fragment_admin_form.xml (3 EditText fields + Save button)
- Room list uses RoomAdapter (ListAdapter + DiffUtil)
- Directions use RouteStepAdapter
- Offline data lives in Room DB, synced via /api/sync/bootstrap
- Admin actions are online-only (no offline admin)
- Room images stored via multipart upload to Laravel backend
- Gallery selection via ActivityResultContract with landscape validation
- Image compression for optimal upload performance

## Risk Areas
- API_BASE_URL must be set before build — app throws at startup if blank
- ngrok URL changes every session — rebuild required after URL change
- EncryptedSharedPreferences — key rotation issues on device wipe
- NavComponent calls should use action IDs (for example `action_home_to_roomDetail`) instead of direct fragment IDs to avoid silent navigation failures

## Recent Updates
- Home map + floor map integration lives in `HomeFragment` with custom `map_floor_1..5` layouts
- Admin dashboard now has a dedicated logout button (`btn_admin_logout`)
- Room card click regression fixed by restoring `action_home_to_roomDetail` navigation from Home floor layouts

## Agent Routing Defaults
- Building agent: gpt-5.3-codex (high)
- Planner agent: claude-sonnet-3-5v2

## AI Workflow (Claude Code ↔ Codex Bridge)

This project uses a two-agent system:
- **Claude Code** — plans, delegates, synthesizes. Talks to the user.
- **Codex** — executes. Writes code, runs commands, fixes builds.

### Bridge Folder
```
~/ai-bridge/
  inbox/    ← Claude drops TASK-*.md files here
  outbox/   ← Codex writes TASK-*.result.md files here
  archive/  ← Completed pairs (do not touch)
  status.json
```

### How to delegate a task to Codex
Claude Code writes a `TASK-NNN.md` file to `~/ai-bridge/inbox/` in this format:
```
---
task_id: TASK-001
created: <ISO timestamp>
mode: L1 | L2 | L3
priority: low | normal | high
status: pending
project: Fresh_Guide
project_dir: /Users/gearworxdev/Projects/Fresh Guide/Fresh_Guide
---

## Context
Why this task exists.

## Steps
1. Do this
2. Then this

## Files to Touch
- path/to/file.java

## Success Criteria
- Build passes
- Feature works as described
```

### Worker commands
```bash
bridge-launch    # start the worker (auto-picks up tasks)
bridge-logs      # tail live logs
bridge-stop      # stop the worker
bridge-verify    # check status
```

### Task numbering
Check `~/ai-bridge/inbox/` and `~/ai-bridge/archive/` for the latest TASK-NNN to determine the next number.
