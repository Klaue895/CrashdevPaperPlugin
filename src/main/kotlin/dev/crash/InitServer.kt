package dev.crash

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.crash.commands.*
import dev.crash.commands.admin.*
import dev.crash.commands.home.*
import dev.crash.commands.mod.*
import dev.crash.commands.tp.*
import dev.crash.commands.text.*
import dev.crash.commands.user.*
import dev.crash.items.CraftingRecipes
import dev.crash.items.CustomItems
import dev.crash.listener.*
import dev.crash.permission.*
import dev.crash.player.clearPlayerData
import dev.crash.player.crashPlayer
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

internal fun initServer(){
    if(!File(INSTANCE.dataFolder.path + "/player/").exists()){
        File(INSTANCE.dataFolder.path + "/player/").mkdir()
    }
    loadPluginConfig()
    loadRanks()
    initDatabase()
    loadWarps()
    initQuests()
    initGuilds()
    loadLootTables()
    loadFromDb()
    CustomItems.loadItems()
    initTradingInventories()
    loadWorlds()
    loadDropTables()
    updateCreditScore()
    CraftingRecipes.loadRecipes()
    loadMaxEnchantmentLevel()
    loadPrices()
    registerCommands()
    registerEvents()
}

internal fun registerEvents(){
    val listener = listOf(AnvilListener(), ChatListener(), CheatListener(), ChunkListener(), ClaimListener(), CraftingListener(), DamageListener(),
    DeathListener(), DeSpawnListener(), ElytraListener(), InteractListener(), InventoryListener(), JoinListener(), LeaveListener(), MobSpawnListener(),
    PortalListener(), QuestListener(), RespawnListener(), SignListener(), ProjectileListener())

    val pluginManager = Bukkit.getPluginManager()
    listener.forEach {
        pluginManager.registerEvents(it, INSTANCE)
    }
    if(CONFIG.votifierEnabled){
        pluginManager.registerEvents(VoteListener(), INSTANCE)
    }
}

internal fun registerCommands(){
    INSTANCE.getCommand("ci")!!.setExecutor(CICommand())
    INSTANCE.getCommand("ci")!!.tabCompleter = CICommand()
    INSTANCE.getCommand("discord")!!.setExecutor(DiscordCommand())
    if(CONFIG.votifierEnabled) INSTANCE.getCommand("vote")!!.setExecutor(VoteCommand())
    INSTANCE.getCommand("multiv")!!.setExecutor(MultivCommand())
    INSTANCE.getCommand("multiv")!!.tabCompleter = MultivCommand()
    INSTANCE.getCommand("ec")!!.setExecutor(EcCommand())
    INSTANCE.getCommand("back")!!.setExecutor(BackCommand())
    INSTANCE.getCommand("claim")!!.setExecutor(ClaimCommand())
    INSTANCE.getCommand("claim")!!.tabCompleter = ClaimCommand()
    INSTANCE.getCommand("drop")!!.setExecutor(DropCommand())
    INSTANCE.getCommand("drop")!!.tabCompleter = DropCommand()
    INSTANCE.getCommand("exp")!!.setExecutor(ExpCommand())
    INSTANCE.getCommand("exp")!!.tabCompleter = ExpCommand()
    INSTANCE.getCommand("home")!!.setExecutor(HomeCommand())
    INSTANCE.getCommand("home")!!.tabCompleter = HomeCommand()
    INSTANCE.getCommand("delhome")!!.setExecutor(DelhomeCommand())
    INSTANCE.getCommand("delhome")!!.tabCompleter = HomeCommand()
    INSTANCE.getCommand("sethome")!!.setExecutor(SetHomeCommand())
    INSTANCE.getCommand("homes")!!.setExecutor(HomesCommand())
    INSTANCE.getCommand("invec")!!.setExecutor(InvEcCommand())
    INSTANCE.getCommand("invsee")!!.setExecutor(InvSeeCommand())
    INSTANCE.getCommand("m")!!.setExecutor(MessageCommand())
    INSTANCE.getCommand("modchat")!!.setExecutor(ModChatCommand())
    INSTANCE.getCommand("profile")!!.setExecutor(ProfileCommand())
    INSTANCE.getCommand("quests")!!.setExecutor(QuestsCommand())
    INSTANCE.getCommand("quests")!!.tabCompleter = QuestsCommand()
    INSTANCE.getCommand("rank")!!.setExecutor(RankCommand())
    INSTANCE.getCommand("rank")!!.tabCompleter = RankCommand()
    INSTANCE.getCommand("reloaddb")!!.setExecutor(ReloadCommand())
    INSTANCE.getCommand("reloaduser")!!.setExecutor(ReloadUserCommand())
    INSTANCE.getCommand("tempban")!!.setExecutor(TempbanCommand())
    INSTANCE.getCommand("tempban")!!.tabCompleter = TempbanCommand()
    INSTANCE.getCommand("warn")!!.setExecutor(WarnCommand())
    INSTANCE.getCommand("warn")!!.tabCompleter = WarnCommand()
    INSTANCE.getCommand("tpa")!!.setExecutor(TpaCommand())
    INSTANCE.getCommand("tpaccept")!!.setExecutor(TpAcceptCommand())
    INSTANCE.getCommand("tpdeny")!!.setExecutor(TpDenyCommand())
    INSTANCE.getCommand("credits")!!.setExecutor(CreditsCommand())
    INSTANCE.getCommand("credits")!!.tabCompleter = CreditsCommand()
    INSTANCE.getCommand("key")!!.setExecutor(KeyCommand())
    INSTANCE.getCommand("key")!!.tabCompleter = KeyCommand()
    INSTANCE.getCommand("weekly")!!.setExecutor(WeeklyCommand())
    INSTANCE.getCommand("mute")!!.setExecutor(MuteCommand())
    INSTANCE.getCommand("mute")!!.tabCompleter = MuteCommand()
    INSTANCE.getCommand("unmute")!!.setExecutor(UnmuteCommand())
    INSTANCE.getCommand("crypto")!!.setExecutor(CryptoCommand())
    INSTANCE.getCommand("checkitem")!!.setExecutor(CheckItemCommand())
    INSTANCE.getCommand("guild")!!.setExecutor(GuildCommand())
    INSTANCE.getCommand("guild")!!.tabCompleter = GuildCommand()
    INSTANCE.getCommand("warp")!!.setExecutor(WarpCommand())
    INSTANCE.getCommand("warp")!!.tabCompleter = WarpCommand()
}

