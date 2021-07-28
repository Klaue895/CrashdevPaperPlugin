package de.rlg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.rlg.items.CustomItems
import de.rlg.items.ciName
import de.rlg.items.getByTypeCmd
import de.rlg.listener.addMessageListener
import de.rlg.permission.rankData
import de.rlg.player.rlgPlayer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.kbrewster.mojangapi.MojangAPI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.block.Sign
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.entity.WanderingTrader
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

var prices = HashMap<Material, HashMap<Int, Long>>()
var amountmap = HashMap<Int, Int>()
var shops: MutableList<Shop> = ArrayList()
var signs = HashMap<Block, Shop>()
var setupShop = HashMap<Player, Shop>()
var shopinventories: MutableList<Inventory> = ArrayList()
var playershopinventories = HashMap<Inventory, Shop>()
var creditsScoreBoard = ""

fun initTradingInventories() {
    amountmap.clear()
    amountmap[0] = 1
    amountmap[1] = 8
    amountmap[2] = 16
    amountmap[3] = 64

    val overview: Inventory = Bukkit.createInventory(null, 9, Component.text("Shop"))
    overview.setItem(0, CustomItems.defaultCustomItem(Material.WRITTEN_BOOK, "§1Buch-Shop", arrayListOf(), 1, Pair("rlgAction", "book")))
    overview.setItem(1, CustomItems.defaultCustomItem(Material.OAK_LOG, "§cHolz-Shop", arrayListOf(), 0, Pair("rlgAction", "wood")))
    overview.setItem(2, CustomItems.defaultCustomItem(Material.POLISHED_ANDESITE, "§6Bau-Shop", arrayListOf(), 0, Pair("rlgAction", "build")))
    overview.setItem(3, CustomItems.defaultCustomItem(Material.STICK, "§eCrypto-Shop", arrayListOf(), 1, Pair("rlgAction", "crypto")))
    TradingInventories.overview = overview

    val bookOverview: Inventory = Bukkit.createInventory(null, 9, Component.text("Buch-Shop"))
    bookOverview.setItem(0, CustomItems.defaultCustomItem(Material.WRITTEN_BOOK, "Kaufe 1 Einsteiger-Buch für 100 Credits", arrayListOf(), 2,
    Pair("rlgAction", "book beginner")))
    bookOverview.setItem(1, CustomItems.defaultCustomItem(Material.WRITTEN_BOOK, "Kaufe 1 Magie-Buch für 5000 Credits", arrayListOf(), 1,
    Pair("rlgAction", "book magic")))
    TradingInventories.bookOverview = bookOverview

    val woodMaterials: List<Material> = arrayListOf(Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG)
    val woodOverview: Inventory = Bukkit.createInventory(null, 54, Component.text("Holz-Shop"))
    for (j in woodMaterials.indices) {
        for (i in 0 until amountmap.size) {
            woodOverview.setItem(i + 9 * j, CustomItems.defaultCustomItem(woodMaterials[j],
                "Kaufe " + amountmap[i] + " " + woodMaterials[j].toString().lowercase(Locale.ROOT).toStartUppercaseMaterial() + " für " + (amountmap[i]!! * 16) + " Credits", arrayListOf(),0,
            Pair("rlgAction", "wood ${woodMaterials[j].name.lowercase()} ${amountmap[i]}")))
        }
    }
    TradingInventories.woodOverview = woodOverview

    val buildOverview: Inventory = Bukkit.createInventory(null, 54, Component.text("Bau-Shop"))
    for (i in 0 until amountmap.size) {
        buildOverview.setItem(i, CustomItems.defaultCustomItem(Material.STONE,
            "Kaufe " + amountmap[i] + " " + Material.STONE.toString().lowercase(Locale.ROOT).toStartUppercaseMaterial() + " für " + (amountmap[i]!! * 16) + " Credits", arrayListOf(), 0,
        Pair("rlgAction", "build stone ${amountmap[i]}")))
    }
    val buildMaterials: List<Material> = ArrayList(
        listOf(
            Material.POLISHED_ANDESITE,
            Material.POLISHED_DIORITE,
            Material.POLISHED_GRANITE,
            Material.BRICKS,
            Material.WHITE_CONCRETE
        )
    )
    for (j in buildMaterials.indices) {
        for (i in 0 until amountmap.size) {
            buildOverview.setItem(i + (9 * j) + 9, CustomItems.defaultCustomItem(buildMaterials[j],
                "Kaufe " + amountmap[i] + " " + buildMaterials[j].toString().lowercase(Locale.ROOT).toStartUppercaseMaterial() + " für " + (amountmap[i]!! * 32) + " Credits", arrayListOf(), 0,
            Pair("rlgAction", "build ${buildMaterials[j].name.lowercase()} ${amountmap[i]}")))
        }
    }
    TradingInventories.buildOverview = buildOverview

    val inventory: Inventory = Bukkit.createInventory(null, 27, Component.text("Normal Shop"))
    val itemStack = CustomItems.defaultCustomItem(Material.GRAY_STAINED_GLASS_PANE, " ", arrayListOf())
    for (i in 0 until inventory.size) {
        inventory.setItem(i, itemStack)
    }
    ShopInventories.normalView = inventory

    val blackMarketInv: Inventory = Bukkit.createInventory(null, 9, Component.text("Schwarzmarkt"))
    blackMarketInv.setItem(0, CustomItems.defaultCustomItem(Material.NAME_TAG, "Keys-Shop", arrayListOf(), 1, Pair("rlgAction", "key")))
    BlackMarketInventories.blackMarketOverview = blackMarketInv

    val blackMarketKeysInv: Inventory = Bukkit.createInventory(null, 9, Component.text("Schwarzmarkt"))
    blackMarketKeysInv.setItem(0, CustomItems.defaultCustomItem(Material.NAME_TAG, "Kaufe 1 CommonKey für 5000 Credits", arrayListOf(), 1,
    Pair("rlgAction", "key common")))
    blackMarketKeysInv.setItem(1, CustomItems.defaultCustomItem(Material.NAME_TAG, "Kaufe 1 EpicKey für 20000 Credits", arrayListOf(), 2,
    Pair("rlgAction", "key epic")))
    blackMarketKeysInv.setItem(2, CustomItems.defaultCustomItem(Material.NAME_TAG, "Kaufe 1 SupremeKey für 50000 Credits", arrayListOf(), 3,
    Pair("rlgAction", "key supreme")))
    BlackMarketInventories.blackMarketKeyOverview = blackMarketKeysInv
}

