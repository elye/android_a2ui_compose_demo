package com.example.a2uilibrarydemo.a2ui

/**
 * A single tutorial "prompt" that demonstrates one (or a related group) of A2UI Basic Catalog V1
 * components. Each prompt is expressed as a sequence of A2UI protocol JSON-Lines messages,
 * mimicking what an AI agent server would stream to the client to build up a Compose UI surface.
 *
 * The [messages] list always starts with a `version` message, followed by a `createSurface`
 * message, followed by one or more `updateComponents` messages. The very first `updateComponents`
 * message MUST define a component with id `"root"`, since the A2UI Compose renderer always shows
 * the component named `"root"` as the entry point of a surface.
 */
data class DemoPrompt(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val messages: List<String>,
)

/** Fixed id used for every demo surface; only one surface is ever active at a time. */
const val DEMO_SURFACE_ID = "demo-surface"

/** The canonical A2UI Basic Catalog V1 identifier, as published at a2ui.org. */
const val BASIC_CATALOG_V1_ID = "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"

/**
 * Every A2UI protocol message is its own self-contained JSON envelope that MUST carry the
 * `"version"` field alongside the message-type key (e.g. `createSurface`, `updateComponents`) in
 * the very same object — the parser validates `version` per-line, not once for the whole stream.
 */
private const val PROTOCOL_VERSION = "v0.9"

private fun createSurfaceMessage(catalogId: String = BASIC_CATALOG_V1_ID) =
    """{"version":"$PROTOCOL_VERSION","createSurface":{"surfaceId":"$DEMO_SURFACE_ID","catalogId":"$catalogId"}}"""

/** Wraps a raw JSON object describing the `updateComponents.components` array contents. */
private fun updateComponents(componentsJson: String) =
    """{"version":"$PROTOCOL_VERSION","updateComponents":{"surfaceId":"$DEMO_SURFACE_ID","components":[$componentsJson]}}"""

/**
 * Seeds a value in the surface's data model at [path]. This is REQUIRED for any component
 * property that should be interactively writable (e.g. CheckBox's `value`, TextField's `value`,
 * Slider's `value`, ChoicePicker's `value`, DateTimeInput's `value`): the A2UI Compose runtime's
 * `bindUpdater()` only produces a working write-callback (and thus an enabled/interactive widget)
 * when the corresponding JSON property is expressed as a data-binding reference
 * `{"path":"<path>"}` rather than a plain literal. A plain literal only ever flows through the
 * read-only `bind()` path, so the component renders correctly but its `onValueChange`/
 * `onCheckedChange` callback is null and the widget is effectively disabled — this is exactly
 * why CheckBox (and friends) previously rendered but didn't respond to taps.
 */
private fun updateDataModel(path: String, valueJson: String): String {
    // updateDataModel requires an ABSOLUTE path (leading "/"), unlike a component's own
    // "path" data-binding reference which is resolved relative to the surface's root and
    // works fine without one. Confirmed via the runtime's own validation error message:
    // "Data model update path must be absolute".
    val absolutePath = if (path.startsWith("/")) path else "/$path"
    return """{"version":"$PROTOCOL_VERSION","updateDataModel":{"surfaceId":"$DEMO_SURFACE_ID","path":"$absolutePath","value":$valueJson}}"""
}

/** Builds the standard header used by every demo: just the createSurface message. */
private fun header(catalogId: String = BASIC_CATALOG_V1_ID): List<String> =
    listOf(createSurfaceMessage(catalogId))

/**
 * The full list of tutorial prompts, one (or a few) per A2UI Basic Catalog V1 component,
 * grouped by category for display in the picker list.
 */
