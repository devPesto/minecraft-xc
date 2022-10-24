package phonon.xv.component

import java.util.logging.Logger
import org.tomlj.TomlTable
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import phonon.xv.core.VehicleComponent
import phonon.xv.core.VehicleComponentType
import phonon.xv.util.mapToObject
import phonon.xv.util.toml.*

/**
 * Represents a list of multiple ArmorStand player seats
 */
public data class SeatsComponent(
    val count: Int = 0,
    // armor stand local offsets in a packed array format
    // [x0, y0, z0, x1, y1, z1, ...]
    val offsets: DoubleArray = doubleArrayOf(),
): VehicleComponent {
    override val type = VehicleComponentType.SEATS
    
    // armor stand entities
    var armorstands: Array<Entity?> = Array(count) { null }

    // quick lookup for passenger in each seat
    var passengers: Array<Player?> = Array(count) { null }

    /**
     * Get seat location relative to input transform component.
     * Useful for initializing seat locations
     */
    public fun getSeatLocation(
        i: Int, // index
        transform: TransformComponent,
    ): Location {
        // get seat local offset
        val offsetX = this.offsets[i*3]
        val offsetY = this.offsets[i*3 + 1]
        val offsetZ = this.offsets[i*3 + 2]

        return Location(
            transform.world,
            transform.x + transform.yawCos * offsetX - transform.yawSin * offsetZ,
            transform.y + offsetY,
            transform.z + transform.yawSin * offsetX + transform.yawCos * offsetZ,
            transform.yawf,
            0f,
        )
    }

    companion object {
        @Suppress("UNUSED_PARAMETER")
        public fun fromToml(toml: TomlTable, _logger: Logger? = null): SeatsComponent {
            // map with keys as constructor property names
            val properties = HashMap<String, Any>()
            
            val count = toml.getLong("count")?.toInt() ?: 0
            properties["count"] = count
            
            val offsets = DoubleArray(count * 3)
            toml.getArray("offsets")?.let { arr ->
                for ( i in 0 until count ) {
                    offsets[i*3 + 0] = arr.getNumberAs<Double>(i*3 + 0)
                    offsets[i*3 + 1] = arr.getNumberAs<Double>(i*3 + 1)
                    offsets[i*3 + 2] = arr.getNumberAs<Double>(i*3 + 2)
                }
            }
            properties["offsets"] = offsets

            return mapToObject(properties, SeatsComponent::class)
        }
    }
}