package com.example.planify.ui.bottomsheet;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.planify.data.remote.CalendarApiService;
import com.example.planify.data.remote.model.CalendarEvent;
import com.example.planify.databinding.BottomsheetAddEventBinding;
import com.example.planify.ui.viewmodel.CalendarViewModel;
import com.example.planify.utils.DateUtils;
import com.example.planify.utils.SoundManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;

/**
 * Unified bottom sheet for creating AND editing Google Calendar events.
 * UI matches the Add Task bottom sheet (without priority level).
 */
public class AddEventBottomSheet extends BottomSheetDialogFragment {
    private BottomsheetAddEventBinding binding;
    private CalendarViewModel calendarViewModel;

    // Shared state
    private long selectedDate = System.currentTimeMillis();
    private int startHour = 9, startMin = 0;
    private int endHour = 10, endMin = 0;

    // Edit mode state
    private boolean isEditMode = false;
    private String editEventId;

    /** Factory: create mode — new event on a given date */
    public static AddEventBottomSheet newInstance(long selectedDate) {
        AddEventBottomSheet sheet = new AddEventBottomSheet();
        Bundle args = new Bundle();
        args.putLong("selectedDate", selectedDate);
        sheet.setArguments(args);
        return sheet;
    }

    /** Factory: edit mode — edit an existing CalendarEvent */
    public static AddEventBottomSheet newEditInstance(CalendarEvent event) {
        AddEventBottomSheet sheet = new AddEventBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean("editMode", true);
        args.putString("eventId", event.id);
        args.putString("title", event.title);
        args.putString("description", event.description);
        args.putLong("startTime", event.startTime);
        args.putLong("endTime", event.endTime);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetAddEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        calendarViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);

        // Parse arguments
        if (getArguments() != null) {
            isEditMode = getArguments().getBoolean("editMode", false);

            if (isEditMode) {
                editEventId = getArguments().getString("eventId");
                long eventStart = getArguments().getLong("startTime", System.currentTimeMillis());
                long eventEnd = getArguments().getLong("endTime", System.currentTimeMillis() + 3600000);
                selectedDate = eventStart;

                // Pre-fill fields
                binding.etEventTitle.setText(getArguments().getString("title", ""));
                
                // Parse HTML description from Google Calendar
                String rawDesc = getArguments().getString("description", "");
                if (rawDesc.contains("<") && rawDesc.contains(">")) {
                    // Contains HTML — render it properly
                    android.text.Spanned formatted = android.text.Html.fromHtml(rawDesc, android.text.Html.FROM_HTML_MODE_COMPACT);
                    binding.etEventDescription.setText(formatted);
                    binding.etEventDescription.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                } else {
                    binding.etEventDescription.setText(rawDesc);
                }

                // Parse start/end times
                Calendar startCal = Calendar.getInstance();
                startCal.setTimeInMillis(eventStart);
                startHour = startCal.get(Calendar.HOUR_OF_DAY);
                startMin = startCal.get(Calendar.MINUTE);

                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(eventEnd);
                endHour = endCal.get(Calendar.HOUR_OF_DAY);
                endMin = endCal.get(Calendar.MINUTE);

                // Update UI for edit mode
                binding.tvSheetTitle.setText("✎ EDIT_EVENT");
                binding.headerAccent.setVisibility(View.GONE);
                binding.btnDeleteEvent.setVisibility(View.VISIBLE);
                binding.btnCreateEvent.setText("✓ SAVE_CHANGES");
            } else {
                if (getArguments().containsKey("selectedDate")) {
                    selectedDate = getArguments().getLong("selectedDate");
                }
            }
        }

        // Set initial button texts
        updateDateButton();
        updateTimeButtons();

