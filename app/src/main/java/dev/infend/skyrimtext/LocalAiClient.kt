package dev.infend.skyrimtext

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import java.io.File

/**
 * LLM inference is deliberately outside the game/UI process.
 * A native SIGABRT, SIGSEGV or low-memory kill must not destroy the player's RPG session.
 */
class LocalAiClient(private val context: Context) {
    private val ui=Handler(Looper.getMainLooper())
    private val guard=Any()
    private var busy=false
    private var closed=false
    private var generation=0L
    private var pending: Message?=null
    private var activeListener: AiStoryClient.Listener?=null
    private var messenger: Messenger?=null
    private var binding: ServiceConnection?=null
    private var watchdog: Runnable?=null
    private val incoming=Messenger(Handler(Looper.getMainLooper()) { reply ->
        if(reply.what==LocalAiInferenceService.MSG_STATUS) {
            val id=reply.data.getLong("id",-1L)
            val listener=synchronized(guard) {
                if(!closed&&busy&&id==generation)activeListener else null
            }
            listener?.onLocalStatus(reply.data.getString("status").orEmpty())
        }else if(reply.what==LocalAiInferenceService.MSG_TEXT) {
            val id=reply.data.getLong("id",-1L)
            val listener=synchronized(guard) {
                if(!closed&&busy&&id==generation)activeListener else null
            }
            if(listener!=null) {
                val partial=reply.data.getString("text").orEmpty()
                val tokens=reply.data.getInt("tokens",0)
                if(partial.isNotEmpty())listener.onText(partial)
                if(tokens>0)listener.onLocalStatus("Генерация: "+tokens+" токенов")
            }
        }else if(reply.what==LocalAiInferenceService.MSG_RESULT) {
            val id=reply.data.getLong("id",-1L)
            val listener: AiStoryClient.Listener?
            synchronized(guard) {
                if(closed||id!=generation||!busy) return@Handler true
                listener=activeListener
                activeListener=null
                busy=false
            }
            watchdog?.let{ui.removeCallbacks(it)}
            watchdog=null
            val error=reply.data.getString("error")
            val answer=reply.data.getString("answer")
            if(!error.isNullOrBlank())listener?.onError(error)
            else if(answer.isNullOrBlank())listener?.onError("Локальная модель не вернула текст")
            else {
                listener?.onLocalMetrics(reply.data.getInt("tokens",0),
                    reply.data.getFloat("speed",0f),reply.data.getLong("elapsedMs",0))
                listener?.onText(answer)
                listener?.onComplete(answer)
            }
        }
        true
    })

    fun isRunning():Boolean=synchronized(guard){busy}

    fun generate(modelPath:String,game:GameEngine,history:String,command:String,
                 controlled:Boolean,chanceOnly:Boolean,listener:AiStoryClient.Listener):Boolean {
        val system=AiStoryClient.localSystemPrompt(controlled,chanceOnly)
        val prompt=AiStoryClient.localUserPrompt(game,history,command,controlled)
        // Full scenes require a structured JSON with ten choices. CPU-only 9B
        // inference is slow: keep the budget finite rather than waiting on 2048 tokens.
        val maxTokens=if(chanceOnly)128 else if(controlled)2304 else 768
        return start(modelPath,system,prompt,maxTokens,listener)
    }

    /** A tiny smoke-test with no RPG state changes, available in AI settings. */
    fun testModel(modelPath:String,listener:AiStoryClient.Listener):Boolean =
        start(modelPath,"Отвечай коротко и точно, без рассуждений.",
            "Напиши только слово: ГОТОВО",24,listener)

