package com.example.smoketest

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.input)
        val button = findViewById<Button>(R.id.btn_go)
        val output = findViewById<TextView>(R.id.output)

        button.setOnClickListener {
            val text = input.text.toString().trim()
            output.text = if (text.isEmpty()) "（输入为空）" else "你输入了：$text"
        }
    }
}
