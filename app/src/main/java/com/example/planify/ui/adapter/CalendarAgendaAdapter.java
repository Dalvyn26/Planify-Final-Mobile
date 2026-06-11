package com.example.planify.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planify.R;
import com.example.planify.data.local.entity.Task;
import com.example.planify.data.remote.model.CalendarEvent;
import com.example.planify.ui.bottomsheet.AddEventBottomSheet;
import com.example.planify.ui.bottomsheet.AddTaskBottomSheet;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.example.planify.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class CalendarAgendaAdapter extends RecyclerView.Adapter<CalendarAgendaAdapter.AgendaViewHolder> {
    private final Context context;
    private final TaskViewModel taskViewModel;
    private final FragmentManager fragmentManager;
    private final List<Object> items = new ArrayList<>();

    public CalendarAgendaAdapter(Context context, TaskViewModel taskViewModel, FragmentManager fragmentManager) {
        this.context = context;
        this.taskViewModel = taskViewModel;
        this.fragmentManager = fragmentManager;
    }

    public void setData(List<Object> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AgendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_agenda, parent, false);
        return new AgendaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AgendaViewHolder holder, int position) {
        Object item = items.get(position);
        if (item instanceof Task) {
            Task task = (Task) item;
            holder.tvIcon.setText("✓");
            holder.tvTitle.setText(task.title);
            holder.tvTime.setText(task.dueDate != null ? DateUtils.formatTime(task.dueDate) : "");
            int dotColor;
            switch (task.priority) {
                case 2: dotColor = context.getColor(R.color.priority_high); break;
                case 1: dotColor = context.getColor(R.color.priority_medium); break;
                default: dotColor = context.getColor(R.color.priority_low); break;
            }
            holder.priorityDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(dotColor));
            holder.priorityDot.setVisibility(View.VISIBLE);

            // Show "TASK" badge
            if (holder.tvPriority != null) {
                holder.tvPriority.setText("TASK");
                holder.tvPriority.setVisibility(View.VISIBLE);
            }
            
            holder.itemView.setOnClickListener(v -> {
                com.example.planify.utils.SoundManager.getInstance().playClick();
                AddTaskBottomSheet editSheet = AddTaskBottomSheet.newInstance(task);
                editSheet.show(fragmentManager, "edit_task_sheet");
            });
        } else if (item instanceof CalendarEvent) {
            CalendarEvent event = (CalendarEvent) item;
            holder.tvIcon.setText("📅");
            holder.tvTitle.setText(event.title);
            String timeStr = DateUtils.formatTime(event.startTime)
                + " – " + DateUtils.formatTime(event.endTime);
            holder.tvTime.setText(timeStr);
            holder.priorityDot.setVisibility(View.GONE);

            // Show "EVENT" badge
            if (holder.tvPriority != null) {
                holder.tvPriority.setText("EVENT");
                holder.tvPriority.setVisibility(View.VISIBLE);
            }

            // Click to edit/delete calendar event
            holder.itemView.setOnClickListener(v -> {
                com.example.planify.utils.SoundManager.getInstance().playClick();
                AddEventBottomSheet editSheet = AddEventBottomSheet.newEditInstance(event);
                editSheet.show(fragmentManager, "edit_event_sheet");
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AgendaViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvTime, tvPriority;
        View priorityDot;

        AgendaViewHolder(View view) {
            super(view);
            tvIcon = view.findViewById(R.id.tvAgendaIcon);
            tvTitle = view.findViewById(R.id.tvAgendaTitle);
            tvTime = view.findViewById(R.id.tvAgendaTime);
            priorityDot = view.findViewById(R.id.priorityIndicator);
            tvPriority = view.findViewById(R.id.tvAgendaPriority);
        }
    }
}
