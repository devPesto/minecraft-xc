/**
 * Config
 * 
 * Contains global config state variables read in from 
 * plugin config.yml file
 */

package phonon.xc

import java.nio.file.Paths
import java.nio.file.Path
import java.util.UUID
import java.util.EnumSet
import java.util.EnumMap
import java.util.logging.Logger
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.tomlj.Toml
import phonon.xc.utils.mapToObject
import phonon.xc.utils.EnumArrayMap
import phonon.xc.utils.Hitbox
import phonon.xc.utils.HitboxSize
import phonon.xc.utils.BlockCollisionHandler
import phonon.xc.utils.blockCollisionHandlers


/**
 * Immutable XC config
 */
public data class Config(
    // paths to item config folders
    public val pathFilesGun: Path = Paths.get("plugins", "xc", "gun"),
    public val pathFilesAmmo: Path = Paths.get("plugins", "xc", "ammo"),
    public val pathFilesMelee: Path = Paths.get("plugins", "xc", "melee"),
    public val pathFilesMisc: Path = Paths.get("plugins", "xc", "misc"),
    public val pathFilesArmor: Path = Paths.get("plugins", "xc", "armor"),

    // flag that entity targetable
    public val entityTargetable: EnumArrayMap<EntityType, Boolean> = Hitbox.defaultEntityTargetable(),
    
    // entity hitbox sizes
    public val entityHitboxSizes: EnumArrayMap<EntityType, HitboxSize> = Hitbox.defaultEntityHitboxSizes(),
    
    // block collision handlers
    public val blockCollision: EnumArrayMap<Material, BlockCollisionHandler> = blockCollisionHandlers(),
    
    // material types for custom items
    public val materialGun: Material = Material.WARPED_FUNGUS_ON_A_STICK,
    public val materialAimDownSights: Material = Material.CARROT_ON_A_STICK, // phantom model for ads
    public val materialMelee: Material = Material.IRON_SWORD,
    public val materialMisc: Material = Material.GOLDEN_HORSE_ARMOR,
    public val materialAmmo: Material = Material.IRON_HORSE_ARMOR,
    public val materialArmor: Material = Material.LEATHER_HORSE_ARMOR,
    
    // auto fire max ticks before stopping
    public val autoFireMaxTicksSinceLastRequest: Int = 4,

    // recoil recovery rate per tick
    public val recoilRecoveryRate: Double = 0.2,

    // block damage
    public val blockDamageExplosion: Boolean = true,

    // particle effects
    
    // number of bullet impact particles to spawn
    public val particleBulletTrailSpacing: Double = 0.2,
    public val particleBulletTrailMinDistance: Double = 0.3,
    public val particleBulletImpactCount: Int = 12,

    // ADVANCED

    // stops player crawling when switching to a non-crawl to shoot weapon
    public val crawlOnlyAllowedOnCrawlWeapons: Boolean = false,
    
    // number of players before pipelined sway system enabled
    public val playersBeforePipelinedSway: Int = 4,

    // debug timings default value
    public val defaultDoDebugTimings: Boolean = false,
) {

    companion object {
        /**
         * Parse and return a Config from a config.toml file.
         */
        public fun fromToml(source: Path, logger: Logger? = null): Config {
            val toml = Toml.parse(source)

            // map with keys as Config constructor property names
            val configOptions = HashMap<String, Any>()

            // parse toml file into configOptions

            // item config folder paths
            toml.getTable("configs")?.let { configsPaths -> 
                val pluginDataFolder = XC.plugin!!.getDataFolder().getPath()
                configsPaths.getString("gun")?.let { path -> configOptions["pathFilesGun"] = Paths.get(pluginDataFolder, path) }
                configsPaths.getString("ammo")?.let { path -> configOptions["pathFilesAmmo"] = Paths.get(pluginDataFolder, path) }
                configsPaths.getString("melee")?.let { path -> configOptions["pathFilesMelee"] = Paths.get(pluginDataFolder, path) }
                configsPaths.getString("misc")?.let { path -> configOptions["pathFilesMisc"] = Paths.get(pluginDataFolder, path) }
                configsPaths.getString("armor")?.let { path -> configOptions["pathFilesArmor"] = Paths.get(pluginDataFolder, path) }
            }

            // materials
            toml.getString("material.gun")?.let { s ->
                Material.getMaterial(s)?.let { configOptions["materialGun"] = it } ?: run {
                    logger?.warning("[material.gun] Invalid material: ${s}")
                }
            }
            toml.getString("material.aim_down_sights")?.let { s ->
                Material.getMaterial(s)?.let { configOptions["materialAimDownSights"] = it } ?: run {
                    logger?.warning("[material.aim_down_sights] Invalid material: ${s}")
                }
            }
            toml.getString("material.melee")?.let { s ->
                Material.getMaterial(s)?.let { configOptions["materialMelee"] = it } ?: run {
                    logger?.warning("[material.melee] Invalid material: ${s}")
                }
            }
            toml.getString("material.misc")?.let { s ->
                Material.getMaterial(s)?.let { configOptions["materialMisc"] = it } ?: run {
                    logger?.warning("[material.misc] Invalid material: ${s}")
                }
            }
            toml.getString("material.ammo")?.let { s ->
                Material.getMaterial(s)?.let { configOptions["materialAmmo"] = it } ?: run {
                    logger?.warning("[material.ammo] Invalid material: ${s}")
                }
            }
            toml.getString("material.armor")?.let { s ->
                Material.getMaterial(s)?.let { configOptions["materialArmor"] = it } ?: run {
                    logger?.warning("[material.armor] Invalid material: ${s}")
                }
            }
            
            // auto fire max ticks since last request config
            toml.getLong("auto_fire.max_ticks_since_last_request")?.let { configOptions["autoFireMaxTicksSinceLastRequest"] = it.toInt() }
            
            // recoil recovery rate
            toml.getDouble("recoil.recovery_rate")?.let { configOptions["recoilRecoveryRate"] = it }
            
            // crawl config
            toml.getBoolean("crawl.only_allowed_on_crawl_weapons")?.let { configOptions["crawlOnlyAllowedOnCrawlWeapons"] = it }

            // block damage config
            toml.getBoolean("block_damage.explosion")?.let { configOptions["blockDamageExplosion"] = it }
            
            // sway config
            toml.getLong("sway.players_before_pipelined_sway")?.let { configOptions["playersBeforePipelinedSway"] = it.toInt() }
            
            // default debug timings config
            toml.getBoolean("debug.do_timings_default")?.let { configOptions["defaultDoDebugTimings"] = it }

            return mapToObject(configOptions, Config::class)
        }
    }
}