    private fun start(modelPath:String,system:String,prompt:String,maxTokens:Int,
                      listener:AiStoryClient.Listener):Boolean {
        val model=File(modelPath)
        if(!model.isFile||model.length()<64L*1024L*1024L)return false
        var fail:String?=null
        try {
            val manager=context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info=ActivityManager.MemoryInfo()
            manager.getMemoryInfo(info)
            // A 9B model needs substantial resident RAM even with mmap; do not start
            // a high-risk native load when the OS reports severe memory pressure.
            if(info.lowMemory || info.availMem<3L*1024*1024*1024)
                fail="Недостаточно свободной оперативной памяти. Закрой другие приложения и попробуй снова."
        } catch(_:Exception) { /* Some devices restrict memory inspection. */ }
        val id:Long
        synchronized(guard) {
            if(busy||closed)return false
            busy=true
            id=++generation
            activeListener=listener
        }
        if(fail!=null) {
            failGeneration(id,fail)
            return true
        }
        val request=Message.obtain(null,LocalAiInferenceService.MSG_GENERATE)
        val data=Bundle()
        data.putLong("id",id)
        data.putString("path",modelPath)
        data.putString("system",system)
        data.putString("prompt",prompt)
        data.putInt("maxTokens",maxTokens)
        request.data=data
        request.replyTo=incoming
        pending=request
        listener.onLocalStatus("Подключение к отдельному процессу модели…")
        // Native completion streams tokens, but a hardware hang must still time out.
        val expiry=Runnable {
            val timedOut=synchronized(guard){busy&&generation==id&&!closed}
            if(timedOut){
                failGeneration(id,"Локальная модель слишком долго не отвечает. Это может быть медленный CPU-инференс или зависание GGUF. Проверь коротким тестом в настройках.")
                try{messenger?.send(Message.obtain(null,LocalAiInferenceService.MSG_CANCEL))}catch(_:Exception){}
                releaseBinding()
            }
        }
        watchdog=expiry
        ui.postDelayed(expiry,12L*60L*1000L)
        val current=messenger
        if(current!=null) {
            send(current,request,id)
        }else {
            try {
                val connection=object:ServiceConnection{
                    override fun onServiceConnected(name:ComponentName?,binder:IBinder?) {
                        if(binding!==this)return
                        if(binder==null){failGeneration(id,"Локальный движок не запустился");return}
                        val server=Messenger(binder)
                        messenger=server
                        val msg=pending
                        pending=null
                        if(msg!=null&&synchronized(guard){!closed&&busy&&generation==id})
                            send(server,msg,id)
                    }
                    override fun onServiceDisconnected(name:ComponentName?) {
                        messenger=null
                        failGeneration(synchronized(guard){generation},"Процесс локальной модели завершился. Возможно, не хватило памяти или GGUF несовместим с движком.")
                        releaseBinding()
                    }
                    override fun onBindingDied(name:ComponentName?) {
                        messenger=null
                        failGeneration(synchronized(guard){generation},"Служба локальной модели аварийно завершилась. Попробуй уменьшить модель или освободить память.")
                        releaseBinding()
                    }
                    override fun onNullBinding(name:ComponentName?) {
                        failGeneration(synchronized(guard){generation},"Не удалось связаться с локальным движком.")
                        releaseBinding()
                    }
                }
                binding=connection
                if(!context.bindService(Intent(context,LocalAiInferenceService::class.java),
                       connection,Context.BIND_AUTO_CREATE)) {
                    binding=null
                    failGeneration(id,"Android не запустил процесс локальной модели.")
                }
            }catch(ex:Exception) {
                binding=null
                failGeneration(id,"Невозможно запустить локальную службу: "+(ex.message?:"ошибка Android"))
            }
        }
        return true
    }

    private fun send(server:Messenger,message:Message,id:Long) {
        try{server.send(message)}
        catch(ex:RemoteException) {messenger=null;failGeneration(id,"Локальный движок недоступен: "+ex.message)}
    }

    private fun failGeneration(id:Long,error:String){
        val listener:AiStoryClient.Listener?
        synchronized(guard){
            if(closed||id!=generation||!busy)return
            listener=activeListener
            activeListener=null
            busy=false
        }
        watchdog?.let {ui.removeCallbacks(it)}
        watchdog=null
        ui.post { listener?.onError(error) }
    }

    fun cancel() {
        watchdog?.let{ui.removeCallbacks(it)}
        watchdog=null
        synchronized(guard) {
            generation++
            activeListener=null
            busy=false
            pending=null
        }
        // The native completion is not cooperatively cancellable. Its own process
        // can be terminated without crashing the foreground game.
        try{messenger?.send(Message.obtain(null,LocalAiInferenceService.MSG_CANCEL))}
        catch(_:Exception){}
        releaseBinding()
    }

    private fun releaseBinding(){
        val conn=binding
        binding=null
        messenger=null
        if(conn!=null)try{context.unbindService(conn)}catch(_:Exception){}
    }

    fun shutdown() {
        synchronized(guard){closed=true}
        cancel()
    }
}
