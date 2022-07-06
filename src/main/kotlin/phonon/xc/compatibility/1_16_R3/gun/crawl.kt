/**
 * Utility to make player crawl for shooting.
 * Packaged with `gun` since this automatically handles gun ads
 * models when starting/stopping crawling.
 * 
 * Based on GSit method:
 * - If block above is air, put a fake barrier above player
 * - Else uses a fake shulker entity above player which forces player into
 * crawling position (since shulker is like a block)
 *   HOWEVER, shulker HEAD will NEVER BE INVISIBLE so looks ugly...
 * https://github.com/Gecolay/GSit/blob/main/v1_17_R1/src/main/java/dev/geco/gsit/mcv/v1_17_R1/objects/GCrawl.java
 * https://github.com/Gecolay/GSit/blob/main/v1_17_R1/src/main/java/dev/geco/gsit/mcv/v1_17_R1/objects/BoxEntity.java
 */

package phonon.xc.compatibility.v1_16_R3.gun.crawl

import java.util.UUID
import net.minecraft.server.v1_16_R3.EnumDirection
import net.minecraft.server.v1_16_R3.EntityTypes
import net.minecraft.server.v1_16_R3.EntityShulker
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionEffect
import org.bukkit.persistence.PersistentDataType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import phonon.xc.XC
import phonon.xc.utils.Message
import phonon.xc.gun.useAimDownSights
import phonon.xc.gun.AmmoInfoMessagePacket
import phonon.xc.utils.progressBar10

import phonon.xc.compatibility.v1_16_R3.gun.item.*

// ==================================================================
// Entity Shulker in 1.16.5:
// ==================================================================
// public class EntityShulker extends EntityGolem implements IMonster {

//     private static final UUID bp = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF27F");
//     private static final AttributeModifier bq = new AttributeModifier(EntityShulker.bp, "Covered armor bonus", 20.0D, AttributeModifier.Operation.ADDITION);
//     public static final DataWatcherObject<EnumDirection> b = DataWatcher.a(EntityShulker.class, DataWatcherRegistry.n); // PAIL protected -> public, rename ATTACH_FACE
//     protected static final DataWatcherObject<Optional<BlockPosition>> c = DataWatcher.a(EntityShulker.class, DataWatcherRegistry.m);
//     protected static final DataWatcherObject<Byte> d = DataWatcher.a(EntityShulker.class, DataWatcherRegistry.a);
//     public static final DataWatcherObject<Byte> COLOR = DataWatcher.a(EntityShulker.class, DataWatcherRegistry.a);
//     private float br;
//     private float bs;
//     private BlockPosition bt = null;
//     private int bu;

//     public EntityShulker(EntityTypes<? extends EntityShulker> entitytypes, World world) {
//         super(entitytypes, world);
//         this.f = 5;
//     }
//    ...
//
//     @Override
//     protected void initDatawatcher() {
//         super.initDatawatcher();
//         this.datawatcher.register(EntityShulker.b, EnumDirection.DOWN);
//         this.datawatcher.register(EntityShulker.c, Optional.empty());
//         this.datawatcher.register(EntityShulker.d, (byte) 0);
//         this.datawatcher.register(EntityShulker.COLOR, (byte) 16);
//     }

//     @Override
//     public void loadData(NBTTagCompound nbttagcompound) {
//         super.loadData(nbttagcompound);
//         this.datawatcher.set(EntityShulker.b, EnumDirection.fromType1(nbttagcompound.getByte("AttachFace")));
//         this.datawatcher.set(EntityShulker.d, nbttagcompound.getByte("Peek"));
//         this.datawatcher.set(EntityShulker.COLOR, nbttagcompound.getByte("Color"));
//         if (nbttagcompound.hasKey("APX")) {
//             int i = nbttagcompound.getInt("APX");
//             int j = nbttagcompound.getInt("APY");
//             int k = nbttagcompound.getInt("APZ");
    
