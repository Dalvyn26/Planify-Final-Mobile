package com.example.planify.ui.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.planify.databinding.ActivitySettingsBinding;
import com.example.planify.utils.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private PreferenceManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = PreferenceManager.getInstance(this);

        // Custom back button (matching EditProfile header style)
        binding.btnBack.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            finish();
        });

        // Dark mode switch
        int currentTheme = prefs.getThemeMode();
        binding.switchDarkMode.setChecked(currentTheme == AppCompatDelegate.MODE_NIGHT_YES);
        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            prefs.saveThemeMode(mode);
            AppCompatDelegate.setDefaultNightMode(mode);
            recreate();
        });

        // Work Duration SeekBar (15-60 min, seekbar 0-45 maps to +15)
        int workDuration = prefs.getPomoDuration();
        binding.seekWorkDuration.setProgress(workDuration - 15);
        binding.tvWorkDurationValue.setText(workDuration + " min");
        binding.seekWorkDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = progress + 15;
                binding.tvWorkDurationValue.setText(val + " min");
                prefs.savePomoDuration(val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Short Break SeekBar (3-15 min, seekbar 0-12 maps to +3)
        int shortBreak = prefs.getPomoShortBreak();
        binding.seekShortBreak.setProgress(shortBreak - 3);
        binding.tvShortBreakValue.setText(shortBreak + " min");
        binding.seekShortBreak.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = progress + 3;
                binding.tvShortBreakValue.setText(val + " min");
                prefs.savePomoShortBreak(val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Long Break SeekBar (10-30 min, seekbar 0-20 maps to +10)
        int longBreak = prefs.getPomoLongBreak();
        binding.seekLongBreak.setProgress(longBreak - 10);
        binding.tvLongBreakValue.setText(longBreak + " min");
        binding.seekLongBreak.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = progress + 10;
                binding.tvLongBreakValue.setText(val + " min");
                prefs.savePomoLongBreak(val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // NumberPicker for interval (2-6)
        binding.pickerInterval.setMinValue(2);
        binding.pickerInterval.setMaxValue(6);
        binding.pickerInterval.setValue(prefs.getPomoInterval());
        binding.pickerInterval.setOnValueChangedListener((picker, oldVal, newVal) ->
            prefs.savePomoInterval(newVal));
    }
}
