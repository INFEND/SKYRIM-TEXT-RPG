package dev.infend.skyrimtext

import android.app.Activity
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
    private var requestData: Bundle?=null
    private var recoveryUsed=false
    private var lastPartial=""
    private var lastTokens=0
    private var activeListener: AiStoryClient.Listener?=null
    private var messenger: Messenger?=null
    private var binding: ServiceConnection?=null
    private var watchdog: Runnable?=null
    private var connectWatchdog: Runnable?=null
    private var activationAttempted=false
    private var awaitingActivation=false
    private val incoming=Messenger(Handler(Looper.getMainLooper()) { reply ->
        if(reply.what==LocalAiProtocol.MSG_STATUS) {
            val id=reply.data.getLong("id",-1L)
            val listener=synchronized(guard) {
                if(!closed&&busy&&id==generation)activeListener else null
            }
            listener?.onLocalStatus(reply.data.getString("status").orEmpty())
        }else if(reply.what==LocalAiProtocol.MSG_TEXT) {
            val id=reply.data.getLong("id",-1L)
            val listener=synchronized(guard) {
                if(!closed&&busy&&id==generation)activeListener else null
            }
            if(listener!=null) {
                val partial=reply.data.getString("text").orEmpty()
                val tokens=reply.data.getInt("tokens",0)
                if(partial.isNotEmpty()) {
                    // Reset the inactivity watchdog only when generation advances.
                    if(tokens>lastTokens)armWatchdog(id)
                    lastPartial=partial
                    lastTokens=tokens
                    listener.onText(partial)
                }
                if(tokens>0)listener.onLocalStatus("Генерация: "+tokens+" токенов")
            }
        }else if(reply.what==LocalAiProtocol.MSG_RESULT) {
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
            requestData=null
            lastPartial=""
            lastTokens=0
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
        return start(modelPath,system,prompt,maxTokens,listener,controlled&&!chanceOnly)
    }

    /** A tiny smoke-test with no RPG state changes, available in AI settings. */
    fun testModel(modelPath:String,listener:AiStoryClient.Listener):Boolean =
        start(modelPath,"Отвечай коротко и точно, без рассуждений.",
            "Напиши только слово: ГОТОВО",24,listener)

    private fun start(modelPath:String,system:String,prompt:String,maxTokens:Int,
                      listener:AiStoryClient.Listener,scene:Boolean=false):Boolean {
        val model=File(modelPath)
        if(!model.isFile||model.length()<64L*1024L*1024L)return false
        if(!LocalAiEngineModule.installed(context)) {
            ui.post { listener.onError("Установи движок локального ИИ в управлении ресурсами.") }
            return true
        }
        val modelUri=LocalAiModelProvider.uri(context,modelPath) ?: return false
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
        val request=Message.obtain(null,LocalAiProtocol.MSG_GENERATE)
        val data=Bundle()
        data.putLong("id",id)
        data.putString("path",modelUri.toString())
        data.putString("system",system)
        data.putString("prompt",prompt)
        data.putInt("maxTokens",maxTokens)
        data.putBoolean("scene",scene)
        request.data=data
        request.replyTo=incoming
        pending=request
        requestData=Bundle(data)
        recoveryUsed=false
        lastPartial=""
        lastTokens=0
        activationAttempted=false
        awaitingActivation=false
        listener.onLocalStatus("Подключение к отдельному процессу модели…")
        armWatchdog(id)
        val current=messenger
        if(current!=null) {
            pending=null
            send(current,request,id)
        }else connect(id)
        return true
    }

    private fun armWatchdog(id:Long) {
        watchdog?.let{ui.removeCallbacks(it)}
        val expiry=Runnable {
            val timedOut=synchronized(guard){busy&&generation==id&&!closed}
            if(timedOut){
                failGeneration(id,"Локальная модель слишком долго не отвечает. Это может быть медленный CPU-инференс или зависание GGUF. Проверь коротким тестом в настройках.")
                try{messenger?.send(Message.obtain(null,LocalAiProtocol.MSG_CANCEL))}catch(_:Exception){}
                releaseBinding()
            }
        }
        watchdog=expiry
        ui.postDelayed(expiry,12L*60L*1000L)
    }

    private fun recoverProcess(id:Long,reason:String) {
        if(!currentGeneration(id)) {releaseBinding();return}
        releaseBinding()
        // Never silently replay a request after a native worker crash, even
        // before the first token. The player can explicitly retry the locked action.
        failGeneration(id,reason.trimEnd('.')+" (получено $lastTokens токенов). Повтори действие вручную")
    }

    private fun currentGeneration(id:Long):Boolean=synchronized(guard){!closed&&busy&&id==generation}
    private fun stopConnectTimeout(){connectWatchdog?.let{ui.removeCallbacks(it)};connectWatchdog=null}
    private fun connect(id:Long) {
        if(!currentGeneration(id))return
        stopConnectTimeout()
        releaseBinding()
        val connection=object:ServiceConnection{
            override fun onServiceConnected(name:ComponentName?,binder:IBinder?) {
                if(binding!==this||!currentGeneration(id))return
                stopConnectTimeout()
                if(binder==null){recoverConnection(id,"Служба вернула пустое подключение");return}
                val server=Messenger(binder)
                messenger=server
                val msg=pending
                pending=null
                if(msg!=null)send(server,msg,id)
            }
            override fun onServiceDisconnected(name:ComponentName?) {
                if(binding!==this)return
                val active=synchronized(guard){generation}
                recoverProcess(active,"Процесс локального ИИ завершился. Возможны нехватка памяти или сбой нативного движка")
            }
            override fun onBindingDied(name:ComponentName?) {
                if(binding!==this)return
                val active=synchronized(guard){generation}
                if(pending!=null)recoverConnection(active,"Android разорвал подключение к службе")
                else recoverProcess(active,"Служба локального ИИ аварийно завершилась")
            }
            override fun onNullBinding(name:ComponentName?) {
                if(binding===this)recoverConnection(synchronized(guard){generation},"Служба не предоставила подключение")
            }
        }
        binding=connection
        try {
            val intent=Intent().setComponent(ComponentName(LocalAiEngineModule.PACKAGE,"dev.infend.skyrimtext.LocalAiInferenceService"))
            if(!context.bindService(intent,connection,Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT)) {
                recoverConnection(id,"Android отказал в запуске службы (bindService=false)")
                return
            }
            val timeout=Runnable {if(binding===connection&&messenger==null&&currentGeneration(id))recoverConnection(id,"Служба не подключилась за 10 секунд")}
            connectWatchdog=timeout
            ui.postDelayed(timeout,10000L)
        }catch(ex:Exception) {
            recoverConnection(id,"Ошибка подключения: "+(ex.message?:ex.javaClass.simpleName))
        }
    }

    private fun recoverConnection(id:Long,reason:String) {
        if(!currentGeneration(id))return
        releaseBinding()
        if(!activationAttempted&&pending!=null&&context is Activity&&!context.isFinishing&&!context.isDestroyed) {
            activationAttempted=true
            awaitingActivation=true
            activeListener?.onLocalStatus("Активация отдельного движка Android…")
            try {
                context.startActivityForResult(LocalAiEngineModule.activationIntent(true),LocalAiEngineModule.REQUEST_ACTIVATE)
                return
            }catch(ex:Exception) {
                awaitingActivation=false
                failGeneration(id,reason+". Не удалось открыть экран активации: "+(ex.message?:ex.javaClass.simpleName))
                return
            }
        }
        failGeneration(id,reason+". Движок: "+LocalAiEngineModule.diagnostic(context)+". Открой приложение SKYRIM Local AI Engine один раз; проверь разрешения автозапуска и фоновой активности OriginOS.")
    }

    fun onEngineActivationResult(ok:Boolean) {
        if(!awaitingActivation)return
        awaitingActivation=false
        val id=synchronized(guard){generation}
        if(!currentGeneration(id))return
        if(!ok){failGeneration(id,"Активация движка отменена. Открой SKYRIM Local AI Engine и повтори тест.");return}
        activeListener?.onLocalStatus("Повторное подключение к активированному движку…")
        ui.postDelayed({if(currentGeneration(id))connect(id)},350L)
    }

    private fun send(server:Messenger,message:Message,id:Long) {
        try{server.send(message)}
        catch(ex:RemoteException) {recoverProcess(id,"Локальный движок недоступен: "+ex.message)}
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
        stopConnectTimeout()
        pending=null
        requestData=null
        lastPartial=""
        lastTokens=0
        awaitingActivation=false
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
            requestData=null
            lastPartial=""
            lastTokens=0
            awaitingActivation=false
        }
        // The native completion is not cooperatively cancellable. Its own process
        // can be terminated without crashing the foreground game.
        try{messenger?.send(Message.obtain(null,LocalAiProtocol.MSG_CANCEL))}
        catch(_:Exception){}
        releaseBinding()
    }

    private fun releaseBinding(){
        stopConnectTimeout()
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
