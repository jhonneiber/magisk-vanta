package com.topjohnwu.magisk.ui.webui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.magisk.core.R as CoreR

private const val ASSET_DOMAIN = "appassets.androidplatform.net"
private const val ASSET_PATH_PREFIX = "/webroot/"
private const val ASSET_INDEX_URL = "https://$ASSET_DOMAIN${ASSET_PATH_PREFIX}index.html"

/**
 * Serves a module's webroot/ directory straight from the root-backed
 * filesystem (no copy step, no local HTTP server). Only files inside the
 * module's own webroot are ever reachable — path traversal outside of it
 * (e.g. "../../..") is rejected, and nothing else on the device is exposed
 * this way.
 */
private class ModuleAssetPathHandler(
    private val webRoot: ExtendedFile,
) : WebViewAssetLoader.PathHandler {

    private val rootCanonicalPath: String by lazy {
        runCatching { webRoot.canonicalPath }.getOrDefault(webRoot.path)
    }

    override fun handle(path: String): WebResourceResponse? {
        val relative = path.ifEmpty { "index.html" }
        val target = webRoot.getChildFile(relative)

        val targetCanonicalPath = runCatching { target.canonicalPath }.getOrDefault(target.path)
        if (!targetCanonicalPath.startsWith(rootCanonicalPath)) {
            return null
        }
        if (!target.exists() || !target.isFile) {
            return null
        }

        val mime = MimeTypeMap.getFileExtensionFromUrl(target.name)
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: "application/octet-stream"

        return runCatching {
            WebResourceResponse(mime, null, target.newInputStream())
        }.getOrNull()
    }
}

/** Exposed to module JS as `window.magisk.moduleId()`. */
private class ModuleJsBridge(private val viewModel: WebUiViewModel) {
    @JavascriptInterface
    fun moduleId(): String = viewModel.moduleId
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebUiScreen(
    viewModel: WebUiViewModel,
    moduleName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootDenied by viewModel.rootDenied.collectAsStateWithLifecycle()
    var loading by remember { mutableStateOf(true) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(moduleName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (rootDenied) {
                Text(
                    text = stringResource(CoreR.string.webui_root_denied),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val assetLoader = WebViewAssetLoader.Builder()
                            .setDomain(ASSET_DOMAIN)
                            .addPathHandler(
                                ASSET_PATH_PREFIX,
                                ModuleAssetPathHandler(viewModel.webRoot)
                            )
                            .build()

                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // Everything the page needs comes through the
                            // asset loader; no reason to also grant raw
                            // file:// / content:// access.
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false

                            addJavascriptInterface(ModuleJsBridge(viewModel), "magisk")

                            webViewClient = object : WebViewClientCompat() {
                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest
                                ): WebResourceResponse? =
                                    assetLoader.shouldInterceptRequest(request.url)

                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest
                                ): Boolean {
                                    // Never let the module's page navigate this
                                    // WebView to anything outside our local
                                    // asset domain.
                                    return request.url.host != ASSET_DOMAIN
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    loading = false
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceErrorCompat
                                ) {
                                    loading = false
                                }
                            }

                            loadUrl(ASSET_INDEX_URL)
                        }
                    }
                )
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
