package dev.infend.skyrimtext

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LlamaAndroid
import java.io.File

/**
 * The streaming GGUF engine lives in its own :llm process.
 * If the JNI runtime fails, Android can retain the game, saves and UI.
 *
 * The previous llama-android AAR buffered the entire response on CPU.
 * Here the native callback emits partial tokens and the correct chat template
 * is applied from GGUF before inference.
 */
class LocalAiInferenceService : Service() {
    companion object {
        const val MSG_GENERATE = 1
        const val MSG_CANCEL = 2
        const val MSG_RESULT = 3
        const val MSG_STATUS = 4
        const val MSG_TEXT = 5
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper()) { message ->
        when (message.what) {
            MSG_GENERATE -> handleGenerate(message)
            MSG_CANCEL -> Process.killProcess(Process.myPid())
        }
        true
    }
    private val binder = Messenger(handler)
    private val stateLock = Any()
    private val engine by lazy { LlamaAndroid(contentResolver) }

    @Volatile private var busy = false
    private var loadedPath = ""
    private var loadedContext: Int? = null
    private var activeReply: Messenger? = null
    private var activeId: Long = -1L
    private var textBuffer = StringBuilder()
    private var tokenCount = 0
    private var lastUiUpdate = 0L
    private var generatedAt = 0L

    override fun onBind(intent: Intent?): IBinder = binder.binder

    private fun handleGenerate(message: Message) {
        val receiver = message.replyTo ?: return
        val data = message.data
        val id = data.getLong("id", -1L)
        val path = data.getString("path").orEmpty()
        val system = data.getString("system").orEmpty()
        val prompt = data.getString("prompt").orEmpty()
        val limit = data.getInt("maxTokens", 1024).coerceIn(24, 3072)

        if (busy) {
            finish(receiver, id, null, "Предыдущая локальная генерация ещё выполняется.", 0, 0f, 0L)
            return
        }
        if (!File(path).isFile || !path.endsWith(".gguf", true)) {
            finish(receiver, id, null, "Импортированный GGUF не найден.", 0, 0f, 0L)
            return
        }
        if (system.isBlank() || prompt.isBlank() || system.length + prompt.length > 8000) {
            finish(receiver, id, null, "Некорректный или слишком длинный запрос модели.", 0, 0f, 0L)
            return
        }
        synchronized(stateLock) {
            busy = true
            activeReply = receiver
            activeId = id
            textBuffer = StringBuilder()
            tokenCount = 0
            lastUiUpdate = 0L
            generatedAt = 0L
        }
        status(receiver, id, "Запуск локального ИИ…")
        scope.launch {
            var answer: String? = null
            var failure: String? = null
            var count = 0
            var speed = 0f
            var elapsed = 0L
            try {
                val context = ensureLoaded(path, receiver, id)
                status(receiver, id, "Подготовка игрового контекста…")
                val messages = listOf<Map<String, Any>>(
                    mapOf("role" to "system", "content" to system),
                    mapOf("role" to "user", "content" to prompt)
                )
                // Unlike a raw concatenated prompt, this uses the model's own
                // GGUF chat template, including the assistant-generation prefix.
                val formatted = engine.getFormattedChat(context, messages, "").firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: ("<|im_start|>system\n" + system + "<|im_end|>\n" +
                        "<|im_start|>user\n" + prompt + "<|im_end|>\n<|im_start|>assistant\n")
                status(receiver, id, "Генерация ответа, токены появятся по мере готовности…")
                synchronized(stateLock) { generatedAt = SystemClock.elapsedRealtime() }
                val started = SystemClock.elapsedRealtime()
                val result = engine.launchCompletion(context, mapOf(
                    "prompt" to formatted,
                    "emit_partial_completion" to true,
                    "n_predict" to limit,
                    "temperature" to 0.7,
                    "top_p" to 0.9,
                    "top_k" to 40,
                    "penalty_repeat" to 1.05
                ))
                elapsed = SystemClock.elapsedRealtime() - started
                val streamed = synchronized(stateLock) {
                    count = tokenCount
                    textBuffer.toString()
                }
                if (result == null) error("Нативный движок не завершил генерацию.")
                answer = streamed.ifBlank {
                    (result["text"] as? String)
                        ?: (result["content"] as? String)
                        ?: ""
                }
                if (answer.isBlank()) error("Модель не вернула текст. Проверь GGUF и шаблон диалога.")
                speed = if (elapsed > 0) (count * 1000f / elapsed) else 0f
            } catch (error: Throwable) {
                failure = error.message ?: "Ошибка движка llama.cpp"
            } finally {
                synchronized(stateLock) {
                    busy = false
                    activeReply = null
                    activeId = -1L
                }
            }
            finish(receiver, id, answer, failure, count, speed, elapsed)
        }
    }

