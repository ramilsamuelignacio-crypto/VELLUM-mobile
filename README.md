# Vellum — Sketching Studio (Android)

Vellum is a native Android sketching studio built with **Kotlin**, **Jetpack Compose**, and **Material 3**, rewritten from the original React/TypeScript web application.

## Features

- **Expressive Brush & Drawing Engine**:
  - **7 Tools**: Pen, Highlighter, Eraser (offscreen `DST_OUT` compositing so dot grids & paper stay crisp), Line, Rectangle, Circle, and Select (with bounding box and draggable rotation handle).
  - **3 Brush Engines**: Hard Round (`Ink`) with variable-width quadratic pressure ribbon, textured `HB Pencil` with cached procedural grain stamps, and `Soft Airbrush` with radial gradient stamps.
  - **Stroke Stabilization & Pressure**: Adjustable line smoothing (`0–85%`), stylus/touch pressure sensitivity toggle, 6 curated ink swatches + custom RGB color picker dialog, and stroke widths (`1–32px`).
- **Infinite Canvas, Symmetry & Guides**:
  - Two-finger pinch-to-zoom (`25% – 400%`), two-finger pan, and a dedicated touch Pan toggle.
  - Symmetry modes (`Off`, `Horizontal`, `Vertical`, `Radial` 6-fold, `Kaleidoscope` 12-fold) with world-space recentering.
  - `Perspective` and `Ruler` guides, toggleable dot grid, subtle paper grain, and shape alignment snapping guides.
- **Layer Management & Reference Images**:
  - Create, select, hide/show, and reorder layers.
  - Pin a draggable floating reference image with adjustable opacity (`15% – 100%`) using the zero-permission Android Photo Picker (`PickVisualMedia`).
- **Rooms, QR Invite & PNG Export**:
  - Room switching (`studio-1` default, `vellum://room/<code>` deep link support) with local SQLite persistence (`VellumDatabaseHelper`).
  - `RoomShareModal` with ZXing-generated scannable QR code, copy room code/link, share QR image, create fresh room, and join by code.
  - Full-canvas PNG export to `Pictures/Vellum` via `MediaStore` and system share sheet (`FileProvider`).
- **Adaptive Studio Workspace**:
  - Phone/Medium layout with `TouchQuickBar`, bottom `TouchControlBar`, and `TouchBottomSheet` (`Tools`, `Canvas`, `Layers`).
  - Tablet/Expanded layout with `ToolDock` rail, floating HUDs, and `LayerPanel` sidebar.

## Architecture Notes & TODOs

- **Local Persistence (`SketchRepository` + `VellumDatabaseHelper`)**: Strokes and layers persist per room locally in SQLite.
- **Real-Time Multi-User Sync (TODO)**: Live multi-user WebSocket stroke/cursor broadcasting from the original Express/`ws` backend is not connected in standalone offline mode (`okhttp` is declared and commented out in `app/build.gradle.kts` ready for wiring to a WebSocket endpoint).

## Build Requirements

- Android Gradle Plugin `9.1.1`, Gradle `9.3.1`, JDK `21`, Android SDK `36` (`minSdk = 26`, `targetSdk = 36`).