//             this.datawatcher.set(EntityShulker.c, Optional.of(new BlockPosition(i, j, k)));
//         } else {
//             this.datawatcher.set(EntityShulker.c, Optional.empty());
//         }
    
//     }
    
//     @Override
//     public void saveData(NBTTagCompound nbttagcompound) {
//         super.saveData(nbttagcompound);
//         nbttagcompound.setByte("AttachFace", (byte) ((EnumDirection) this.datawatcher.get(EntityShulker.b)).c());
//         nbttagcompound.setByte("Peek", (Byte) this.datawatcher.get(EntityShulker.d));
//         nbttagcompound.setByte("Color", (Byte) this.datawatcher.get(EntityShulker.COLOR));
//         BlockPosition blockposition = this.eM();
    
//         if (blockposition != null) {
//             nbttagcompound.setInt("APX", blockposition.getX());
//             nbttagcompound.setInt("APY", blockposition.getY());
//             nbttagcompound.setInt("APZ", blockposition.getZ());
//         }
    
//     }

//  ...
// }
// ==================================================================

/**
 * Shulker entity used to force player into crawling position
 * when cannot place a barrier block.
 */
public class BoxEntity(
    location: Location,
): EntityShulker(EntityTypes.SHULKER, (location.getWorld() as CraftWorld).getHandle()) {

    init {
        this.persist = false

        this.setInvisible(true)
        this.setNoGravity(true)
        this.setInvulnerable(true)
        this.setNoAi(true)
        this.setSilent(true)
        this.setAttachedFace(EnumDirection.UP)
    }

    // for 1.16.5: set no ai:
    // https://www.spigotmc.org/threads/nms-entity-disable-ai.404800/#post-3609716
    public fun setNoAi(noAi: Boolean) {
        this.k(noAi)
    }

    public fun setAttachedFace(dir: EnumDirection) {
        this.datawatcher.set(EntityShulker.b, dir)
    }

    /**
     * Set raw peek amount between [0, 100]
     */
    public fun setRawPeekAmount(amount: Byte) {
        this.datawatcher.set(EntityShulker.d, amount)
    }

    public fun canChangeDimensions(): Boolean { return false }

    public fun isAffectedByFluids(): Boolean { return false }

    public fun isSensitiveToWater(): Boolean { return false }

    public fun rideableUnderWater(): Boolean { return true }

}

// block data for barrier block to place above player
private val FAKE_BLOCK_DATA = Material.BARRIER.createBlockData()

/**
 * Crawl position state for a player.
 */
