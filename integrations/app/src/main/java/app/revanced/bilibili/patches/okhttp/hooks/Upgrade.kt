package app.revanced.bilibili.patches.okhttp.hooks

import android.content.pm.PackageManager
import app.revanced.bilibili.integrations.BuildConfig
import app.revanced.bilibili.patches.okhttp.ApiHook
import app.revanced.bilibili.settings.Settings
import app.revanced.bilibili.utils.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

/**
 * versionSum format: "$version $versionCode $patchVersion $patchVersionCode $sn $size $md5 publishTime"
 *
 * eg. "7.66.0 7660300 1.17 10170 14056308 135819602 2c2e2008ecb46c927981078811402151 1709975253"
 */
class BUpgradeInfo(
    versionSum: String,
    val url: String,
    val changelog: String,
) {
    private val versionInfo = versionSum.split(' ')
    val version get() = versionInfo[0]
    val versionCode get() = versionInfo[1].toLong()
    val patchVersion get() = versionInfo[2]
    val patchVersionCode get() = versionInfo[3].toInt()
    val sn get() = versionInfo[4].toLong()
    val size get() = versionInfo[5].toLong()
    val md5 get() = versionInfo[6]
    val publishTime get() = versionInfo[7].toLong()
}

object Upgrade : ApiHook() {
    private val changelogRegex = Regex("""版本信息：(.*?)\n(.*)""", RegexOption.DOT_MATCHES_ALL)
    var fromSelf = false

    fun customUpdate(fromSelf: Boolean = false): Boolean {
        return (fromSelf || Settings.CustomUpdate()) && isOsArchArm64 && isPrebuilt
    }

    override fun shouldHook(url: String, status: Int): Boolean {
        return (Settings.BlockUpdate() || customUpdate(fromSelf = fromSelf))
                && url.contains("/x/v2/version/fawkes/upgrade")
    }

    override fun hook(url: String, status: Int, request: String, response: String): String {
        return if (customUpdate(fromSelf = fromSelf))
            (runCatchingOrNull { checkUpgrade().toString() }
                ?: """{"code":-1,"message":"检查更新失败，请稍后再试/(ㄒoㄒ)/~~""")
                .also { fromSelf = false }
        else if (Settings.BlockUpdate())
            """{"code":-1,"message":"哼，休想要我更新！<(￣︶￣)>"}"""
        else response
    }

    private fun checkUpgrade(): JSONObject {
        return mapOf("code" to -1, "message" to "未发现新版漫游X集成包！").toJSONObject()
    }
}
