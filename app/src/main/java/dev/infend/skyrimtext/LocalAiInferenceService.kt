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
        val maxTokens=input.getInt("maxTokens",1536).coerceIn(128,2048)
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
        scope.launch {
            var answer:String?=null
            var error:String?=null
            try {
                if(cachedPath!=path||cached==null){
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
                answer=Llama.complete(model,prompt=prompt,systemPrompt=instructions,
                    maxTokens=maxTokens).text
                if(answer.isNullOrBlank())kotlin.error("Модель вернула пустой ответ")
            }catch(ex:Throwable){
                error=ex.message?:"Ошибка локального движка"
            }finally{busy=false}
            reply(recipient,id,answer,error)
        }
    }

    private fun reply(to:Messenger,id:Long,answer:String?,error:String?){
        val msg=Message.obtain(null,MSG_RESULT)
        msg.data=Bundle().apply{
            putLong("id",id)
            if(answer!=null)putString("answer",answer)
            if(error!=null)putString("error",error)
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
