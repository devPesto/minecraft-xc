package phonon.xv.system

import java.util.Queue
import java.util.Stack
import com.google.gson.JsonObject
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import phonon.xv.XV
import phonon.xv.ITEM_KEY_ELEMENTS
import phonon.xv.core.ComponentsStorage
import phonon.xv.core.INVALID_VEHICLE_ID
import phonon.xv.core.EntityVehicleData
import phonon.xv.core.Vehicle
import phonon.xv.core.VehicleComponentType
import phonon.xv.core.VehicleComponents
import phonon.xv.core.VehicleElement
import phonon.xv.core.VehicleElementId
import phonon.xv.core.VehicleElementPrototype
import phonon.xv.core.VehiclePrototype
import java.util.UUID

/**
 * Indicates reason for creating a vehicle. Customizes what creation
 * time specific parameters are injected into the vehicle.
 */
public enum class CreateVehicleReason {
    NEW,
    LOAD,
    ;
}

/**
 * A request to create a vehicle, containing optional creation sources
 * (player, item, json object, etc.) which are used to gather specific
 * creation parameters.
 */
public data class CreateVehicleRequest(
    val prototype: VehiclePrototype,
    val reason: CreateVehicleReason,
    val location: Location? = null,
    val player: Player? = null,
    val item: ItemStack? = null,
    val json: JsonObject? = null,
)

// TODO when we create a vehicle its gonna give the player
// a loading bar, but this functionality is common
// to refueling and reloading a cannon. (when we add that)
// where do we put it?

public fun XV.systemCreateVehicle(
    componentStorage: ComponentsStorage,
    requests: Queue<CreateVehicleRequest>
) {
    val xv = this // alias

    while ( requests.isNotEmpty() ) {
        val (
            prototype,
            reason,
            location,
            player,
            item,
            json,
        ) = requests.remove()

        if ( xv.vehicleStorage.size >= xv.vehicleStorage.maxVehicles ) {
            xv.logger.severe("Failed to create new vehicle ${prototype.name} at ${location}: vehicle storage full")
            continue
        }
        
        try {
            // creation item meta and persistent data container
            val itemMeta = item?.itemMeta
            val itemData = itemMeta?.persistentDataContainer
            val itemElementsDataContainer = itemData?.get(ITEM_KEY_ELEMENTS, PersistentDataType.TAG_CONTAINER)

            // json elements
            val jsonElements = json?.get("elements")?.asJsonObject

            // inject creation properties into all element prototypes
            val elementComponents: List<VehicleComponents> = prototype.elements.map { elemPrototype ->
                var components = elemPrototype.components.clone()
                val itemElementsData = itemElementsDataContainer?.get(elemPrototype.itemKey(), PersistentDataType.TAG_CONTAINER)

                when ( reason ) {
                    // spawning a new vehicle ingame from item or command
                    CreateVehicleReason.NEW -> {
                        // inject creation time properties, keep in this order:

                        // item properties stored in item meta persistent data container
                        components = if ( itemElementsData !== null ) {
                            components.injectItemProperties(itemElementsData)
                        } else {
                            components
                        }

                        // main spawn player, location, etc. properties
                        // player properties (this creates armor stands internally)
                        components = components.injectSpawnProperties(location, player)

                        components
                    }
                    
                    // loading from serialized json: only inject json properties
                    CreateVehicleReason.LOAD -> {
                        // current json object is the whole vehicle json, we
                        // want just the json object with our element
                        val elemJson = jsonElements?.get(elemPrototype.name)?.asJsonObject?.get("components")?.asJsonObject
                        if ( elemJson !== null ) {
                            components = components.injectJsonProperties(elemJson)
                        }

                        components
                    }
                }
            }

            // try to insert each prototype into its archetype
            val elementIds: List<VehicleElementId?> = elementComponents.map { components ->
                componentStorage.lookup[components.layout]!!.insert(components)
            }
            
            // if any are null, creation failed. remove non-null created elements
            // from their archetypes
            if ( elementIds.any { it === null } ) {
                xv.logger.severe("Failed to create vehicle ${prototype.name} at ${location}")

                elementIds.forEachIndexed { index, id ->
                    if ( id !== null ) {
                        componentStorage.lookup[elementComponents[index].layout]?.free(id)
                    } else {
                        xv.logger.severe("Failed to create element ${prototype.elements[index].name}")
                    }
                }

                // skip this request
                continue
            }

            // create vehicle elements
            val elements = elementIds.mapIndexed { idx, id ->
                val elemUuid = jsonElements?.get(prototype.elements[idx].name)?.asJsonObject?.get("uuid")?.asString?.let { UUID.fromString(it) }
                    ?: UUID.randomUUID()
                
                val elem = VehicleElement(
                    name="${prototype.name}.${prototype.elements[idx].name}.${id}",
                    uuid=elemUuid,
                    id=id!!,
                    prototype=prototype.elements[idx],
                    layout=elementComponents[idx].layout,
                    components=elementComponents[idx],
                )
                xv.uuidToElement[elem.uuid] = elem
                elem
            }
            
            // set parent/children hierarchy
            for ( (idx, elem) in elements.withIndex() ) {
                val parentIdx = prototype.parentIndex[idx]
                if ( parentIdx != -1 ) {
                    elem.parent = elements[parentIdx]
                }
                
                val childrenIdx = prototype.childrenIndices[idx]
                if ( childrenIdx.isNotEmpty() ) {
                    elem.children = childrenIdx.map { elements[it] }
                }
            }

            val vehicleUuid = when ( reason ) {
                CreateVehicleReason.NEW -> UUID.randomUUID() // generate random uuid if new
                // use existing if load
                CreateVehicleReason.LOAD -> UUID.fromString( json!!["uuid"]!!.asString )
            }
            // insert new vehicle
            val vehicleId = xv.vehicleStorage.insert(
                vehicleUuid,
                prototype=prototype,
                elements=elements,
            )
            xv.uuidToVehicle[vehicleUuid] = xv.vehicleStorage.get(vehicleId)!!
            // xv.logger.info("Created vehicle with uuid $vehicleUuid")

            // this should never happen, but check
            if ( vehicleId == INVALID_VEHICLE_ID ) {
                xv.logger.severe("Failed to create vehicle ${prototype.name} at ${location}: vehicle storage full")
                // free elements
                elements.forEach { elem -> componentStorage.lookup[elem.layout]?.free(elem.id) }
                continue
            }

            // do element post-processing (note: can use prototype because
            // it still holds references to components)
            // for elements with armorstands models, this does entity -> element mapping
            for ( (idx, components) in elementComponents.withIndex() ) {
                components.afterVehicleCreated(
                    vehicle=xv.vehicleStorage.get(vehicleId)!!,
                    element=elements[idx],
                    entityVehicleData=entityVehicleData,
                )
            }

            // test stuff
            player?.sendMessage("Created your vehicle at x:${location}")
        }
        catch ( e: Exception ) {
            xv.logger.severe("Failed to create vehicle ${prototype.name} at ${location}")
            e.printStackTrace()
        }
    }
}