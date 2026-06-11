package com.example.planify.ui.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.activity.OnBackPressedCallback;

import com.example.planify.R;
import com.example.planify.databinding.ActivityMainBinding;
import com.example.planify.utils.CalendarSyncManager;
import com.example.planify.utils.Constants;
import com.example.planify.utils.PreferenceManager;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private CalendarSyncManager calendarSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        int themeMode = PreferenceManager.getInstance(this).getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);

        super.onCreate(savedInstanceState);

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create notification channel
        createNotificationChannel();

        // Initialize SoundManager in background to avoid slow onStart
        new Thread(() -> {
            com.example.planify.utils.SoundManager.init(getApplicationContext());
        }).start();

        // Setup OnBackPressed for Android 13+
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController != null) {
                    int currentDest = navController.getCurrentDestination() != null
                            ? navController.getCurrentDestination().getId() : -1;
                    if (currentDest == R.id.homeFragment) {
                        moveTaskToBack(true);
                    } else {
                        setEnabled(false);
                        onBackPressed();
                        setEnabled(true);
                    }
                } else {
                    setEnabled(false);
                    onBackPressed();
                    setEnabled(true);
                }
            }
        });
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNav, navController);
            
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                com.example.planify.utils.SoundManager.getInstance().playClick();
                
                // Manually sync BottomNav selection to handle cases where navigation 
                // is triggered outside of the BottomNav clicks (like the 'Play' button).
                int destId = destination.getId();
                android.view.Menu menu = binding.bottomNav.getMenu();
                for (int i = 0; i < menu.size(); i++) {
                    android.view.MenuItem item = menu.getItem(i);
                    if (item.getItemId() == destId) {
                        item.setChecked(true);
                        break;
                    }
                }
            });
        }

        // Hide bottom nav when keyboard appears
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            binding.bottomNav.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            return insets;
        });

        // Prefetch Calendar Data (2 months back, current, 2 months ahead)
        new androidx.lifecycle.ViewModelProvider(this).get(com.example.planify.ui.viewmodel.CalendarViewModel.class).prefetchRange();

        // Initialize Calendar Sync Manager
        calendarSyncManager = new CalendarSyncManager(this);

        // Handle notification tap navigation
        handleNotificationNavigation(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start periodic sync and run an immediate delta sync on app resume
        if (calendarSyncManager != null) {
            calendarSyncManager.startPeriodicSync();
            // Immediate sync on resume (catches events added while app was in background)
            calendarSyncManager.runInitialImportIfNeeded(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (calendarSyncManager != null) {
            calendarSyncManager.stopPeriodicSync();
        }
    }

    private void createNotificationChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        // Pomodoro timer channel
        NotificationChannel pomoChannel = new NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "Planify Pomodoro",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        pomoChannel.setDescription("Pomodoro timer notifications");
        nm.createNotificationChannel(pomoChannel);

        // Task reminder channel — HIGH importance for heads-up alerts
        NotificationChannel reminderChannel = new NotificationChannel(
            Constants.REMINDER_CHANNEL_ID,
            "PLANIFY MISSION REMINDERS",
            NotificationManager.IMPORTANCE_HIGH
        );
        reminderChannel.setDescription("Deadline reminders for upcoming tasks");
        reminderChannel.enableVibration(true);
        reminderChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
        nm.createNotificationChannel(reminderChannel);
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationNavigation(intent);
    }

    private void handleNotificationNavigation(android.content.Intent intent) {
        if (intent != null && "pomodoro".equals(intent.getStringExtra("navigate_to"))) {
            if (navController != null) {
                navController.navigate(R.id.pomodoroFragment);
            }
        }
    }

    // onBackPressed is handled via Dispatcher above
}
