package com.example.smoketest

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlin.concurrent.thread

/**
 * 后台数据流监听服务（模拟版，测试用）。
 *
 * 模拟对一个公开数据流的 HTTP 长连接监听：循环等待后产出条目，
 * 条目类型为「文件」时把文件名打印到日志。
 * 注意：纯本地模拟，不发起任何真实网络请求。
 */
class StreamListenerService : Service() {

    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_STICKY
        running = true
        thread(name = "stream-listener") {
            var seq = 0
            while (running) {
                Thread.sleep(5_000L) // 模拟长连接等待间隔
                seq++
                val item = mockItem(seq)
                if (item.type == "file") {
                    Log.i(TAG, "收到文件: ${item.name}")
                } else {
                    Log.d(TAG, "收到消息: ${item.name}")
                }
            }
        }
        Log.i(TAG, "数据流监听服务已启动（模拟）")
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        Log.i(TAG, "数据流监听服务已停止")
        super.onDestroy()
    }

    private data class Item(val type: String, val name: String)

    /** 模拟数据流条目：每 4 条里 3 条是文件 */
    private fun mockItem(seq: Int): Item = when (seq % 4) {
        0 -> Item("file", "photo_$seq.jpg")
        1 -> Item("file", "video_$seq.mp4")
        2 -> Item("text", "心跳消息 #$seq")
        else -> Item("file", "audio_$seq.m4a")
    }

    companion object {
        private const val TAG = "StreamListener"
    }
}