fun transferBalance(player: Player, target: Player, amount: Long): Boolean {
    val playerRlgPlayer = player.rlgPlayer()
    if (playerRlgPlayer.balance < amount) {
        player.sendMessage("Du hast nicht genügend Credits, um diese Transaktion durchzuführen")
        return false
    }
    playerRlgPlayer.balance -= amount
    player.sendMessage(amount.withPoints() + " Credits wurden an " + target.name + " gesendet")
    player.changeCredits(playerRlgPlayer.balance)
    val targetRlgPlayer = target.rlgPlayer()
    targetRlgPlayer.balance += amount
    target.sendMessage("Du hast " + amount.withPoints() + " Credits von " + player.name + " erhalten")
    target.changeCredits(targetRlgPlayer.balance)
    player.updateScoreboard()
    target.updateScoreboard()
    return true
}

fun giveBalance(target: Player, amount: Long, reason: String) {
    val rlgPlayer = target.rlgPlayer()
    rlgPlayer.balance += amount
    target.changeCredits(rlgPlayer.balance)
    if (amount < 0) {
        target.sendMessage("§4Du hast " + abs(amount).withPoints() + " Credits für " + reason + " ausgegeben")
    } else {
        target.sendMessage("§2Du hast " + abs(amount).withPoints() + " Credits durch " + reason + " erhalten")
    }
    target.updateScoreboard()
}

fun pay(target: Player, amount: Long, reason: String): Boolean {
    val rlgPlayer = target.rlgPlayer()
    return if (rlgPlayer.balance >= amount) {
        rlgPlayer.balance -= amount
        target.changeCredits(rlgPlayer.balance)
        target.sendMessage("§2Du hast ${amount.withPoints()} Credits für $reason ausgegeben!")
        target.updateScoreboard()
        true
    } else {
        target.sendMessage("§4Du hast nicht genügend Credits!")
        false
    }
}

fun sellItem(player: Player) {
    val itemStack = player.inventory.itemInMainHand
    if (itemStack.hasItemMeta() && itemStack.itemMeta.persistentDataContainer.has(NamespacedKey(INSTANCE, "rlgCheated"), PersistentDataType.STRING)) {
        return
    }
    val rlgPlayer = player.rlgPlayer()
    val multiplier = rlgPlayer.rankData().shopMultiplier
    val cmd = if(!itemStack.itemMeta.hasCustomModelData()) 0 else itemStack.itemMeta.customModelData
    val isNotCrypto = (itemStack.type == Material.STICK && cmd !in 1..5) || itemStack.type != Material.STICK
    val value = if(isNotCrypto) (prices[itemStack.type]!![cmd]!! * itemStack.amount * multiplier).toLong() else prices[itemStack.type]!![cmd]!! * itemStack.amount
    giveBalance(player, value, "Shop")
    if(isNotCrypto){
        questCount(player, 12, value.toInt(), true)
        questCount(player, 8, value.toInt(), false)
        rlgPlayer.changeXP(floor(value / 10.0).toLong())
    }
    player.inventory.removeItem(itemStack)
    player.closeInventory()
    player.updateScoreboard()
}

