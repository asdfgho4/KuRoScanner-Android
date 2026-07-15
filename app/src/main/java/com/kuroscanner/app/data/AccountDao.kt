package com.kuroscanner.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAll(): List<Account>

    @Query("SELECT * FROM accounts WHERE uid = :uid")
    suspend fun findByUid(uid: String): Account?

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun findById(id: Long): Account?

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int
}