package dev.chieppa.stretch.gallery

/** What the documents page shows. All of it is example content, not app text. */
internal object SampleDocuments {
    val markdown = """
        # Release notes

        This editor saves **plain markdown**, and blocks no one edits stay exactly as they were in the file. Use *emphasis*, ~~strikethrough~~, `inline code` and [links](https://example.com) as usual. Type `/` in an empty block for the block types.

        ## What changed

        - The render loop runs on its own thread
        - Scenes reload when their files change
          - Shaders too
        - [x] Write the migration notes
        - [ ] Update the screenshots

        1. Open the project
        2. Pick a theme
        3. Start drawing

        > A quote can run over a few lines
        > and keeps its inline **formatting**.

        > [!WARNING]
        > Saved layouts from before this release need one more save.

        ## How it fits together

        ```mermaid
        flowchart LR
            Input[Pen input] --> Stroke{Pressure?}
            Stroke -- yes --> Brush[Pressure brush]
            Stroke -- no --> Line[Plain line]
            Brush --> Canvas((Canvas))
            Line --> Canvas
        ```

        ```kotlin
        fun main() {
            val greeting = "Hello"  // A comment
            repeat(3) { println("${'$'}greeting, ${'$'}it") }
        }
        ```

        | Feature | Windows | macOS |
        |---------|:------:|------:|
        | Editor  | Yes     | Yes   |
        | Diagrams | Yes    | Yes   |

        ![A missing picture]()

        ---

        That is every kind of block.
    """.trimIndent()

    val patch = """
        diff --git a/src/render/Loop.kt b/src/render/Loop.kt
        index 83db48f..bf2a2a3 100644
        --- a/src/render/Loop.kt
        +++ b/src/render/Loop.kt
        @@ -12,9 +12,10 @@ class Loop(private val scene: Scene) {
             private var running = false

             fun start() {
        -        running = true
        -        thread { while (running) tick() }
        +        check(!running) { "Already running" }
        +        running = true
        +        thread(name = "render") { while (running) tick() }
             }

             private fun tick() {
        diff --git a/docs/old-notes.md b/docs/new-notes.md
        similarity index 100%
        rename from docs/old-notes.md
        rename to docs/new-notes.md
        diff --git a/assets/logo.png b/assets/logo.png
        Binary files a/assets/logo.png and b/assets/logo.png differ
    """.trimIndent()

    val oldCode = (
        listOf("package demo", "", "class Greeter(private val name: String) {", "    fun greet(): String {", "        return \"Hello \" + name", "    }") +
            (1..30).map { "    fun helper$it() = $it" } +
            listOf("}")
        ).joinToString("\n")

    val newCode = oldCode
        .replace("return \"Hello \" + name", "return \"Hello, \$name!\"")
        .replace("fun helper20() = 20", "fun helper20() = 20 * 2")

    val html = """
        <!doctype html>
        <html>
        <head>
          <style>
            body { font: 15px system-ui, sans-serif; margin: 32px; }
            .card { background: linear-gradient(135deg, #f5b83d, #d95926); color: #1a1416; border-radius: 12px; padding: 24px; }
            button { font: inherit; padding: 6px 14px; }
            input { font: inherit; padding: 5px; }
          </style>
        </head>
        <body>
          <h1>A real page</h1>
          <div class="card">Full CSS, and scripts: this button has been clicked <b id="count">0</b> times.</div>
          <p><button onclick="document.getElementById('count').textContent++">Click me</button>
             <input placeholder="Type here">
             <select><option>One</option><option>Two</option><option>Three</option></select></p>
          <p><a href="https://example.com">A link goes to the app, not the view.</a></p>
          <div style="height: 900px; border-left: 4px solid #3987e5; padding-left: 12px">A tall section, to scroll.</div>
        </body>
        </html>
    """.trimIndent()
}
