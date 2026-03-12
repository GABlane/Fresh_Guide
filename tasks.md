# Tasks — FreshGuide

> Last updated: 2026-03-11

## In Progress

## Pending
- [ ] Configure emulators for phone + tablet testing [high]
- [ ] Phase 3: Exit route logic + directional arrows [medium]
- [ ] Phase 4: Safety reminders + evacuation instructions [medium]
- [ ] Phase 5: Location selection UI [medium]
- [ ] Room info modal (floor layout) [medium]
- [ ] Registration screen (Create New Account flow) [low]
- [ ] Admin screen UI polish — layouts are functional stubs [medium]

## Before March 18 (backend integration)
- [ ] Set real backend URL in `ApiClient.java` (currently `10.0.2.2:8000` — emulator localhost) [high]
- [ ] Wire Schedule + Settings to real screens in custom nav bar (currently "Coming soon") [high]
- [ ] Test sync bootstrap end-to-end (student login → sync → room list loads) [high]

## Blocked

## Completed
- [x] Custom bottom nav bar (pill) — 2026-03-11
  - [x] Replaced BottomNavigationView with custom layout (icon + label stack)
  - [x] Active pill background; Home/Profile wired; Schedule/Settings placeholders
- [x] MAIN floor layout screen — 2026-03-11
  - [x] FloorLayoutFragment + chips 1–4 + fixed 10 room slots
  - [x] Guard desk shown only on 1st floor
  - [x] MAIN tap → floor layout; room tap → RoomDetail
- [x] Directions bottom sheet — 2026-03-11
  - [x] Home FAB opens sheet; room list from local DB; Start Directions triggers route fetch; empty states
- [x] Backend seed — campus buildings + MAIN floors 1–4 — 2026-03-11
  - [x] 10 rooms per floor (Room 101..110, 201..210, 301..310, 401..410)
  - [x] Route seed updated to Room 101
- [x] Home page campus map — 2026-03-11
  - [x] `CampusMapView.java` tuned to target composition (positions, gate lines, dashed entrance/exit)
  - [x] Pinned labels; removed user marker
  - [x] Building tap: MAIN → floor layout; others → room list
  - [x] Directions FAB (long‑press re‑centre)
  - [x] `fragment_home.xml` adjusted — logo/search/chips/map spacing + white FAB
  - [x] Floor chips: 1st–4th, pill style with white fill when unselected
- [x] Room integration — 2026-03-10
  - [x] `RoomDao.searchByBuilding(code, query)` — JOIN rooms/floors/buildings LiveData query
  - [x] `RoomRepository.searchRoomsByBuilding()` wrapper
  - [x] `RoomListViewModel` — MediatorLiveData combining query + buildingCode filters
  - [x] `RoomListFragment` — reads `buildingCode`/`buildingName` args, shows "Rooms in X" header
  - [x] `nav_graph.xml` — added `buildingCode` + `buildingName` args to `roomListFragment`
- [x] Backend seed — 6 UCC campus buildings + Ground Floor — 2026-03-10
  - [x] `CampusDataSeeder.php` — MAIN, COURT, LIB, REG, ENT, EXIT with descriptions
  - [x] `php artisan migrate:fresh --seed` verified clean
- [x] Project initialized — CLAUDE.md, tasks.md, .gitignore, README.md — 2026-02-28
- [x] Phase 1: Multiple screen support — 2026-02-28
  - [x] Resource qualifiers (layout-sw600dp, values-sw600dp, values/dimens.xml)
  - [x] Color palette and themes (green #29A829, orange #FFA500, Material3)
  - [x] Adaptive launcher icon — ic_launcher_foreground.xml (vector, centered in safe zone)
  - [x] Logo mark vectorized from PNG via potrace → ic_logo_mark.xml
  - [x] logo_with_text.png scaled to all 5 density folders (mdpi → xxxhdpi)
  - [x] Responsive layouts — max-width 480dp card, no stretching on tablet/desktop
- [x] Splash screen — 3-step animation: mark → full logo → spinner → login — 2026-02-28
- [x] Login screen — username/password/toggle/sign in/create account — 2026-02-28
- [x] .gitignore — build outputs, .gradle, .idea state, keystore, OS files — 2026-02-28
- [x] README.md — logo, overview, team credits, phase tracker — 2026-02-28
- [x] Initial commit pushed to GitHub (GABlane/Fresh_Guide) — 2026-02-28
- [x] Dashboard screen — header, greeting, search, 2×2 action cards, recently viewed, bottom nav — 2026-02-28