fun tradingInventory(player: Player) {
    val rlgPlayer = player.rlgPlayer()
    try {
        if (player.inventory.itemInMainHand.type == Material.AIR) {
            val overview: Inventory = Bukkit.createInventory(null, 9, Component.text("Shop"))
            overview.contents = TradingInventories.overview!!.contents.copyOf()
            if(rlgPlayer.xpLevel < 10){
                overview.setItem(3, CustomItems.defaultCustomItem(Material.STICK, "§eCrypto-Shop", arrayListOf("", "§4Level 10 benötigt"), 1, Pair("rlgAction", "crypto")))
            }
            showTradingInventory(player, overview, "Shop")
            return
        }
    } catch (e: NullPointerException) {return}
    val isGiven = player.inventory.itemInMainHand
    if(isGiven.itemMeta.persistentDataContainer.has(NamespacedKey(INSTANCE, "rlgCheated"), PersistentDataType.STRING)) return
    val multiplier = rlgPlayer.rankData().shopMultiplier
    val cmd = if(!isGiven.itemMeta.hasCustomModelData()) 0 else isGiven.itemMeta.customModelData
    val price: Long = try {
        (prices[isGiven.type]!![cmd]!!.toLong() * isGiven.amount * (if((isGiven.type == Material.STICK && cmd !in 1..5) || isGiven.type != Material.STICK) multiplier else 1.0)).toLong()
    } catch (e: NullPointerException) {
        player.sendMessage("§4Das Item steht nicht zum Verkauf!")
        return
    }
    val result = Bukkit.createInventory(null, 27, Component.text("Verkaufe für ${price.withPoints()} Credits"))
    result.setItem(11, CustomItems.defaultCustomItem(Material.RED_WOOL, "§4Ablehnen", arrayListOf()))
    result.setItem(13, CustomItems.defaultCustomItem(isGiven.type, "Verkaufen für ${price.withPoints()} Credits", arrayListOf(), cmd).asQuantity(isGiven.amount))
    result.setItem(15, CustomItems.defaultCustomItem(Material.GREEN_WOOL, "§2Annehmen", arrayListOf()))
    tradingInventoryCopies.add(result)
    player.openInventory(result)
}

fun showTradingInventory(player: Player, inventory: Inventory?, iname: String) {
    val cloned = Bukkit.createInventory(null, inventory!!.size, Component.text(iname))
    cloned.contents = inventory.contents.copyOf()
    shopinventories.add(cloned)
    player.closeInventory()
    player.openInventory(cloned)
}

fun clickHandler(item: ItemStack, player: Player) {
    val data = item.itemMeta.persistentDataContainer.get(NamespacedKey(INSTANCE, "rlgAction"), PersistentDataType.STRING) ?: return
    val dataArray = data.split(" ")
    when(dataArray[0]) {
        "book" -> {
            if(dataArray.size == 1) {
                showTradingInventory(player, TradingInventories.bookOverview, "Magic-Shop")
                return
            }
            when(dataArray[1]){
                "beginner" -> buyBook(player, 2)
                "magic" -> buyBook(player, 1)
                "shop" -> buyBook(player, 3)
                else -> showTradingInventory(player, TradingInventories.bookOverview, "Magic-Shop")
            }
        }
        "wood" -> {
            if(dataArray.size == 1) {
                showTradingInventory(player, TradingInventories.woodOverview, "Holz-Shop")
                return
            }
            val material = Material.valueOf(dataArray[1].uppercase())
            buyItem(material, dataArray[2].toInt(), 16, player)
        }
        "build" -> {
            if(dataArray.size == 1) {
                showTradingInventory(player, TradingInventories.buildOverview, "Bau-Shop")
                return
            }
            when(dataArray[1]){
                "stone" -> buyItem(Material.STONE, dataArray[2].toInt(), 16, player)
                else -> buyItem(Material.valueOf(dataArray[1].uppercase()), dataArray[2].toInt(), 32, player)
            }
        }
        "key" -> {
            if(dataArray.size == 1) {
                showTradingInventory(player, BlackMarketInventories.blackMarketKeyOverview, "Keys-Shop")
                return
            }
            when(dataArray[1]){
                "common" -> buyKey(1, player, 5000)
                "epic" -> buyKey(2, player, 20000)
                "supreme" -> buyKey(3, player, 50000)
                else -> showTradingInventory(player, BlackMarketInventories.blackMarketKeyOverview, "Keys-Shop")
            }
        }
        "crypto" -> {
            if(player.rlgPlayer().xpLevel < 15) return
            if(dataArray.size == 1){
                val cryptoOverview: Inventory = Bukkit.createInventory(null, 45, Component.text("Crypto Shop"))
                for (i in 0 until amountmap.size){
                    cryptoOverview.setItem(i + (9*0), CustomItems.defaultCustomItem(Material.STICK, "Kaufe ${amountmap[i]} Bitcoin für ${(amountmap[i]!! * prices[Material.STICK]!![1]!!).withPoints()} Credits", arrayListOf(), 1,
                        Pair("rlgAction", "crypto bitcoin ${amountmap[i]}")))
                    cryptoOverview.setItem(i + (9*1), CustomItems.defaultCustomItem(Material.STICK, "Kaufe ${amountmap[i]} Ethereum für ${(amountmap[i]!! * prices[Material.STICK]!![2]!!).withPoints()} Credits", arrayListOf(), 2,
                        Pair("rlgAction", "crypto ethereum ${amountmap[i]}")))
                    cryptoOverview.setItem(i + (9*2), CustomItems.defaultCustomItem(Material.STICK, "Kaufe ${amountmap[i]} Litecoin für ${(amountmap[i]!! * prices[Material.STICK]!![3]!!).withPoints()} Credits", arrayListOf(), 3,
                        Pair("rlgAction", "crypto litecoin ${amountmap[i]}")))
                    cryptoOverview.setItem(i + (9*3), CustomItems.defaultCustomItem(Material.STICK, "Kaufe ${amountmap[i]} Nano für ${(amountmap[i]!! * prices[Material.STICK]!![5]!!).withPoints()} Credits", arrayListOf(), 5,
                        Pair("rlgAction", "crypto nano ${amountmap[i]}")))
                    cryptoOverview.setItem(i + (9*4), CustomItems.defaultCustomItem(Material.STICK, "Kaufe ${amountmap[i]} Dogecoin für ${(amountmap[i]!! * prices[Material.STICK]!![4]!!).withPoints()} Credits", arrayListOf(), 4,
                        Pair("rlgAction", "crypto dogecoin ${amountmap[i]}")))
                }
                showTradingInventory(player, cryptoOverview, "Crypto-Shop")
                return
            }
            buyCrypto(dataArray[1], dataArray[2].toInt(), player)
        }
    }
}

