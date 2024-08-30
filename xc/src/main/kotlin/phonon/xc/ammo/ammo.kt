/**
 * Ammo module
 */

package phonon.xc.ammo

import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack
import org.tomlj.Toml
import phonon.xc.XC
import phonon.xc.util.*
import phonon.xc.util.IntoItemStack
import java.nio.file.Path
import java.util.logging.Logger


class Ammo(
    // id, same as custom model id
    val id: Int = Int.MAX_VALUE,

    // ammo item/visual properties
    val itemName: String = "ammo",
    private val itemLore: List<String> = listOf(),
) : IntoItemStack {
    /**
     * Create a new ItemStack from ammo properties.
     */
    override fun toItemStack(xc: XC): ItemStack {
        val item = ItemStack(xc.config.materialAmmo, 1)
        item.editMeta {
            it.displayName(itemName.parse())
            it.setCustomModelData(id)
            it.lore(itemLore.map { line -> line.parse() })
        }
        return item
    }


    companion object {
        /**
         * Parse and return a Ammo from a `ammo.toml` file.
         * Return null if something fails or no file found.
         */
        fun fromToml(source: Path, logger: Logger? = null): Ammo? {
            try {
                val toml = Toml.parse(source)

                // map with keys as constructor property names
                val properties = HashMap<String, Any>()

                // parse toml file into properties

                // ammo id/model data
                toml.getLong("id")?.let { properties["id"] = it.toInt() }

                // item properties
                toml.getTable("item")?.let { item ->
                    item.getString("name")?.let { properties["itemName"] = it.parseLegacy('&') }
                    item.getArray("lore")
                        ?.let { properties["itemLore"] = it.toList().map { s -> s.toString().parse() } }
                }

                return mapToObject(properties, Ammo::class)
            } catch (e: Exception) {
                logger?.warning("Failed to parse ammo file: ${source.toString()}, ${e}")
                return null
            }
        }
    }
}