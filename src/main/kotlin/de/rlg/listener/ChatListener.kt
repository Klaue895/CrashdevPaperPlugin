package de.rlg.listener

import de.rlg.*
import de.rlg.permission.rankData
import de.rlg.player.rlgPlayer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ChatListener : Listener {

    @EventHandler
    fun onChat(chatEvent: AsyncChatEvent){
        val player: Player = chatEvent.player
        val rlgPlayer = player.rlgPlayer()
        val message: String = (chatEvent.message() as TextComponent).content().replace("$", "§")
        when {
            checkMessage(message, player) || rlgPlayer.mutedUntil > System.currentTimeMillis() -> {
                chatEvent.isCancelled = true
                return
            }
            setup1.containsKey(player) -> {
                setupShop2(player, message)
                chatEvent.isCancelled = true
                return
            }
            setup2.containsKey(player) -> {
                setupShop3(player, message)
                chatEvent.isCancelled = true
                return
            }
            guildSetupProgress.containsKey(player) -> {
                guildSetup(player, message)
                chatEvent.isCancelled = true
                return
            }
        }
        if(rlgPlayer.guildId == 0){
            chatEvent.composer { _, _, _ -> Component.text("${rankData[rlgPlayer.rank]!!.prefix} ${player.name}> $message")}
        }else {
            chatEvent.composer { _, _, _ -> Component.text("${rankData[rlgPlayer.rank]!!.prefix} §8[§6${rlgPlayer.guild()!!.suffix}§8]§r ${player.name}> $message")}
        }
    }
}