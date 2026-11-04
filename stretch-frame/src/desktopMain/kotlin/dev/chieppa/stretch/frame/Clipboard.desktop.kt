package dev.chieppa.stretch.frame

import java.awt.Toolkit
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

internal actual fun systemClipboard(): ClipboardAccess = AwtClipboard

/** The AWT system clipboard. Custom data travels as strings under a MIME type made from its type name. */
private object AwtClipboard : ClipboardAccess {
    private val clipboard get() = Toolkit.getDefaultToolkit().systemClipboard

    override suspend fun read(types: Set<String>): ClipData? {
        val contents = runCatching { clipboard.getContents(null) }.getOrNull() ?: return null
        val text = if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            runCatching { contents.getTransferData(DataFlavor.stringFlavor) as String }.getOrNull()
        } else {
            null
        }
        val custom = types.mapNotNull { type ->
            val flavor = flavorOf(type)
            if (!contents.isDataFlavorSupported(flavor)) return@mapNotNull null
            runCatching { type to (contents.getTransferData(flavor) as String) }.getOrNull()
        }.toMap()
        return if (text == null && custom.isEmpty()) null else ClipData(text, custom)
    }

    override fun write(data: ClipData) {
        val selection = ClipTransferable(data)
        runCatching { clipboard.setContents(selection, selection) }
    }
}

private fun flavorOf(type: String) =
    DataFlavor("application/x-${type.replace(Regex("[^A-Za-z0-9.+-]"), "-")};class=java.lang.String", type)

private class ClipTransferable(private val data: ClipData) : Transferable, ClipboardOwner {
    private val flavors = buildList {
        data.custom.keys.forEach { add(it to flavorOf(it)) }
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> =
        (flavors.map { it.second } + listOfNotNull(data.text?.let { DataFlavor.stringFlavor })).toTypedArray()

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = transferDataFlavors.any { it.equals(flavor) }

    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor.equals(DataFlavor.stringFlavor) && data.text != null) return data.text
        val type = flavors.firstOrNull { it.second.equals(flavor) }?.first ?: throw UnsupportedFlavorException(flavor)
        return data.custom.getValue(type)
    }

    override fun lostOwnership(clipboard: java.awt.datatransfer.Clipboard?, contents: Transferable?) = Unit
}