fun buyBook(player: Player, type: Int) {
    if (isSpace(player.inventory, 1)) {
        val price: Long = when (type) {
            1 -> 5000
            2 -> 100
            3 -> 25000
            else -> throw IllegalStateException("Unexpected value: $type")
        }
        if (pay(player, price, "Shop")) {
            player.inventory.addItem(when(type) {
                1 -> CustomItems.magicBook()
                2 -> CustomItems.beginnerBook()
                else -> ItemStack(Material.STRUCTURE_VOID)
            })
            player.updateScoreboard()
        }
    } else {
        player.sendMessage("§4Dein Inventar ist voll!")
        player.closeInventory()
    }
}

fun buyItem(m: Material, amount: Int, priceperone: Int, player: Player) {
    if (isSpace(player.inventory, 1)) {
        if (pay(
                player,
                amount.toLong() * priceperone,
                m.toString().lowercase(Locale.ROOT).toStartUppercaseMaterial()
            )
        ) {
            val itemStack = ItemStack(m)
            itemStack.amount = amount
            player.inventory.addItem(itemStack)
            player.updateScoreboard()
        }
    } else {
        player.sendMessage("§4Dein Inventar ist voll!")
        player.closeInventory()
    }
}

fun buyCrypto(type: String, amount: Int, player: Player) {
    if (isSpace(player.inventory, 1)) {
        try {
            if (pay(player, amount.toLong() * getCryptoPrice(type), type.toStartUppercaseMaterial())) {
                val itemStack = getCryptoItem(type)
                itemStack.amount = amount
                player.inventory.addItem(itemStack)
                player.updateScoreboard()
            }
        } catch (ex: NullPointerException) {}
    } else {
        player.sendMessage("§4Dein Inventar ist voll!")
        player.closeInventory()
    }
}

fun buyKey(type: Int, player: Player, price: Long) {
    if (isSpace(player.inventory, 1)) {
        if (pay(player, price, "Schwarzmarkt")) {
            player.inventory.addItem(genKey(type))
        }
    } else {
        player.sendMessage("§4Dein Inventar ist voll!")
        player.closeInventory()
    }
}

object BlackMarketInventories {
    var blackMarketOverview: Inventory? = null
    var blackMarketKeyOverview: Inventory? = null
}

object TradingInventories {
    var overview: Inventory? = null
    var bookOverview: Inventory? = null
    var woodOverview: Inventory? = null
    var buildOverview: Inventory? = null
}

object ShopInventories {
    var normalView: Inventory? = null
}

fun setupShop1(chest: Chest, sign: Sign, player: Player) {
    val shop = Shop(chest, sign, player)
    setupShop[player] = shop
    player.sendMessage("§6Bitte leg das Item, was du ankaufen/verkaufen willst, in den ersten Slot der Kiste!\n§2Schreib in den Chat den Verkaufspreis §l§4pro 1 Item in Credits§r§2 rein (Keine Kommastellen!)\nSchreib - wenn du nicht verkaufen möchtest")
    player.addMessageListener { asyncChatEvent, s ->
        setupShop2(player, s)
        asyncChatEvent.isCancelled = true
        true
    }
}

