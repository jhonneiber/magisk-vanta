package com.topjohnwu.magisk.ui.webui

import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.arch.BaseViewModel
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.ktx.toast
import com.topjohnwu.magisk.core.utils.RootUtils
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.ExtendedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backs the WebUI screen. Exposes both the module's file access (for
 * serving webroot/) and the shell/package-info operations needed by the
 * `window.ksu` bridge — matching the contract of the real `kernelsu` npm
 * package (github.com/tiann/KernelSU, package "kernelsu"), since that's
 * what module WebUI pages actually call, not anything Magisk-specific.
 */
class WebUiViewModel : BaseViewModel() {

    var moduleId: String = ""
    var moduleName: String = ""

    private val _rootDenied = MutableStateFlow(false)
    val rootDenied: StateFlow<Boolean> = _rootDenied.asStateFlow()

    private val moduleDir get() = RootUtils.fs.getFile(Const.MODULE_PATH, moduleId)

    /** The module's webroot directory, backed by the root filesystem via libsu. */
    val webRoot: ExtendedFile get() = moduleDir.getChildFile("webroot")

    fun checkRootAccess() {
        _rootDenied.value = !Info.isRooted
    }

    // ---- window.ksu.exec / window.magisk.exec --------------------------

    data class ExecResult(val errno: Int, val stdout: String, val stderr: String)

    fun execRaw(command: String): ExecResult {
        if (!Info.isRooted) return ExecResult(-1, "", "root unavailable")
        val result = Shell.cmd("cd '${Const.MODULE_PATH}/$moduleId' && $command").exec()
        val errno = if (result.isSuccess) 0 else result.code
        return ExecResult(errno, result.out.joinToString("\n"), result.err.joinToString("\n"))
    }

    // ---- window.ksu.moduleInfo -------------------------------------------

    fun moduleInfoJson(): String {
        val props = mutableMapOf<String, String>()
        runCatching {
            Shell.cmd("dos2unix < '${Const.MODULE_PATH}/$moduleId/module.prop'").exec().out
                .forEach { line ->
                    val parts = line.split("=", limit = 2).map { it.trim() }
                    if (parts.size == 2 && parts[0].isNotEmpty() && !parts[0].startsWith("#")) {
                        props[parts[0]] = parts[1]
                    }
                }
        }
        return JSONObject().apply {
            put("id", moduleId)
            put("name", props["name"] ?: moduleId)
            put("version", props["version"] ?: "")
            put("versionCode", props["versionCode"]?.toIntOrNull() ?: 0)
            put("author", props["author"] ?: "")
            put("description", props["description"] ?: "")
        }.toString()
    }

    // ---- window.ksu.toast --------------------------------------------------

    fun showToast(message: String) {
        AppContext.toast(message, android.widget.Toast.LENGTH_SHORT)
    }

    // ---- window.ksu.listPackages / getPackagesInfo --------------------------

    fun listPackagesJson(type: String): String {
        val pm = AppContext.packageManager
        val apps = runCatching { pm.getInstalledApplications(0) }.getOrDefault(emptyList())
        val filtered = apps.filter {
            val isSystem = (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            when (type) {
                "system" -> isSystem
                "user" -> !isSystem
                else -> true
            }
        }
        val arr = JSONArray()
        filtered.forEach { arr.put(it.packageName) }
        return arr.toString()
    }

    fun getPackagesInfoJson(packagesJson: String): String {
        val pm = AppContext.packageManager
        val names = runCatching {
            val arr = JSONArray(packagesJson)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())

        val result = JSONArray()
        for (pkg in names) {
            runCatching {
                val info = pm.getPackageInfo(pkg, 0)
                val appInfo = info.applicationInfo
                result.put(JSONObject().apply {
                    put("packageName", pkg)
                    put("label", appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg)
                    put("versionName", info.versionName ?: "")
                    @Suppress("DEPRECATION")
                    put("versionCode", info.versionCode)
                    put("uid", appInfo?.uid ?: -1)
                    put(
                        "isSystem",
                        (appInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
                    )
                })
            }
        }
        return result.toString()
    }
}
