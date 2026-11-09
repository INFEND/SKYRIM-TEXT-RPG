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
    private val incoming=Messenger(Handler(Looper.getMainLooper()) { reply ->
        if(reply.what==LocalAiInferenceService.MSG_RESULT) {
            val id=reply.data.getLong("id",-1L)
            val listener: AiStoryClient.Listener?
            synchronized(guard) {
                if(closed||id!=generation||!busy) return@Handler true
                listener=activeListener
                activeListener=null
                busy=false
            }
            val error=reply.data.getString("error")
            val answer=reply.data.getString("answer")
            if(!error.isNullOrBlank())listener?.onError(error)
            else if(answer.isNullOrBlank())listener?.onError("Локальная модель не вернула текст")
            else {
                listener?.onText(answer)
                listener?.onComplete(answer)
            }
        }
        true
    })

    fun isRunning():Boolean=synchronized(guard){busy}

    fun generate(modelPath:String,game:GameEngine,history:String,command:String,
                 controlled:Boolean,chanceOnly:Boolean,listener:AiStoryClient.Listener):Boolean {
        val model=File(modelPath)
        if(!model.isFile||model.length()<64L*1024L*1024L)return false
        val system=AiStoryClient.localSystemPrompt(controlled,chanceOnly)
        val prompt=AiStoryClient.localUserPrompt(game,history,command,controlled)
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
        data.putInt("maxTokens",if(chanceOnly)192 else if(controlled)2048 else 1536)
        request.data=data
        request.replyTo=incoming
        pending=request
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
                    }
                    override fun onBindingDied(name:ComponentName?) {
                        messenger=null
                        failGeneration(synchronized(guard){generation},"Служба локальной модели аварийно завершилась. Попробуй уменьшить модель или освободить память.")
                    }
                    override fun onNullBinding(name:ComponentName?) {
                        failGeneration(synchronized(guard){generation},"Не удалось связаться с локальным движком.")
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
        ui.post { listener?.onError(error) }
    }

    fun cancel() {
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
