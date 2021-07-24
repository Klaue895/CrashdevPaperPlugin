package de.rlg.commands

import de.rlg.*
import de.rlg.player.rlgPlayer
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.*
import kotlin.collections.HashMap

class GuildCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val player = sender.asPlayer()
        when(args.size){
            1 -> {
                when(args[0]){
                    "setup" -> guildSetup(player, "")
                    "leave" -> player.rlgPlayer().removeFromGuild()
                    "delete" -> player.rlgPlayer().deleteGuild()
                    "accept" -> {
                        if(inviteTargetMap.containsKey(player.uniqueId)){

                        }
                    }
                }
            }
            2 -> {
                when(args[0]){
                    "setup" -> {
                        if(args[1] == "cancel"){
                            guildSetupProgress.remove(player)
                            player.sendMessage("§aGuild Setup wurde abgebrochen!")
                        }
                    }
                    "invite" -> {
                        val rlgPlayer = player.rlgPlayer()
                        if(rlgPlayer.guildId == 0) {
                            player.sendMessage("§4Du bist in keiner Guild!")
                            return true
                        }
                        val target = Bukkit.getPlayer(args[1])
                        if(target == null){
                            player.sendMessage("§4Du musst einen gültigen Spieler angeben!")
                            return true
                        }
                        val targetRLGPlayer = target.rlgPlayer()
                        if(targetRLGPlayer.guildId != 0){
                            player.sendMessage("§4Der Spieler ist bereits in einer Guild!")
                            return true
                        }
                        else if(inviteTargetMap.containsKey(target.uniqueId)){
                            player.sendMessage("§4Der Spieler hat bereits eine Einladung in eine andere Guild!")
                            return true
                        }
                        inviteTargetMap[target.uniqueId] = player.uniqueId
                    }
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
        val player = sender.asPlayer()
        return when(args.size){
            1 -> {
                val result = arrayListOf<String>()
                try { if(player.rlgPlayer().guild()!!.owner_uuid == player.uniqueId.toString()) result.add("delete") else result.add("leave") }catch (ex: NullPointerException) {
                    result.addAll(arrayListOf("accept", "setup"))}
                result
            }
            2 -> {
                when(args[0]){
                    "setup" -> arrayListOf("cancel")
                    else -> null
                }
            }
            else -> null
        }
    }
}

val inviteTargetMap = HashMap<UUID, UUID>()