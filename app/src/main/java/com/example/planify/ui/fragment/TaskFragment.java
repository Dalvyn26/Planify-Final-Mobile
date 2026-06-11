package com.example.planify.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planify.databinding.FragmentTaskBinding;
import com.example.planify.ui.adapter.TaskAdapter;
import com.example.planify.ui.bottomsheet.AddTaskBottomSheet;
import com.example.planify.ui.viewmodel.CalendarViewModel;
import com.example.planify.ui.viewmodel.PomodoroViewModel;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.utils.NetworkUtils;
import com.google.android.material.snackbar.Snackbar;

public class TaskFragment extends Fragment {
    private FragmentTaskBinding binding;
    private TaskViewModel taskViewModel;
    private CalendarViewModel calendarViewModel;
    private TaskAdapter activeAdapter;
    private TaskAdapter completedAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        calendarViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup offline banner
        updateOfflineBanner();
        binding.tvRetry.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            updateOfflineBanner();
        });

        // Setup Active RecyclerView
        activeAdapter = new TaskAdapter(requireContext(), taskViewModel, calendarViewModel, getParentFragmentManager(), false);
        binding.rvActiveTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvActiveTasks.setAdapter(activeAdapter);

        // Setup Completed RecyclerView
        completedAdapter = new TaskAdapter(requireContext(), taskViewModel, calendarViewModel, getParentFragmentManager(), false);
        binding.rvCompletedTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCompletedTasks.setAdapter(completedAdapter);

        // Observe both active and completed tasks
        taskViewModel.getActiveTasks().observe(getViewLifecycleOwner(), activeTasks -> {
            activeAdapter.setData(activeTasks, null);
            int activeCount = activeTasks != null ? activeTasks.size() : 0;
            binding.tvPendingCount.setText(activeCount + " PENDING");
            binding.containerActive.setVisibility(activeCount > 0 ? View.VISIBLE : View.GONE);
            updateEmptyState();
        });

        taskViewModel.getCompletedTasks().observe(getViewLifecycleOwner(), completedTasks -> {
            completedAdapter.setData(null, completedTasks);
            int compCount = completedTasks != null ? completedTasks.size() : 0;
            binding.tvCompletedCount.setText(compCount + " COMPLETED");
            binding.containerCompleted.setVisibility(compCount > 0 ? View.VISIBLE : View.GONE);
            updateEmptyState();
        });

        // Swipe to delete for Active
        ItemTouchHelper activeHelper = createSwipeHelper(activeAdapter);
        activeHelper.attachToRecyclerView(binding.rvActiveTasks);

        // Swipe to delete for Completed
        ItemTouchHelper completedHelper = createSwipeHelper(completedAdapter);
        completedHelper.attachToRecyclerView(binding.rvCompletedTasks);

        // FAB
        binding.fabAddTask.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            AddTaskBottomSheet sheet = AddTaskBottomSheet.newInstance(null);
            sheet.show(getParentFragmentManager(), "AddTaskBottomSheet");
        });
    }

    private void updateEmptyState() {
        boolean activeEmpty = activeAdapter.getItemCount() == 0;
        boolean compEmpty = completedAdapter.getItemCount() == 0;
        binding.tvEmptyTasks.setVisibility(activeEmpty && compEmpty ? View.VISIBLE : View.GONE);
    }

    private ItemTouchHelper createSwipeHelper(TaskAdapter adapter) {
        return new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                      @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int pos = viewHolder.getAdapterPosition();
                    adapter.onSwipeDelete(pos, deletedTask -> {
                        Snackbar.make(binding.getRoot(), "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", v -> {
                                deletedTask.isArchived = false;
                                taskViewModel.updateTask(deletedTask);
                            })
                            .show();
                    });
                }
            });
    }

    private void updateOfflineBanner() {
        boolean connected = NetworkUtils.isConnected(requireContext());
        binding.offlineBanner.setVisibility(connected ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
