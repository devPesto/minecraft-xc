/**
 * Contains particle wrappers for spawning particles
 * and runnable tasks for spawning particles.
 */

package phonon.xc.utils.particle

import org.bukkit.World
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.block.data.BlockData


/**
 * Particle bullet trail between two bullet points
 */
public data class ParticleBulletTrail(
    val world: World,
    val particle: Particle,
    val particleData: Particle.DustOptions?,
    val xStart: Double,
    val yStart: Double,
    val zStart: Double,
    val dirX: Double,
    val dirY: Double,
    val dirZ: Double,
    val length: Double,        // bullet trail length
    val netDistance: Double,   // net distance bullet has traveled
    val minDistance: Double,   // min distance before spawning
    val spacing: Double,
    val force: Boolean,
)

/**
 * Runnable task to spawn bullet trails.
 */
public class TaskSpawnParticleBulletTrails(
    val particles: ArrayList<ParticleBulletTrail>,
): Runnable {
    override fun run() {
        for ( p in particles ) {
            val particleData = p.particleData
            var x = p.xStart
            var y = p.yStart
            var z = p.zStart
            val dx = p.dirX * p.spacing
            val dy = p.dirY * p.spacing
            val dz = p.dirZ * p.spacing

            var r = 0.0
            var dist = p.netDistance
            while ( r < p.length ) {
                if ( dist > p.minDistance ) {
                    if ( particleData != null ) {
                        p.world.spawnParticle(
                            p.particle,
                            x,
                            y,
                            z,
                            1,
                            0.0, // offset.x
                            0.0, // offset.y
                            0.0, // offset.z
                            0.0, // extra
                            particleData,
                            p.force,
                        )
                    } else {
                        p.world.spawnParticle(
                            p.particle,
                            x,
                            y,
                            z,
                            1,
                            0.0, // offset.x
                            0.0, // offset.y
                            0.0, // offset.z
                            0.0, // extra
                            p.force,
                        )
                    }
                }

                x += dx
                y += dy
                z += dz
                
                r += p.spacing
                dist += p.spacing
            }
        }
    }
}


/**
 * Particle that appears
 */
public data class ParticleBulletImpact(
    val world: World,
    val count: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val blockData: BlockData,
    val force: Boolean,
)

/**
 * Runnable task to spawn bullet impact particles
 */
public class TaskSpawnParticleBulletImpacts(
    val particles: ArrayList<ParticleBulletImpact>,
): Runnable {
    override fun run() {
        for ( p in particles ) {
            p.world.spawnParticle(
                Particle.BLOCK_CRACK,
                p.x,
                p.y,
                p.z,
                p.count,
                0.0, // offset.x
                0.0, // offset.y
                0.0, // offset.z
                0.0, // extra
                p.blockData,
                p.force,
            )

            // white dust
            p.world.spawnParticle(
                Particle.REDSTONE,
                p.x,
                p.y,
                p.z,
                6,
                0.1, // offset.x
                0.1, // offset.y
                0.1, // offset.z
                0.0, // extra
                Particle.DustOptions(Color.WHITE, 0.8f),
                // Particle.DustOptions(Color.RED, 1.0f), // FOR DEBUGGING
                p.force,
            )
        }
    }
}