public data class Crawling(
    val player: Player,
    val initialLocation: Location, // initial location
    val prevLocationX: Double,   // location on previous tick
    val prevLocationY: Double,   // location on previous tick
    val prevLocationZ: Double,   // location on previous tick
    val blAboveX: Int,
    val blAboveY: Int,
    val blAboveZ: Int,
    val blAboveMaterial: Material,
    val boxEntity: BoxEntity?,
) {
    /**
     * Update current crawl state to a new location.
     */
    public fun update(newLocation: Location): Crawling {        
        val newBlAboveX = newLocation.getBlockX()
        val newBlAboveY = newLocation.getBlockY() + 1
        val newBlAboveZ = newLocation.getBlockZ()
        
        // if block location changed and previous material was air, cleanup barrier
        if ( this.blAboveMaterial == Material.AIR ) {
            if ( newBlAboveX != blAboveX || newBlAboveY != blAboveY || newBlAboveZ != blAboveZ ) {
                // if material still air, send packet that previous block is air
                val blAbove = player.getWorld().getBlockAt(this.blAboveX, this.blAboveY, this.blAboveZ)
                if ( blAbove.getType() == Material.AIR ) {
                    sendFakeBlockPacket(player, this.blAboveX, this.blAboveY, this.blAboveZ, blAbove.getBlockData())
                }
            }
        }

        val yHeightInBlock = newLocation.getY() - Math.floor(newLocation.getY())
        
        val newBlAboveMaterial = player.getWorld().getBlockAt(newBlAboveX, newBlAboveY, newBlAboveZ).getType()

        val boxEntityOrNull = if ( yHeightInBlock < 0.5 && newBlAboveMaterial == Material.AIR ) {
            // block above can be set to a fake barrier
            sendFakeBlockPacket(player, newBlAboveX, newBlAboveY, newBlAboveZ, FAKE_BLOCK_DATA)

            // remove shulker for player
            this.boxEntity?.let { it -> removeBoxEntityPacket(this.player, it) }

            // return current box entity
            this.boxEntity
        } else {
            val boxEntity = if ( this.boxEntity != null ) {
                // re-use same box entity, with updated location
                this.boxEntity.setLocation(
                    newLocation.getX(),
                    newLocation.getY() + 1.25,
                    newLocation.getZ(),
                    0f,
                    0f,
                )
                this.boxEntity
            } else {
                val newBoxEntity = BoxEntity(newLocation.clone().add(0.0, 1.0, 0.0))
                newBoxEntity.setRawPeekAmount(0.toByte())
                newBoxEntity.setLocation(
                    newLocation.getX(),
                    newLocation.getY() + 1.25,
                    newLocation.getZ(),
                    0f,
                    0f,
                )
                newBoxEntity
            }
            
            sendBoxEntityPacket(player, boxEntity)

            boxEntity
        }
    
        return Crawling(
            player = this.player,
            initialLocation = this.initialLocation,
            prevLocationX = newLocation.getX(),
            prevLocationY = newLocation.getY(),
            prevLocationZ = newLocation.getZ(),
            blAboveX = newBlAboveX,
            blAboveY = newBlAboveY,
            blAboveZ = newBlAboveZ,
            blAboveMaterial = newBlAboveMaterial,
            boxEntity = boxEntityOrNull,
        )
    }

    public fun cleanup() {
        // if block above is air and material is still air, cleanup barrier
        if ( this.blAboveMaterial == Material.AIR ) {
            val blAbove = player.getWorld().getBlockAt(this.blAboveX, this.blAboveY, this.blAboveZ)
            sendFakeBlockPacket(player, this.blAboveX, this.blAboveY, this.blAboveZ, blAbove.getBlockData())
        }
        // remove box entity for player if it exists
        this.boxEntity?.let { it -> removeBoxEntityPacket(this.player, it) }
    }
}

/**
 * Initializes a crawl state for a player 
 */
public fun forceCrawl(player: Player): Crawling {
    val playerLocation = player.getLocation()
    val blAboveX = playerLocation.getBlockX()
    val blAboveY = playerLocation.getBlockY() + 1
    val blAboveZ = playerLocation.getBlockZ()
    
    val yHeightInBlock = playerLocation.getY() - Math.floor(playerLocation.getY())
    
    val blAboveMaterial = player.getWorld().getBlockAt(blAboveX, blAboveY, blAboveZ).getType()
    
    val boxEntityOrNull = if ( yHeightInBlock < 0.5 && blAboveMaterial == Material.AIR ) {
        // block above can be set to a fake barrier
        sendFakeBlockPacket(player, blAboveX, blAboveY, blAboveZ, FAKE_BLOCK_DATA)

        // null box entity
        null
    } else { // must create a fake shulker entity
        val loc = playerLocation.clone().add(0.0, 1.25, 0.0)
        // println("box loc = $loc")
        val boxEntity = BoxEntity(loc)
        boxEntity.setRawPeekAmount(0.toByte())
        boxEntity.setLocation(
            loc.getX(),
            loc.getY(),
            loc.getZ(),
            0f,
            0f,
        )
        sendBoxEntityPacket(player, boxEntity)

        boxEntity
    }

    return Crawling(
        player = player,
        initialLocation = playerLocation,
        prevLocationX = playerLocation.getX(),
        prevLocationY = playerLocation.getY(),
        prevLocationZ = playerLocation.getZ(),
        blAboveX = blAboveX,
        blAboveY = blAboveY,
        blAboveZ = blAboveZ,
        blAboveMaterial = blAboveMaterial,
        boxEntity = boxEntityOrNull,
    )
}


