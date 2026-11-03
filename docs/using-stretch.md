# Using Stretch from an app

## Platform

- **Appearance and size.** `StretchTheme(StretchThemes.system(light, dark), scale = size)` follows the OS's light or dark appearance and draws everything at the interface size a person picked. Popped-out drawer windows keep the same size.
- **Clipboard.** Mark a part of the app that has its own selection with `Modifier.clipboardTarget(router, handler)`; Ctrl C, X and V (Cmd on macOS) and the router's `actions()` go to whichever target has the focus. Text fields keep their own clipboard.
- **macOS.** Ctrl in a `KeyChord` is Cmd on macOS, and key caps show the Mac symbols. Inside `StretchWindow`, `MacMenuBar(menus, registry, keymap, mode)` fills the menu bar from the app's actions and hooks up About, Settings and Quit; it shows nothing elsewhere.
- **Viewport.** Give `Viewport` a `ViewportCamera` for wheel, middle-button and two-finger navigation, and `rulers = true` with guides. Put `Modifier.marquee(camera)` and then `Modifier.transformHandles(...)` on the scene layer.
- **GPU viewport.** Put a `NativeSurface(state, renderer)` in the `Viewport`: a native window that a `SurfaceRenderer` draws `state` into on its own thread, with mouse, pen and touch passing through to the Compose UI under it. Any GPU API can implement `SurfaceRenderer`; `stretch-gpu`'s `WebGpuRenderer` runs a `GpuScene` with WebGPU. Compose cannot draw over the surface, so the scene draws a `ViewportOverlay` for gizmos and handles, and the app calls `NativeSurfaces.enable()` first thing in `main` so popups open in windows of their own over it. With `Viewport(nativeContent = true)` the rulers sit beside the surface, the corner slots float over it in popups, and the app adds `guidesOverlay`, `marqueeOverlay` and `handlesOverlay` to its scene's overlay (with `Modifier.marquee(state, camera)` and `Modifier.transformHandleInput` for the pointer side). `Modifier.orbitNavigation(camera)` orbits a 3D camera with the middle button and the wheel; on a touch screen two fingers pan, pinch to move in and out, and twist to turn it. To find what is under a point, cast `camera.ray(at, width, height)` and test it with `Ray`'s plane, sphere, triangle and box hits, or give the surface a `SurfacePickQueue` and draw IDs in `GpuScene.renderIds` to pick on the GPU. Windows only for now.

## Documents

`stretch-document` keeps one open document: `DocumentSession(format, initial, FileStorage)` wraps its undo history and knows when it has unsaved changes (`rememberEdited`), even after undoing back to the save. `session.whenSaved { close() }` asks with `UnsavedChangesDialog` first when there are changes; `save`, `saveAs` and `open` use `rememberFileChooser(window)`, the OS's own dialogs. `Autosave(session, Recovery.forApp())` keeps a recovery copy while there are changes, and `Recovery.pending()` with `RecoveryDialog` offers them after a crash. `RecentFiles` and `RecentFilesMenuItems` list recent files; keeping the list between runs is the app's job, like all its settings.

## App identity

The `dev.chieppa.stretch.app` Gradle plugin (in `stretch-gradle-plugin/`) gives the app one name, id and icon everywhere: the packaged launcher and installers, the taskbar, dock and Start menu, and Task Manager, which shows the launcher's name instead of "OpenJDK Platform binary". Apply it in the app module, next to the Compose plugin:

```kotlin
plugins { id("dev.chieppa.stretch.app") }

stretchApp {
    name = "Squash"
    id = "dev.chieppa.squash"
    icon = layout.projectDirectory.file("app-icon.png") // square PNG, 1024px best
}
```

It fills in Compose's `nativeDistributions` (name, description, numeric version, icons, macOS bundle id, Windows upgrade id) unless the app set them, and puts the icon and identity on the classpath so `StretchWindow` badges its windows at every size. `./gradlew :app:runDistributable` runs through the real launcher; plain `run` is still `java.exe` to Task Manager, but the window, taskbar button and taskbar grouping carry the app's icon and id.