        // Date picker button
        binding.btnSetDate.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            Calendar dateCal = Calendar.getInstance();
            dateCal.setTimeInMillis(selectedDate);
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, day);
                selectedDate = picked.getTimeInMillis();
                updateDateButton();
            },
            dateCal.get(Calendar.YEAR),
            dateCal.get(Calendar.MONTH),
            dateCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Start time picker button
        binding.btnStartTime.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            new TimePickerDialog(requireContext(), (tp, h, m) -> {
                startHour = h; startMin = m;
                updateTimeButtons();
            }, startHour, startMin, true).show();
        });

        // End time picker button
        binding.btnEndTime.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            new TimePickerDialog(requireContext(), (tp, h, m) -> {
                endHour = h; endMin = m;
                updateTimeButtons();
            }, endHour, endMin, true).show();
        });

        // Main action button (Create / Save)
        binding.btnCreateEvent.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();

            String title = binding.etEventTitle.getText() != null
                ? binding.etEventTitle.getText().toString().trim() : "";
            if (title.isEmpty()) {
                binding.etEventTitle.setError("Title is required");
                return;
            }

            Calendar startCal = Calendar.getInstance();
            startCal.setTimeInMillis(selectedDate);
            startCal.set(Calendar.HOUR_OF_DAY, startHour);
            startCal.set(Calendar.MINUTE, startMin);
            startCal.set(Calendar.SECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(selectedDate);
            endCal.set(Calendar.HOUR_OF_DAY, endHour);
            endCal.set(Calendar.MINUTE, endMin);
            endCal.set(Calendar.SECOND, 0);

            String desc = binding.etEventDescription.getText() != null
                ? binding.etEventDescription.getText().toString().trim() : "";

            binding.btnCreateEvent.setEnabled(false);

            if (isEditMode) {
                // --- EDIT MODE ---
                binding.btnCreateEvent.setText("SAVING...");
                calendarViewModel.updateEvent(editEventId, title, desc,
                    startCal.getTimeInMillis(), endCal.getTimeInMillis(),
                    new CalendarApiService.UpdateEventCallback() {
                        @Override
                        public void onSuccess() {
                            if (isAdded()) {
                                calendarViewModel.refresh();
                                Toast.makeText(requireContext(), "✓ Event updated!", Toast.LENGTH_SHORT).show();
                                dismiss();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (isAdded()) {
                                binding.btnCreateEvent.setEnabled(true);
                                binding.btnCreateEvent.setText("✓ SAVE_CHANGES");
                                Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            } else {
                // --- CREATE MODE ---
                binding.btnCreateEvent.setText("CREATING...");
                calendarViewModel.createEvent(title, desc, startCal.getTimeInMillis(),
                    endCal.getTimeInMillis(), new CalendarApiService.CreateEventCallback() {
                        @Override
                        public void onSuccess(String eventId) {
                            if (isAdded()) {
                                // Mark this event as "calendar-only" — prevent sync from importing it as Task
                                com.example.planify.utils.PreferenceManager.getInstance(requireContext())
                                    .addExcludedEventId(eventId);
                                calendarViewModel.refresh();
                                Toast.makeText(requireContext(), "✓ Event synced to Google Calendar!", Toast.LENGTH_SHORT).show();
                                dismiss();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (isAdded()) {
                                binding.btnCreateEvent.setEnabled(true);
                                binding.btnCreateEvent.setText("+ CREATE_EVENT");
                                Toast.makeText(requireContext(), "Failed to create event", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            }
        });

        // Delete button (only visible in edit mode)
        binding.btnDeleteEvent.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();

            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.setContentView(R.layout.dialog_brutalist_confirm);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            android.widget.TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
            android.widget.TextView tvMsg = dialog.findViewById(R.id.tvDialogMessage);
            android.widget.TextView btnCancel = dialog.findViewById(R.id.btnDialogCancel);
            android.widget.TextView btnConfirm = dialog.findViewById(R.id.btnDialogConfirm);

            tvTitle.setText("DELETE EVENT?");
            tvMsg.setText("This will permanently remove this event from Google Calendar.");

            btnCancel.setOnClickListener(cv -> dialog.dismiss());
            btnConfirm.setOnClickListener(cv -> {
                dialog.dismiss();
                calendarViewModel.deleteEvent(editEventId, new CalendarApiService.DeleteCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            // Clean up excluded list
                            com.example.planify.utils.PreferenceManager.getInstance(requireContext())
                                .removeExcludedEventId(editEventId);
                            calendarViewModel.refresh();
                            Toast.makeText(requireContext(), "✓ Event deleted!", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });

            dialog.show();
        });
    }

    private void updateDateButton() {
        binding.btnSetDate.setText("⊡  " + DateUtils.formatDate(selectedDate));
    }

    private void updateTimeButtons() {
        binding.btnStartTime.setText("🕒  START: " + String.format("%02d:%02d", startHour, startMin));
        binding.btnEndTime.setText("🕒  END: " + String.format("%02d:%02d", endHour, endMin));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
