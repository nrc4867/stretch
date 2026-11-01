# Stretch

A shared app shell for creative tools on Kotlin Multiplatform and Compose: design tokens and themes, components, and the app plumbing every app needs (commands, undo, keys). Apps pick a theme and add only what is their own.

- Design: [Stretch: design](https://claude.ai/artifact/VtgHzTtWDUYiVa6eKSqpJR)
- Design system: [Stretch](https://claude.ai/artifact/1Ehyi2v5v8QB1nyCXCxPZF)

## Build, run and test

Needs JDK 25 for Gradle (`JAVA_HOME`); Gradle provisions the toolchain. Apps run on the JetBrains Runtime 25 (the gallery's run task picks it), whose custom title bar API lets `StretchWindow` draw its own title bar inside a native frame on Windows and macOS. On other runtimes and on Linux the window keeps the system title bar.

```
./gradlew check          # unit, contrast and component tests, generated-code checks
./gradlew :gallery:run   # every component in every theme
./gradlew :gallery:run -Pdevtools=47811   # also serves screenshots of its windows: curl 127.0.0.1:47811/capture
```

IntelliJ IDEA: open the folder; the Gallery and Check run configurations are included.

## Modules

| Module | Holds |
| --- | --- |
| `stretch-core` | Commands, undo and gestures, session log, actions and keymap, the Settled interface |
| `stretch-theme` | Tokens, the four themes, fonts, icons |
| `stretch-ui` | Components: buttons, toggle, slider, chips, segmented, fields, swatch, key caps, callout, progress, spinner, skeleton, grid, field icons and errors, checkbox, radio, tabs, badge, empty state, scrollbars, tooltip, tree, dropzone, sortable lists and grids, stats, charts, table, color picker, shading ramp, curve editor, angle dial, timeline |
| `stretch-frame` | The app frame: top bar, rails (reorderable, with a bottom group), resizable side and bottom drawers and their layout, menus, context menu, dropdown, popover, palette, dialog, toast, toolbar, viewport, view navigator, status bar, window title bar, action keys, key display |
| `stretch-input` | Pointer and pen samples; Windows Ink pen input on desktop |
| `stretch-devtools` | Development only: a 127.0.0.1 endpoint that screenshots the app's own windows (`-Pdevtools=<port>` on the gallery's run task) |
| `stretch-gradle-plugin` | The `dev.chieppa.stretch.app` plugin: app name, id and icon for launchers, installers and running windows |
| `stretch-test` | Command scenarios, a controllable Settled, Compose UI test helpers |
| `gallery` | Every component in every theme, and an app frame demo |

## Generated code

`stretch-theme/.../Tokens.kt` comes from `design/tokens.json` and `StretchIcons.kt` from `design/icons/*.svg`. Change the source, then run `./gradlew :stretch-theme:generateTokens :stretch-theme:generateIcons`; `check` fails when they are out of date.

## Using Stretch from an app

Include it as a composite build in the app's `settings.gradle.kts` and depend on `dev.chieppa.stretch:stretch-ui` (and `stretch-core`).

### App identity

The `dev.chieppa.stretch.app` Gradle plugin (in `stretch-gradle-plugin/`) gives the app one name, id and icon everywhere: the packaged launcher and installers, the taskbar, dock and Start menu, and Task Manager, which shows the launcher's name instead of "OpenJDK Platform binary". Add `includeBuild("<stretch>/stretch-gradle-plugin")` to the app's `pluginManagement`, then in the app module, next to the Compose plugin:

```kotlin
plugins { id("dev.chieppa.stretch.app") }

stretchApp {
    name = "Squash"
    id = "dev.chieppa.squash"
    icon = layout.projectDirectory.file("app-icon.png") // square PNG, 1024px best
}
```

It fills in Compose's `nativeDistributions` (name, description, numeric version, icons, macOS bundle id, Windows upgrade id) unless the app set them, and puts the icon and identity on the classpath so `StretchWindow` badges its windows at every size. `./gradlew :app:runDistributable` runs through the real launcher; plain `run` is still `java.exe` to Task Manager, but the window, taskbar button and taskbar grouping carry the app's icon and id.

## License

`AGPL-3.0-only`; see `LICENSE` and `NOTICE`. Contributions need the CLA in `CLA.md`. Bundled fonts are under the SIL Open Font License (`licenses/`).
