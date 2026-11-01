# Stretch

A shared app shell for creative tools on Kotlin Multiplatform and Compose: design tokens and themes, components, and the app plumbing every app needs (commands, undo, keys). Apps pick a theme and add only what is their own.

- Design: [Stretch: design](https://claude.ai/artifact/VtgHzTtWDUYiVa6eKSqpJR)
- Design system: [Stretch](https://claude.ai/artifact/1Ehyi2v5v8QB1nyCXCxPZF)

## Build, run and test

Needs JDK 25 for Gradle (`JAVA_HOME`); Gradle provisions the toolchain.

```
./gradlew check          # unit, contrast and component tests, generated-code checks
./gradlew :gallery:run   # every component in every theme
```

IntelliJ IDEA: open the folder; the Gallery and Check run configurations are included.

## Modules

| Module | Holds |
| --- | --- |
| `stretch-core` | Commands, undo and gestures, session log, actions and keymap, the Settled interface |
| `stretch-theme` | Tokens, the four themes, fonts, icons |
| `stretch-ui` | Components: buttons, toggle, slider, chips, segmented, fields, swatch, key caps, callout, progress, spinner, skeleton, grid, field icons, dropzone, sortable lists and grids, stats, charts, table |
| `stretch-frame` | The app frame: top bar, rails (reorderable, with a bottom group), resizable drawers and their layout, menus, dropdown, popover, palette, dialog, tooltip, toast, toolbar, viewport, action keys, key display |
| `stretch-input` | Pointer and pen samples; Windows Ink pen input on desktop |
| `stretch-test` | Command scenarios, a controllable Settled, Compose UI test helpers |
| `gallery` | Every component in every theme, and an app frame demo |

## Generated code

`stretch-theme/.../Tokens.kt` comes from `design/tokens.json` and `StretchIcons.kt` from `design/icons/*.svg`. Change the source, then run `./gradlew :stretch-theme:generateTokens :stretch-theme:generateIcons`; `check` fails when they are out of date.

## Using Stretch from an app

Include it as a composite build in the app's `settings.gradle.kts` and depend on `dev.chieppa.stretch:stretch-ui` (and `stretch-core`).

## License

`AGPL-3.0-only`; see `LICENSE` and `NOTICE`. Contributions need the CLA in `CLA.md`. Bundled fonts are under the SIL Open Font License (`licenses/`).
