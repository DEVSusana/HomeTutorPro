package com.devsusana.hometutorpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devsusana.hometutorpro.data.local.entities.ClassLogEntity

@Dao
interface ClassLogDao {

    @Query("SELECT * FROM class_logs WHERE studentId = :studentId AND professorId = :professorId ORDER BY date DESC")
    suspend fun getLogsByStudent(studentId: Long, professorId: String): List<ClassLogEntity>

    @Query("SELECT * FROM class_logs WHERE professorId = :professorId ORDER BY date DESC")
    suspend fun getAllLogs(professorId: String): List<ClassLogEntity>

    @Query("SELECT * FROM class_logs WHERE studentId = :studentId AND professorId = :professorId AND date = :date AND scheduleId = :scheduleId LIMIT 1")
    suspend fun getLogOccurrence(studentId: Long, professorId: String, date: Long, scheduleId: String): ClassLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ClassLogEntity): Long

    @Query("DELETE FROM class_logs WHERE studentId = :studentId AND professorId = :professorId AND date = :date AND scheduleId = :scheduleId")
    suspend fun deleteLogOccurrence(studentId: Long, professorId: String, date: Long, scheduleId: String)

    @Query("DELETE FROM class_logs WHERE id = :id AND professorId = :professorId")
    suspend fun deleteLogById(id: Long, professorId: String)
}
