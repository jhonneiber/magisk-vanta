package com.topjohnwu.magisk.ui.webui

import com.topjohnwu.magisk.arch.BaseViewModel
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.utils.RootUtils
import com.topjohnwu.superuser.nio.ExtendedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebUiViewModel : BaseViewModel() {

    var moduleId: String = ""
    var moduleName: String = ""

    private val _rootDenied = MutableStateFlow(false)
    val rootDenied: StateFlow<Boolean> = _rootDenied.asStateFlow()

    /**
     * The module's webroot directory, backed by the root filesystem via libsu.
     * Reads go through the daemon (RootUtils.fs), so the app process itself
     * never needs a raw shell to serve module assets to the WebView.
     */
    val webRoot: ExtendedFile
        get() = RootUtils.fs.getFile(Const.MODULE_PATH, moduleId, "webroot")

    fun checkRootAccess() {
        // The module ships a webroot/index.html, but if the device has no
        // working root at all there's nothing the module's WebUI could ever
        // do, so surface that up front instead of loading a dead page.
        _rootDenied.value = !Info.isRooted
    }
}
