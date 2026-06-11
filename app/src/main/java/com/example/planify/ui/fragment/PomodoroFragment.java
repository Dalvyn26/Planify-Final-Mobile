package com.example.planify.ui.fragment;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.planify.R;
import com.example.planify.data.local.entity.Task;
import com.example.planify.databinding.FragmentPomodoroBinding;
import com.example.planify.ui.viewmodel.CalendarViewModel;
import com.example.planify.ui.viewmodel.PomodoroViewModel;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.utils.Constants;
import com.example.planify.utils.SoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PomodoroFragment extends Fragment {
    private FragmentPomodoroBinding binding;
    private PomodoroViewModel pomodoroViewModel;
    private TaskViewModel taskViewModel;
    private android.app.Dialog activeDialog;

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private final ActivityResultLauncher<String> notificationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    // Permission granted, start the timer now
                    SoundManager.getInstance().playPomodoroStart();
                    pomodoroViewModel.startTimer();
                } else {
                    Toast.makeText(requireContext(), "Notification permission needed for timer", Toast.LENGTH_SHORT).show();
                    // Still start the timer, just no notification
                    SoundManager.getInstance().playPomodoroStart();
                    pomodoroViewModel.startTimer();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPomodoroBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Activity scope to persist timer across tabs
        pomodoroViewModel = new ViewModelProvider(requireActivity()).get(PomodoroViewModel.class);
        // Activity scope so TaskViewModel is shared with HomeFragment for live stats
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        // ALWAYS check arguments first
        if (getArguments() != null && getArguments().containsKey("selectedTaskId")) {
            int argId = getArguments().getInt("selectedTaskId", -1);
            if (argId != -1) {
                pomodoroViewModel.selectedTaskId.setValue(argId);
            }
        }

        setupActiveTask();
        setupControls();
        setupTimerObservers();
        
        pomodoroViewModel.setOnTimerFinishedListener(() -> {
            if (isAdded()) onTimerFinished();
        });

        updateSessionProgress();
    }

    private void setupActiveTask() {
        // Observe selectedTaskId from PomodoroViewModel
        pomodoroViewModel.selectedTaskId.observe(getViewLifecycleOwner(), taskId -> {
            if (binding == null) return;
            
            if (taskId == null || taskId == -1) {
                showNoTaskState();
            } else {
                // Fetch the task detail by ID
                taskViewModel.getTaskById(taskId).observe(getViewLifecycleOwner(), task -> {
                    if (task != null) {
                        showTaskDetails(task);
                    } else {
                        showNoTaskState();
                    }
                });
            }
        });

        // Click on the whole card to select a task
        binding.layoutActiveTask.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            showTaskSelectionDialog();
        });
    }

    private void showNoTaskState() {
        binding.tvActiveTaskTitle.setText("SELECT A TASK");
        binding.tvActiveTaskDesc.setText("Click here to choose what to work on.");
        binding.tvActiveTaskPriority.setVisibility(View.GONE);
        binding.cbTaskActive.setVisibility(View.GONE);
        binding.cbTaskActive.setEnabled(false);
    }

    private void showTaskDetails(Task task) {
        binding.tvActiveTaskTitle.setText(task.title);
        binding.tvActiveTaskDesc.setText(task.description != null && !task.description.isEmpty() 
            ? task.description : "No description");
        
        // Priority Level UI
        binding.tvActiveTaskPriority.setVisibility(View.VISIBLE);
        switch (task.priority) {
            case 2: // High
                binding.tvActiveTaskPriority.setText("HIGH PRIORITY");
                binding.tvActiveTaskPriority.setBackgroundColor(getResources().getColor(R.color.priority_high, null));
                break;
            case 1: // Medium
                binding.tvActiveTaskPriority.setText("MEDIUM PRIORITY");
                binding.tvActiveTaskPriority.setBackgroundColor(getResources().getColor(R.color.priority_medium, null));
                break;
            default: // Low
                binding.tvActiveTaskPriority.setText("LOW PRIORITY");
                binding.tvActiveTaskPriority.setBackgroundColor(getResources().getColor(R.color.priority_low, null));
                break;
        }

        binding.cbTaskActive.setVisibility(View.VISIBLE);
        binding.cbTaskActive.setEnabled(true);
        binding.cbTaskActive.setChecked(false);
        binding.cbTaskActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                taskViewModel.completeTask(task);
                SoundManager.getInstance().playTaskComplete();
                
                // Delete from Google Calendar if exists
                CalendarViewModel calendarViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);
                if (task.calendarEventId != null) {
                    calendarViewModel.deleteEvent(task.calendarEventId, new com.example.planify.data.remote.CalendarApiService.DeleteCallback() {
                        @Override
                        public void onSuccess() {
                            calendarViewModel.refresh();
                        }
                        @Override
                        public void onError(Exception e) {}
                    });
                }
                
                pomodoroViewModel.selectedTaskId.setValue(-1); // Reset selection after completion
            }
        });
    }

    private void showTaskSelectionDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_brutalist_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvDialogMessage);
        tvTitle.setText("CHOOSE YOUR MISSION");
        tvMsg.setVisibility(View.GONE);

        LinearLayout listContainer = new LinearLayout(requireContext());
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 24, 0, 24);
        
        taskViewModel.getAllActiveTasks().observe(getViewLifecycleOwner(), tasks -> {
            listContainer.removeAllViews();
            if (tasks == null || tasks.isEmpty()) {
                TextView empty = new TextView(requireContext());
                empty.setText("No active tasks found. Create one first!");
                empty.setPadding(16, 16, 16, 16);
                empty.setTextColor(getResources().getColor(R.color.on_surface, null));
                listContainer.addView(empty);
            } else {
                for (Task t : tasks) {
                    TextView taskItem = new TextView(requireContext());
                    taskItem.setText("► " + t.title);
                    taskItem.setPadding(32, 32, 32, 32);
                    taskItem.setTextSize(14);
                    taskItem.setTextColor(getResources().getColor(R.color.on_surface, null));
                    taskItem.setBackgroundResource(R.drawable.bg_border_only);
                    taskItem.setTypeface(null, android.graphics.Typeface.BOLD);
                    
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 8, 0, 8);
                    taskItem.setLayoutParams(lp);
                    
                    taskItem.setOnClickListener(v -> {
                        SoundManager.getInstance().playClick();
                        pomodoroViewModel.selectedTaskId.setValue(t.id);
                        dialog.dismiss();
                    });
                    listContainer.addView(taskItem);
                }
            }
        });

        ViewGroup root = (ViewGroup) tvMsg.getParent();
        root.addView(listContainer, root.indexOfChild(tvMsg) + 1);
        
        dialog.findViewById(R.id.btnDialogConfirm).setVisibility(View.GONE);
        dialog.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupControls() {
        binding.btnStartPomodoro.setOnClickListener(v -> {
            PomodoroViewModel.TimerState state = pomodoroViewModel.timerState.getValue();
            if (state == PomodoroViewModel.TimerState.IDLE || state == PomodoroViewModel.TimerState.PAUSED) {
                // Check notification permission before starting
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        return;
                    }
                }
                SoundManager.getInstance().playPomodoroStart();
                pomodoroViewModel.startTimer();
            } else {
                SoundManager.getInstance().playClick();
                pomodoroViewModel.pauseTimer();
            }
        });

        binding.btnSkipPomodoro.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            showConfirmDialog("SKIP SESSION?", "Are you sure you want to skip this focus session?", () -> {
                skipSession();
            });
        });

        binding.btnResetPomodoro.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            showConfirmDialog("RESET TIMER?", "This will reset all progress for the current set. Continue?", () -> {
                pomodoroViewModel.stopTimer();
            });
        });
    }

    private void showConfirmDialog(String title, String message, Runnable onConfirm) {
        if (!isAdded()) return;
        activeDialog = new android.app.Dialog(requireContext());
        activeDialog.setContentView(R.layout.dialog_brutalist_confirm);
        if (activeDialog.getWindow() != null) {
            activeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = activeDialog.findViewById(R.id.tvDialogTitle);
        TextView tvMsg = activeDialog.findViewById(R.id.tvDialogMessage);
        TextView btnCancel = activeDialog.findViewById(R.id.btnDialogCancel);
        TextView btnConfirm = activeDialog.findViewById(R.id.btnDialogConfirm);

        tvTitle.setText(title);
        tvMsg.setText(message);

        btnCancel.setOnClickListener(v -> activeDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            activeDialog.dismiss();
            onConfirm.run();
        });

        activeDialog.show();
    }

    private void skipSession() {
        PomodoroViewModel.SessionType current = pomodoroViewModel.sessionType.getValue();
        if (current == PomodoroViewModel.SessionType.WORK) {
            Integer completed = pomodoroViewModel.completedSessions.getValue();
            pomodoroViewModel.completedSessions.setValue((completed != null ? completed : 0) + 1);
            updateSessionProgress();
            SoundManager.getInstance().playTaskComplete();
            Toast.makeText(requireContext(), "Yeay, Kamu Berhasil!, Kerja Bagus", Toast.LENGTH_SHORT).show();
            // Save pomodoro to DB for stats
            savePomodoroToDb();
        }

        // Stop service (not just pause) so it doesn't send stale time callbacks
        pomodoroViewModel.stopAndAdvance();
        advanceToNextSession();
    }

    private void setupTimerObservers() {
        pomodoroViewModel.timerState.observe(getViewLifecycleOwner(), state -> {
            if (binding == null) return;
            if (state == PomodoroViewModel.TimerState.RUNNING) {
                binding.ivPlayPauseIcon.setImageResource(R.drawable.ic_pause);
                binding.tvPlayPauseText.setText("PAUSE");
            } else {
                binding.ivPlayPauseIcon.setImageResource(R.drawable.ic_play);
                binding.tvPlayPauseText.setText("START");
            }
        });

        pomodoroViewModel.timeRemainingMs.observe(getViewLifecycleOwner(), remaining -> {
            if (binding == null || remaining == null) return;
            updateTimerDisplay(remaining);
        });

        pomodoroViewModel.sessionType.observe(getViewLifecycleOwner(), type -> {
            if (binding == null) return;
            String label;
            switch (type) {
                case SHORT_BREAK: label = "SHORT BREAK"; break;
                case LONG_BREAK: label = "LONG BREAK"; break;
                default: label = "DEEP WORK"; break;
            }
            binding.tvTag.setText(label);
            binding.tvTimerSubLabel.setText(type == PomodoroViewModel.SessionType.WORK 
                ? "Focus session in progress. Stay on task." 
                : "Time to recharge. Great job!");
        });

        pomodoroViewModel.completedSessions.observe(getViewLifecycleOwner(), count -> {
            if (binding == null) return;
            updateSessionProgress();
        });
    }

    private void onTimerFinished() {
        vibrateDevice();
        PomodoroViewModel.SessionType current = pomodoroViewModel.sessionType.getValue();

        if (current == PomodoroViewModel.SessionType.WORK) {
            Integer completed = pomodoroViewModel.completedSessions.getValue();
            pomodoroViewModel.completedSessions.setValue((completed != null ? completed : 0) + 1);
            SoundManager.getInstance().playTaskComplete();
            Toast.makeText(getContext(), "Yeay, Kamu Berhasil!, Kerja Bagus", Toast.LENGTH_SHORT).show();
            showNotification(getString(R.string.break_time));
            // KEY FIX: save completed pomodoro to DB so stats update
            savePomodoroToDb();
        } else {
            showNotification(getString(R.string.back_to_work));
        }

        advanceToNextSession();
    }

    /**
     * Increments pomodoroCount on the active task in DB AND increments the global
     * daily pomodoro counter in SharedPreferences so HomeFragment stats update.
     */
    private void savePomodoroToDb() {
        // Increment per-task pomodoro count in DB
        Integer taskId = pomodoroViewModel.selectedTaskId.getValue();
        if (taskId != null && taskId != -1) {
            taskViewModel.incrementPomodoro(taskId);
        }

        if (isAdded()) {
            android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("PlanifyPrefs", android.content.Context.MODE_PRIVATE);

            // 1. Daily counter for Home TODAY_STATS
            String todayKey = "pomodoro_today_" + java.text.SimpleDateFormat
                .getDateInstance().format(new java.util.Date());
            int todayCount = prefs.getInt(todayKey, 0);

            // 2. TOTAL counter for Profile MISSION_STATS (per user)
            int userId = prefs.getInt("user_id", 0);
            String totalKey = "total_pomodoros_" + userId;
            int totalCount = prefs.getInt(totalKey, 0);

            prefs.edit()
                .putInt(todayKey, todayCount + 1)
                .putInt(totalKey, totalCount + 1)
                .apply();
        }
    }

    private void advanceToNextSession() {
        PomodoroViewModel.SessionType current = pomodoroViewModel.sessionType.getValue();
        PomodoroViewModel.SessionType next;

        int interval = pomodoroViewModel.getInterval();
        Integer completed = pomodoroViewModel.completedSessions.getValue();
        int completedVal = (completed != null ? completed : 0);

        if (current == PomodoroViewModel.SessionType.WORK) {
            if (completedVal % interval == 0 && completedVal > 0) {
                next = PomodoroViewModel.SessionType.LONG_BREAK;
            } else {
                next = PomodoroViewModel.SessionType.SHORT_BREAK;
            }
        } else {
            next = PomodoroViewModel.SessionType.WORK;
        }

        // Set session FIRST, then reset — resetTimer() reads sessionType to get the right duration
        pomodoroViewModel.sessionType.setValue(next);
        pomodoroViewModel.resetTimer();

        // Auto-start after a brief pause for UI to update
        if (binding != null) {
            binding.getRoot().postDelayed(() -> {
                if (binding != null && isAdded()) {
                    pomodoroViewModel.startTimer();
                }
            }, 2000); // 2 seconds gives time for resetTimer block to clear
        }
    }

    private void updateTimerDisplay(long remaining) {
        int minutes = (int) (remaining / 1000) / 60;
        int seconds = (int) (remaining / 1000) % 60;
        binding.tvTimerLarge.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateSessionProgress() {
        if (binding == null) return;
        int interval = pomodoroViewModel.getInterval();
        Integer completed = pomodoroViewModel.completedSessions.getValue();
        int completedVal = (completed != null ? completed : 0);
        
        int currentInSet = completedVal % interval;
        if (currentInSet == 0 && completedVal > 0) currentInSet = interval;
        
        binding.tvSessionRatio.setText(currentInSet + "/" + interval + " POMODOROS");

        binding.segmentedProgressBar.removeAllViews();
        for (int i = 0; i < interval; i++) {
            View segment = new View(getContext());
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            params.setMargins(4, 0, 4, 0);
            segment.setLayoutParams(params);
            
            if (i < currentInSet) {
                segment.setBackgroundColor(getResources().getColor(R.color.on_surface, null));
            } else {
                segment.setBackgroundResource(R.drawable.bg_border_only);
            }
            binding.segmentedProgressBar.addView(segment);
        }
    }

    private void vibrateDevice() {
        try {
            Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
            }
        } catch (Exception e) { /* Ignore */ }
    }

    private void showNotification(String message) {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pomodoro)
                .setContentTitle("Planify Focus")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception e) { /* Ignore */ }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }
        binding = null;
    }
}
