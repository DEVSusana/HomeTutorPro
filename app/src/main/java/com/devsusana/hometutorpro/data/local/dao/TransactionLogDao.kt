package com.devsusana.hometutorpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devsusana.hometutorpro.data.local.entities.TransactionLogEntity

@Dao
interface TransactionLogDao {

    @Query("SELECT * FROM transaction_logs WHERE studentId = :studentId AND professorId = :professorId ORDER BY timestamp DESC")
    suspend fun getTransactionsByStudent(studentId: Long, professorId: String): List<TransactionLogEntity>

    @Query("SELECT * FROM transaction_logs WHERE professorId = :professorId ORDER BY timestamp DESC")
    suspend fun getAllTransactions(professorId: String): List<TransactionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionLogEntity): Long

    @Query("DELETE FROM transaction_logs WHERE id = :id AND professorId = :professorId")
    suspend fun deleteTransactionById(id: Long, professorId: String)
}
