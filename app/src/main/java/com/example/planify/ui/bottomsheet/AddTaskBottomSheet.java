package com.example.planify.ui.bottomsheet;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.planify.R;
import com.example.planify.data.local.entity.Task;
import com.example.planify.data.remote.CalendarApiService;
import com.example.planify.databinding.BottomsheetAddTaskBinding;
import com.example.planify.ui.viewmodel.CalendarViewModel;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.utils.Constants;
import com.example.planify.utils.DateUtils;
import com.example.planify.utils.NetworkUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {
    private BottomsheetAddTaskBinding binding;
    private TaskViewModel taskViewModel;
    private CalendarViewModel calendarViewModel;
    
    private int selectedPriority = Constants.PRIORITY_MEDIUM;
    private Long selectedDueDate = null;
    private int selectedHour = 23;
    private int selectedMinute = 59;
    
    private Task taskToEdit = null;
    private boolean isEditMode = false;

    public static AddTaskBottomSheet newInstance(@Nullable Task task) {
        AddTaskBottomSheet fragment = new AddTaskBottomSheet();
        if (task != null) {
            fragment.taskToEdit = task;
            fragment.isEditMode = true;
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetAddTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        calendarViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);

        setupPriorityButtons();
        setupDueDateButton();
        setupDueTimeButton();
        setupActionButtons();

        if (isEditMode && taskToEdit != null) {
            populateTaskData();
        } else if (getArguments() != null && getArguments().containsKey("prefilledDate")) {
            selectedDueDate = getArguments().getLong("prefilledDate");
            binding.btnSetDueDate.setText(DateUtils.formatDate(selectedDueDate));
        }
    }

    private void populateTaskData() {
        binding.tvSheetTitle.setText("EDIT_MISSION");
        binding.etTaskTitle.setText(taskToEdit.title);
        binding.etTaskDescription.setText(taskToEdit.description);
        
        selectedPriority = taskToEdit.priority;
        highlightPriority(selectedPriority);
        
        if (taskToEdit.dueDate != null) {
            selectedDueDate = taskToEdit.dueDate;
            binding.btnSetDueDate.setText(DateUtils.formatDate(selectedDueDate));
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDueDate);
            selectedHour = cal.get(Calendar.HOUR_OF_DAY);
            selectedMinute = cal.get(Calendar.MINUTE);
            binding.btnSetDueTime.setText(String.format(java.util.Locale.getDefault(), "🕒 %02d:%02d", selectedHour, selectedMinute));
        }
        
        binding.btnAddTask.setText("UPDATE MISSION");
        binding.btnDeleteTask.setVisibility(View.VISIBLE);
        binding.btnDeleteTask.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            taskViewModel.deleteTask(taskToEdit);
            Toast.makeText(requireContext(), "Mission Deleted", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    private void setupPriorityButtons() {
        highlightPriority(selectedPriority);

        binding.btnPriorityLow.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            selectedPriority = Constants.PRIORITY_LOW;
            highlightPriority(Constants.PRIORITY_LOW);
        });
        binding.btnPriorityMedium.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            selectedPriority = Constants.PRIORITY_MEDIUM;
            highlightPriority(Constants.PRIORITY_MEDIUM);
        });
        binding.btnPriorityHigh.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            selectedPriority = Constants.PRIORITY_HIGH;
            highlightPriority(Constants.PRIORITY_HIGH);
        });
    }

    private void highlightPriority(int priority) {
        if (!isAdded()) return;
        int low = requireContext().getColor(R.color.priority_low);
        int med = requireContext().getColor(R.color.priority_medium);
        int high = requireContext().getColor(R.color.priority_high);
        int transparent = requireContext().getColor(R.color.transparent);
        
        binding.btnPriorityLow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(priority == Constants.PRIORITY_LOW ? low : transparent));
        binding.btnPriorityMedium.setBackgroundTintList(android.content.res.ColorStateList.valueOf(priority == Constants.PRIORITY_MEDIUM ? med : transparent));
        binding.btnPriorityHigh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(priority == Constants.PRIORITY_HIGH ? high : transparent));

        int textSelected = requireContext().getColor(R.color.on_surface_inverted);
        int textUnselected = requireContext().getColor(R.color.on_surface);

        binding.btnPriorityLow.setTextColor(priority == Constants.PRIORITY_LOW ? textSelected : textUnselected);
        binding.btnPriorityMedium.setTextColor(priority == Constants.PRIORITY_MEDIUM ? textSelected : textUnselected);
        binding.btnPriorityHigh.setTextColor(priority == Constants.PRIORITY_HIGH ? textSelected : textUnselected);
    }

    private void setupDueDateButton() {
        binding.btnSetDueDate.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            Calendar cal = Calendar.getInstance();
            if (selectedDueDate != null) cal.setTimeInMillis(selectedDueDate);
            new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, selectedHour, selectedMinute, 0);
                selectedDueDate = selected.getTimeInMillis();
                binding.btnSetDueDate.setText(DateUtils.formatDate(selectedDueDate));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupDueTimeButton() {
        binding.btnSetDueTime.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            new android.app.TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
                selectedHour = hourOfDay;
                selectedMinute = minute;
                binding.btnSetDueTime.setText(String.format(java.util.Locale.getDefault(), "🕒 %02d:%02d", selectedHour, selectedMinute));
                
                if (selectedDueDate != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(selectedDueDate);
                    cal.set(Calendar.HOUR_OF_DAY, selectedHour);
                    cal.set(Calendar.MINUTE, selectedMinute);
                    cal.set(Calendar.SECOND, 0);
                    selectedDueDate = cal.getTimeInMillis();
                }
            }, selectedHour, selectedMinute, true).show();
        });
    }

    private void setupActionButtons() {
        binding.btnAddTask.setOnClickListener(v -> {
            String title = binding.etTaskTitle.getText() != null ? binding.etTaskTitle.getText().toString().trim() : "";
            if (title.isEmpty()) {
                binding.etTaskTitle.setError(getString(R.string.title_required));
                return;
            }

            if (selectedDueDate != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(selectedDueDate);
                cal.set(Calendar.HOUR_OF_DAY, selectedHour);
                cal.set(Calendar.MINUTE, selectedMinute);
                cal.set(Calendar.SECOND, 0);
                selectedDueDate = cal.getTimeInMillis();
            }

            if (isEditMode && taskToEdit != null) {
                // UPDATE LOGIC
                taskToEdit.title = title;
                taskToEdit.description = binding.etTaskDescription.getText() != null ? binding.etTaskDescription.getText().toString().trim() : "";
                taskToEdit.priority = selectedPriority;
                taskToEdit.dueDate = selectedDueDate;
                
                taskViewModel.updateTask(taskToEdit);
                
                // Update Reminders (cancel old, set new)
                com.example.planify.utils.ReminderManager.cancelReminders(requireContext(), taskToEdit.id);
                if (taskToEdit.dueDate != null && taskToEdit.dueDate > System.currentTimeMillis()) {
                    com.example.planify.utils.ReminderManager.scheduleReminders(requireContext(), taskToEdit);
                }
                
                Toast.makeText(requireContext(), "Mission Updated", Toast.LENGTH_SHORT).show();
            } else {
                // ADD LOGIC
                Task task = new Task();
                task.title = title;
                task.description = binding.etTaskDescription.getText() != null ? binding.etTaskDescription.getText().toString().trim() : "";
                task.priority = selectedPriority;
                task.dueDate = selectedDueDate;
                task.isCompleted = false;
                task.createdAt = System.currentTimeMillis();
                task.pomodoroCount = 0;

                taskViewModel.insertTask(task, taskId -> {
                    task.id = taskId;
                    if (task.dueDate != null && task.dueDate > System.currentTimeMillis()) {
                        com.example.planify.utils.ReminderManager.scheduleReminders(requireContext(), task);
                    }
                    // Google Calendar integration — only if signed in
                    com.google.android.gms.auth.api.signin.GoogleSignInAccount gAccount =
                        com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(requireContext());
                    if (selectedDueDate != null && gAccount != null && NetworkUtils.isConnected(requireContext())) {
                        calendarViewModel.createEvent(task.title, task.description, selectedDueDate - 3600000, selectedDueDate,
                            new CalendarApiService.CreateEventCallback() {
                                @Override public void onSuccess(String eventId) { taskViewModel.updateCalendarEventId(taskId, eventId); }
                                @Override public void onError(Exception e) {
                                    android.util.Log.w("AddTaskBottomSheet", "Calendar sync failed: " + e.getMessage());
                                }
                            });
                    }
                });
            }
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
