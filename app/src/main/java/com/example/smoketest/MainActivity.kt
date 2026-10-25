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
    }
}