/**
 * Currently works by sending packet to respawn the shulker box.
 * Teleport packet does not seem to work, despite seeming correct
 * based on what packet should be...wtf?
 */
internal fun sendBoxEntityPacket(player: Player, boxEntity: BoxEntity) {
    // println("sendUpdateCrawlPacket $loc")

    // send packets to create fake shulker box
    val protocolManager = ProtocolLibrary.getProtocolManager()

    val entityConstructor = protocolManager.createPacketConstructor(PacketType.Play.Server.SPAWN_ENTITY_LIVING, boxEntity)
    val spawnPacket = entityConstructor.createPacket(boxEntity)

    protocolManager.sendServerPacket(player, spawnPacket)

    // entity metadata
    // https://wiki.vg/Entity_metadata#Shulker
    // https://aadnk.github.io/ProtocolLib/Javadoc/com/comphenix/protocol/wrappers/WrappedDataWatcher.html
    // https://github.com/aadnk/PacketWrapper/blob/master/PacketWrapper/src/main/java/com/comphenix/packetwrapper/WrapperPlayServerEntityMetadata.java
    // getWatchableObjects()
    val dataWatcher = WrappedDataWatcher(boxEntity.getDataWatcher())
    val dataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA, false)
    dataPacket.getIntegers().write(0, boxEntity.getId())
    dataPacket.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects())
    protocolManager.sendServerPacket(player, dataPacket)

    // TELEPORT PACKET DOES NOT SEEM TO WORK??? WTF?
    // https://github.com/aadnk/PacketWrapper/blob/master/PacketWrapper/src/main/java/com/comphenix/packetwrapper/WrapperPlayServerEntityTeleport.java    // getWatchableObjects()
    // val packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT, false)
    
    // // entity id
    // packet.getIntegers().write(0, boxEntity.getId())

    // // x, y, z
    // packet.getDoubles().write(0, loc.getX())
    // packet.getDoubles().write(1, loc.getY())
    // packet.getDoubles().write(2, loc.getZ())

    // // yaw, pitch
    // packet.getBytes().write(0, 0)
    // packet.getBytes().write(1, 0)

    // // on ground
    // packet.getBooleans().write(0, false)

    // protocolManager.sendServerPacket(player, packet)
}

internal fun removeBoxEntityPacket(player: Player, boxEntity: BoxEntity) {
    val protocolManager = ProtocolLibrary.getProtocolManager()
    val packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY)
    
    // entity id
    packet.getIntegerArrays().write(0, intArrayOf(boxEntity.getId()))

    protocolManager.sendServerPacket(player, packet)
}


/**
 * Send fake block material packet to player.
 * https://github.com/aadnk/PacketWrapper/blob/master/PacketWrapper/src/main/java/com/comphenix/packetwrapper/WrapperPlayServerBlockChange.java
 */
internal fun sendFakeBlockPacket(player: Player, x: Int, y: Int, z: Int, blockData: BlockData) {
    player.sendBlockChange(Location(player.getWorld(), x.toDouble(), y.toDouble(), z.toDouble(), 0f, 0f), blockData)
}

// /**
//  * Cancel's player fov change with server client abilities packet?
//  * https://github.com/aadnk/PacketWrapper/blob/master/PacketWrapper/src/main/java/com/comphenix/packetwrapper/WrapperPlayServerAbilities.java
//  */
//  DOES NOT WORK!!!
// internal fun resetFovPacket(player: Player) {
//     val protocolManager = ProtocolLibrary.getProtocolManager()
//     val packet = protocolManager.createPacket(PacketType.Play.Server.ABILITIES)
    
