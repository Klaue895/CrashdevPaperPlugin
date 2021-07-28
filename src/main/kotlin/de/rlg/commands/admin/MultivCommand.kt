package de.rlg.commands.admin

import de.rlg.*
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class MultivCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender.asPlayer()
        if (player.hasPermission("rlg.multiv")) {
            if (args[0].contentEquals("tp")) {
                if (args.size >= 2) {
                    val w: World = Bukkit.getWorld(args[1])!!
                    var p: Player? = null
                    try {
                        p = Bukkit.getPlayer(args[2])
                    }catch (ex: ArrayIndexOutOfBoundsException){ }
                    if (p == null) {
                        p = player
                    }
                    p.teleport(w.spawnLocation.add(0.5, 0.0, 0.5))
                    println(p.name + " teleported to " + w.name)
                } else {
                    player.sendMessage("Ungültige Argumente")
                }
            } else if (args[0].contentEquals("create")) {
                if (args.size >= 3) {
                    val worldName: String = args[1]
                    val environment: String = args[2]
                    var worldtype = "NORMAL"
                    try {
                        worldtype = args[3]
                    } catch (ignored: NullPointerException) {
                    } catch (ignored: ArrayIndexOutOfBoundsException) {
                    }
                    try {
                        val seed: String = args[4]
                        player.sendMessage("§6Welt wird erstellt...")
                        Bukkit.createWorld(
                            WorldCreator.name(worldName).environment(World.Environment.valueOf(environment)).type(
                                WorldType.valueOf(
                                    worldtype
                                )
                            ).seed(seed.toLong())
                        )
                        player.sendMessage("§2Welt wurde erstellt!")
                    } catch (ignored: NullPointerException) {
                        player.sendMessage("§6Welt wird erstellt...")
                        Bukkit.createWorld(
                            WorldCreator.name(worldName).environment(World.Environment.valueOf(environment)).type(
                                WorldType.valueOf(
                                    worldtype
                                )
                            )
                        )
                        player.sendMessage("§2Welt wurde erstellt!")
                    } catch (ignored: ArrayIndexOutOfBoundsException) {
                        player.sendMessage("§6Welt wird erstellt...")
                        Bukkit.createWorld(
                            WorldCreator.name(worldName).environment(World.Environment.valueOf(environment)).type(
                                WorldType.valueOf(
                                    worldtype
                                )
                            )
                        )
                        player.sendMessage("§2Welt wurde erstellt!")
                    }
                    addWorld(worldName)
                } else {
                    player.sendMessage("§4Bitte die richtigen Arguments benutzen!")
                }
            } else if (args[0].contentEquals("portal")) {
                if (args.size == 2) {
                    val block = player.world.getBlockAt(player.location.add(0.0, -1.0, 0.0))
                    addPortal(block, args[1])
                    block.type = Material.END_PORTAL
                } else {
                    player.sendMessage("Ungültige Argumente")
                }
            } else if(args[0].contentEquals("remove")) {
                if(args.size == 2) {
                    val worldName = args[1]
                    removeWorld(worldName)
                    player.sendMessage("§2Die Welt $worldName wird beim nächsten Server Restart deaktiviert")
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String>? {
        return when(args.size) {
            1 -> mutableListOf("create", "remove", "tp", "portal")
            2 -> {
                val list: MutableList<String> = ArrayList()
                for (world in Bukkit.getWorlds()) {
                    list.add(world.name)
                }
                list
            }
            3 -> {
                when(args[0]) {
                    "create" -> mutableListOf("NORMAL", "NETHER", "THE_END")
                    "tp" -> {
                        val list: MutableList<String> = ArrayList()
                        val currentString: String = args[2]
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (player.name.startsWith(currentString)) {
                                list.add(player.name)
                            }
                        }
                        list
                    }
                    else -> null
                }
            }
            4 -> {
                when(args[0]){
                    "create" -> mutableListOf("NORMAL", "AMPLIFIED", "FLAT", "LARGE_BIOMES")
                    else -> null
                }
            }
            5 -> {
                when(args[0]){
                    "create" -> mutableListOf(sender.asPlayer().world.seed.toString())
                    else -> null
                }
            }
            else -> null
        }
    }
}

fun addWorld(worldName: String) {
    val worlds = INSTANCE.config.getStringList("worlds")
    worlds.add(worldName)
    INSTANCE.saveConfig()
}

fun removeWorld(worldName: String) {
    val worlds = INSTANCE.config.getStringList("worlds")
    worlds.remove(worldName)
    INSTANCE.saveConfig()
}

fun addPortal(block: Block, target: String) {
    transaction {
        PortalTable.insert {
            it[targetWorld] = target
            it[portalPos] = block.toSQLString()
        }
    }
}

fun removePortal(block: Block) {
    transaction {
        PortalTable.deleteWhere {
            PortalTable.portalPos eq block.toSQLString()
        }
    }
}