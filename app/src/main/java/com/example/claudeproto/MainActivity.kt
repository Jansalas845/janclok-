package com.example.claudeproto

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var engine: LlmEngine
    private var server: LocalServer? = null
    private val crashFile get() = File(filesDir, "crash.txt")
    private val flagFile get() = File(filesDir, "loading.flag")

    inner class Bridge {
        @JavascriptInterface
        fun pickModel() = runOnUiThread {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            }
            startActivityForResult(i, 1)
        }
    }

    // Carga el modelo dejando una marca: si la app muere durante la carga, la marca queda.
    private fun safeLoad(): String {
        flagFile.writeText("1")
        return try {
            engine.load(); flagFile.delete(); "ok"
        } catch (e: Throwable) {
            flagFile.delete(); "Error: " + (e.message ?: e.toString())
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { crashFile.writeText(e.stackTraceToString()) } catch (_: Throwable) {}
            old?.uncaughtException(t, e)
        }
        engine = LlmEngine(this)
        var startErr = ""
        try { server = LocalServer(this, engine).also { it.start() } }
        catch (e: Throwable) { startErr = "No arranco el servidor: " + e.toString() }

        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "Android")

        var report = startErr
        if (crashFile.exists()) {
            report += "\nERROR ANTERIOR:\n" + crashFile.readText().take(3000)
            crashFile.delete()
        }
        if (flagFile.exists()) {
            report += "\nLa ultima carga del modelo cerro la app (fallo nativo, memoria o version). No se cargara solo; elige el archivo de nuevo."
        }
        if (report.isNotBlank()) {
            val safe = report.replace("&", "&amp;").replace("<", "&lt;")
            web.loadDataWithBaseURL(null,
                "<body style='font-family:sans-serif;padding:16px'><h3>Diagnostico</h3>" +
                "<pre style='white-space:pre-wrap;font-size:12px'>$safe</pre>" +
                "<p><a href='http://127.0.0.1:8765/'>Continuar a la app</a></p></body>",
                "text/html", "utf-8", null)
        } else {
            web.loadUrl("http://127.0.0.1:8765/")
        }
        if (engine.modelFile.exists() && !flagFile.exists()) thread { safeLoad() }
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
                safeLoad()
            } catch (e: Throwable) { "Error: " + (e.message ?: e.toString()) }
            runOnUiThread { web.evaluateJavascript("onModelResult(${org.json.JSONObject.quote(msg)})", null) }
        }
    }

    override fun onDestroy() { server?.stop(); super.onDestroy() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
