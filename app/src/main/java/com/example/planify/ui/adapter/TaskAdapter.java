package com.example.planify.ui.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planify.R;
import com.example.planify.data.local.entity.Task;
import com.example.planify.ui.viewmodel.CalendarViewModel;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.ui.bottomsheet.AddTaskBottomSheet;
import com.example.planify.utils.Constants;
import com.example.planify.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ACTIVE = 0;
    private static final int TYPE_COMPLETED = 1;
    private static final int TYPE_HEADER = 2;

    public static final String HEADER_ACTIVE = "HEADER_ACTIVE";
    public static final String HEADER_COMPLETED = "HEADER_COMPLETED";

    private final Context context;
    private final TaskViewModel viewModel;
    private final FragmentManager fragmentManager;
    private final boolean showHeaders;
    private final List<Object> items = new ArrayList<>();
    private int completedCount = 0;
    private int activeCount = 0;

    private final CalendarViewModel calendarViewModel;

    public TaskAdapter(Context context, TaskViewModel viewModel, CalendarViewModel calendarViewModel, FragmentManager fm, boolean showHeaders) {
        this.context = context;
        this.viewModel = viewModel;
        this.calendarViewModel = calendarViewModel;
        this.fragmentManager = fm;
        this.showHeaders = showHeaders;
    }

    public void setData(List<Task> active, List<Task> completed) {
        items.clear();
        completedCount = 0;
        activeCount = 0;
        
        if (showHeaders) {
            activeCount = active != null ? active.size() : 0;
            items.add(HEADER_ACTIVE);
        }
        
        if (active != null) items.addAll(active);
        
        if (completed != null && !completed.isEmpty()) {
            completedCount = completed.size();
            if (showHeaders) {
                items.add(HEADER_COMPLETED);
            }
            items.addAll(completed);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (HEADER_ACTIVE.equals(item) || HEADER_COMPLETED.equals(item)) {
            return TYPE_HEADER;
        }
        Task task = (Task) item;
        return task.isCompleted ? TYPE_COMPLETED : TYPE_ACTIVE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_task_header, parent, false);
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_COMPLETED) {
            View v = inflater.inflate(R.layout.item_task_completed, parent, false);
            return new CompletedViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_task_active, parent, false);
            return new ActiveViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder vh = (HeaderViewHolder) holder;
            Object item = items.get(position);
            if (HEADER_ACTIVE.equals(item)) {
                if (vh.tvSectionTitle != null) vh.tvSectionTitle.setText("ACTIVE_MISSIONS");
                if (vh.tvSectionCount != null) vh.tvSectionCount.setText(activeCount + " PENDING");
            } else if (HEADER_COMPLETED.equals(item)) {
                if (vh.tvSectionTitle != null) vh.tvSectionTitle.setText("COMPLETED_MISSIONS");
                if (vh.tvSectionCount != null) vh.tvSectionCount.setText(completedCount + " COMPLETED");
            }
            return;
        }

        Task task = (Task) items.get(position);

        if (holder instanceof ActiveViewHolder) {
            ActiveViewHolder vh = (ActiveViewHolder) holder;
            vh.tvTitle.setText(task.title);

            if (task.description != null && !task.description.isEmpty()) {
                vh.tvDesc.setVisibility(View.VISIBLE);
                vh.tvDesc.setText(task.description);
            } else {
                vh.tvDesc.setVisibility(View.GONE);
            }

            // Priority and XP tags
            String priorityText = "LOW";
            int xpValue = 100;
            int priorityColor = context.getResources().getColor(R.color.priority_low, null);
            
            if (task.priority == Constants.PRIORITY_MEDIUM) {
                priorityText = "MED";
                xpValue = 250;
                priorityColor = context.getResources().getColor(R.color.priority_medium, null);
            } else if (task.priority == Constants.PRIORITY_HIGH) {
                priorityText = "URGENT";
                xpValue = 500;
                priorityColor = context.getResources().getColor(R.color.priority_high, null);
            }
            
            vh.tvPriorityTag.setText(priorityText);
            vh.tvPriorityTag.setBackgroundTintList(android.content.res.ColorStateList.valueOf(priorityColor));
            vh.tvPriorityTag.setTextColor(context.getResources().getColor(R.color.on_surface_inverted, null));
            vh.tvXpTag.setText("XP: " + xpValue);

            // Due Date & Time display
            if (task.dueDate != null) {
                vh.tvDueDate.setVisibility(View.VISIBLE);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault());
                vh.tvDueDate.setText("🕒 DUE: " + sdf.format(new java.util.Date(task.dueDate)));
            } else {
                vh.tvDueDate.setVisibility(View.GONE);
            }

            // Item Click -> Open Edit BottomSheet
            vh.itemView.setOnClickListener(v -> {
                com.example.planify.utils.SoundManager.getInstance().playClick();
                AddTaskBottomSheet editSheet = AddTaskBottomSheet.newInstance(task);
                editSheet.show(fragmentManager, "edit_task_sheet");
            });

            // Hide description by default, reveal on long press
            vh.tvDesc.setVisibility(View.GONE);
            vh.itemView.setOnLongClickListener(v -> {
                if (task.description != null && !task.description.isEmpty()) {
                    com.example.planify.utils.SoundManager.getInstance().playClick();
                    boolean isVisible = vh.tvDesc.getVisibility() == View.VISIBLE;
                    vh.tvDesc.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                }
                return true;
            });

            final int finalXpValue = xpValue;

            // Custom Checkbox
            vh.checkbox.setImageResource(android.R.color.transparent);
            vh.checkbox.setBackgroundResource(R.drawable.bg_border_only);
            
            vh.tvTitle.setPaintFlags(vh.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            vh.tvDesc.setPaintFlags(vh.tvDesc.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            
            vh.checkbox.setOnClickListener(v -> {
                showBrutalistConfirm(
                    "MISSION COMPLETE?",
                    "Are you sure you want to mark this mission as accomplished?",
                    () -> {
                        com.example.planify.utils.SoundManager.getInstance().playSuccess();
                        vh.checkbox.setBackgroundColor(context.getColor(R.color.on_surface));
                        vh.tvTitle.setPaintFlags(vh.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        vh.tvDesc.setPaintFlags(vh.tvDesc.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        
                        // Cool animation: Pulse then fly out to the right
                        vh.itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).withEndAction(() -> {
                            vh.itemView.animate().translationX(500f).alpha(0f).setDuration(300).withEndAction(() -> {
                                vh.itemView.setTranslationX(0);
                                vh.itemView.setScaleX(1f);
                                vh.itemView.setScaleY(1f);
                                vh.itemView.setAlpha(1f); // reset for recycling
                                viewModel.markCompleted(task.id);
                                addXpToUser(finalXpValue);
                                
                                android.widget.Toast.makeText(context, "+ " + finalXpValue + " EXP GAINED!", android.widget.Toast.LENGTH_SHORT).show();

                            // Delete from Google Calendar if exists
                            if (task.calendarEventId != null && calendarViewModel != null) {
                                calendarViewModel.deleteEvent(task.calendarEventId, new com.example.planify.data.remote.CalendarApiService.DeleteCallback() {
                                    @Override
                                    public void onSuccess() {
                                        // No need to refresh — local DB already updated
                                        android.util.Log.d("TaskAdapter", "Calendar event deleted: " + task.calendarEventId);
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        android.util.Log.w("TaskAdapter", "Calendar delete failed: " + e.getMessage());
                                    }
                                });
                            }
                        }); // close second withEndAction
                        }); // close first withEndAction
                    }
                );
            });

            if (vh.btnPlay != null) {
                vh.btnPlay.setOnClickListener(v -> {
                    showBrutalistConfirm(
                        "START FOCUS?",
                        "Do you want to start a focus session for this task?",
                        () -> {
                            com.example.planify.utils.SoundManager.getInstance().playClick();
                            Bundle args = new Bundle();
                            args.putInt("selectedTaskId", task.id);
                            
                            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                                .setLaunchSingleTop(true)
                                .setRestoreState(true)
                                .setPopUpTo(R.id.homeFragment, false, true)
                                .build();

                            try {
                                Navigation.findNavController(v).navigate(R.id.pomodoroFragment, args, navOptions);
                            } catch (Exception e) { /* fallback */ }
                        }
                    );
                });
                vh.btnPlay.setVisibility(View.VISIBLE);
            }
            
            vh.itemView.setAlpha(1.0f); // Reset alpha

        } else if (holder instanceof CompletedViewHolder) {
            CompletedViewHolder vh = (CompletedViewHolder) holder;
            vh.tvTitle.setText(task.title);
            vh.tvTitle.setPaintFlags(vh.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            StringBuilder meta = new StringBuilder();
            if (task.completedAt != null) {
                meta.append("Completed ").append(DateUtils.formatDate(task.completedAt));
            }
            vh.tvMeta.setText(meta.toString());

            vh.itemView.setOnClickListener(v -> {
                com.example.planify.utils.SoundManager.getInstance().playClick();
                AddTaskBottomSheet editSheet = AddTaskBottomSheet.newInstance(task);
                editSheet.show(fragmentManager, "edit_task_sheet");
            });

            if (vh.btnDelete != null) {
                vh.btnDelete.setOnClickListener(v -> {
                    com.example.planify.utils.SoundManager.getInstance().playClick();
                    viewModel.deleteTask(task);
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void onSwipeDelete(int position, OnDeleteCallback callback) {
        if (position < 0 || position >= items.size()) return;
        Object item = items.get(position);
        if (!(item instanceof Task)) {
            notifyItemChanged(position);
            return;
        }
        Task task = (Task) item;
        items.remove(position);
        notifyItemRemoved(position);
        viewModel.deleteTask(task);
        callback.onDeleted(task);
    }

    private void showBrutalistConfirm(String title, String message, Runnable onConfirm) {
        android.app.Dialog dialog = new android.app.Dialog(context);
        dialog.setContentView(R.layout.dialog_brutalist_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvDialogMessage);
        TextView btnCancel = dialog.findViewById(R.id.btnDialogCancel);
        TextView btnConfirm = dialog.findViewById(R.id.btnDialogConfirm);

        tvTitle.setText(title);
        tvMsg.setText(message);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        dialog.show();
    }

    private void addXpToUser(int xpGained) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", 0);
        String key = "user_xp_" + userId;
        int currentXp = prefs.getInt(key, 0);
        
        int oldLevel = (currentXp / 1000) + 1;
        int newXp = currentXp + xpGained;
        int newLevel = (newXp / 1000) + 1;
        
        prefs.edit().putInt(key, newXp).apply();
        
        if (newLevel > oldLevel) {
            com.example.planify.utils.SoundManager.getInstance().playLevelUp();
            showLevelUpDialog(newLevel);
        }
    }

    private void showLevelUpDialog(int newLevel) {
        android.app.Dialog dialog = new android.app.Dialog(context);
        dialog.setContentView(R.layout.dialog_brutalist_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMsg = dialog.findViewById(R.id.tvDialogMessage);
        TextView btnConfirm = dialog.findViewById(R.id.btnDialogConfirm);
        View btnCancel = dialog.findViewById(R.id.btnDialogCancel);

        tvTitle.setText("LEVEL_UP!");
        tvMsg.setText("Congratulations, Commander!\nYou've reached LEVEL " + newLevel + ".");
        
        btnConfirm.setText("CONTINUE");
        if (btnCancel != null) btnCancel.setVisibility(View.GONE);

        btnConfirm.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    public interface OnDeleteCallback {
        void onDeleted(Task task);
    }

    static class ActiveViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvPriorityTag, tvXpTag, tvDueDate;
        ImageButton checkbox;
        ImageButton btnPlay;

        ActiveViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTaskTitle);
            tvDesc = view.findViewById(R.id.tvTaskDesc);
            tvDueDate = view.findViewById(R.id.tvDueDate);
            tvPriorityTag = view.findViewById(R.id.tvPriorityTag);
            tvXpTag = view.findViewById(R.id.tvXpTag);
            checkbox = view.findViewById(R.id.checkboxTask);
            btnPlay = view.findViewById(R.id.btnPlayPomodoro);
        }
    }

    static class CompletedViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta;
        ImageButton btnDelete;

        CompletedViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvCompletedTitle);
            tvMeta = view.findViewById(R.id.tvCompletedMeta);
            btnDelete = view.findViewById(R.id.btnDeleteCompleted);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvSectionTitle;
        TextView tvSectionCount;
        HeaderViewHolder(View view) {
            super(view);
            tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
            tvSectionCount = view.findViewById(R.id.tvSectionCount);
        }
    }
}
