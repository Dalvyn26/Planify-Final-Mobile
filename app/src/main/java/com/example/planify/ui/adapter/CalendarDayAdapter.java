package com.example.planify.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.planify.R;
import com.example.planify.data.remote.model.CalendarEvent;
import com.example.planify.utils.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {
    private final List<DayItem> days = new ArrayList<>();
    private long selectedDate;
    private List<CalendarEvent> events = new ArrayList<>();
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(long dateMs);
    }

    public CalendarDayAdapter(int year, int month, long selectedDate, OnDateClickListener listener) {
        this.selectedDate = selectedDate;
        this.listener = listener;
        buildDays(year, month);
    }

    public void updateMonth(int year, int month, long selectedDate) {
        this.selectedDate = selectedDate;
        buildDays(year, month);
        notifyDataSetChanged();
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    private void buildDays(int year, int month) {
        days.clear();
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Previous month padding
        Calendar prevCal = (Calendar) cal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int prevDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = firstDayOfWeek - 1; i >= 0; i--) {
            prevCal.set(Calendar.DAY_OF_MONTH, prevDays - i);
            days.add(new DayItem(prevCal.getTimeInMillis(), false));
        }

        // Current month days
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            days.add(new DayItem(cal.getTimeInMillis(), true));
        }

        // Next month padding
        int remaining = 42 - days.size(); // 6 rows × 7 columns
        Calendar nextCal = Calendar.getInstance();
        nextCal.set(year, month + 1, 1);
        for (int day = 1; day <= remaining; day++) {
            nextCal.set(Calendar.DAY_OF_MONTH, day);
            days.add(new DayItem(nextCal.getTimeInMillis(), false));
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayItem item = days.get(position);
        int dayNum = DateUtils.getDayOfMonth(item.dateMs);
        holder.tvDay.setText(String.valueOf(dayNum));

        boolean isToday = DateUtils.isToday(item.dateMs);
        boolean isSelected = isSameDay(item.dateMs, selectedDate);
        boolean isCurrentMonth = item.isCurrentMonth;

        Calendar dayCal = Calendar.getInstance();
        dayCal.setTimeInMillis(item.dateMs);
        boolean isSunday = dayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        boolean isHoliday = isHoliday(item.dateMs);

        // Reset backgrounds
        holder.tvDay.setBackground(null);
        holder.dayContainer.setBackground(null);

        if (isSelected) {
            holder.dayContainer.setBackground(holder.itemView.getContext()
                .getDrawable(R.drawable.bg_selected_circle));
            holder.tvDay.setTextColor(holder.itemView.getContext().getColor(R.color.on_surface));
        } else if (isToday) {
            holder.tvDay.setBackground(holder.itemView.getContext()
                .getDrawable(R.drawable.bg_today_circle));
            holder.tvDay.setTextColor(Color.WHITE);
        } else {
            int color;
            if (isSunday || (isHoliday && isCurrentMonth)) {
                color = Color.parseColor("#FF0000"); // Red for Sunday/Holiday
            } else {
                color = isCurrentMonth
                    ? holder.itemView.getContext().getColor(R.color.on_surface)
                    : holder.itemView.getContext().getColor(R.color.on_surface_muted);
            }
            holder.tvDay.setTextColor(color);
        }

        // Underscore for holidays (only if not Sunday, or as requested)
        holder.vHolidayUnderscore.setVisibility((isHoliday && !isSunday && isCurrentMonth) ? View.VISIBLE : View.GONE);

        // Dot indicators
        holder.dotContainer.removeAllViews();
        if (hasEvents(item.dateMs)) {
            int dotColor = holder.itemView.getContext().getColor(R.color.on_surface);
            addDot(holder.dotContainer, dotColor);
        }

        holder.itemView.setOnClickListener(v -> {
            long prev = selectedDate;
            selectedDate = item.dateMs;
            notifyItemChanged(getDayPosition(prev));
            notifyItemChanged(position);
            if (listener != null) listener.onDateClick(item.dateMs);
        });
    }

    private boolean isHoliday(long dateMs) {
        long start = DateUtils.getStartOfDay(dateMs);
        long end = DateUtils.getEndOfDay(dateMs);
        for (CalendarEvent e : events) {
            if (e.startTime >= start && e.startTime <= end) {
                String title = e.title.toLowerCase();
                if (title.contains("holiday") || title.contains("libur") || 
                    title.contains("idul") || title.contains("lebaran") || 
                    title.contains("nyepi") || title.contains("waisak") || 
                    title.contains("natal") || title.contains("tahun baru")) return true;
            }
        }
        return false;
    }

    private boolean isSameDay(long a, long b) {
        Calendar ca = Calendar.getInstance(); ca.setTimeInMillis(a);
        Calendar cb = Calendar.getInstance(); cb.setTimeInMillis(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
            && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private boolean hasEvents(long dateMs) {
        long start = DateUtils.getStartOfDay(dateMs);
        long end = DateUtils.getEndOfDay(dateMs);
        for (CalendarEvent e : events) {
            if (e.startTime >= start && e.startTime <= end) {
                // Don't show generic dot for holidays if we already show underline?
                // Actually, dots are for any events.
                return true;
            }
        }
        return false;
    }

    private int getDayPosition(long dateMs) {
        for (int i = 0; i < days.size(); i++) {
            if (isSameDay(days.get(i).dateMs, dateMs)) return i;
        }
        return -1;
    }

    private void addDot(LinearLayout container, int color) {
        Context ctx = container.getContext();
        View dot = new View(ctx);
        int size = (int) (6 * ctx.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(2, 0, 2, 0);
        dot.setLayoutParams(params);
        dot.setBackgroundColor(color);
        container.addView(dot);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;
        LinearLayout dotContainer;
        View vHolidayUnderscore;
        LinearLayout dayContainer;

        DayViewHolder(View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvDayNumber);
            dotContainer = v.findViewById(R.id.dotContainer);
            vHolidayUnderscore = v.findViewById(R.id.holidayUnderscore);
            dayContainer = v.findViewById(R.id.dayContainer);
        }
    }

    static class DayItem {
        long dateMs;
        boolean isCurrentMonth;
        DayItem(long dateMs, boolean isCurrentMonth) {
            this.dateMs = dateMs;
            this.isCurrentMonth = isCurrentMonth;
        }
    }
}
