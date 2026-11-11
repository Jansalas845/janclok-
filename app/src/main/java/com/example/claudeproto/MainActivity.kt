package com.example.claudeproto

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var engine: LlmEngine
    private lateinit var server: LocalServer

    inner class Bridge {
        @JavascriptInterface
        fun pickModel() = runOnUiThread {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            }
            @Suppress("DEPRECATION") startActivityForResult(i, 1)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = LlmEngine(this)
        server = LocalServer(this, engine).also { it.start() }
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("http://127.0.0.1:8765/")
        if (engine.modelFile.exists()) thread { runCatching { engine.load() } }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (req != 1 || res != RESULT_OK) return
        thread {
            val msg = try {
                contentResolver.openInputStream(uri)!!.use { i ->
                    engine.modelFile.outputStream().use { o -> i.copyTo(o) }
                }
                engine.load(); "ok"
            } catch (e: Throwable) { "Error: " + (e.message ?: e.toString()) }
            runOnUiThread { web.evaluateJavascript("onModelResult(${org.json.JSONObject.quote(msg)})", null) }
        }
    }

    override fun onDestroy() { server.stop(); super.onDestroy() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
