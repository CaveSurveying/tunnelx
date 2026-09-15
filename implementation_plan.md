# Implementation Plan for SVG Background Import (JSVG)

## Goal
Add support for importing SVG files as background images in TunnelX sketches using the JSVG library. The SVG will be referenced from its original location; no copying.

## Design Decisions (Confirmed)
- **Library**: JSVG only.
- **Display Target**: Sketch background via existing rendering pipeline.
- **File Handling**: Reference original SVG path; do not copy.

## Changes Required
### src/MainBox.java
- Add menu item **"Import SVG…"** under **File** menu.
- Wire its ActionListener to call `SvgImporter.importSvg(this)`.
- Remove any duplicate imports for `JFileChooser`, `FileNameExtensionFilter`, and `File`.

### src/SvgImporter.java (new)
- Provide a static method `importSvg(MainBox parent)`.
- Show a `JFileChooser` filtered for `.svg` files.
- Load selected SVG via JSVG, render to a `BufferedImage`.
- Set the image as the sketch background using `ImageWarp.setBackImage` (or equivalent).
- Perform loading on a background thread (`SwingWorker`) to keep UI responsive.

### src/ImageWarp.java
- Extend `SketchBackground` to handle `FA_FILE_SVG`.
- When a SVG file is detected, invoke `SvgImporter` to obtain a `BufferedImage` and draw it.

### src/FileAbstraction.java
- Ensure `FA_FILE_SVG` constant already exists (line 87) and `GetFileType()` returns it for `.svg` extensions (already handled).

### Build Adjustments
- Instruct user to place `jsvg.jar` (and its dependencies) in a `lib/` directory.
- Compile with classpath `-cp "lib/jsvg.jar"`.

## Verification Plan
1. Compile the project: `javac -cp "lib/jsvg.jar" src/*.java`.
2. Run the application: `java Tunnel.MainBox`.
3. Use **File → Import SVG…** to select an SVG file.
4. Confirm the SVG renders as the sketch background.
5. Verify no UI freezes; loading should occur off the EDT.
6. Check that the original SVG file remains in its location and is not copied.

---
*All decisions have been confirmed. Proceed with implementation.*

## Goal Description
Add support for importing SVG files as background images in TunnelX sketches, using the JSVG library. The SVG file will be referenced from its original location (no copy). The imported SVG will be rendered as the sketch background.

## Design Decisions (User confirmed)
- **Library:** JSVG only.
- **Display target:** Sketch background via existing rendering pipeline.
- **File handling:** Reference original SVG location; do not copy.

## Proposed Changes
---
### src/MainBox.java
- Add a new menu item `miImportSVG` under the **File** menu.
- Wire its `ActionListener` to invoke `SvgImporter.importSvg(this)`.
- Clean up any duplicate imports for `JFileChooser`, `FileNameExtensionFilter`, and `File`.

### src/SvgImporter.java [NEW]
- Public class with a static method `importSvg(MainBox parent)`.
- Shows a `JFileChooser` filtered for `.svg` files.
- Loads the selected SVG via JSVG and returns a `BufferedImage`.
- Updates the UI by setting the image as the current sketch background (e.g., via `ImageWarp.setBackImage`).

### src/ImageWarp.java
- Extend `SketchBackground` handling to recognize `FA_FILE_SVG`.
- When a SVG file is detected, call `SvgImporter` to obtain a `BufferedImage` and render it.

### Build Adjustments
- Instruct the user to place `jsvg.jar` (and its dependencies) in a `lib/` directory.
- Compile with `javac -cp "lib/jsvg.jar" src/*.java`.

## Verification Plan
### Manual Verification
- Launch `java Tunnel.MainBox`.
- Use **File → Import SVG…** to select an SVG file.
- Verify the SVG appears correctly as the sketch background.
- Ensure the UI remains responsive; loading should occur off the EDT using `SwingWorker`.

---
*Implementation plan reflects the confirmed choices. Proceed with code changes.*
