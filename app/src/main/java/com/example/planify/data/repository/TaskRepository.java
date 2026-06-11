package com.example.planify.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.planify.data.local.AppDatabase;
import com.example.planify.data.local.TaskDao;
import com.example.planify.data.local.entity.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private final TaskDao taskDao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final int currentUserId;

    public TaskRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        taskDao = db.taskDao();
        executor = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());
        
        android.content.SharedPreferences prefs = context.getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("user_id", 0);
    }

    public LiveData<List<Task>> getActiveTasks() {
        return taskDao.getActiveTasks(currentUserId);
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return taskDao.getCompletedTasks(currentUserId);
    }

    public LiveData<List<Task>> getTasksByDate(long startOfDay, long endOfDay) {
        return taskDao.getTasksByDate(currentUserId, startOfDay, endOfDay);
    }

    public LiveData<List<Task>> getAllActiveTasks() {
        return taskDao.getAllActiveTasks(currentUserId);
    }

    public LiveData<List<Task>> getMissionLog() {
        return taskDao.getMissionLog(currentUserId);
    }

    public LiveData<Task> getTaskById(int taskId) {
        return taskDao.getTaskById(taskId);
    }

    public void insertTask(Task task, InsertCallback callback) {
        task.userId = currentUserId;
        executor.execute(() -> {
            long id = taskDao.insertTask(task);
            mainHandler.post(() -> {
                if (callback != null) callback.onInserted((int) id);
            });
        });
    }

    public void updateTask(Task task) {
        executor.execute(() -> taskDao.updateTask(task));
    }

    public void deleteTask(Task task) {
        if (!task.isCompleted) {
            // Active mission: delete permanently
            executor.execute(() -> taskDao.deleteTask(task));
        } else if (task.isArchived) {
            // Already in mission log: delete permanently
            executor.execute(() -> taskDao.deleteTask(task));
        } else {
            // Completed mission from Home: move to mission log (archive)
            task.isArchived = true;
            executor.execute(() -> taskDao.updateTask(task));
        }
    }

    public void markCompleted(int taskId) {
        executor.execute(() -> taskDao.markCompleted(taskId, System.currentTimeMillis()));
    }

    public void incrementPomodoro(int taskId) {
        executor.execute(() -> taskDao.incrementPomodoro(taskId));
    }

    public void updateCalendarEventId(int taskId, String eventId) {
        executor.execute(() -> taskDao.updateCalendarEventId(taskId, eventId));
    }

    public void getStats(StatsCallback callback) {
        executor.execute(() -> {
            int total = taskDao.getTotalTaskCount(currentUserId);
            int completed = taskDao.getCompletedTaskCount(currentUserId);
            int pomodoros = taskDao.getTotalPomodoros(currentUserId);
            mainHandler.post(() -> {
                if (callback != null) callback.onStats(total, completed, pomodoros);
            });
        });
    }

    public void getPomodorosToday(long startOfDay, long endOfDay, IntCallback callback) {
        executor.execute(() -> {
            int count = taskDao.getPomodorosForDay(currentUserId, startOfDay, endOfDay);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(count);
            });
        });
    }

    public void getCompletedTaskCountForDay(long startOfDay, long endOfDay, IntCallback callback) {
        executor.execute(() -> {
            int count = taskDao.getCompletedTaskCountForDay(currentUserId, startOfDay, endOfDay);
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(count);
            });
        });
    }

    public LiveData<Integer> getTotalPomodorosLive() {
        return taskDao.getTotalPomodorosLive(currentUserId);
    }

    public void getStreakDays(StreakCallback callback) {
        executor.execute(() -> {
            // Calculate consecutive days with at least one completed task
            int streak = 0;
            long now = System.currentTimeMillis();
            long oneDayMs = 24 * 60 * 60 * 1000L;
            for (int i = 0; i < 365; i++) {
                long start = now - (i + 1) * oneDayMs;
                long end = now - i * oneDayMs;
                Task t = taskDao.getCompletedTaskForDay(currentUserId, start, end);
                if (t != null) {
                    streak++;
                } else {
                    break;
                }
            }
            int finalStreak = streak;
            mainHandler.post(() -> {
                if (callback != null) callback.onStreak(finalStreak);
            });
        });
    }

    public interface InsertCallback {
        void onInserted(int id);
    }

    public interface StatsCallback {
        void onStats(int total, int completed, int totalPomodoros);
    }

    public interface IntCallback {
        void onResult(int value);
    }

    public interface StreakCallback {
        void onStreak(int days);
    }
}
