/**
 * Packet handlers for aim down sights model for player
 */

package phonon.xc.gun

import net.minecraft.world.InteractionHand
import org.bukkit.Material
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.math.max

// return item slot to no item
private val NMS_ITEM_NONE = CraftItemStack.asNMSCopy(ItemStack(Material.AIR))

// inventory container id
private const val PLAYER_CONTAINER_ID = 0

// item slot for aim down sights model
private val SLOT_OFFHAND = InteractionHand.OFF_HAND


class AimDownSightsModelPacketManager(
    gun: Gun,
    materialAimDownSights: Material,
) : AimDownSightsModel {
    private val nmsItemAdsModel: net.minecraft.world.item.ItemStack

    init {
        val modelId = max(gun.itemModelAimDownSights, gun.itemModelDefault)

        // create nms item stack for ads model
        val item = ItemStack(materialAimDownSights).apply {
            editMeta { it.setCustomModelData(modelId) }
        }
        nmsItemAdsModel = CraftItemStack.asNMSCopy(item)
    }


    override fun create(player: Player) {
        val nmsPlayer = (player as CraftPlayer).handle
        nmsPlayer.setItemInHand(SLOT_OFFHAND, nmsItemAdsModel)
    }


    companion object {
        /**
         * Remove aim down sights model from a player.
         */
        fun destroy(player: Player) {
            val nmsPlayer = (player as CraftPlayer).handle
            nmsPlayer.setItemInHand(SLOT_OFFHAND, NMS_ITEM_NONE)
        }
    }
}
