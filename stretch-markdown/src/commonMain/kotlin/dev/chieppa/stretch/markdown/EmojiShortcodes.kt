package dev.chieppa.stretch.markdown

/** A GitHub emoji shortcode such as `:rocket:`. */
internal val shortcodePattern = Regex(""":([a-z0-9_+-]{1,40}):""")

/** The shortcodes bots and people write most in pull requests, and the emoji GitHub shows for each. Others stay as written. */
private val shortcodes = mapOf(
    "+1" to "👍", "-1" to "👎", "thumbsup" to "👍", "thumbsdown" to "👎", "smile" to "😄", "laughing" to "😆", "tada" to "🎉",
    "confused" to "😕", "heart" to "❤️", "rocket" to "🚀", "eyes" to "👀", "white_check_mark" to "✅",
    "heavy_check_mark" to "✔️", "check" to "✔️", "x" to "❌", "heavy_multiplication_x" to "✖️", "warning" to "⚠️",
    "exclamation" to "❗", "question" to "❓", "grey_question" to "❔", "information_source" to "ℹ️", "no_entry" to "⛔",
    "no_entry_sign" to "🚫", "stop_sign" to "🛑", "red_circle" to "🔴", "green_circle" to "🟢", "yellow_circle" to "🟡",
    "orange_circle" to "🟠", "large_blue_circle" to "🔵", "white_circle" to "⚪", "black_circle" to "⚫",
    "heavy_plus_sign" to "➕", "heavy_minus_sign" to "➖", "arrow_up" to "⬆️", "arrow_down" to "⬇️",
    "arrow_right" to "➡️", "arrow_left" to "⬅️", "arrow_up_small" to "🔼", "arrow_down_small" to "🔽",
    "small_red_triangle" to "🔺", "small_red_triangle_down" to "🔻", "chart_with_upwards_trend" to "📈",
    "chart_with_downwards_trend" to "📉", "bar_chart" to "📊", "umbrella" to "☂️", "loudspeaker" to "📢", "mega" to "📣",
    "bug" to "🐛", "lock" to "🔒", "unlock" to "🔓", "key" to "🔑", "memo" to "📝", "pencil2" to "✏️", "sparkles" to "✨",
    "fire" to "🔥", "boom" to "💥", "zap" to "⚡", "bulb" to "💡", "construction" to "🚧", "package" to "📦",
    "wrench" to "🔧", "hammer" to "🔨", "hammer_and_wrench" to "🛠️", "gear" to "⚙️", "link" to "🔗", "mag" to "🔍",
    "book" to "📖", "books" to "📚", "calendar" to "📆", "date" to "📅", "clock1" to "🕐", "hourglass" to "⌛",
    "hourglass_flowing_sand" to "⏳", "robot" to "🤖", "recycle" to "♻️", "vertical_traffic_light" to "🚦",
    "construction_worker" to "👷", "shield" to "🛡️", "test_tube" to "🧪", "art" to "🎨", "lipstick" to "💄",
    "bookmark" to "🔖", "pushpin" to "📌", "paperclip" to "📎", "clipboard" to "📋", "page_facing_up" to "📄",
    "file_folder" to "📁", "inbox_tray" to "📥", "outbox_tray" to "📤", "speech_balloon" to "💬", "bell" to "🔔",
    "star" to "⭐", "star2" to "🌟", "trophy" to "🏆", "100" to "💯", "ok_hand" to "👌", "pray" to "🙏", "wave" to "👋",
    "raised_hands" to "🙌", "clap" to "👏", "muscle" to "💪", "thinking" to "🤔", "sweat_smile" to "😅", "cry" to "😢",
    "skull" to "💀", "ghost" to "👻", "green_heart" to "💚", "broken_heart" to "💔", "arrows_counterclockwise" to "🔄",
    "repeat" to "🔁", "twisted_rightwards_arrows" to "🔀", "rewind" to "⏪", "fast_forward" to "⏩", "pause_button" to "⏸️",
    "white_large_square" to "⬜", "black_large_square" to "⬛", "large_orange_diamond" to "🔶", "large_blue_diamond" to "🔷",
    "new" to "🆕", "free" to "🆓", "up" to "🆙", "cool" to "🆒", "sos" to "🆘", "ballot_box_with_check" to "☑️",
    "heavy_exclamation_mark" to "❗", "bangbang" to "‼️", "triangular_flag_on_post" to "🚩", "checkered_flag" to "🏁",
    "seedling" to "🌱", "evergreen_tree" to "🌲", "snowflake" to "❄️", "sunny" to "☀️", "cloud" to "☁️",
    "dart" to "🎯", "gift" to "🎁", "balloon" to "🎈", "confetti_ball" to "🎊", "beers" to "🍻", "coffee" to "☕",
    "see_no_evil" to "🙈", "rotating_light" to "🚨", "ambulance" to "🚑", "lady_beetle" to "🐞", "beetle" to "🐞",
)

/** The emoji [name], a shortcode without its colons, stands for, or null for one it does not know. */
internal fun emojiOf(name: String): String? = shortcodes[name]

/** [text] with every shortcode it knows replaced by its emoji. */
internal fun replaceShortcodes(text: String): String =
    if (':' !in text) text else shortcodePattern.replace(text) { emojiOf(it.groupValues[1]) ?: it.value }
