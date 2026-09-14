package de.albiladi.todoappfunctions.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getById(taskId: Long): Task?

    @Query(
        """
        SELECT * FROM tasks
        ORDER BY date ASC, time ASC
        """
    )
    fun observeAll(): Flow<List<Task>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE date BETWEEN :fromDate AND :toDate
          AND isCompleted = :completed
        ORDER BY date ASC, time ASC
        """
    )
    fun observeForPeriod(
        fromDate: String,
        toDate: String,
        completed: Boolean
    ): Flow<List<Task>>

    @Query(
        """
        UPDATE tasks
        SET isCompleted = :completed
        WHERE id = :taskId
        """
    )
    suspend fun setCompleted(
        taskId: Long,
        completed: Boolean
    ): Int

    @Query(
        """
    SELECT * FROM tasks
    WHERE isCompleted = 0
    ORDER BY
        CASE WHEN date IS NULL THEN 1 ELSE 0 END,
        date,
        time
    """
    )
    suspend fun getAllOpenTasks(): List<Task>
}