val demoPrompts: List<DemoPrompt> = listOf(
    DemoPrompt(
        id = "text",
        title = "Text",
        description = "Show every Text style variant: h1 through h5, caption, and body.",
        category = "Content",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["t-h1","t-h2","t-h3","t-h4","t-h5","t-caption","t-body"]},
            {"id":"t-h1","component":"Text","text":"Heading 1","variant":"h1"},
            {"id":"t-h2","component":"Text","text":"Heading 2","variant":"h2"},
            {"id":"t-h3","component":"Text","text":"Heading 3","variant":"h3"},
            {"id":"t-h4","component":"Text","text":"Heading 4","variant":"h4"},
            {"id":"t-h5","component":"Text","text":"Heading 5","variant":"h5"},
            {"id":"t-caption","component":"Text","text":"This is a caption.","variant":"caption"},
            {"id":"t-body","component":"Text","text":"This is body text. It is the default text style, well suited for paragraphs of content.","variant":"body"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "image",
        title = "Image",
        description = "Show the Image component across its variant and fit options.",
        category = "Content",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["img-row1","img-caption"]},
            {"id":"img-row1","component":"Row","children":["img-icon","img-avatar","img-medium"],"justify":"spaceEvenly"},
            {"id":"img-icon","component":"Image","url":"https://picsum.photos/seed/a2ui-icon/64","variant":"icon","description":"Icon-sized image"},
            {"id":"img-avatar","component":"Image","url":"https://picsum.photos/seed/a2ui-avatar/96","variant":"avatar","description":"Avatar-sized image"},
            {"id":"img-medium","component":"Image","url":"https://picsum.photos/seed/a2ui-medium/240/160","variant":"mediumFeature","fit":"cover","description":"Medium feature image"},
            {"id":"img-caption","component":"Text","text":"Image variants: icon, avatar, and mediumFeature (cover fit).","variant":"caption"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "icon",
        title = "Icon",
        description = "Show a row of built-in Icon components by name.",
        category = "Content",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["icon-row","icon-caption"]},
            {"id":"icon-row","component":"Row","children":["icon-1","icon-2","icon-3","icon-4","icon-5"],"justify":"spaceEvenly"},
            {"id":"icon-1","component":"Icon","name":"favorite"},
            {"id":"icon-2","component":"Icon","name":"star"},
            {"id":"icon-3","component":"Icon","name":"share"},
            {"id":"icon-4","component":"Icon","name":"settings"},
            {"id":"icon-5","component":"Icon","name":"home"},
            {"id":"icon-caption","component":"Text","text":"Icons rendered by name from the catalog's built-in icon set.","variant":"caption"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "video",
        title = "Video",
        description = "Show the Video component playing a sample clip.",
        category = "Media",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["video-1","video-caption"]},
            {"id":"video-1","component":"Video","url":"https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"},
            {"id":"video-caption","component":"Text","text":"A Video component streaming a sample clip.","variant":"caption"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "audio-player",
        title = "AudioPlayer",
        description = "Show the AudioPlayer component with a description.",
        category = "Media",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["audio-1","audio-caption"]},
            {"id":"audio-1","component":"AudioPlayer","url":"https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3","description":"SoundHelix Song 1"},
            {"id":"audio-caption","component":"Text","text":"An AudioPlayer component with a description label.","variant":"caption"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "row-column",
        title = "Row & Column",
        description = "Show nested Row and Column layout components with justify/align options.",
        category = "Layout",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["rc-caption","rc-row"],"align":"stretch"},
            {"id":"rc-caption","component":"Text","text":"A Row containing three Columns, each with a different vertical arrangement.","variant":"caption"},
            {"id":"rc-row","component":"Row","children":["rc-col1","rc-col2","rc-col3"],"justify":"spaceBetween"},
            {"id":"rc-col1","component":"Column","children":["rc-c1-a","rc-c1-b"]},
            {"id":"rc-c1-a","component":"Text","text":"Col 1 - A"},
            {"id":"rc-c1-b","component":"Text","text":"Col 1 - B"},
            {"id":"rc-col2","component":"Column","children":["rc-c2-a","rc-c2-b"]},
            {"id":"rc-c2-a","component":"Text","text":"Col 2 - A"},
            {"id":"rc-c2-b","component":"Text","text":"Col 2 - B"},
            {"id":"rc-col3","component":"Column","children":["rc-c3-a","rc-c3-b"]},
            {"id":"rc-c3-a","component":"Text","text":"Col 3 - A"},
            {"id":"rc-c3-b","component":"Text","text":"Col 3 - B"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "list",
        title = "List",
        description = "Show a vertical List component with several child rows.",
        category = "Layout",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["list-caption","list-1"]},
            {"id":"list-caption","component":"Text","text":"A vertical List of Text items.","variant":"caption"},
            {"id":"list-1","component":"List","direction":"vertical","align":"stretch","children":["list-item-1","list-item-2","list-item-3"]},
            {"id":"list-item-1","component":"Text","text":"First list item"},
            {"id":"list-item-2","component":"Text","text":"Second list item"},
            {"id":"list-item-3","component":"Text","text":"Third list item"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "card",
        title = "Card",
        description = "Show a Card wrapping a Column of content.",
        category = "Layout",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["card-caption","card-1"]},
            {"id":"card-caption","component":"Text","text":"A Card wraps a single child component (often a Column).","variant":"caption"},
            {"id":"card-1","component":"Card","child":"card-content"},
            {"id":"card-content","component":"Column","children":["card-title","card-body"]},
            {"id":"card-title","component":"Text","text":"Card Title","variant":"h4"},
            {"id":"card-body","component":"Text","text":"This is the body content rendered inside a Card component."}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "tabs",
        title = "Tabs",
        description = "Show a Tabs component switching between two children.",
        category = "Layout",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["tabs-caption","tabs-1"]},
            {"id":"tabs-caption","component":"Text","text":"Tabs switch visibility between named child components.","variant":"caption"},
            {"id":"tabs-1","component":"Tabs","tabs":[{"title":"First","child":"tab-1-content"},{"title":"Second","child":"tab-2-content"}]},
            {"id":"tab-1-content","component":"Text","text":"Content for the first tab."},
            {"id":"tab-2-content","component":"Text","text":"Content for the second tab."}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "modal",
        title = "Modal",
        description = "Show a Modal opened via a trigger Button.",
        category = "Layout",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["modal-caption","modal-1"]},
            {"id":"modal-caption","component":"Text","text":"Tap the button to open a Modal dialog.","variant":"caption"},
            {"id":"modal-1","component":"Modal","trigger":"modal-trigger-btn","content":"modal-content"},
            {"id":"modal-trigger-btn","component":"Button","child":"modal-trigger-text","variant":"primary","action":{"event":{"name":"openModal"}}},
            {"id":"modal-trigger-text","component":"Text","text":"Open Modal"},
            {"id":"modal-content","component":"Column","children":["modal-content-title","modal-content-body"]},
            {"id":"modal-content-title","component":"Text","text":"Modal Title","variant":"h4"},
            {"id":"modal-content-body","component":"Text","text":"This content is displayed inside a Modal dialog."}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "divider",
        title = "Divider",
        description = "Show a horizontal Divider between two Text elements.",
        category = "Layout",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["div-top","div-1","div-bottom"]},
            {"id":"div-top","component":"Text","text":"Content above the divider."},
            {"id":"div-1","component":"Divider","axis":"horizontal"},
            {"id":"div-bottom","component":"Text","text":"Content below the divider."}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "button",
        title = "Button",
        description = "Show the default, primary, and borderless Button variants.",
        category = "Input",
        messages = header() + updateComponents(
            """
            {"id":"root","component":"Column","children":["btn-caption","btn-row"]},
            {"id":"btn-caption","component":"Text","text":"Tap a button to send an event action back to the app.","variant":"caption"},
            {"id":"btn-row","component":"Row","children":["btn-default","btn-primary","btn-borderless"],"justify":"spaceEvenly"},
            {"id":"btn-default","component":"Button","child":"btn-default-text","variant":"default","action":{"event":{"name":"buttonClicked","context":{"variant":"default"}}}},
            {"id":"btn-default-text","component":"Text","text":"Default"},
            {"id":"btn-primary","component":"Button","child":"btn-primary-text","variant":"primary","action":{"event":{"name":"buttonClicked","context":{"variant":"primary"}}}},
            {"id":"btn-primary-text","component":"Text","text":"Primary"},
            {"id":"btn-borderless","component":"Button","child":"btn-borderless-text","variant":"borderless","action":{"event":{"name":"buttonClicked","context":{"variant":"borderless"}}}},
            {"id":"btn-borderless-text","component":"Text","text":"Borderless"}
            """.trimIndent()
        ),
    ),
    DemoPrompt(
        id = "text-field",
        title = "TextField",
        description = "Show shortText, longText, number, and obscured TextField variants.",
        category = "Input",
        messages = header() +
            updateDataModel("tf-short", "\"\"") +
            updateDataModel("tf-long", "\"\"") +
            updateDataModel("tf-number", "\"\"") +
            updateDataModel("tf-obscured", "\"\"") +
            updateComponents(
                """
                {"id":"root","component":"Column","children":["tf-short","tf-long","tf-number","tf-obscured"]},
                {"id":"tf-short","component":"TextField","label":"Short text","variant":"shortText","value":{"path":"tf-short"}},
                {"id":"tf-long","component":"TextField","label":"Long text","variant":"longText","value":{"path":"tf-long"}},
                {"id":"tf-number","component":"TextField","label":"Number","variant":"number","value":{"path":"tf-number"}},
                {"id":"tf-obscured","component":"TextField","label":"Password","variant":"obscured","value":{"path":"tf-obscured"}}
                """.trimIndent()
            ),
    ),
    DemoPrompt(
        id = "checkbox",
        title = "CheckBox",
        description = "Show two CheckBox components with different initial states.",
        category = "Input",
        messages = header() +
            updateDataModel("cb-1", "false") +
            updateDataModel("cb-2", "true") +
            updateComponents(
                """
                {"id":"root","component":"Column","children":["cb-1","cb-2"]},
                {"id":"cb-1","component":"CheckBox","label":"Unchecked option","value":{"path":"cb-1"}},
                {"id":"cb-2","component":"CheckBox","label":"Checked option","value":{"path":"cb-2"}}
                """.trimIndent()
            ),
    ),
    DemoPrompt(
        id = "choice-picker",
        title = "ChoicePicker",
        description = "Show mutually-exclusive and multiple-selection ChoicePicker variants.",
        category = "Input",
        messages = header() +
            updateDataModel("cp-single", """["a"]""") +
            updateDataModel("cp-multi", """["red","blue"]""") +
            updateComponents(
                """
                {"id":"root","component":"Column","children":["cp-single-label","cp-single","cp-multi-label","cp-multi"]},
                {"id":"cp-single-label","component":"Text","text":"Single choice (checkbox style)","variant":"caption"},
                {"id":"cp-single","component":"ChoicePicker","label":"Pick one","variant":"mutuallyExclusive","displayStyle":"checkbox","options":[{"label":"Option A","value":"a"},{"label":"Option B","value":"b"},{"label":"Option C","value":"c"}],"value":{"path":"cp-single"}},
                {"id":"cp-multi-label","component":"Text","text":"Multiple choice (chips style)","variant":"caption"},
                {"id":"cp-multi","component":"ChoicePicker","label":"Pick many","variant":"multipleSelection","displayStyle":"chips","options":[{"label":"Red","value":"red"},{"label":"Green","value":"green"},{"label":"Blue","value":"blue"}],"value":{"path":"cp-multi"}}
                """.trimIndent()
            ),
    ),
    DemoPrompt(
        id = "slider",
        title = "Slider",
        description = "Show a Slider component with a label, min, and max.",
        category = "Input",
        messages = header() +
            updateDataModel("slider-1", "42") +
            updateComponents(
                """
                {"id":"root","component":"Column","children":["slider-label","slider-1"]},
                {"id":"slider-label","component":"Text","text":"Volume","variant":"caption"},
                {"id":"slider-1","component":"Slider","label":"Volume","min":0,"max":100,"value":{"path":"slider-1"}}
                """.trimIndent()
            ),
    ),
    DemoPrompt(
        id = "date-time-input",
        title = "DateTimeInput",
        description = "Show DateTimeInput components with date-only and time-only modes.",
        category = "Input",
        messages = header() +
            updateDataModel("dt-date", "\"\"") +
            updateDataModel("dt-time", "\"\"") +
            updateComponents(
                """
                {"id":"root","component":"Column","children":["dt-date","dt-time"]},
                {"id":"dt-date","component":"DateTimeInput","label":"Pick a date","enableDate":true,"enableTime":false,"value":{"path":"dt-date"}},
                {"id":"dt-time","component":"DateTimeInput","label":"Pick a time","enableDate":false,"enableTime":true,"value":{"path":"dt-time"}}
                """.trimIndent()
            ),
    ),
)

/** Prompts grouped by [DemoPrompt.category], preserving first-seen category order. */
val demoPromptsByCategory: Map<String, List<DemoPrompt>> =
    demoPrompts.groupBy { it.category }