fun setupShop2(player: Player, msgPrice: String) {
    val price: Int = try {
        msgPrice.toInt()
    } catch (ignored: NumberFormatException) {
        if (msgPrice.contentEquals("-")) {
            -1
        } else {
            return
        }
    }
    val shop = setupShop[player]
    val shopInv = shop!!.chest.blockInventory
    var type = Material.AIR
    var cmd = 0
    for (itemStack: ItemStack? in shopInv) {
        if (itemStack != null) {
            if (type == Material.AIR) {
                type = itemStack.type
                cmd = if (itemStack.itemMeta.hasCustomModelData()) itemStack.itemMeta.customModelData else 0
            }
            if (cmd != 0) {
                if (!itemStack.itemMeta.hasCustomModelData() || cmd != itemStack.itemMeta.customModelData) {
                    player.sendMessage("Bitte kontaktiere den Shop-Besitzer: Es befinden sich verschiedene Items im Inventar!")
                    return
                }
            }
            if (type != itemStack.type) {
                player.sendMessage("Bitte kontaktiere den Shop-Besitzer: Es befinden sich verschiedene Items im Inventar!")
                return
            }
        }
    }
    val itemStack = shop.chest.blockInventory.getItem(0)
    try {
        assert(itemStack != null)
        itemStack!!.type
    } catch (ignored: NullPointerException) {
        player.sendMessage("§4Es befindet sich kein Item in der Kiste!\nBitte starte das Setup von vorne!")
        setupShop.remove(player)
        return
    }
    shop.setOffer(itemStack, price)
    if (cmd != 0) {
        shop.cmd = cmd
    }
    player.sendMessage("§6Verkaufspreis wurde gesetzt! ($msgPrice)")
    player.sendMessage("§2Schreib in den Chat den Ankaufspreis §l§4pro 1 Item in Credits§r§2 rein (Keine Kommastellen!)\nSchreib - wenn du nicht ankaufen möchtest")
    player.addMessageListener { asyncChatEvent, s ->
        setupShop3(player, s)
        asyncChatEvent.isCancelled = true
        true
    }
}

fun setupShop3(player: Player, msgPrice: String) {
    val price: Int = try {
        msgPrice.toInt()
    } catch (ignored: NumberFormatException) {
        if (msgPrice.contentEquals("-")) {
            -1
        } else {
            return
        }
    }
    val shop = setupShop[player]
    shop!!.buyprice = price
    shops.add(shop)
    setupShop.remove(player)
    val sign = shop.sign
    signs[sign.block] = shop
    val itemString: String = ciName(shop.type!!, shop.cmd) ?: shop.type.toString().lowercase(Locale.ROOT).toStartUppercaseMaterial()
    player.sendMessage("§6Kaufspreis wurde gesetzt! ($msgPrice)")
    player.sendMessage(
        "§2Dein Shop wurde erstellt!\nItem: " + itemString + "\nVerkauf: " + (if (shop.sellprice == -1) "-" else shop.sellprice) + " Credits\n"
                + "Ankauf: " + (if (shop.buyprice == -1) "-" else shop.buyprice) + " Credits"
    )
    Bukkit.getScheduler().runTask(INSTANCE, Runnable {
        sign.line(0, Component.text("[Shop]"))
        sign.line(1, Component.text(itemString))
        sign.line(2, Component.text("Buy: " + (if (shop.sellprice == -1) "-" else shop.sellprice.toString() + " Cr")))
        sign.line(3, Component.text("Sell: " + (if (shop.buyprice == -1) "-" else shop.buyprice.toString() + " Cr")))
        sign.update()
    })
    addShop(shop)
}

class Shop {
    constructor(chest: Chest, sign: Sign, player: Player) {
        this.chest = chest
        this.sign = sign
        uuid = player.uniqueId.toString()
        playername = player.name
    }

    constructor(
        chest: Chest,
        sign: Sign,
        owner_uuid: String,
        playername: String,
        sellprice: Int,
        buyprice: Int,
        type: Material?,
        cmd: Int
    ) {
        this.chest = chest
        this.sign = sign
        uuid = owner_uuid
        this.playername = playername
        this.sellprice = sellprice
        this.buyprice = buyprice
        this.type = type
        this.cmd = cmd
    }

    fun setOffer(item: ItemStack?, sellprice: Int) {
        type = item!!.type
        this.sellprice = sellprice
    }

    var chest: Chest
    var sign: Sign
    var uuid: String
    var playername: String
    var sellprice: Int = 0
    var buyprice: Int = 0
    var type: Material? = null
    var cmd = 0
}

fun showNormalShopView(shop: Shop?, player: Player) {
    val inventory = ShopInventories.normalView
    val cloned = Bukkit.createInventory(null, inventory!!.size, Component.text(shop!!.playername + "'s Shop"))
    val original = inventory.contents
    val clone = original.copyOf()
    cloned.contents = clone
    if (shop.sellprice != -1) {
        cloned.setItem(11, CustomItems.defaultCustomItem(Material.GREEN_WOOL, "§2Buy", arrayListOf(), 0, Pair("rlgAction", "buy")))
    }
    if (shop.buyprice != -1) {
        cloned.setItem(15, CustomItems.defaultCustomItem(Material.ORANGE_WOOL, "§6Sell", arrayListOf(), 0, Pair("rlgAction", "sell")))
    }
    playershopinventories[cloned] = shop
    player.closeInventory()
    player.openInventory(cloned)
}

