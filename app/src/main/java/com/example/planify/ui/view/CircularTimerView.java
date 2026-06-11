package com.example.planify.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.planify.R;
import com.example.planify.utils.DateUtils;

public class CircularTimerView extends View {
    private Paint backgroundArcPaint;
    private Paint progressArcPaint;
    private Paint textPaint;
    private Paint labelPaint;
    private RectF arcRect;
    private float progress = 1.0f; // 0.0 - 1.0
    private long timeRemainingMs = 0;
    private String label = "WORK";

    public CircularTimerView(Context context) {
        super(context);
        init();
    }

    public CircularTimerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularTimerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int surfaceColor = getContext().getColor(R.color.on_surface);
        int mutedColor = getContext().getColor(R.color.on_surface_muted);

        backgroundArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundArcPaint.setStyle(Paint.Style.STROKE);
        backgroundArcPaint.setStrokeWidth(16f);
        backgroundArcPaint.setColor(mutedColor);
        backgroundArcPaint.setAlpha(40);
        backgroundArcPaint.setStrokeCap(Paint.Cap.ROUND);

        progressArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressArcPaint.setStyle(Paint.Style.STROKE);
        progressArcPaint.setStrokeWidth(16f);
        progressArcPaint.setColor(surfaceColor);
        progressArcPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dpToPx(56));
        textPaint.setColor(surfaceColor);
        textPaint.setFakeBoldText(true);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(dpToPx(14));
        labelPaint.setColor(mutedColor);
        labelPaint.setLetterSpacing(0.12f);

        arcRect = new RectF();
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    public void setTimeRemaining(long ms) {
        this.timeRemainingMs = ms;
        invalidate();
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 40f;
        arcRect.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        // Background arc
        canvas.drawArc(arcRect, -90, 360, false, backgroundArcPaint);

        // Progress arc
        float sweepAngle = 360 * progress;
        canvas.drawArc(arcRect, -90, sweepAngle, false, progressArcPaint);

        // Time text
        String timeText = DateUtils.formatMinutesSeconds(timeRemainingMs);
        float textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(timeText, cx, textY - dpToPx(10), textPaint);

        // Label
        canvas.drawText(label, cx, cy + dpToPx(30), labelPaint);
    }
}
