package com.kuroscanner.app.data

import android.content.Context
import androidx.room.Room

class AccountRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "kuroscanner_db"
    ).build()

    private val accountDao = db.accountDao()

    suspend fun getAllAccounts(): List<Account> {
        return accountDao.getAll()
    }

    suspend fun addAccount(name: String, uid: String, token: String, mobile: String, note: String = ""): Long {
        return accountDao.insert(Account(name = name, uid = uid, token = token, mobile = mobile, note = note))
    }

    suspend fun deleteAccount(id: Long) {
        accountDao.deleteById(id)
    }

    suspend fun updateAccountNote(id: Long, note: String) {
        accountDao.findById(id)?.let { account ->
            accountDao.update(account.copy(note = note))
        }
    }

    suspend fun getAccountById(id: Long): Account? {
        return accountDao.findById(id)
    }

    suspend fun findByUid(uid: String): Account? {
        return accountDao.findByUid(uid)
    }

    suspend fun getAccountCount(): Int {
        return accountDao.count()
    }
}