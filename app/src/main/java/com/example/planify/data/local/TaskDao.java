package com.example.planify.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.planify.data.local.entity.Task;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    LiveData<Task> getTaskById(int taskId);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isArchived = 0 AND userId = :userId ORDER BY priority DESC, createdAt DESC")
    LiveData<List<Task>> getActiveTasks(int userId);

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isArchived = 0 AND userId = :userId ORDER BY completedAt DESC")
    LiveData<List<Task>> getCompletedTasks(int userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate BETWEEN :startOfDay AND :endOfDay AND isArchived = 0")
    LiveData<List<Task>> getTasksByDate(int userId, long startOfDay, long endOfDay);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isArchived = 0 AND userId = :userId")
    LiveData<List<Task>> getAllActiveTasks(int userId);

    @Query("SELECT * FROM tasks WHERE isArchived = 1 AND userId = :userId ORDER BY createdAt DESC")
    LiveData<List<Task>> getMissionLog(int userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId")
    int getTotalTaskCount(int userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND userId = :userId")
    int getCompletedTaskCount(int userId);

    @Query("SELECT SUM(pomodoroCount) FROM tasks WHERE userId = :userId AND pomodoroCount > 0 AND (completedAt BETWEEN :startOfDay AND :endOfDay OR (isCompleted = 0 AND createdAt <= :endOfDay))")
    int getPomodorosForDay(int userId, long startOfDay, long endOfDay);

    @Query("SELECT SUM(pomodoroCount) FROM tasks WHERE userId = :userId")
    int getTotalPomodoros(int userId);

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND userId = :userId AND completedAt BETWEEN :startOfDay AND :endOfDay LIMIT 1")
    Task getCompletedTaskForDay(int userId, long startOfDay, long endOfDay);

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND userId = :userId AND completedAt BETWEEN :startOfDay AND :endOfDay")
    int getCompletedTaskCountForDay(int userId, long startOfDay, long endOfDay);

    @Query("SELECT SUM(pomodoroCount) FROM tasks WHERE userId = :userId")
    LiveData<Integer> getTotalPomodorosLive(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(Task task);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :time WHERE id = :id")
    void markCompleted(int id, long time);

    @Query("UPDATE tasks SET pomodoroCount = pomodoroCount + 1 WHERE id = :id")
    void incrementPomodoro(int id);

    @Query("UPDATE tasks SET calendarEventId = :eventId WHERE id = :id")
    void updateCalendarEventId(int id, String eventId);

    /** Returns null if no task with this Google Calendar event ID exists (used to prevent duplicates) */
    @Query("SELECT * FROM tasks WHERE calendarEventId = :eventId LIMIT 1")
    Task getTaskByCalendarEventId(String eventId);

    @Query("DELETE FROM tasks WHERE userId = :userId")
    void deleteAllTasks(int userId);
}
