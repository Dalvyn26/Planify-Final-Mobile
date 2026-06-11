package com.example.planify.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String description;
    public int priority;        // 0=Low, 1=Medium, 2=High
    public Long dueDate;        // timestamp milliseconds, nullable
    public boolean isCompleted;
    public Long completedAt;    // nullable
    public long createdAt;
    public String calendarEventId; // nullable, Google Calendar event ID
    public int pomodoroCount;   // default 0
    public boolean isArchived;  // for soft-delete (Mission Log)
    public int userId;          // ID of the owner user
}
