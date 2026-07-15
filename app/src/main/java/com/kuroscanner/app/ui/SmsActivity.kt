package com.kuroscanner.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kuroscanner.app.R
import com.kuroscanner.app.api.KuRoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsActivity : AppCompatActivity() {
    private lateinit var codeInput: EditText
    private lateinit var confirmBtn: Button
    private lateinit var cancelBtn: Button
    private var token: String = ""
    private var qrcode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)

        token = intent.getStringExtra("token") ?: ""
        qrcode = intent.getStringExtra("qrcode") ?: ""

        codeInput = findViewById(R.id.codeInput)
        confirmBtn = findViewById(R.id.confirmBtn)
        cancelBtn = findViewById(R.id.cancelBtn)

        confirmBtn.setOnClickListener {
            val code = codeInput.text.toString()
            if (code.isEmpty()) {
                Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmLogin(code)
        }

        cancelBtn.setOnClickListener {
            finish()
        }
    }

    private fun confirmLogin(code: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = KuRoApi.confirmLoginGameByQrCode(qrcode, token, false, code)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    Toast.makeText(this@SmsActivity, "验证成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@SmsActivity, "验证失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}