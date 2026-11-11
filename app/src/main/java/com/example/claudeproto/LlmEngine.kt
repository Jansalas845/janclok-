package com.example.claudeproto

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.json.JSONArray
import java.io.File

/** Modelo de lenguaje 100% en el dispositivo (MediaPipe LLM Inference, formato .task). */
class LlmEngine(private val ctx: Context) {
    @Volatile private var llm: LlmInference? = null
    val modelFile: File get() = File(ctx.filesDir, "model.task")

    fun isReady() = llm != null

    @Synchronized
    fun load(): Boolean {
        if (!modelFile.exists()) return false
        llm?.close(); llm = null
        val opts = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(1024)
            .build()
        llm = LlmInference.createFromOptions(ctx, opts)
        return true
    }

    @Synchronized
    fun chat(msgs: JSONArray): String {
        val m = llm ?: throw IllegalStateException("Modelo no cargado")
        val sb = StringBuilder()
        for (i in maxOf(0, msgs.length() - 6) until msgs.length()) {   // ventana corta de contexto
            val o = msgs.getJSONObject(i)
            val role = if (o.getString("role") == "user") "user" else "model"
            sb.append("<start_of_turn>").append(role).append('\n')
                .append(o.getString("content")).append("<end_of_turn>\n")
        }
        sb.append("<start_of_turn>model\n")                              // formato Gemma
        return m.generateResponse(sb.toString()).trim()
    }
}
