/**
 * Contain public api custom events emitted by plugin.
 */

package phonon.xc.event

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import phonon.xc.gun.Gun
import phonon.xc.throwable.ThrowableItem
import phonon.xc.util.damage.DamageType


/**
 * Event emitted when an entity is hit directly by a projectile
 * and takes damage. This only occurs for entities that have
 * hitboxes enabled.
 */
data class XCProjectileDamageEvent(
    val gun: Gun,
    val location: Location,
    val target: Entity,
    val source: Entity,
    val damage: Double,
    val distance: Double,
) : Event(), Cancellable {
    // event cancelled
    private var cancelled: Boolean = false

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    override fun getHandlers(): HandlerList {
        return XCProjectileDamageEvent.handlers
    }


    companion object {
        private val handlers: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }
}


/**
 * Event emitted when an entity is hit directly by a throwable item
 * and takes damage. This only occurs for entities that have
 * hitboxes enabled.
 */
data class XCThrowableDamageEvent(
    val throwable: ThrowableItem,
    val location: Location,
    val target: Entity,
    val source: Entity,
) : Event(), Cancellable {
    // event cancelled
    private var cancelled: Boolean = false

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    override fun getHandlers(): HandlerList {
        return XCThrowableDamageEvent.handlers
    }


    companion object {
        private val handlers: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }
}


/**
 * Event emitted when an entity takes damage for an explosion
 * (so entity within explosion distance). This only occurs for
 * entities that have hitboxes enabled.
 */
data class XCExplosionDamageEvent(
    val target: Entity,
    val damage: Double, // base damage, not modified by armor
    val damageType: DamageType,
    val distance: Double, // distance from explosion center
    val source: Entity?,
    val weaponType: Int,
    val weaponId: Int,
    val weaponMaterial: Material,
) : Event(), Cancellable {
    // event cancelled
    private var cancelled: Boolean = false

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    override fun getHandlers(): HandlerList {
        return XCExplosionDamageEvent.handlers
    }


    companion object {
        private val handlers: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }
}