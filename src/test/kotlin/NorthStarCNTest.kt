import top.kkoishi.json.Kson
import top.kkoishi.json.io.ArrayTypeParser
import top.kkoishi.json.io.JsonReader
import top.kkoishi.json.parse.Factorys
import top.kkoishi.json.reflect.Type
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Path
import kotlin.io.path.readText

@Suppress("RemoveExplicitTypeArguments")
class NorthStarCNTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser: ArrayTypeParser<Array<Server>> =
                Factorys.getArrayTypeFactory().create(Type<Array<Server>>(Array<Server>::class.java))
            val ins: InputStream = FileInputStream("./NorthStarCN.json")
            val reader = JsonReader(InputStreamReader(ins), true)
            val arr = reader.read()
            println(arr.isJsonArray())
            reader.close()
            val kson = Kson()
            println(kson.fromJsonString<Array<Server>>(Array<Server>::class.java, Path.of("./NorthStarCN.json").readText()))
            var totalCount = 0
            for (server in parser.getTypedArray(arr, arrayOf<Server>())) {
                println(server.report())
                totalCount += server.playerCount
            }
            println("NSCN在线人数: $totalCount")
        }

        @JvmStatic
        private val MAPS = mapOf<String, String>(
            "mp_angel_city" to "天使城",
            "mp_black_water_canal" to "黑水运河",
            "mp_grave" to "新兴城镇",
            "mp_colony02" to "殖民地",
            "mp_complex3" to "综合设施",
            "mp_crashsite3" to "坠机现场",
            "mp_drydock" to "干坞",
            "mp_eden" to "伊甸",
            "mp_thaw" to "系外行星",
            "mp_forwardbase_kodai" to "虎大前进基地",
            "mp_glitch" to "异常",
            "mp_homestead" to "家园",
            "mp_relic02" to "遗迹",
            "mp_rise" to "崛起",
            "mp_wargames" to "战争游戏",
            "mp_lobby" to "大厅",
            "mp_lf_deck" to "甲板",
            "mp_lf_meadow" to "草原",
            "mp_lf_stacks" to "堆积地",
            "mp_lf_township" to "小镇",
            "mp_lf_traffic" to "交通",
            "mp_lf_uma" to "UMA",
            "mp_coliseum" to "竞技场",
            "mp_coliseum_column" to "梁柱",
        )
    }



    private inner class Server(
        private val description: String,
        private val gameState: Int,
        private val hasPassword: Boolean,
        private val lastHeartbeat: Long,
        private val map: String,
        private val maxPlayers: Int,
        private val modInfo: ModInfo,
        private val name: String,
        val playerCount: Int,
        private val playlist: String,
    ) {
        fun report(): String {
            return "$name ${MAPS[map]} 人数: $playerCount/$maxPlayers 描述: $description"
        }
    }

    private class ModInfo(Mods: Array<Mod>)

    private class Mod(
        private val Name: String,
        private val Pdiff: String,
        private val RequiredOnClient: Boolean,
        private val Version: String,
    )
}