fun showOwnerView(shop: Shop, player: Player) {
    val inventory = ShopInventories.normalView
    val cloned = Bukkit.createInventory(null, inventory!!.size, Component.text(shop.playername + "'s Shop"))
    val clone = inventory.contents.copyOf()
    cloned.contents = clone
    if (shop.sellprice != -1) {
        cloned.setItem(11, CustomItems.defaultCustomItem(Material.GREEN_WOOL, "§2Buy", arrayListOf(), 0, Pair("rlgAction", "buy")))
    }
    if (shop.uuid.contentEquals(player.uniqueId.toString())) {
        cloned.setItem(13, CustomItems.defaultCustomItem(Material.BARRIER, "§4Shop löschen", arrayListOf(), 0, Pair("rlgAction", "delete")))
    }
    if (shop.buyprice != -1) {
        cloned.setItem(15, CustomItems.defaultCustomItem(Material.ORANGE_WOOL, "§6Sell", arrayListOf(), 0, Pair("rlgAction", "sell")))
    }
    playershopinventories[cloned] = shop
    player.closeInventory()
    player.openInventory(cloned)
}

fun showBuySellView(shop: Shop, player: Player, buy: Boolean) {
    val inventory = ShopInventories.normalView
    val cloned = Bukkit.createInventory(null, inventory!!.size, Component.text(shop.playername + "'s Shop"))
    val clone = inventory.contents.copyOf()
    cloned.contents = clone
    for (i in 0 until amountmap.size) {
        val itemStack = ItemStack((shop.type)!!)
        if (shop.type!!.maxStackSize < (amountmap[i])!!) break
        itemStack.amount = (amountmap[i])!!
        val im = itemStack.itemMeta
        if (shop.cmd != 0) {
            im.setCustomModelData(shop.cmd)
        }
        if (buy) {
            im.displayName(Component.text(
                "Kaufe " + amountmap[i] + " " + shop.type.toString().lowercase(Locale.ROOT).toStartUppercaseMaterial() + " für " +
                        (amountmap[i]!! * shop.sellprice).withPoints() + " Credits"))
            im.persistentDataContainer.set(NamespacedKey(INSTANCE, "rlgAction"), PersistentDataType.STRING, "buy ${amountmap[i]}")
        } else {
            im.displayName(Component.text(
                "Verkaufe " + amountmap[i] + " " + shop.type.toString().lowercase(Locale.ROOT).toStartUppercaseMaterial() + " für " + (
                        amountmap[i]!! * shop.buyprice).withPoints() + " Credits"))
            im.persistentDataContainer.set(NamespacedKey(INSTANCE, "rlgAction"), PersistentDataType.STRING, "sell ${amountmap[i]}")
        }
        itemStack.itemMeta = im
        cloned.setItem(i + 11, itemStack)
    }
    playershopinventories[cloned] = shop
    player.closeInventory()
    player.openInventory(cloned)
}

fun shopInvClickHandler(player: Player, clicked: ItemStack, shop: Shop) {
    try {
        if (clicked.hasItemMeta() && clicked.itemMeta.hasLore() && clicked.itemMeta.lore()!!.contains(Component.text("Aus Creative-Inventar"))) {
            return
        }
        if (clicked.itemMeta.hasDisplayName()) {
            when (val action = clicked.itemMeta.persistentDataContainer[NamespacedKey(INSTANCE, "rlgAction"), PersistentDataType.STRING]!!) {
                "buy" -> showBuySellView(shop, player, true)
                "sell" -> showBuySellView(shop, player, false)
                "delete" -> removeShop(shop, player)
                else -> if (clicked.type != Material.GRAY_STAINED_GLASS_PANE) {
                    val firstWord = action.split(" ").toTypedArray()[0]
                    val amount = action.split(" ").toTypedArray()[1].toInt()
                    if (firstWord.contentEquals("buy")) {
                        buyFromPlayerShop(player, shop, amount)
                    } else if (firstWord.contentEquals("sell")) {
                        sellToPlayerShop(player, shop, amount)
                    }
                }
            }
        }
    } catch (ignored: NullPointerException) { }
}