//     // is creative
//     packet.getBooleans().write(0, false)
//     // is flying
//     packet.getBooleans().write(1, false)
//     // is flying allowed
//     packet.getBooleans().write(2, false)
//     // is godmode
//     packet.getBooleans().write(3, false)
//     // flyspeed
//     packet.getFloat().write(0, 0.2f)
//     // walkspeed
//     packet.getFloat().write(1, 0.2f)

//     protocolManager.sendServerPacket(player, packet)
// }

/**
 * Request to start crawling.
 */
@JvmInline
public value class CrawlStart(
    val player: Player,
)

/**
 * Request to stop crawling.
 */
@JvmInline
public value class CrawlStop(
    val player: Player,
)

// Slowness effect while crawling. Max potion amplifier should be 255 i think... (ambient = true, particles = false)
private val SLOWNESS_EFFECT: PotionEffect = PotionEffect(PotionEffectType.SLOW, Int.MAX_VALUE, 255, true, false)
// No jump effect: when jump negative, prevents player from jumping (ambient = true, particles = false)
private val NO_JUMP_EFFECT: PotionEffect = PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, -128, true, false)

/**
 * Process start crawl requests for players. Returns new queue for next tick.
 */
public fun startCrawlSystem(requests: List<CrawlStart>): ArrayList<CrawlStart> {
    for ( r in requests ) {
        val player = r.player

        // make sure we do not double start crawl
        if ( XC.isCrawling(player) ) {
            continue
        }

        // println("START CRAWL $player")
        // println("CURRENT WALK SPEED: ${player.getWalkSpeed()}")

        // SLOWNESS_EFFECT.apply(player)
        NO_JUMP_EFFECT.apply(player)
        player.setWalkSpeed(0.0f)

        // send packet that cancels slowness fov change
        // NOT NEEDED WHEN USING player.setWalkSpeed(0.0f)
        // resetFovPacket(player)

        XC.crawling[player.getUniqueId()] = forceCrawl(player)

        // start fake swimming motion
        player.setSwimming(true)
    }

    return ArrayList(4)
}

/**
 * Process stop crawl requests for players. Returns new queue for next tick.
 */
public fun stopCrawlSystem(requests: List<CrawlStop>): ArrayList<CrawlStop> {
    for ( r in requests ) {
        val player = r.player
        val playerId = player.getUniqueId()
        // println("STOP CRAWL $player")

        // player.removePotionEffect(PotionEffectType.SLOW)
        player.removePotionEffect(PotionEffectType.JUMP)
        player.setWalkSpeed(0.2f) // default speed

        XC.crawling.remove(playerId)?.cleanup()

        // remove and stop crawl request task
        XC.crawlRequestTasks.remove(playerId)?.let { task ->
            task.cancel()
            // blank the progress bar
            Message.announcement(player, "")
        }
        
        // remove flag that player crawling and ready to shoot
        XC.crawlingAndReadyToShoot.remove(playerId)

        // remove aim down sights model
        XC.removeAimDownSightsOffhandModel(player)

        // stop fake swimming
        player.setSwimming(false)
    }

    return ArrayList(4)
}

/**
 * Update tick for all crawling players.
 * Returns new HashMap with updated crawl state for players.
 */
