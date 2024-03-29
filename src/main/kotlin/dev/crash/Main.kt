package dev.crash

import dev.crash.player.load
import dev.crash.player.crashPlayer
import dev.crash.player.unload
import org.bukkit.Bukkit
import org.bukkit.enchantments.Enchantment
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

lateinit var INSTANCE : Main
class Main : JavaPlugin() {

    override fun onLoad(){
        INSTANCE = this
        println("[INFO] CrashdevPaperPlugin is loaded!")
    }

    override fun onEnable() {
        INSTANCE = this
        val f = Enchantment::class.java.getDeclaredField("acceptingNew")
        f.isAccessible = true
        f.set(null, true)
        initServer()
        Bukkit.getOnlinePlayers().forEach {
            it.unload()
            it.load()
        }
        object : BukkitRunnable(){
            override fun run() {
                Bukkit.getOnlinePlayers().forEach {
                    it.crashPlayer().save()
                }
            }
        }.runTaskTimerAsynchronously(INSTANCE, 0, 20*60)
        println("[INFO] CrashdevPaperPlugin was enabled!")
    }

    override fun onDisable() {
        allJobs.forEach {
            it.cancel()
        }
        Bukkit.getOnlinePlayers().forEach {
            it.unload()
        }
        Bukkit.getScheduler().pendingTasks.forEach {
            it.cancel()
        }
        Bukkit.getScheduler().cancelTasks(this)
        println("[INFO] CrashdevPaperPlugin is disabled!")
    }
}