fun buyFromPlayerShop(player: Player, shop: Shop, amount: Int) {
    val price = amount * shop.sellprice
    val shopInv = shop.chest.blockInventory
    var itemsinchest = 0
    for (itemStack: ItemStack? in shopInv) {
        if (itemStack != null) {
            if (itemStack.type != shop.type) {
                player.sendMessage("§4Bitte kontaktiere den Shop-Besitzer, es befinden sich falsche Items in der Kiste!")
                return
            }
            if (shop.cmd != 0) {
                if (!itemStack.itemMeta.hasCustomModelData() || itemStack.itemMeta.customModelData != shop.cmd) {
                    player.sendMessage("§4Bitte kontaktiere den Shop-Besitzer, es befinden sich falsche Items in der Kiste!")
                    return
                }
            }
            itemsinchest += itemStack.amount
        }
    }
    if (amount <= itemsinchest) {
        if (pay(player, price.toLong(), shop.playername + "'s Shop")) {
            var itemStack = ItemStack((shop.type)!!)
            if (shop.cmd != 0) {
                itemStack = getByTypeCmd(shop.type, shop.cmd)!!
            }
            itemStack.amount = amount
            removeItems(shopInv, shop.type!!, amount, shop.cmd)
            player.inventory.addItem(itemStack)
            try {
                val owner = Bukkit.getPlayer(UUID.fromString(shop.uuid))!!
                giveBalance(owner, price.toLong(), "deinen Shop")
            } catch (ignored: NullPointerException) {
                addCreditsToPlayer(shop.uuid, price.toLong())
            }
        }
    } else {
        player.sendMessage("§4Der Shop ist leer!")
    }
}

fun sellToPlayerShop(player: Player, shop: Shop, amount: Int) {
    val shopInv = shop.chest.blockInventory
    val playerInv: Inventory = player.inventory
    val playerContents = playerInv.contents
    var spaceinchest = 27 * shop.type!!.maxStackSize
    for (itemStack: ItemStack? in shopInv) {
        if (itemStack != null) {
            if (itemStack.type == shop.type) {
                if (shop.cmd != 0) {
                    if (!itemStack.itemMeta.hasCustomModelData() || itemStack.itemMeta.customModelData != shop.cmd) {
                        player.sendMessage("§4Bitte kontaktiere den Shop-Besitzer, es befinden sich falsche Items in der Kiste!")
                        break
                    }
                }
                spaceinchest -= itemStack.amount
            } else {
                player.sendMessage("§4Bitte kontaktiere den Shop-Besitzer, es befinden sich falsche Items in der Kiste!")
                break
            }
        }
    }
    var amountitemsinplayer = 0
    for (itemStack: ItemStack? in playerContents) {
        if (itemStack != null) {
            if (itemStack.type == shop.type) {
                if (shop.cmd != 0) {
                    if (itemStack.itemMeta.hasCustomModelData() && itemStack.itemMeta.customModelData == shop.cmd) {
                        amountitemsinplayer += itemStack.amount
                    }
                } else {
                    amountitemsinplayer += itemStack.amount
                }
            }
        }
    }
    if (amount <= spaceinchest) {
        if (amount <= amountitemsinplayer) {
            val revenue = amount * shop.buyprice
            val shopBalance: Long = getCredits(shop.uuid)
            if (shopBalance > revenue) {
                removeItems(playerInv, shop.type!!, amount, shop.cmd)
                var itemStack = ItemStack((shop.type)!!, amount)
                if (shop.cmd != 0) {
                    itemStack = getByTypeCmd(shop.type, shop.cmd)!!
                    itemStack.amount = amount
                }
                shopInv.addItem(itemStack)
                giveBalance(player, revenue.toLong(), shop.playername + "'s Shop")
                try {
                    val owner = Bukkit.getPlayer(UUID.fromString(shop.uuid))!!
                    giveBalance(owner, -revenue.toLong(), "deinen Shop")
                } catch (ignored: NullPointerException) {
                    addCreditsToPlayer(shop.uuid, -revenue.toLong())
                }
            }
        } else {
            player.sendMessage("§4Du hast nicht dieses Item")
        }
    } else {
        player.sendMessage("§4Der Shop ist voll!")
    }
}

fun signClickHandler(player: Player, sign: Sign) {
    val shop = signs[sign.block]
    if (player.uniqueId.toString().contentEquals(shop!!.uuid)) {
        showOwnerView(shop, player)
    } else {
        showNormalShopView(shop, player)
    }
}

fun removeShop(shop: Shop, player: Player) {
    if (player.uniqueId.toString().contentEquals(shop.uuid)) {
        val sign = shop.sign
        Bukkit.getScheduler().runTask(INSTANCE, Runnable {
            sign.line(0, Component.text(""))
            sign.line(1, Component.text(""))
            sign.line(2, Component.text(""))
            sign.line(3, Component.text(""))
            sign.update()
        })
        player.closeInventory()
        removeShop(shop)
        shops.remove(shop)
        signs.remove(sign.block)
        player.sendMessage("§2Dein Shop wurde erfolgreich gelöscht!")
    }
}

fun Player.changeCredits(newAmount: Long){
    val player = this
    transaction { 
        PlayersTable.update(where = {PlayersTable.uuid eq player.uniqueId.toString()}){
            it[balance] = newAmount
        }
    }
}