public fun crawlRefreshSystem(requests: HashMap<UUID, Crawling>): HashMap<UUID, Crawling> {
    val newCrawlingState = HashMap<UUID, Crawling>()

    for ( (playerId, prevCrawlState) in requests ) {
        val (player,
            initialLocation,
            prevLocationX,
            prevLocationY,
            prevLocationZ,
            blAboveX,
            blAboveY,
            blAboveZ,
            prevBlAboveMaterial,
            boxEntity,
        ) = prevCrawlState

        // not needed, toggle event should cancel stop swimming event
        // player.setSwimming(true)

        // TODO: periodically send barrier update packet
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO

        // if player location has changed or block above changed, send crawl update packet
        val currLocation = player.getLocation()
        val currBlockAboveMaterial = currLocation.world?.getBlockAt(blAboveX, blAboveY, blAboveZ)?.getType() ?: Material.AIR
        newCrawlingState[player.getUniqueId()] = if ( 
            currLocation.x != prevLocationX ||
            currLocation.y != prevLocationY ||
            currLocation.z != prevLocationZ ||
            currBlockAboveMaterial != prevBlAboveMaterial
        ) {
            // if travelled too far from initial location (e.g. water bucket or falling down)
            // cancel crawl next tick
            if ( initialLocation.distance(currLocation) > 1.0 ) {
                XC.crawlStopQueue.add(CrawlStop(player))
            }

            prevCrawlState.update(currLocation)
        } else {
            prevCrawlState
        }

        // if only allowed to crawl while using a crawl required weapon,
        // check if player using a crawl weapon.
        // if not, cancel crawl
        if ( XC.config.crawlOnlyAllowedOnCrawlWeapons ) {
            val gun = getGunInHand(player)
            if ( gun == null || gun.crawlRequired == false ) {
                XC.crawlStopQueue.add(CrawlStop(player))
            }
        }
    }

    return newCrawlingState
}


/**
 * Request to start crawling.
 */
@JvmInline
public value class CrawlToShootRequest(
    val player: Player,
)

/**
 * Crawl finished. Starts crawl system.
 */
@JvmInline
public value class CrawlToShootRequestFinish(
    val player: Player,
)

/**
 * Crawl cancelled (e.g. player moved during request).
 */
@JvmInline
public value class CrawlToShootRequestCancel(
    val player: Player,
)


/**
 * System that queues a "crawl to shoot" request.
 * Return new empty queue for next tick.
 */
public fun requestCrawlToShootSystem(requests: List<CrawlToShootRequest>, timestamp: Long): ArrayList<CrawlToShootRequest> {
    for ( r in requests ) {
        val player = r.player
        val playerId = player.getUniqueId()

        // if request task already exists, skip if "stale" (measure as <5 seconds from previous task)
        val previousTask = XC.crawlRequestTasks[playerId]
        if ( previousTask != null && timestamp < (previousTask.startTimestamp + 2000)  ) {
            continue
        }
        
        // Do redundant player main hand is gun check here
        // since events could override the first shoot event, causing
        // inventory slot or item to change
        val equipment = player.getInventory()
        val inventorySlot = equipment.getHeldItemSlot()
        val item = equipment.getItem(inventorySlot)
        if ( item == null ) {
            continue
        }

        val gun = getGunFromItem(item)
        if ( gun == null || gun.crawlRequired == false ) {
            continue
        }

        var itemMeta = item.getItemMeta()
        val itemData = itemMeta.getPersistentDataContainer()

        // initiate crawling
        XC.crawlStartQueue.add(CrawlStart(player))

        // set crawl to shoot id: this ensures this is same item
        // being used to start crawl and to shoot
        val crawlId = XC.newCrawlToShootId()
        itemData.set(XC.namespaceKeyItemCrawlToShootId!!, PersistentDataType.INTEGER, crawlId)
        
        // update item meta with new data
        item.setItemMeta(itemMeta)
        equipment.setItem(inventorySlot, item)

        // launch crawl to shoot preparation task
        val task = CrawlToShootRequestTask(
            player = player,
            crawlId = crawlId,
            finishTime = gun.crawlTimeMillis.toDouble(),
            startTimestamp = timestamp,
            itemGunMaterial = XC.config.materialGun,
        )

        // runs every 2 ticks = 100 ms
        task.runTaskTimerAsynchronously(XC.plugin!!, 0L, 1L)
        
        XC.crawlRequestTasks[playerId] = task
    }

    return ArrayList(4)
}

