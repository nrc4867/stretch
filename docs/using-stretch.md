# Using Stretch from an app

## Platform

- **Appearance and size.** `StretchTheme(StretchThemes.system(light, dark), scale = size)` follows the OS's light or dark appearance and draws everything at the interface size a person picked. Popped-out drawer windows keep the same size.
- **Clipboard.** Mark a part of the app that has its own selection with `Modifier.clipboardTarget(router, handler)`; Ctrl C, X and V (Cmd on macOS) and the router's `actions()` go to whichever target has the focus. Text fields keep their own clipboard. In a browser the clipboard holds text only: custom data stays in the page, and pasting asks for the browser's permission.
- **Menus.** `BarMenus(menus)` in the `TopBar`'s `menus` slot gives File, Edit and so on inside the bar on Windows and Linux. On macOS `WindowTopBar` leaves them out, and `MacMenuBar` fills the system's menu bar instead.
- **macOS.** Ctrl in a `KeyChord` is Cmd on macOS, and key caps show the Mac symbols. Inside `StretchWindow`, `MacMenuBar(menus, registry, keymap, mode)` fills the menu bar from the app's actions and hooks up About, Settings and Quit; it shows nothing elsewhere.
- **Viewport.** Give `Viewport` a `ViewportCamera` for wheel, middle-button and two-finger navigation, and `rulers = true` with guides. Put `Modifier.marquee(camera)` and then `Modifier.transformHandles(...)` on the scene layer.
- **GPU viewport.** Put a `NativeSurface(state, renderer)` in the `Viewport`: a native window that a `SurfaceRenderer` draws `state` into on its own thread, with mouse, pen and touch passing through to the Compose UI under it. Any GPU API can implement `SurfaceRenderer`; `stretch-gpu`'s `WebGpuRenderer` runs a `GpuScene` with WebGPU. Compose cannot draw over the surface, so the scene draws a `ViewportOverlay` for gizmos and handles, and the app calls `NativeSurfaces.enable()` first thing in `main` so popups open in windows of their own over it. An app's own popups should use `StretchPopup` in place of Compose's `Popup`: it shows them at their final size and place, and moving one moves its window without redrawing it. With `Viewport(nativeContent = true)` the rulers sit beside the surface, the corner slots float over it in popups, and the app adds `guidesOverlay`, `marqueeOverlay` and `handlesOverlay` to its scene's overlay (with `Modifier.marquee(state, camera)` and `Modifier.transformHandleInput` for the pointer side). `Modifier.orbitNavigation(camera)` orbits a 3D camera with the middle button and the wheel; on a touch screen two fingers pan, pinch to move in and out, and twist to turn it. To find what is under a point, cast `camera.ray(at, width, height)` and test it with `Ray`'s plane, sphere, triangle and box hits, or give the surface a `SurfacePickQueue` and draw IDs in `GpuScene.renderIds` to pick on the GPU. Several views can share one surface, and so one swap chain: lay one `NativeSurface` over the whole area with a `Viewport(nativeContent = true)` per view on top of it, then give each view a `setViewport` and `setScissorRect` on the render pass and its own camera (`Mat4.orthographic` for a locked view). On the desktop surfaces are Windows only for now.
- **The web.** `stretchPage(title) { ... }` in a `wasmJsMain` entry point runs the app as a page, and `WindowTopBar` becomes the plain bar with the menus inside it. There are no pop-out drawers and no `MacMenuBar`. A `NativeSurface` is a canvas behind the Compose page, seen through a hole the surface clears, so Compose content and plain popups draw over it and `NativeSurfaces.enable()` is not needed; the scene can still draw a `ViewportOverlay`. Everything runs on the page's one thread, so a `SurfaceRenderer` renders at the browser's animation frame and long work blocks the UI. `webGpuAvailable()` says whether the browser offers WebGPU. A pen reaches Compose with its pressure but as `PointerType.Touch`, with no tilt. A browser keeps some shortcuts for itself, such as Ctrl N, T and W in a tab. Serve the site over HTTPS (`localhost` also works) for WebGPU, the clipboard and file access. Compose downloads fallback fonts from Google Fonts for characters the app's fonts lack.

## Documents

`stretch-document` keeps one open document: `DocumentSession(format, initial, documentStorage)` wraps its undo history and knows when it has unsaved changes (`rememberEdited`), even after undoing back to the save. `session.whenSaved { close() }` asks with `UnsavedChangesDialog` first when there are changes; `save`, `saveAs` and `open` use `rememberFileChooser()`, the platform's own dialogs. `Autosave(session, Recovery.forApp())` keeps a recovery copy while there are changes, and `Recovery.pending()` with `RecoveryDialog` offers them after a crash; storage, choosers and recovery are all `suspend`, since browsers answer later. `LeaveGuard(edited)` makes the browser ask before a tab with changes closes, and does nothing on the desktop. In a browser, documents live in files the person picks through the File System Access API, which also brings recent files back on a later visit once they allow it again; where a browser lacks that API, opening uses a file input and saving downloads the file. Recovery copies stay in IndexedDB, in that browser only. `RecentFiles` and `RecentFilesMenuItems` list recent files; keeping the list between runs is the app's job, like all its settings.

## Diffs

