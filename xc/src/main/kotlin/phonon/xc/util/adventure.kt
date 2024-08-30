package phonon.xc.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Parses a component using the legacy serializer
 */
fun String.parseLegacy(symbol: Char = '&'): Component {
    return LegacyComponentSerializer.legacy(symbol).deserialize(this)
}

fun String.parse(): Component {
    return Component.text(this)
}

/**
 * Parses a component using MiniMessage
 * See https://docs.advntr.dev/minimessage/index.html
 */
fun String.parseMiniMessage(): Component {
    return MiniMessage.miniMessage().deserialize(this)
}


