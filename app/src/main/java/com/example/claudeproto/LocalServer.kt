package com.example.claudeproto

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import org.json.JSONObject

/** Backend propio: API HTTP local (127.0.0.1) dentro de la app. Sirve la UI y /api/*. */
class LocalServer(private val ctx: Context, private val engine: LlmEngine) :
    NanoHTTPD("127.0.0.1", 8765) {

    private fun json(o: JSONObject, st: Response.Status = Response.Status.OK): Response =
        newFixedLengthResponse(st, "application/json; charset=utf-8", o.toString())

    override fun serve(session: IHTTPSession): Response = try {
        val uri = session.uri
        when {
            session.method == Method.GET && uri == "/api/status" ->
                json(JSONObject().put("modelLoaded", engine.isReady()))
            session.method == Method.POST && uri == "/api/chat" -> {
                val files = HashMap<String, String>()
                session.parseBody(files)
                val body = JSONObject(files["postData"] ?: "{}")
                json(JSONObject().put("reply", engine.chat(body.getJSONArray("messages"))))
            }
            session.method == Method.GET -> {
                val path = if (uri == "/") "index.html" else uri.trimStart('/')
                val mime = when (path.substringAfterLast('.')) {
                    "html" -> "text/html; charset=utf-8"; "js" -> "text/javascript"; "css" -> "text/css"
                    else -> "application/octet-stream"
                }
                newChunkedResponse(Response.Status.OK, mime, ctx.assets.open("web/$path"))
            }
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404")
        }
    } catch (e: Exception) {
        json(JSONObject().put("error", e.message ?: "error"), Response.Status.INTERNAL_ERROR)
    }
}
