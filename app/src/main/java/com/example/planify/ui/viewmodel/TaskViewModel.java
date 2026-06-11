package com.example.planify.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.planify.data.local.entity.Task;
import com.example.planify.data.repository.TaskRepository;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    private final LiveData<List<Task>> activeTasks;
    private final LiveData<List<Task>> completedTasks;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
        activeTasks = repository.getActiveTasks();
        completedTasks = repository.getCompletedTasks();
    }

    public LiveData<List<Task>> getActiveTasks() {
        return activeTasks;
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return completedTasks;
    }

    public LiveData<List<Task>> getTasksByDate(long start, long end) {
        return repository.getTasksByDate(start, end);
    }

    public LiveData<List<Task>> getAllActiveTasks() {
        return repository.getAllActiveTasks();
    }

    public LiveData<List<Task>> getMissionLog() {
        return repository.getMissionLog();
    }

    public LiveData<Task> getTaskById(int taskId) {
        return repository.getTaskById(taskId);
    }

    public void insertTask(Task task, TaskRepository.InsertCallback callback) {
        repository.insertTask(task, callback);
    }

    public void updateTask(Task task) {
        repository.updateTask(task);
    }

    public void deleteTask(Task task) {
        if (task != null) {
            com.example.planify.utils.ReminderManager.cancelReminders(getApplication(), task.id);
        }
        repository.deleteTask(task);
    }

    public void markCompleted(int taskId) {
        repository.markCompleted(taskId);
    }

    public void completeTask(Task task) {
        if (task != null) {
            repository.markCompleted(task.id);
            // Cancel all pending reminders for this task
            com.example.planify.utils.ReminderManager.cancelReminders(getApplication(), task.id);
        }
    }

    public void incrementPomodoro(int taskId) {
        repository.incrementPomodoro(taskId);
    }

    public void updateCalendarEventId(int taskId, String eventId) {
        repository.updateCalendarEventId(taskId, eventId);
    }

    public void getStats(TaskRepository.StatsCallback callback) {
        repository.getStats(callback);
    }

    public void getPomodorosToday(long start, long end, TaskRepository.IntCallback callback) {
        repository.getPomodorosToday(start, end, callback);
    }

    public void getCompletedTaskCountForDay(long start, long end, TaskRepository.IntCallback callback) {
        repository.getCompletedTaskCountForDay(start, end, callback);
    }

    public LiveData<Integer> getTotalPomodorosLive() {
        return repository.getTotalPomodorosLive();
    }

    public void getStreakDays(TaskRepository.StreakCallback callback) {
        repository.getStreakDays(callback);
    }
}
