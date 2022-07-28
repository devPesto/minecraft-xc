/**
 * Contain gun on block hit and on entity hit handlers.
 * 
 * Separate hit block and hit entity handlers are used to avoid
 * having null optional entity or block in handler inputs.
 * Alternative would be to have a unified hit handler type
 * `hitHandler(Gun, Location, Entity?, Block?, Entity)` but this
 * would introduce more handler complexity in managing different
 * hit cases. 
 */

package phonon.xc.throwable

import java.util.concurrent.ThreadLocalRandom
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Damageable
import phonon.xc.XC
import phonon.xc.utils.damage.*
import phonon.xc.utils.ChunkCoord3D
import phonon.xc.utils.Hitbox
import phonon.xc.utils.explosion.createExplosion
import phonon.xc.event.XCProjectileDamageEvent


/**
 * Common throwable timer expired handler function type. Inputs are
 * (
 *  throwable: Throwable,
 *  location: Location,
 *  source: Entity,
 * ) -> Unit
 */
typealias ThrowableTimerExpiredHandler = (HashMap<ChunkCoord3D, ArrayList<Hitbox>>, Throwable, Location, Entity) -> Unit

/**
 * Common hit block handler function type. Inputs are
 * (
 *  throwable: Throwable,
 *  location: Location,
 *  target: Block,
 *  source: Entity,
 * ) -> Unit
 */
typealias ThrowableBlockHitHandler = (HashMap<ChunkCoord3D, ArrayList<Hitbox>>, Throwable, Location, Block, Entity) -> Unit

/**
 * Common hit entity handler function type. Inputs are
 * (
 *  throwable: Throwable,
 *  location: Location,
 *  target: Entity,
 *  source: Entity,
 * ) -> Unit
 */
typealias ThrowableEntityHitHandler = (HashMap<ChunkCoord3D, ArrayList<Hitbox>>, Throwable, Location, Entity, Entity) -> Unit


/**
 * Map string name to built-in timer expipred handlers.
 */
public fun getThrowableTimerExpiredHandler(name: String): ThrowableTimerExpiredHandler? {
    return when ( name.lowercase() ) {
        // "explosion" -> blockExplosionHitHandler
        // "fire" -> blockFireHitHandler
        else -> null
    }
}

/**
 * Map string name to built-in hit block handlers.
 */
public fun getThrowableBlockHitHandler(name: String): ThrowableBlockHitHandler? {
    return when ( name.lowercase() ) {
        // "explosion" -> blockExplosionHitHandler
        // "fire" -> blockFireHitHandler
        else -> null
    }
}

/**
 * Map string name to built-in hit block handlers.
 */
public fun getThrowableEntityHitHandler(name: String): ThrowableEntityHitHandler? {
    return when ( name.lowercase() ) {
        // "damage" -> entityDamageHitHandler
        // "explosion" -> entityExplosionHitHandler
        else -> null
    }
}


/**
 * Empty timer expired handler.
 */
public val noTimerExpiredHandler: ThrowableTimerExpiredHandler = {_, _, _, _ -> }


/**
 * Empty entity hit handler.
 */
public val noEntityHitHandler: ThrowableEntityHitHandler = {_, _, _, _, _ -> }

// /**
//  * Entity hit handler with damage (standard entity damage hit handler).
//  */
// public val entityDamageHitHandler = fun(
//     hitboxes: HashMap<ChunkCoord3D, ArrayList<Hitbox>>,
//     gun: Gun,
//     location: Location,
//     target: Entity,
//     source: Entity,
//     distance: Double,
// ) {
//     if ( target is LivingEntity && target is Damageable ) {
//         if ( target is Player && !XC.canPvpAt(location) ) {
//             return
//         }

//         // FOR DEBUGGING
//         // val baseDamage = gun.projectileDamageAtDistance(distance)
//         // println("baseDamage: $baseDamage, distance: $distance")

//         // final damage after 
//         // 1. gun damage drop: gun.damageAtDistance(distance)
//         // 2. applying armor/resistance
//         val damage = damageAfterArmorAndResistance(
//             gun.projectileDamageAtDistance(distance),
//             target,
//             gun.projectileArmorReduction,
//             gun.projectileResistanceReduction,
//         )
//         target.damage(damage, null)
//         target.setNoDamageTicks(0)