internal fun loadWorlds(){
    val worlds = INSTANCE.config.getStringList("worlds")
    if(worlds.isEmpty()){
        worlds.add("event")
        INSTANCE.config.set("worlds", worlds)
        INSTANCE.saveConfig()
    }
    worlds.forEach {
        WorldCreator(it).createWorld()
    }
}

internal fun loadFromDb(){
    transaction {
        chunks.clear()
        ChunkTable.selectAll().forEach {
            val chunkKey = Chunk.getChunkKey(it[ChunkTable.x], it[ChunkTable.z])
            val chunkClass = ChunkClass(it[ChunkTable.x], it[ChunkTable.z], it[ChunkTable.world], it[ChunkTable.uuid], it[ChunkTable.name],
                it[ChunkTable.shared].split(" ").toMutableList())
            if(chunks.containsKey(chunkClass.world)){
                chunks[chunkClass.world]!![chunkKey] = chunkClass
            }else {
                chunks[chunkClass.world] = hashMapOf(chunkKey to chunkClass)
            }
            chunkClassList.add(chunkClass)
        }
        keyChests.clear()
        val keyChestsFile = File(INSTANCE.dataFolder.path + "/keychests.json")
        if(keyChestsFile.exists()){
            val keyChestsMap = jacksonObjectMapper().readValue<HashMap<String, Int>>(keyChestsFile)
            keyChestsMap.forEach {
                keyChests[getBlockByPositionString(it.key)] = it.value
            }
        }else {
            keyChestsFile.createNewFile()
            jacksonObjectMapper().writeValue(keyChestsFile, hashMapOf<String, Int>())
        }
        portals.clear()
        val portalFile = File(INSTANCE.dataFolder.path + "/portals.json")
        if(portalFile.exists()){
            val portalMap = jacksonObjectMapper().readValue<HashMap<String, String>>(portalFile)
            portalMap.forEach {
                portals[getBlockByPositionString(it.key)] = it.value
            }
        }else {
            portalFile.createNewFile()
            jacksonObjectMapper().writeValue(portalFile, hashMapOf<String, String>())
        }
    }
    updateTabOfPlayers()
    clearPlayerData()
    Bukkit.getOnlinePlayers().forEach {
        it.setResourcePack(CONFIG.texturePackURL, CONFIG.texturePackHash)
        val playerTextComponent = Component.text("${it.crashPlayer().rankData().prefix} ${it.name}")
        it.updateScoreboard()
        it.playerListName(playerTextComponent)
        it.displayName(playerTextComponent)
        it.customName(playerTextComponent)
        it.isCustomNameVisible = true
    }
}

data class PricesSaveObj(val material: String, val cmd: Int, val price: Long)
internal fun loadPrices(){
    prices.clear()
    val file = File(INSTANCE.dataFolder.path + "/prices.json")
    if(file.exists()){
        val pricesObjects = jacksonObjectMapper().readValue<List<PricesSaveObj>>(file)
        pricesObjects.forEach {
            val m = Material.valueOf(it.material)
            if(!prices.containsKey(m)){
                prices[m] = hashMapOf()
            }
            prices[m]!![it.cmd] = it.price
        }
    }else {
        file.createNewFile()
        jacksonObjectMapper().writeValue(file, listOf<PricesSaveObj>())
    }
}