    private fun ensureLoaded(path: String, receiver: Messenger, id: Long): Int {
        if (loadedPath == path && loadedContext != null) return loadedContext!!
        loadedContext?.let {
            try { engine.releaseContext(it) } catch (_: Throwable) {}
        }
        loadedPath = ""
        loadedContext = null
        status(receiver, id, "Загрузка модели Qwen из памяти телефона…")
        val uri = Uri.fromFile(File(path))
        val descriptor = contentResolver.openFileDescriptor(uri, "r")
            ?: error("Не удалось открыть GGUF для нативного движка.")
        val fd = descriptor.detachFd()
        val config = mapOf<String, Any>(
            "model" to uri.toString(),
            "model_fd" to fd,
            "embedding" to false,
            "n_ctx" to 4096,
            "n_batch" to 512,
            "n_threads" to 4,
            "n_gpu_layers" to 0,
            "use_mmap" to true,
            "use_mlock" to false,
            "vocab_only" to false
        )
        val result = engine.startEngine(config) { token -> onNativeToken(token) }
            ?: error("Не удалось загрузить GGUF. Возможно, библиотека не поддерживает архитектуру модели.")
        val context = (result["contextId"] as? Number)?.toInt()
            ?: error("Движок не вернул идентификатор контекста.")
        loadedPath = path
        loadedContext = context
        status(receiver, id, "Модель загружена, контекст готов.")
        return context
    }

    private fun onNativeToken(token: String) {
        if (token.isEmpty()) return
        val receiver: Messenger
        val id: Long
        val snapshot: String
        val count: Int
        val elapsed: Long
        synchronized(stateLock) {
            if (!busy || activeReply == null) return
            if (textBuffer.length < 100000) textBuffer.append(token)
            tokenCount++
            val now = SystemClock.elapsedRealtime()
            if (tokenCount > 1 && now - lastUiUpdate < 250) return
            lastUiUpdate = now
            receiver = activeReply!!
            id = activeId
            snapshot = textBuffer.toString()
            count = tokenCount
            elapsed = if (generatedAt > 0) now - generatedAt else 0
        }
        val msg = Message.obtain(null, MSG_TEXT)
        msg.data = Bundle().apply {
            putLong("id", id)
            putString("text", snapshot)
            putInt("tokens", count)
            putLong("elapsedMs", elapsed)
        }
        try { receiver.send(msg) } catch (_: Exception) {}
    }

    private fun status(receiver: Messenger, id: Long, value: String) {
        val msg = Message.obtain(null, MSG_STATUS)
        msg.data = Bundle().apply {
            putLong("id", id)
            putString("status", value)
        }
        try { receiver.send(msg) } catch (_: Exception) {}
    }

    private fun finish(receiver: Messenger, id: Long, answer: String?, error: String?,
                       tokens: Int, speed: Float, elapsed: Long) {
        val msg = Message.obtain(null, MSG_RESULT)
        msg.data = Bundle().apply {
            putLong("id", id)
            if (answer != null) putString("answer", answer)
            if (error != null) putString("error", error)
            putInt("tokens", tokens)
            putFloat("speed", speed)
            putLong("elapsedMs", elapsed)
        }
        try { receiver.send(msg) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        if (!busy) loadedContext?.let {
            try { engine.releaseContext(it) } catch (_: Throwable) {}
        }
        loadedContext = null
        super.onDestroy()
    }
}
