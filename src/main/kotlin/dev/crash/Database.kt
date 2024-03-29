package dev.crash

import dev.crash.permission.ranks
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

internal fun initDatabase(){
    TransactionManager.defaultDatabase = Database.connect("jdbc:mysql://${CONFIG.dbIp}/${CONFIG.dbName}", user = CONFIG.dbUser, password = CONFIG.dbPw)
    transaction {
        if(!ChunkTable.exists()) {
            SchemaUtils.create(ChunkTable)
        }
        if(!PlayerTable.exists()) {
            SchemaUtils.create(PlayerTable)
        }
        if(!GuildTable.exists()) {
            SchemaUtils.create(GuildTable)
        }
        if(!KeyIndexTable.exists()) {
            SchemaUtils.create(KeyIndexTable)
        }
    }
}

object ChunkTable : Table("chunks"){
    val x = integer("x")
    val z = integer("z")
    val world = varchar("world", 20)
    val uuid = varchar("uuid", 36)
    val name = varchar("name", 100)
    val shared = text("shared")
}

object KeyIndexTable : Table("keyindex"){
    val token = varchar("token", 20)
    val type = integer("type")
    override val primaryKey = PrimaryKey(token)
}

object GuildTable : IntIdTable("guilds"){
    val suffix = varchar("suffix", 4)
    val name = varchar("name", 20)
    val owner_uuid = varchar("owner_uuid", 36)
    val owner_name = text("owner_name")
    val member_names = text("member_names")
    val member_uuids = text("member_uuids")
}

object PlayerTable : Table("players"){
    val uuid = varchar("uuid", 36)
    val rank = integer("rank").default(0)
    val remainingClaims = integer("remainingClaims").default(ranks[0]!!.claims)
    val remainingHomes = integer("remainingHomes").default(ranks[0]!!.homes)
    val addedClaims = integer("addedClaims").default(0)
    val addedHomes = integer("addedHomes").default(0)
    val balance = long("balance").default(0)
    val xpLevel = integer("xpLevel").default(0)
    val xp = long("xp").default(0)
    val vxpLevel = integer("vxpLevel").default(0)
    val guildId = integer("guildId").default(0)
    override val primaryKey = PrimaryKey(uuid)
}