internal class CrawlToShootRequestTask(
    val player: Player,
    val crawlId: Int,
    val finishTime: Double,
    val startTimestamp: Long,
    val itemGunMaterial: Material,
): BukkitRunnable() {
    
    val itemRequestIdKey = XC.namespaceKeyItemCrawlToShootId!!
    val finishTaskQueue = XC.playerCrawlRequestFinishQueue
    val cancelTaskQueue = XC.playerCrawlRequestCancelQueue

    private fun cancelTask() {
        cancelTaskQueue.add(CrawlToShootRequestCancel(player))
        this.cancel()
    }

    override fun run() {
        // check if player log off or died
        if ( !player.isOnline() || player.isDead() ) {
            this.cancelTask()
            return
        }
        
        // check if item swapped
        val itemInHand = player.getInventory().getItemInMainHand()
        if ( itemInHand.getType() != itemGunMaterial ) {
            this.cancelTask()
            return
        }
        val itemCurrData = itemInHand.getItemMeta().getPersistentDataContainer()
        val itemCrawlId = itemCurrData.get(itemRequestIdKey, PersistentDataType.INTEGER) ?: -1
        if ( itemCrawlId != crawlId ) {
            this.cancelTask()
            return
        }

        val timeElapsedMillis = (System.currentTimeMillis() - startTimestamp).toDouble()
        if ( timeElapsedMillis > finishTime ) {
            // done: add finish task to queue
            finishTaskQueue.add(CrawlToShootRequestFinish(player))
            this.cancel()
        } else {
            val progress = timeElapsedMillis / finishTime
            Message.announcement(player, "${progressBar10(progress)}")
        }
    }
}

/**
 * Finish crawl to shoot requests.
 * Return new empty queue for next tick.
 */
public fun finishCrawlToShootRequestSystem(requests: List<CrawlToShootRequestFinish>) {
    for ( r in requests ) {
        val player = r.player
        val playerId = player.getUniqueId()

        // remove crawl request task
        XC.crawlRequestTasks.remove(playerId)

        // check if player still crawling: if not, skip
        if ( !XC.isCrawling(player) ) {
            continue
        }

        // check player still using a crawl weapon
        // TODO: todo properly need to check if crawl id same.
        // but this case is so rare that it's not worth the effort.
        val equipment = player.getInventory()
        val inventorySlot = equipment.getHeldItemSlot()
        val item = equipment.getItem(inventorySlot)
        if ( item == null ) {
            XC.crawlStopQueue.add(CrawlStop(player))
            continue
        }

        val gun = getGunFromItem(item)
        if ( gun == null || gun.crawlRequired == false ) {
            XC.crawlStopQueue.add(CrawlStop(player))
            continue
        }

        // set aim down sights model
        var itemMeta = item.getItemMeta()
        val itemData = itemMeta.getPersistentDataContainer()

        // send ammo message
        val ammo = itemData.get(XC.namespaceKeyItemAmmo!!, PersistentDataType.INTEGER) ?: 0
        XC.gunAmmoInfoMessageQueue.add(AmmoInfoMessagePacket(player, ammo, gun.ammoMax))

        // if player is aim down sights, add offhand model
        if ( useAimDownSights(player) ) {
            itemMeta = setGunItemMetaModel(itemMeta, gun, ammo, true)
            item.setItemMeta(itemMeta)
            equipment.setItem(inventorySlot, item)

            XC.createAimDownSightsOffhandModel(gun.itemModelAimDownSights, player)
        }

        // mark player ready to shoot
        XC.crawlingAndReadyToShoot[playerId] = true
    }
}

/**
 * Finish cancelled crawl to shoot requests.
 * Return new empty queue for next tick.
 */
public fun cancelCrawlToShootRequestSystem(requests: List<CrawlToShootRequestCancel>) {
    for ( r in requests ) {
        val player = r.player
        val playerId = player.getUniqueId()

        // remove crawl request task
        XC.crawlRequestTasks.remove(playerId)

        // stop crawling
        if ( XC.isCrawling(player) ) {
            XC.crawlStopQueue.add(CrawlStop(player))
        }
        
        // clear crawl to shoot progress bar
        Message.announcement(player, "")
    }
}