package com.kuroscanner.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.kuroscanner.app.R
import com.kuroscanner.app.data.Account
import com.kuroscanner.app.data.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var accountRecyclerView: RecyclerView
    private lateinit var accountAdapter: AccountAdapter
    private lateinit var addBtn: Button
    private lateinit var startScreenBtn: Button
    private lateinit var startStreamBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var autoScreenCheckBox: CheckBox
    private lateinit var autoExitCheckBox: CheckBox
    private lateinit var autoLoginCheckBox: CheckBox
    private lateinit var roomIdInput: EditText
    private lateinit var streamSourceSpinner: Spinner
    private lateinit var accountRepository: AccountRepository

    private val REQUEST_CAMERA_PERMISSION = 100
    private var selectedAccount: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        accountRepository = AccountRepository(this)

        initViews()
        loadAccounts()

        addBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, LoginActivity.RESULT_LOGIN_SUCCESS)
        }

        startScreenBtn.setOnClickListener {
            if (selectedAccount == null) {
                Toast.makeText(this, "请先选择账户", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startCameraScan()
        }

        startStreamBtn.setOnClickListener {
            if (selectedAccount == null) {
                Toast.makeText(this, "请先选择账户", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val roomId = roomIdInput.text.toString()
            if (roomId.isEmpty()) {
                Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "直播扫描功能开发中", Toast.LENGTH_SHORT).show()
        }

        stopBtn.setOnClickListener {
            Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show()
        }

        streamSourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initViews() {
        accountRecyclerView = findViewById(R.id.accountRecyclerView)
        accountAdapter = AccountAdapter { account ->
            selectedAccount = account
            accountAdapter.notifyDataSetChanged()
        }
        accountRecyclerView.layoutManager = LinearLayoutManager(this)
        accountRecyclerView.adapter = accountAdapter

        addBtn = findViewById(R.id.addBtn)
        startScreenBtn = findViewById(R.id.startScreenBtn)
        startStreamBtn = findViewById(R.id.startStreamBtn)
        stopBtn = findViewById(R.id.stopBtn)
        autoScreenCheckBox = findViewById(R.id.autoScreenCheckBox)
        autoExitCheckBox = findViewById(R.id.autoExitCheckBox)
        autoLoginCheckBox = findViewById(R.id.autoLoginCheckBox)
        roomIdInput = findViewById(R.id.roomIdInput)
        streamSourceSpinner = findViewById(R.id.streamSourceSpinner)

        val streamSources = arrayOf("哔哩哔哩", "抖音")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, streamSources)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        streamSourceSpinner.adapter = spinnerAdapter
    }

    private fun loadAccounts() {
        GlobalScope.launch(Dispatchers.IO) {
            val accounts = accountRepository.getAllAccounts()
            withContext(Dispatchers.Main) {
                accountAdapter.setAccounts(accounts)
            }
        }
    }

    private fun startCameraScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }

        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("扫描二维码")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(false)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraScan()
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LoginActivity.RESULT_LOGIN_SUCCESS && resultCode == Activity.RESULT_OK) {
            data?.let {
                val name = it.getStringExtra("name") ?: ""
                val token = it.getStringExtra("token") ?: ""
                val uid = it.getStringExtra("uid") ?: ""
                val mobile = it.getStringExtra("mobile") ?: ""

                GlobalScope.launch(Dispatchers.IO) {
                    accountRepository.addAccount(name, uid, token, mobile)
                    withContext(Dispatchers.Main) {
                        loadAccounts()
                        Toast.makeText(this@MainActivity, "账户添加成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return
        }

        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                Toast.makeText(this, "扫描结果: ${result.contents}", Toast.LENGTH_LONG).show()
                handleQrCode(result.contents)
            } else {
                Toast.makeText(this, "扫描取消", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleQrCode(qrCode: String) {
        selectedAccount?.let { account ->
            Toast.makeText(this, "正在使用账户 ${account.name} 登录游戏", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class AccountAdapter(private val onSelect: (Account) -> Unit) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {
        private var accounts = emptyList<Account>()
        private var selectedPosition = -1

        fun setAccounts(newAccounts: List<Account>) {
            accounts = newAccounts
            selectedPosition = -1
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
            return AccountViewHolder(view)
        }

        override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
            val account = accounts[position]
            holder.name.text = account.name
            holder.mobile.text = account.mobile
            holder.note.text = if (account.note.isNotEmpty()) account.note else "无备注"

            holder.itemView.isSelected = position == selectedPosition
            holder.itemView.setBackgroundColor(if (position == selectedPosition) 
                ContextCompat.getColor(holder.itemView.context, R.color.primary) 
            else 
                ContextCompat.getColor(holder.itemView.context, android.R.color.white)
            )
            holder.name.setTextColor(if (position == selectedPosition) 
                ContextCompat.getColor(holder.itemView.context, android.R.color.white) 
            else 
                ContextCompat.getColor(holder.itemView.context, R.color.textPrimary)
            )

            holder.itemView.setOnClickListener {
                selectedPosition = position
                notifyDataSetChanged()
                onSelect(account)
            }

            holder.deleteBtn.setOnClickListener {
                Toast.makeText(holder.itemView.context, "删除功能开发中", Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount(): Int = accounts.size

        class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.accountName)
            val mobile: TextView = itemView.findViewById(R.id.accountMobile)
            val note: TextView = itemView.findViewById(R.id.accountNote)
            val deleteBtn: Button = itemView.findViewById(R.id.deleteBtn)
        }
    }
}