`stretch-diff`'s `DiffViewer(files)` shows `List<FileDiff>`: make them with `diffTexts(old, new, path)` from two whole texts, where a null path means that side has no file, or `parseUnifiedDiff(patch)` from `git diff` or `diff -u` output. Only rows on screen are built, so large diffs stay smooth. `rememberDiffViewerState(split, wrap)` holds the layout and scrolling, with `nextChange()` and `previousChange()`; `showBar = false` hides the built-in bar for an app with its own controls. `DiffSlots` adds `onLineClick`, `lineAnnotation` under a line for comment threads, and `fileActions` and `hunkActions` for buttons like Stage. Text is in `DiffStrings`. A file's language comes from its path.

## Markdown

`stretch-markdown`'s `MarkdownEditor(state)` edits `rememberMarkdownEditorState(text)`; read `state.text` for the markdown, and `state.setText` to replace it. `state.history` is a Stretch `History`, so a `DocumentSession` given that history tracks unsaved changes, and typing in one go is one undo step.

`MarkdownMode.LivePreview` shows the markers of the block with the caret, and `MarkdownMode.Wysiwyg` never shows them; in both, typing `## ` or `**word**` becomes formatting. Set the mode with `state.mode`. `modes` lists what the app offers: `modes = MarkdownMode.entries` puts a switch in the toolbar, one mode shows none. `readOnly = true` makes it a viewer.

A block is one heading, paragraph, list item, quote, GitHub alert (`> [!NOTE]`), fenced code, table, image or divider. Anything else, like HTML and front matter, stays as source. Blocks no one edits stay byte for byte, line endings included; an edited table is padded so its columns line up in the file. Cmd or Ctrl with B, I, E and K format the selection, Ctrl Shift X strikes through, Tab nests a list item, `/` in an empty block lists the block types, and Ctrl A twice or Shift with an arrow at a block's edge selects whole blocks to copy, cut or delete. Text selections stay within one block.

Pass `images` to load pictures, since Stretch has no network of its own, and `diagrams` to draw diagrams; without them pictures show as a marked placeholder and diagrams as code. `DiagramRenderer` can draw any language. `onLinkClick` gets a link clicked with Cmd or Ctrl held, or any click in a viewer. Text is in `MarkdownStrings`.

## Web pages and Mermaid

`stretch-web`'s `HtmlView(HtmlContent.Inline(html))` or `HtmlContent.Url(url)` shows a page. Pages cannot open windows, download files, show dialogs or ask for permissions. Links go to `navigation`, which keeps the view where it is unless it returns true, so an app can open them in the person's browser. `HtmlPolicy(scripts, network)` limits an inline document, and nothing in the document can lift that. An inline document with no background shows the theme behind it, and starts from the theme's text color and light or dark. `ScriptPage` runs a JavaScript library in a hidden page.

`stretch-mermaid`'s `MermaidDiagrams()` draws `mermaid` blocks: pass one, kept with `remember`, as the editor's `diagrams`. The first diagram takes a few seconds to start the engine and shows its code until then.

On the desktop the engine is the JCEF in the JetBrains Runtime. Run and package the app with one (see `gallery/build.gradle.kts` for the toolchain line), and apply the `dev.chieppa.stretch.app` plugin: it adds Chromium to the package when the app depends on `stretch-web` or `stretch-mermaid`, and leaves it out otherwise. On Windows that takes a package from about 145 MB to about 515 MB. Set `stretchApp { webEngine = true }` if they arrive through another library of the app's. `WebEngine.available` is false on a runtime without JCEF, where `HtmlView` shows `unavailable` and diagrams show as code. Input methods, such as those for CJK languages, do not reach a page.

In the browser `HtmlView` is a sandboxed iframe. It sits above the Compose canvas, so popups cannot cover it, a `Url` page's links stay in the frame, and with `scripts = false` links in an inline document do too. A site can refuse to be framed.

## App identity

The `dev.chieppa.stretch.app` Gradle plugin (in `stretch-gradle-plugin/`) gives the app one name, id and icon everywhere: the packaged launcher and installers, the taskbar, dock and Start menu, and Task Manager, which shows the launcher's name instead of "OpenJDK Platform binary". It's published with the modules, so add the same GitHub Packages repository to the app's `pluginManagement`, then apply it in the app module, next to the Compose plugin:

```kotlin
plugins { id("dev.chieppa.stretch.app") version "<version>" }

stretchApp {
    name = "Squash"
    id = "dev.chieppa.squash"
    icon = layout.projectDirectory.file("app-icon.png") // square PNG, 1024px best
}
```

The plugin covers desktop packaging. In a multiplatform module, add its generated resources to the desktop source set, as `gallery/build.gradle.kts` does.

It fills in Compose's `nativeDistributions` (name, description, numeric version, which on macOS is 1.0.0 until the app reaches 1.0 since macOS takes no version starting with 0, icons, macOS bundle id, Windows upgrade id) unless the app set them, and puts the icon and identity on the classpath so `StretchWindow` badges its windows at every size. `./gradlew :app:runDistributable` runs through the real launcher; plain `run` is still `java.exe` to Task Manager, but the window, taskbar button and taskbar grouping carry the app's icon and id.
