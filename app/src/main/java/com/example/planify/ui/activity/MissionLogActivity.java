package com.example.planify.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planify.R;
import com.example.planify.ui.adapter.TaskAdapter;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.utils.SoundManager;

public class MissionLogActivity extends AppCompatActivity {

    private TaskViewModel taskViewModel;
    private TaskAdapter logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_log);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            finish();
        });

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        com.example.planify.ui.viewmodel.CalendarViewModel calendarViewModel = new ViewModelProvider(this).get(com.example.planify.ui.viewmodel.CalendarViewModel.class);
        
        RecyclerView rvMissionLog = findViewById(R.id.rvMissionLog);
        TextView tvLogCount = findViewById(R.id.tvLogCount);
        TextView tvEmptyLog = findViewById(R.id.tvEmptyLog);

        logAdapter = new TaskAdapter(this, taskViewModel, calendarViewModel, getSupportFragmentManager(), false);
        rvMissionLog.setLayoutManager(new LinearLayoutManager(this));
        rvMissionLog.setAdapter(logAdapter);

        taskViewModel.getMissionLog().observe(this, archivedTasks -> {
            logAdapter.setData(null, archivedTasks);
            int count = archivedTasks != null ? archivedTasks.size() : 0;
            tvLogCount.setText(count + " RECORDS");
            tvEmptyLog.setVisibility(count > 0 ? View.GONE : View.VISIBLE);
        });
    }
}
