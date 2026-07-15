package com.kuroscanner.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kuroscanner.app.R
import com.kuroscanner.app.api.KuRoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class LoginActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout

    companion object {
        const val RESULT_LOGIN_SUCCESS = 1001
        const val EXTRA_USER_INFO = "user_info"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewPager = findViewById(R.id.viewPager)
        tabs = findViewById(R.id.tabLayout)

        val adapter = LoginPagerAdapter(supportFragmentManager)
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
    }

    fun onLoginSuccess(name: String, token: String, uid: String, mobile: String) {
        val intent = Intent()
        intent.putExtra("name", name)
        intent.putExtra("token", token)
        intent.putExtra("uid", uid)
        intent.putExtra("mobile", mobile)
        setResult(RESULT_LOGIN_SUCCESS, intent)
        finish()
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun startGeeTest(phoneNumber: String) {
        val intent = Intent(this, GeeTestActivity::class.java)
        intent.putExtra("phoneNumber", phoneNumber)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val geeTestData = data?.getStringExtra("geeTestData") ?: ""
            val phoneNumber = data?.getStringExtra("phoneNumber") ?: ""
            (viewPager.adapter as LoginPagerAdapter).getCurrentFragment()?.onGeeTestResult(geeTestData, phoneNumber)
        }
    }

    class LoginPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val fragments = listOf(SmsLoginFragment(), QrCodeLoginFragment(), CookieLoginFragment())
        private val titles = listOf("短信登录", "扫码登录", "Cookie登录")

        override fun getCount(): Int = fragments.size
        override fun getItem(position: Int): Fragment = fragments[position]
        override fun getPageTitle(position: Int): CharSequence = titles[position]

        fun getCurrentFragment(): LoginFragment? {
            return fragments.firstOrNull()
        }
    }

    interface LoginFragment {
        fun onGeeTestResult(geeTestData: String, phoneNumber: String)
    }

    class SmsLoginFragment : Fragment(), LoginFragment {
        private lateinit var phoneInput: EditText
        private lateinit var codeInput: EditText
        private lateinit var sendBtn: Button
        private lateinit var confirmBtn: Button
        private lateinit var timer: CountDownTimer
        private var remainingSeconds = 60
        private var currentPhoneNumber = ""

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_sms_login, container, false)

            phoneInput = view.findViewById(R.id.phoneInput)
            codeInput = view.findViewById(R.id.codeInput)
            sendBtn = view.findViewById(R.id.sendBtn)
            confirmBtn = view.findViewById(R.id.confirmBtn)

            phoneInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    sendBtn.isEnabled = s?.length == 11
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            sendBtn.setOnClickListener {
                currentPhoneNumber = phoneInput.text.toString()
                (activity as LoginActivity).startGeeTest(currentPhoneNumber)
            }

            confirmBtn.setOnClickListener {
                val code = codeInput.text.toString()
                if (code.isEmpty()) {
                    (activity as LoginActivity).showToast("请输入验证码")
                    return@setOnClickListener
                }
                loginBySms(currentPhoneNumber, code)
            }

            return view
        }

        override fun onGeeTestResult(geeTestData: String, phoneNumber: String) {
            sendSmsCode(phoneNumber, geeTestData)
        }

        private fun sendSmsCode(phoneNumber: String, geeTestData: String) {
            GlobalScope.launch(Dispatchers.IO) {
                val result = KuRoApi.getSmsCode(phoneNumber, geeTestData)
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        (activity as LoginActivity).showToast("验证码已发送")
                        startTimer()
                    } else {
                        (activity as LoginActivity).showToast("发送失败")
                    }
                }
            }
        }

        private fun loginBySms(phoneNumber: String, code: String) {
            GlobalScope.launch(Dispatchers.IO) {
                val result = KuRoApi.loginByMobileCaptcha(phoneNumber, code)
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        try {
                            val json = Gson().fromJson(result, JsonObject::class.java)
                            val token = json.getAsJsonObject("data").getAsJsonPrimitive("token").asString
                            val uid = json.getAsJsonObject("data").getAsJsonPrimitive("uid").asString
                            val name = json.getAsJsonObject("data").getAsJsonPrimitive("name").asString
                            (activity as LoginActivity).onLoginSuccess(name, token, uid, phoneNumber)
                        } catch (e: Exception) {
                            (activity as LoginActivity).showToast("登录失败")
                        }
                    } else {
                        (activity as LoginActivity).showToast("登录失败")
                    }
                }
            }
        }

        private fun startTimer() {
            sendBtn.isEnabled = false
            timer = object : CountDownTimer(60000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingSeconds = (millisUntilFinished / 1000).toInt()
                    sendBtn.text = "$remainingSeconds秒后重发"
                }
                override fun onFinish() {
                    sendBtn.isEnabled = true
                    sendBtn.text = "发送验证码"
                    remainingSeconds = 60
                }
            }
            timer.start()
        }
    }

    class QrCodeLoginFragment : Fragment(), LoginFragment {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_qrcode_login, container, false)
            view.findViewById<Button>(R.id.refreshBtn).setOnClickListener {
                (activity as LoginActivity).showToast("功能开发中")
            }
            return view
        }

        override fun onGeeTestResult(geeTestData: String, phoneNumber: String) {}
    }

    class CookieLoginFragment : Fragment(), LoginFragment {
        private lateinit var cookieInput: EditText
        private lateinit var loginBtn: Button

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_cookie_login, container, false)
            cookieInput = view.findViewById(R.id.cookieInput)
            loginBtn = view.findViewById(R.id.loginBtn)

            loginBtn.setOnClickListener {
                (activity as LoginActivity).showToast("功能开发中")
            }

            return view
        }

        override fun onGeeTestResult(geeTestData: String, phoneNumber: String) {}
    }
}