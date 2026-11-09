package dev.infend.skyrimtext

import android.os.Handler
import android.os.Looper
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/** Local llama.cpp session. Cancellation invalidates results while native inference finishes. */
class LocalAiClient {
    private val main=Handler(Looper.getMainLooper())
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val lock=Any()
    private var busy=false
    private var generation=0L
    private var closed=false
    private var loadedPath=""
    private var loadedModel:LlamaModel?=null

    fun isRunning():Boolean=synchronized(lock){busy}
    fun generate(modelPath:String,game:GameEngine,history:String,command:String,
                 controlled:Boolean,chanceOnly:Boolean,listener:AiStoryClient.Listener):Boolean {
        if(!File(modelPath).isFile)return false
        val instructions=AiStoryClient.localSystemPrompt(controlled,chanceOnly)
        val prompt=AiStoryClient.localUserPrompt(game,history,command,controlled)
        val id:Long
        synchronized(lock) {
            if(busy||closed)return false
            busy=true
            id=++generation
        }
        scope.launch {
            var answer:String?=null
            var failure:String?=null
            try {
                if(loadedPath!=modelPath||loadedModel==null){
                    loadedModel?.let { Llama.releaseModel(it) }
                    loadedModel=null
                    loadedPath=""
                    loadedModel=Llama.loadModel(modelPath,LlamaConfig(
                        contextSize=8192,threads=4,gpuLayers=0,
                        temperature=if(chanceOnly)0.15f else 0.7f,topP=0.9f
                    ))
                    loadedPath=modelPath
                }
                val model=loadedModel ?: error("Модель не загружена")
                val result=Llama.complete(model,prompt=prompt,systemPrompt=instructions,
                    maxTokens=if(chanceOnly)256 else if(controlled)4096 else 3072)
                if(result.text.isBlank())error("Модель вернула пустой ответ")
                answer=result.text
            } catch(ex:Throwable){
                failure=ex.message?:"Ошибка локального вывода"
            } finally {
                synchronized(lock){busy=false}
                if(synchronized(lock){closed}){
                    loadedModel?.let { Llama.releaseModel(it) }
                    loadedModel=null
                    loadedPath=""
                }
            }
            val output=answer
            val error=failure
            main.post {
                if(synchronized(lock){closed||id!=generation})return@post
                if(error!=null)listener.onError(error)
                else if(output!=null){listener.onText(output);listener.onComplete(output)}
            }
        }
        return true
    }
    fun cancel(){synchronized(lock){generation++}}
    fun shutdown(){
        synchronized(lock){generation++;closed=true}
        if(!isRunning())scope.launch {
            loadedModel?.let { Llama.releaseModel(it) }
            loadedModel=null
            loadedPath=""
        }
    }
}
