package com.example.planify.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.planify.R;
import com.example.planify.data.local.entity.Task;
import com.example.planify.data.remote.model.CalendarEvent;
import com.example.planify.databinding.FragmentCalendarBinding;
import com.example.planify.ui.adapter.CalendarAgendaAdapter;
import com.example.planify.ui.adapter.CalendarDayAdapter;
import com.example.planify.ui.bottomsheet.AddEventBottomSheet;
import com.example.planify.ui.bottomsheet.AddTaskBottomSheet;
import com.example.planify.ui.viewmodel.CalendarViewModel;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.utils.DateUtils;
import com.example.planify.utils.NetworkUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.calendar.CalendarScopes;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarFragment extends Fragment {
    private FragmentCalendarBinding binding;
    private CalendarViewModel calendarViewModel;
    private TaskViewModel taskViewModel;
    private CalendarDayAdapter dayAdapter;
    private CalendarAgendaAdapter agendaAdapter;

    private int currentYear;
    private int currentMonth; // 0-11
    private long selectedDate;
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_RECOVER = 9002;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarViewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        Calendar cal = Calendar.getInstance();
        currentYear = cal.get(Calendar.YEAR);
        currentMonth = cal.get(Calendar.MONTH);
        selectedDate = cal.getTimeInMillis();

        calendarViewModel.error.observe(getViewLifecycleOwner(), e -> {
            if (e != null) {
                if (e instanceof com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                    startActivityForResult(((com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) e).getIntent(), RC_RECOVER);
                } else {
                    String msg = e.getMessage();
                    // Suppress expected non-errors (user hasn't connected Google Calendar yet)
                    if (msg != null && (msg.contains("Not signed in") || msg.contains("not signed in"))) {
                        return; // Normal state, not an error
                    }
                    Toast.makeText(requireContext(), "Calendar Error: " + msg, Toast.LENGTH_LONG).show();
                }
            }
        });

        binding.swipeRefreshCalendar.setOnRefreshListener(() -> {
            calendarViewModel.refresh();
        });
        calendarViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefreshCalendar.setRefreshing(loading);
        });

        setupCalendarGrid();
        setupDailyAgenda();
        setupMonthNavigation();
        setupFab();
        setupOfflineBanner();

        calendarViewModel.monthEvents.observe(getViewLifecycleOwner(), events -> {
            dayAdapter.setEvents(events);
            updateDailyAgenda();
        });

        loadMonth();
    }

    private void setupCalendarGrid() {
        dayAdapter = new CalendarDayAdapter(currentYear, currentMonth, selectedDate, date -> {
            selectedDate = date;
            updateDailyAgenda();
            binding.tvSelectedDate.setText(DateUtils.formatFullDate(date));
        });
        binding.rvCalendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.rvCalendarGrid.setAdapter(dayAdapter);
    }

    private void setupDailyAgenda() {
        agendaAdapter = new CalendarAgendaAdapter(requireContext(), taskViewModel, getChildFragmentManager());
        binding.rvDailyAgenda.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDailyAgenda.setAdapter(agendaAdapter);
        binding.tvSelectedDate.setText(DateUtils.formatFullDate(selectedDate));
        updateDailyAgenda();
    }

    private void setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            currentMonth--;
            if (currentMonth < 0) { currentMonth = 11; currentYear--; }
            loadMonth();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            currentMonth++;
            if (currentMonth > 11) { currentMonth = 0; currentYear++; }
            loadMonth();
        });

        binding.btnOpenGoogleCalendar.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage("com.google.android.calendar");
            if (intent != null) {
                startActivity(intent);
            } else {
                // Fallback to web calendar
                android.widget.Toast.makeText(requireContext(), "Opening Calendar ...", android.widget.Toast.LENGTH_SHORT).show();
                Intent webIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://calendar.google.com"));
                startActivity(webIntent);
            }
        });
    }

    private void setupFab() {
        binding.fabAddCalendar.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            AddEventBottomSheet eventSheet = AddEventBottomSheet.newInstance(selectedDate);
            eventSheet.show(getParentFragmentManager(), "AddEventBottomSheet");
        });
    }

    private void setupOfflineBanner() {
        calendarViewModel.isOffline.observe(getViewLifecycleOwner(), offline -> {
            binding.offlineBanner.setVisibility(offline ? View.VISIBLE : View.GONE);
            
            // Check if specifically not signed in
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account == null) {
                binding.tvOfflineMsg.setText("NOT CONNECTED TO GOOGLE CALENDAR");
                binding.tvRefreshCalendar.setText("[ CONNECT ]");
            } else {
                binding.tvOfflineMsg.setText(requireContext().getString(R.string.offline_banner));
                binding.tvRefreshCalendar.setText("[ REFRESH ]");
            }
        });

        binding.tvRefreshCalendar.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account == null) {
                signInWithGoogle();
            } else {
                loadMonth();
            }
        });
    }

    private void signInWithGoogle() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR))
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(requireActivity(), gso);
        startActivityForResult(client.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                task.getResult(ApiException.class);
                loadMonth(); // Refresh calendar view

                // Trigger initial import — imports all existing Google Calendar events as Tasks
                new com.example.planify.utils.CalendarSyncManager(requireContext())
                        .runInitialImportIfNeeded(new com.example.planify.utils.CalendarSyncManager.SyncCallback() {
                            @Override
                            public void onSyncComplete(int imported) {
                                if (!isAdded()) return;
                                if (imported > 0) {
                                    Toast.makeText(requireContext(),
                                            "✓ " + imported + " Google Calendar events imported as tasks!",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(requireContext(),
                                            "Google Calendar connected. No new tasks to import.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onSyncError(String message) {
                                // Silently fail — user is signed in, calendar just failed
                                if (isAdded()) {
                                    Toast.makeText(requireContext(),
                                            "Connected but import failed: " + message,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            } catch (ApiException e) {
                Toast.makeText(requireContext(), "Sign in failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == RC_RECOVER) {
            if (resultCode == getActivity().RESULT_OK) {
                loadMonth();
            } else {
                Toast.makeText(requireContext(), "Authorization failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMonth() {
        binding.tvMonthYear.setText(DateUtils.getMonthYearLabel(currentYear, currentMonth));
        dayAdapter.updateMonth(currentYear, currentMonth, selectedDate);
        calendarViewModel.loadMonthEvents(currentYear, currentMonth);
    }

    private void updateDailyAgenda() {
        long start = com.example.planify.utils.DateUtils.getStartOfDay(selectedDate);
        long end = com.example.planify.utils.DateUtils.getEndOfDay(selectedDate);

        taskViewModel.getTasksByDate(start, end).observe(getViewLifecycleOwner(), tasks -> {
            List<Object> agendaItems = new ArrayList<>();
            java.util.Set<String> syncedEventIds = new java.util.HashSet<>();
            
            if (tasks != null) {
                for (com.example.planify.data.local.entity.Task t : tasks) {
                    // Only show if NOT completed in the agenda
                    if (!t.isCompleted) {
                        agendaItems.add(t);
                        if (t.calendarEventId != null) {
                            syncedEventIds.add(t.calendarEventId);
                        }
                    }
                }
            }

            if (calendarViewModel.monthEvents.getValue() != null) {
                for (com.example.planify.data.remote.model.CalendarEvent event : calendarViewModel.monthEvents.getValue()) {
                    if (event.startTime >= start && event.startTime <= end) {
                        // Deduplicate: If this Google event is already linked to an active local task, don't show it twice
                        if (!syncedEventIds.contains(event.id)) {
                            agendaItems.add(event);
                        }
                    }
                }
            }

            agendaAdapter.setData(agendaItems);
            binding.tvEmptyAgenda.setVisibility(agendaItems.isEmpty() ? View.VISIBLE : View.GONE);
            binding.rvDailyAgenda.setVisibility(agendaItems.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
