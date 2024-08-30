package phonon.xc.util

import org.bukkit.Location
import org.bukkit.World

fun Location.component1(): Double = this.x
fun Location.component2(): Double = this.y
fun Location.component3(): Double = this.z
fun Location.component4(): Float = this.yaw
fun Location.component5(): Float = this.pitch
fun Location.component6(): World = this.world

