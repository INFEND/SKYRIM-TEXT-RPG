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
import android.content.pm.PackageManager

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
        if(packageManager.checkSignatures(message.sendingUid,Process.myUid())!=PackageManager.SIGNATURE_MATCH) return@Handler true
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
    private var loadedRecovery = false
    private var loadedMemoryProfile = false
    private var contextCapacity = 8192
    private var activeReply: Messenger? = null
    private var activeId: Long = -1L
    private var textBuffer = StringBuilder()
    private var tokenCount = 0
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
        val scene = data.getBoolean("scene", false)
        val recovery = data.getBoolean("recovery", false)
        val memoryProfile = data.getBoolean("memoryProfile", false)
        val modelBytes = data.getLong("modelBytes",0L)
        val resume = if (recovery) data.getString("resumeText").orEmpty().takeIf { it.length <= 30000 }.orEmpty() else ""
        val previousTokens = if (resume.isNotEmpty()) data.getInt("resumeTokens", 0).coerceIn(0, limit - 24) else 0

        if (busy) {
            finish(receiver, id, null, "Предыдущая локальная генерация ещё выполняется.", 0, 0f, 0L)
            return
        }
        val modelUri=Uri.parse(path)
        if (modelUri.scheme!="content" || modelUri.authority!="dev.infend.skyrimtext.models" || !path.endsWith("/model.gguf")) {
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
            tokenCount = previousTokens
            generatedAt = 0L
        }
        status(receiver, id, if (recovery) "Восстановление локального ИИ…" else "Запуск локального ИИ…")
        if (recovery && android.os.Build.VERSION.SDK_INT >= 30) {
            try {
                val manager = getSystemService(android.app.ActivityManager::class.java)
                val exit = manager?.getHistoricalProcessExitReasons(packageName, 0, 8)
                    ?.firstOrNull { it.processName == "$packageName:llm" }
                if (exit != null) android.util.Log.w("SkyrimLocalAI", "Previous worker exit: reason=${exit.reason}, status=${exit.status}, pss=${exit.pss} KB")
            } catch (_: Exception) {}
        }
        scope.launch {
            var answer: String? = null
            var failure: String? = null
            var count = 0
            var speed = 0f
            var elapsed = 0L
            try {
                val context = ensureLoaded(path, receiver, id, recovery, memoryProfile, modelBytes)
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
                val direct = AiScenePreview.directPrompt(formatted)
                val prefix = if (resume.isNotEmpty()) resume else AiScenePreview.scenePrefix(direct, scene)
                // Prefill is part of the response, not a generated token.
                synchronized(stateLock) {
                    textBuffer.append(prefix)
                    generatedAt = SystemClock.elapsedRealtime()
                }
                status(receiver, id, "Проверка размера контекста…")
                val inputTokens = (engine.tokenize(context,direct + prefix).firstOrNull()?.get("tokens") as? List<*>)?.size
                if(inputTokens!=null && inputTokens + 16 >= contextCapacity)
                    error("Запрос не помещается в контекст модели: $inputTokens / $contextCapacity токенов.")
                val outputBudget=if(inputTokens==null) limit-previousTokens else
                    minOf(limit-previousTokens,contextCapacity-inputTokens-16)
                if(outputBudget<24)error("Недостаточно места для ответа в контексте модели.")
                status(receiver, id, "Генерация ответа")
                val started = SystemClock.elapsedRealtime()
                val result = engine.launchCompletion(context, mapOf(
                    "prompt" to (direct + prefix),
                    "emit_partial_completion" to true,
                    "n_predict" to outputBudget,
                    "n_threads" to (if(recovery) 2 else 4),
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
                answer = if (count > previousTokens && streamed.length > prefix.length) streamed else {
                    val completion = (result["text"] as? String)
                        ?: (result["content"] as? String)
                        ?: ""
                    if (completion.isBlank()) "" else prefix + completion
                }
                if (answer.isBlank()) error("Модель не вернула текст. Проверь GGUF и шаблон диалога.")
                speed = if (elapsed > 0) ((count - previousTokens) * 1000f / elapsed) else 0f
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

    private fun ensureLoaded(path: String, receiver: Messenger, id: Long, recovery: Boolean,
                             memoryProfile:Boolean, modelBytes:Long): Int {
        if (loadedPath == path && loadedContext != null && loadedRecovery == recovery &&
            loadedMemoryProfile == memoryProfile) return loadedContext!!
        loadedContext?.let {
            try { engine.releaseContext(it) } catch (_: Throwable) {}
        }
        loadedPath = ""
        loadedContext = null
        status(receiver, id, "Загрузка модели Qwen из памяти телефона…")
        val uri = Uri.parse(path)
        status(receiver,id,"Проверка GGUF и нативной библиотеки…")
        val nativeEngine=engine
        if(!nativeEngine.isGGUF(uri))error("Файл не содержит корректный заголовок GGUF.")
        val descriptor = contentResolver.openFileDescriptor(uri, "r")
            ?: error("Не удалось открыть GGUF для нативного движка.")
        val size=descriptor.statSize.takeIf{it>0} ?: modelBytes
        // Upstream FD loader uses non-mmap. Larger files and confirmed LMK use mmap.
        val mmap=memoryProfile || size<=0 || size>8L*1024*1024*1024
        contextCapacity=if(recovery)6144 else 8192
        val fd = descriptor.detachFd()
        val config = mapOf<String, Any>(
            "model" to uri.toString(),
            "model_fd" to fd,
            "embedding" to false,
            // Reserve room for prompt, ten choices and resumed output.
            "n_ctx" to contextCapacity,
            "n_batch" to (if (recovery) 32 else 64),
            "n_threads" to (if (recovery) 2 else 4),
            "n_gpu_layers" to 0,
            "use_mmap" to mmap,
            "use_mlock" to false,
            "vocab_only" to false
        )
        status(receiver,id,"Инициализация llama.cpp: контекст $contextCapacity, mmap "+(if(mmap)"вкл" else "выкл")+"…")
        val result = engine.startEngine(config) { token -> onNativeToken(token) }
            ?: error("Не удалось загрузить GGUF. Возможно, библиотека не поддерживает архитектуру модели.")
        val context = (result["contextId"] as? Number)?.toInt()
            ?: error("Движок не вернул идентификатор контекста.")
        loadedPath = path
        loadedContext = context
        loadedRecovery = recovery
        loadedMemoryProfile = memoryProfile
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
            putInt("pid", Process.myPid())
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