fun addCreditsToPlayer(uuid: String, amount: Long){
    transaction { 
        val bal = PlayersTable.select(where = {PlayersTable.uuid eq uuid}).first()[PlayersTable.balance]
        PlayersTable.update(where = {PlayersTable.uuid eq uuid}){
            it[balance] = bal+amount
        }
    }
}

fun getCredits(uuid: String): Long{
    var bal: Long = 0
    transaction {
        bal = PlayersTable.select(where = {PlayersTable.uuid eq uuid}).first()[PlayersTable.balance].toLong()
    }
    return bal
}

fun removeShop(shop: Shop){
    transaction {
        ShopTable.deleteWhere{
            ShopTable.ownerUUID eq shop.uuid and(ShopTable.chestPos eq shop.chest.world.name + "/" + shop.chest.x + "/" + shop.chest.y + "/" + shop.chest.z)
        }
    }
}

private fun addShop(shop: Shop){
    transaction {
        ShopTable.insert {
            it[signPos] = shop.sign.world.name + "/" + shop.sign.x + "/" + shop.sign.y + "/" + shop.sign.z
            it[chestPos] = shop.chest.world.name + "/" + shop.chest.x + "/" + shop.chest.y + "/" + shop.chest.z
            it[ownerUUID] = shop.uuid
            it[playername] = shop.playername
            it[sellPrice] = shop.sellprice
            it[buyPrice] = shop.buyprice
            it[material] = shop.type.toString()
            it[cmd] = shop.cmd
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun updateCreditScore(){
    allJobs.add(GlobalScope.launch {
        while (true){
            lastUpdate = Date(System.currentTimeMillis())
            creditsScoreBoard = getCreditsScoreboard()
            Bukkit.getScheduler().runTask(INSTANCE, Runnable {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as @e[type=boat] at @s store result score @s kb_isEmpty run data get entity @s Passengers")
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as @a at @s store result score @s kb_isEmpty run data get entity @s RootVehicle")
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as @a[scores={kb_isEmpty=1..}] at @s run scoreboard players set @e[type=boat,sort=nearest,limit=1] kb_isEmpty 1")
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=boat,scores={kb_isEmpty=0}]")
            })
            val response = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,dogecoin,nano,ethereum,litecoin&vs_currencies=usd").readText()
            val obj = jacksonObjectMapper().readValue<CoingeckoPriceInfo>(response)
            prices[Material.STICK]!![1] = (obj["bitcoin"]!!.usd * 100).roundToInt().toLong()
            prices[Material.STICK]!![2] = (obj["ethereum"]!!.usd * 100).roundToInt().toLong()
            prices[Material.STICK]!![3] = (obj["litecoin"]!!.usd * 100).roundToInt().toLong()
            prices[Material.STICK]!![5] = (obj["nano"]!!.usd * 100).roundToInt().toLong()
            prices[Material.STICK]!![4] = (obj["dogecoin"]!!.usd * 100).roundToInt().toLong()
            delay(300000)
        }
    })
}

fun getCreditsScoreboard(): String {
    val messageBuilder = StringBuilder()
    val time = SimpleDateFormat("HH:mm:ss").format(lastUpdate)
    messageBuilder.append("§6§l§nAktuelles Ranking:§r\n§7Letztes Update: $time\n")
    transaction {
        var count = 1
        PlayersTable.selectAll().orderBy(PlayersTable.balance, SortOrder.DESC).limit(5).forEach {
            messageBuilder.append("§7$count. §2${MojangAPI.getName(UUID.fromString(it[PlayersTable.uuid]))}: §6${it[PlayersTable.balance].withPoints()} Credits§r\n")
            count++
        }
    }
    messageBuilder.append("§6Diese Statistik wird alle 5 Minuten aktualisiert!")
    return messageBuilder.toString()
}

fun shopVillager(location: Location) {
    val shop: Villager = location.world.spawnEntity(location, EntityType.VILLAGER) as Villager
    shop.setAI(false)
    shop.isInvulnerable = true
    shop.villagerLevel = 3
    shop.profession = Villager.Profession.LIBRARIAN
    shop.customName = "§6§l§nShop"
    shop.isCustomNameVisible = true
    shop.isSilent = true
    shop.removeWhenFarAway = false
    shop.persistentDataContainer.set(NamespacedKey(INSTANCE, "rlgEntityData"), PersistentDataType.STRING, "shop")
}

fun blackMarketVillager(location: Location) {
    val blackMarket: WanderingTrader = location.world.spawnEntity(location, EntityType.WANDERING_TRADER) as WanderingTrader
    blackMarket.setAI(false)
    blackMarket.isInvulnerable = true
    blackMarket.customName = "§0§lSchwarzmarkt"
    blackMarket.isCustomNameVisible = true
    blackMarket.isSilent = true
    blackMarket.removeWhenFarAway = false
    blackMarket.persistentDataContainer.set(NamespacedKey(INSTANCE, "rlgEntityData"), PersistentDataType.STRING, "blackmarket")
}