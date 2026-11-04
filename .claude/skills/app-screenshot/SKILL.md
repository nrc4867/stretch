---
name: app-screenshot
description: See the running Stretch gallery (or any app that calls DevTools.startIfRequested) by screenshotting only its own windows. Use when checking how a UI change looks in the real app, native title bar and popups included.
---

# Screenshot the app's own windows

`stretch-devtools` serves PNGs of the app's windows on 127.0.0.1. It captures just those windows through the OS (Windows `PrintWindow`), never the rest of the desktop, and works even when other windows cover the app. Windows only for now.

1. Start the gallery in the background with the endpoint on:
   `JAVA_HOME=$HOME/.jdks/corretto-25.0.1 ./gradlew :gallery:run -Pdevtools=47811`
   Wait for the log line `Stretch devtools: http://127.0.0.1:47811`.
2. List windows: `curl -s 127.0.0.1:47811/windows` (index, title, size).
3. Capture into the job's temp dir, then Read the PNG:
   `curl -s "127.0.0.1:47811/capture?scale=0.5" -o "$CLAUDE_JOB_DIR/tmp/shot.png"`
   - `window=<index or part of the title>` picks a window; default is the first.
   - `scale` 0.1 to 1; full scale is device pixels (2x on a 200% display), so 0.5 is usually enough.
   - 409 means the window is minimized; 404 means no window matched.
4. Stop the gallery before rebuilding (a rebuild under a running gallery breaks it with ClassNotFoundException).

An app drawing through OpenGL, as one with a native surface does, reads back unevenly through the OS: a band along the window's right and bottom edges can hold an earlier frame, so a status bar or rail there can show what an earlier screen had. Trust a UI test's semantics over that band.

The endpoint only captures; it does not click or type. To see a state that needs input (focus, hover, an open menu), render it offscreen in a throwaway test with `runComposeUiTest` and `captureToImage`, then delete the test.

Other apps opt in the same way: depend on `:stretch-devtools`, call `DevTools.startIfRequested()` at the start of `main`, and pass `-Dstretch.devtools=<port>` to the JVM.
