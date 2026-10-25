package com.example.smoketest

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val dm = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val cursor = dm.query(DownloadManager.Query().setFilterById(id))
            cursor?.use {
                if (it.moveToFirst()) {
                    val titleIdx = it.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val title = if (titleIdx >= 0) it.getString(titleIdx) else "?"
                    val status = if (statusIdx >= 0) it.getInt(statusIdx) else -1
                    Log.i(TAG, "下载完成: title=$title status=$status")
                    runOnUiThread { output.text = "下载完成: $title" }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.input)
        val button = findViewById<Button>(R.id.btn_go)
        val btnCloud = findViewById<Button>(R.id.btn_cloud)
        output = findViewById<TextView>(R.id.output)

        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // 启动后台数据流监听服务（纯本地模拟，不连真实接口）
        startService(Intent(this, StreamListenerService::class.java))

        button.setOnClickListener {
            val text = input.text.toString().trim()
            when {
                text.isEmpty() -> output.text = "（输入为空）"
                text.startsWith("http://") || text.startsWith("https://") -> startDownload(text)
                else -> output.text = "你输入了：$text"
            }
        }

        btnCloud.setOnClickListener { wakeCloudAssistant() }
    }

    /** 向云端助手发送唤醒 GET 请求：只发送，不处理返回内容 */
    private fun wakeCloudAssistant() {
        output.text = "正在唤醒云端助手…"
        thread(name = "cloud-wake") {
            var result: String
            try {
                val conn = URL(CLOUD_API_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                val code = conn.responseCode
                result = "请求已发送，响应码: $code"
                conn.disconnect()
            } catch (e: Exception) {
                result = "请求失败: ${e.message}"
            }
            runOnUiThread { output.text = result }
        }
    }

    /** 用 Android 原生 DownloadManager 下载用户输入的 http(s) 链接 */
    private fun startDownload(url: String) {
        val fileName = url.substringAfterLast('/')
            .ifEmpty { "download_${System.currentTimeMillis()}" }
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)
        Log.i(TAG, "开始下载 id=$id url=$url")
        output.text = "开始下载：$fileName"
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(downloadReceiver) }
    }

    companion object {
        private const val TAG = "SmokeTest"

        /** 云端助手地址：把「你的云IP」替换成真实服务器 IP */
        private const val CLOUD_API_URL = "http://你的云IP:8080/api/start"
    }
}
