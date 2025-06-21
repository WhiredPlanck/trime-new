/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.core

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebMessagePortCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class WebInputViewClient(
    context: Context,
    private val dataHandler: (String) -> Unit,
    private val portConsumer: (WebMessagePortCompat?) -> Unit,
) : WebViewClientCompat() {
    private val assetsLoader =
        WebViewAssetLoader
            .Builder()
            .setDomain("osfans.trime.com")
            .addPathHandler("/assets/", AssetsPathHandler(context))
            .build()

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest,
    ): WebResourceResponse? = assetsLoader.shouldInterceptRequest(request.url)

    override fun onPageFinished(
        view: WebView,
        url: String?,
    ) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL)) {
            val ports = WebViewCompat.createWebMessageChannel(view)
            val port = ports[1]
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK)) {
                port?.setWebMessageCallback(
                    object : WebMessagePortCompat.WebMessageCallbackCompat() {
                        override fun onMessage(
                            port: WebMessagePortCompat,
                            message: WebMessageCompat?,
                        ) {
                            val data = message?.data ?: return
                            dataHandler(data)
                        }
                    },
                )
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
                WebViewCompat.postWebMessage(view, WebMessageCompat("__init_port__", arrayOf(ports[0])), Uri.parse("*"))
            }
            portConsumer(port)
        }
    }
}
