package com.example.planify.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.planify.R;
import com.example.planify.databinding.FragmentHomeBinding;
import com.example.planify.ui.adapter.TaskAdapter;
import com.example.planify.ui.bottomsheet.AddTaskBottomSheet;
import com.example.planify.ui.viewmodel.CalendarViewModel;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.ui.viewmodel.WeatherViewModel;
import com.example.planify.utils.Constants;
import com.example.planify.utils.DateUtils;
import com.example.planify.utils.NetworkUtils;

import java.util.Calendar;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private TaskViewModel taskViewModel;
    private CalendarViewModel calendarViewModel;
    private WeatherViewModel weatherViewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        calendarViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupBlurView();
        setupGreeting();
        setupWeatherCard();
        setupTodayDate();
        setupTodayTasks();
        setupQuickStats();
        setupFab();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupGreeting();
        // Refresh pomodoro/completion stats every time we return to this screen
        setupQuickStats();
    }

    private void setupBlurView() {
        // No-op: BlurView replaced with flat retro card
    }

    private void setupGreeting() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("PlanifyPrefs", android.content.Context.MODE_PRIVATE);
        String savedName = prefs.getString("user_name", "");
        
        com.google.android.gms.auth.api.signin.GoogleSignInAccount account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(requireContext());
        
        if (!savedName.isEmpty()) {
            binding.tvDashboardUserName.setText(savedName);
        } else if (account != null && account.getDisplayName() != null) {
            binding.tvDashboardUserName.setText(account.getDisplayName());
        } else {
            binding.tvDashboardUserName.setText("COMMANDER");
        }

        // Gamified XP System - User Specific
        int userId = prefs.getInt("user_id", 0);
        int totalXp = prefs.getInt("user_xp_" + userId, 0);
        int level = (totalXp / 1000) + 1;
        int currentLevelXp = totalXp % 1000;
        
        binding.tvGreetingName.setText("LEVEL " + level);
        
        TextView tvXpProgress = binding.getRoot().findViewById(R.id.tvXpProgress);
        if (tvXpProgress != null) {
            tvXpProgress.setText("XP: " + currentLevelXp + " / 1000");
        }

        View viewXpFill = binding.getRoot().findViewById(R.id.viewXpFill);
        View viewXpEmpty = binding.getRoot().findViewById(R.id.viewXpEmpty);
        
        if (viewXpFill != null && viewXpEmpty != null) {
            LinearLayout.LayoutParams fillParams = (LinearLayout.LayoutParams) viewXpFill.getLayoutParams();
            float startWeight = fillParams.weight;
            float endWeight = currentLevelXp;

            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(startWeight, endWeight);
            animator.setDuration(800);
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                float val = (float) animation.getAnimatedValue();
                fillParams.weight = val;
                viewXpFill.setLayoutParams(fillParams);

                LinearLayout.LayoutParams emptyParams = (LinearLayout.LayoutParams) viewXpEmpty.getLayoutParams();
                emptyParams.weight = 1000f - val;
                viewXpEmpty.setLayoutParams(emptyParams);
            });
            animator.start();
        }
    }

    public void refreshXpUI() {
        setupGreeting();
    }

    private void setupWeatherCard() {
        // Request location permission for GPS-based weather
        requestLocationPermissionIfNeeded();

        weatherViewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            binding.progressWeather.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        weatherViewModel.weatherData.observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            binding.tvCityName.setText("CITY_NODE: " + (data.cityName != null ? data.cityName.toUpperCase() : Constants.DEFAULT_CITY));
            if (data.main != null) {
                binding.tvTemperature.setText(String.format("TEMP: %.0f°C", data.main.temp));
            }
            if (data.weather != null && !data.weather.isEmpty()) {
                String desc = data.weather.get(0).description;
                binding.tvWeatherDesc.setText("COND: " + desc.toUpperCase().replace(" ", "_"));
                setWeatherIcon(data.weather.get(0).id);
            }
            binding.tvWeatherError.setVisibility(View.GONE);
        });

        weatherViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.tvWeatherError.setVisibility(View.VISIBLE);
                binding.tvWeatherError.setText(error);
            } else {
                binding.tvWeatherError.setVisibility(View.GONE);
            }
        });

        binding.btnRefreshWeather.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            weatherViewModel.forceRefresh(); // Bypass cache TTL for manual refresh
        });

        weatherViewModel.fetchWeather(); // Respects cache TTL
    }

    private static final int RC_LOCATION = 1001;

    private void requestLocationPermissionIfNeeded() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, RC_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_LOCATION && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permission granted — re-fetch with GPS
            weatherViewModel.forceRefresh();
        }
    }

    private void setWeatherIcon(int weatherId) {
        int iconRes;
        if (weatherId >= 200 && weatherId < 300) {
            iconRes = R.drawable.ic_weather_storm;
        } else if (weatherId >= 300 && weatherId < 400) {
            iconRes = R.drawable.ic_weather_drizzle;
        } else if (weatherId >= 500 && weatherId < 600) {
            iconRes = R.drawable.ic_weather_rain;
        } else if (weatherId >= 600 && weatherId < 700) {
            iconRes = R.drawable.ic_weather_snow;
        } else if (weatherId >= 700 && weatherId < 800) {
            iconRes = R.drawable.ic_weather_fog;
        } else if (weatherId == 800) {
            iconRes = R.drawable.ic_weather_clear;
        } else if (weatherId == 801 || weatherId == 802) {
            iconRes = R.drawable.ic_weather_partly_cloudy;
        } else if (weatherId == 803 || weatherId == 804) {
            iconRes = R.drawable.ic_weather_cloudy;
        } else {
            iconRes = R.drawable.ic_weather_clear;
        }
        binding.ivWeatherIcon.setImageResource(iconRes);
    }

    // ==================== TODAY'S DATE ====================

    private void setupTodayDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.ENGLISH);
        binding.tvTodayDate.setText(sdf.format(new java.util.Date()).toUpperCase());
    }

    // ==================== TASKS & STATS ====================

    private void setupTodayTasks() {
        binding.rvTodayTasks.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

        taskViewModel.getActiveTasks().observe(getViewLifecycleOwner(), active -> {
            refreshXpUI(); // Ensure XP and Level update when a task is completed

            if (active == null || active.isEmpty()) {
                binding.tvEmptyToday.setVisibility(View.VISIBLE);
                binding.rvTodayTasks.setVisibility(View.GONE);
                TextView tvPendingCount = binding.getRoot().findViewById(R.id.tvPendingCount);
                if(tvPendingCount != null) tvPendingCount.setText("0 PENDING");
            } else {
                binding.tvEmptyToday.setVisibility(View.GONE);
                binding.rvTodayTasks.setVisibility(View.VISIBLE);
                
                TextView tvPendingCount = binding.getRoot().findViewById(R.id.tvPendingCount);
                if(tvPendingCount != null) tvPendingCount.setText(active.size() + " PENDING");

                // Use a compact version of TaskAdapter
                TaskAdapter adapter = new TaskAdapter(requireContext(), taskViewModel, calendarViewModel,
                    getParentFragmentManager(), false);
                adapter.setData(active, null);
                binding.rvTodayTasks.setAdapter(adapter);
            }
        });

        // COMPLETED count — observe completedTasks LiveData (uses completedAt, not dueDate)
        taskViewModel.getCompletedTasks().observe(getViewLifecycleOwner(), completedList -> {
            // Count only tasks completed today using completedAt field
            long now = System.currentTimeMillis();
            long startToday = DateUtils.getStartOfDay(now);
            long endToday = DateUtils.getEndOfDay(now);
            int todayCompleted = 0;
            if (completedList != null) {
                for (com.example.planify.data.local.entity.Task t : completedList) {
                    if (t.completedAt != null && t.completedAt >= startToday && t.completedAt <= endToday) {
                        todayCompleted++;
                    }
                }
            }
            binding.tvTodayTaskCount.setText(String.valueOf(todayCompleted));
        });
    }

    private void setupQuickStats() {
        long now = System.currentTimeMillis();
        long start = DateUtils.getStartOfDay(now);
        long end = DateUtils.getEndOfDay(now);
        // Refresh pomodoro count from DB — also check SharedPrefs as backup
        taskViewModel.getPomodorosToday(start, end, count -> {
            if (binding != null) {
                // Also add prefs-tracked count as fallback for sessions without a task
                android.content.SharedPreferences prefs = requireContext()
                    .getSharedPreferences("PlanifyPrefs", android.content.Context.MODE_PRIVATE);
                String todayKey = "pomodoro_today_" + java.text.SimpleDateFormat
                    .getDateInstance().format(new java.util.Date());
                int prefsCount = prefs.getInt(todayKey, 0);
                int finalCount = Math.max(count, prefsCount);
                binding.tvPomodoroCount.setText(String.valueOf(finalCount));
            }
        });
    }

    private void setupFab() {
        binding.fabAddTask.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            AddTaskBottomSheet sheet = AddTaskBottomSheet.newInstance(null);
            sheet.show(getParentFragmentManager(), "AddTaskBottomSheet");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
