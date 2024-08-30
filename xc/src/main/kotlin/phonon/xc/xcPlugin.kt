/*
 * Implement bukkit plugin interface
 */

package phonon.xc

import org.bukkit.command.TabCompleter
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import phonon.xc.commands.AimDownSightsCommand
import phonon.xc.commands.Command
import phonon.xc.listeners.EventListener

class XCPlugin : JavaPlugin() {

    // plugin internal state
    val xc: XC = XC(
        this,
        this.logger
    )

    override fun onEnable() {

        // measure load time
        val timeStart = System.nanoTime()

        val pluginManager = this.server.pluginManager

        // ===================================
        // Initialize main plugin:
        // - save hooks to this plugin 
        // - save hooks to external APIs
        // ===================================

        // world guard hook
        val pluginWorldGuard = pluginManager.getPlugin("WorldGuard")
        val usingWorldGuard = (pluginManager.isPluginEnabled("WorldGuard") && pluginWorldGuard != null).also {
            logger.info("Using WorldGuard v${pluginWorldGuard?.pluginMeta.version}")
        }

        xc.usingWorldGuard(usingWorldGuard)

        // ===================================
        // Plugin reload
        // ===================================

        // register listeners
        pluginManager.registerEvents(EventListener(xc), this)

        // register commands
        this.getCommand("xc")?.setExecutor(Command(xc))
        this.getCommand("aimdownsights")?.setExecutor(AimDownSightsCommand(xc))

        // override command aliases tab complete if they exist
        this.getCommand("xc")?.tabCompleter = this.getCommand("xc")?.executor as TabCompleter
        this.getCommand("ads")?.tabCompleter = this.getCommand("aimdownsights")?.executor as TabCompleter

        // load plugin and start engine
        xc.reload()
        xc.start()

        // print load time
        val timeEnd = System.nanoTime()
        val timeLoad = timeEnd - timeStart
        logger.info("Enabled in ${timeLoad/1e6}ms")

        // print success message
        logger.info("now this is epic")
    }

    override fun onDisable() {
        xc.stop()
        xc.onDisable()

        HandlerList.unregisterAll(this);

        logger.info("wtf i hate xeth now")
    }
}
