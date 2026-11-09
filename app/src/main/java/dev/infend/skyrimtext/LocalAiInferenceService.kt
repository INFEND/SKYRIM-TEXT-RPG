package dev.infend.skyrimtext

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Private :llm process contains all JNI allocations. Its failure cannot
 * directly terminate the game process. No network operations are performed.
 */
class LocalAiInferenceService:Service() {
    companion object {
        const val MSG_GENERATE=1
        const val MSG_CANCEL=2
        const val MSG_RESULT=3
        const val MSG_STATUS=4
    }
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val handler=Handler(Looper.getMainLooper()) { message ->
        when(message.what){
            MSG_GENERATE -> handleGenerate(message)
            MSG_CANCEL -> {
                // Native decode has no safe synchronous interrupt in the bundled AAR.
                // This is safe only because Android runs us in a dedicated process.
                Process.killProcess(Process.myPid())
            }
        }
        true
    }
    private val messenger=Messenger(handler)
    @Volatile private var busy=false
    private var cachedPath=""
    private var cached:LlamaModel?=null

    override fun onBind(intent:Intent?):IBinder=messenger.binder

    private fun handleGenerate(message:Message){
        val recipient=message.replyTo ?: return
        val input=message.data
        val id=input.getLong("id",-1)
        val path=input.getString("path").orEmpty()
        val instructions=input.getString("system").orEmpty()
        val prompt=input.getString("prompt").orEmpty()
        val maxTokens=input.getInt("maxTokens",896).coerceIn(16,1536)
        if(busy){
            reply(recipient,id,null,"Локальная модель ещё обрабатывает предыдущий ход.")
            return
        }
        if(!File(path).isFile || !path.endsWith(".gguf",true)){
            reply(recipient,id,null,"Не найден импортированный GGUF.")
            return
        }
        if(prompt.isBlank()||instructions.isBlank()||prompt.length+instructions.length>7000){
            reply(recipient,id,null,"Слишком большой или пустой игровой запрос.")
            return
        }
        busy=true
        replyStatus(recipient,id,"Запуск локального движка…")
        scope.launch {
            var answer:String?=null
            var error:String?=null
            var tokens=0
            var speed=0f
            var elapsed=0L
            try {
                if(cachedPath!=path||cached==null){
                    replyStatus(recipient,id,"Загрузка GGUF в оперативную память…")
                    cached?.let {Llama.releaseModel(it)}
                    cached=null;cachedPath=""
                    cached=Llama.loadModel(path,LlamaConfig(
                        contextSize=4096,
                        threads=4,
                        gpuLayers=0,
                        temperature=0.7f,
                        topP=0.9f
                    ))
                    cachedPath=path
                }
                val model=cached ?: kotlin.error("GGUF не загрузился")
                replyStatus(recipient,id,"Обработка контекста и генерация на CPU (без потокового вывода)…")
                val started=android.os.SystemClock.elapsedRealtime()
                val result=Llama.complete(model,prompt=prompt,systemPrompt=instructions,
                    maxTokens=maxTokens)
                answer=result.text
                tokens=result.tokensGenerated
                speed=result.tokensPerSecond
                elapsed=android.os.SystemClock.elapsedRealtime()-started
                if(answer.isNullOrBlank())kotlin.error("Модель вернула пустой ответ")
            }catch(ex:Throwable){
                error=ex.message?:"Ошибка локального движка"
            }finally{busy=false}
            reply(recipient,id,answer,error,tokens,speed,elapsed)
        }
    }

    private fun replyStatus(to:Messenger,id:Long,status:String){
        val msg=Message.obtain(null,MSG_STATUS)
        msg.data=Bundle().apply{putLong("id",id);putString("status",status)}
        try{to.send(msg)}catch(_:Exception){}
    }

    private fun reply(to:Messenger,id:Long,answer:String?,error:String?,
                      tokens:Int=0,speed:Float=0f,elapsed:Long=0L){
        val msg=Message.obtain(null,MSG_RESULT)
        msg.data=Bundle().apply{
            putLong("id",id)
            if(answer!=null)putString("answer",answer)
            if(error!=null)putString("error",error)
            putInt("tokens",tokens)
            putFloat("speed",speed)
            putLong("elapsedMs",elapsed)
        }
        try{to.send(msg)}catch(_:Exception){}
    }

    override fun onDestroy(){
        // Android may terminate the worker process after the client disconnects.
        if(!busy)cached?.let {Llama.releaseModel(it)}
        cached=null
        super.onDestroy()
    }
}