//         // add fire ticks
//         if ( gun.hitFireTicks > 0 ) {
//             target.setFireTicks(gun.hitFireTicks)
//         }
//     }

//     // emit event for external plugins to read
//     Bukkit.getPluginManager().callEvent(XCProjectileDamageEvent(
//         target,
//         gun.projectileDamage,
//         gun.projectileDamageType,
//         source,
//     ))
// }

// /**
//  * Entity hit handler with damage and a queued explosion at hit location.
//  */
// public val entityExplosionHitHandler = fun(
//     hitboxes: HashMap<ChunkCoord3D, ArrayList<Hitbox>>,
//     gun: Gun,
//     location: Location,
//     target: Entity,
//     source: Entity,
//     distance: Double,
// ) {
//     // do main damage directly to target
//     if ( target is LivingEntity && target is Damageable ) {
//         if ( target is Player && !XC.canPvpAt(location) ) {
//             return
//         }

//         val damage = damageAfterArmorAndResistance(
//             gun.projectileDamageAtDistance(distance),
//             target,
//             gun.projectileArmorReduction,
//             gun.projectileResistanceReduction,
//         )
//         target.damage(damage, null)
//         target.setNoDamageTicks(0)

//         // add fire ticks
//         if ( gun.hitFireTicks > 0 ) {
//             target.setFireTicks(gun.hitFireTicks)
//         }
//     }

//     // emit event for external plugins to read
//     Bukkit.getPluginManager().callEvent(XCProjectileDamageEvent(
//         target,
//         gun.projectileDamage,
//         gun.projectileDamageType,
//         source,
//     ))

//     // summon explosion effect at location
//     createExplosion(
//         hitboxes,
//         location,
//         source,
//         gun.explosionMaxDistance,
//         gun.explosionDamage,
//         gun.explosionRadius,
//         gun.explosionFalloff,
//         gun.explosionArmorReduction,
//         gun.explosionBlastProtReduction,
//         gun.explosionDamageType,
//         gun.explosionBlockDamagePower,
//         gun.explosionParticles,
//     )
// }

/**
 * Empty block hit handler.
 */
public val noBlockHitHandler: ThrowableBlockHitHandler = {_, _, _, _, _ -> }

// /**
//  * Block hit handler that queues explosion at hit location.
//  */
// public val blockExplosionHitHandler = fun(
//     hitboxes: HashMap<ChunkCoord3D, ArrayList<Hitbox>>,
//     gun: Gun,
//     location: Location,
//     block: Block,
//     source: Entity,
// ) {
//     // summon explosion effect at location
//     createExplosion(
//         hitboxes,
//         location,
//         source,
//         gun.explosionMaxDistance,
//         gun.explosionDamage,
//         gun.explosionRadius,
//         gun.explosionFalloff,
//         gun.explosionArmorReduction,
//         gun.explosionBlastProtReduction,
//         gun.explosionDamageType,
//         gun.explosionBlockDamagePower,
//         gun.explosionParticles,
//     )
// }

// /**
//  * Block hit handler that creates fire on top of hit location.
//  */
// public val blockFireHitHandler = fun(
//     hitboxes: HashMap<ChunkCoord3D, ArrayList<Hitbox>>,
//     gun: Gun,
//     location: Location,
//     block: Block,
//     source: Entity,
// ) {
//     if ( !XC.canCreateFireAt(location) ) {
//         return
//     }
    
//     if ( ThreadLocalRandom.current().nextDouble() < gun.hitBlockFireProbability ) {
//         val blType = block.getType()

//         // set block below on fire
//         if ( blType == Material.AIR ) {
//             val blBelow = block.getRelative(0, -1, 0);
//             if ( blBelow.getType().isSolid() ) {
//                 block.setType(Material.FIRE);
//             }
//         }
//         else if ( blType.isSolid() ) {
//             val blAbove = block.getRelative(0, 1, 0);
//             if ( blAbove.getType() == Material.AIR ) {
//                 blAbove.setType(Material.FIRE);
//             }
//         }
//     }
// }