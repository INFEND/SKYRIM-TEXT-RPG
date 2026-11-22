package dev.infend.skyrimtext;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Locale;

/**
 * Truthful, phase-based progress for asynchronous AI operations.
 * Stage N/M is a workflow milestone, never a fabricated completion percentage.
 */
final class GenerationStatusView {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final LinearLayout root;
    private final ProgressBar activity;
    private final TextView phase;
    private final TextView detail;
    private final TextView clock;
    private long startedAt;
    private boolean running;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            clock.setText("Прошло: " + duration(SystemClock.elapsedRealtime() - startedAt)
                    + " · прогресс времени неизвестен");
            ui.postDelayed(this, 1000);
        }
    };

    GenerationStatusView(Context context, int accent) {
        float density = context.getResources().getDisplayMetrics().density;
        int pad = Math.round(12 * density);
        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xff101415);
        background.setCornerRadius(14 * density);
        background.setStroke(Math.max(1, Math.round(density)), 0xff4b5557);
        root.setBackground(background);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        activity = new ProgressBar(context);
        activity.setIndeterminate(true);
        activity.setIndeterminateTintList(ColorStateList.valueOf(accent));
        header.addView(activity, new LinearLayout.LayoutParams(Math.round(28 * density),
                Math.round(28 * density)));
        phase = new TextView(context);
        phase.setTextColor(accent);
        phase.setTextSize(14);
        phase.setTypeface(null, android.graphics.Typeface.BOLD);
        phase.setPadding(Math.round(10 * density), 0, 0, 0);
        header.addView(phase, new LinearLayout.LayoutParams(0, -2, 1));

        detail = new TextView(context);
        detail.setTextColor(0xffe0e5e5);
        detail.setTextSize(12);
        detail.setPadding(0, Math.round(8 * density), 0, 0);
        root.addView(detail, new LinearLayout.LayoutParams(-1, -2));
        clock = new TextView(context);
        clock.setTextColor(0xffa5b0b2);
        clock.setTextSize(12);
        clock.setPadding(0, Math.round(7 * density), 0, 0);
        root.addView(clock, new LinearLayout.LayoutParams(-1, -2));
        root.setVisibility(View.GONE);
    }

    View view() { return root; }

    void start(int step, int total, String title, String explanation) {
        running = true;
        startedAt = SystemClock.elapsedRealtime();
        root.setVisibility(View.VISIBLE);
        activity.setVisibility(View.VISIBLE);
        report(step, total, title, explanation);
        ui.removeCallbacks(tick);
        ui.post(tick);
    }

    void report(int step, int total, String title, String explanation) {
        if (!running) return;
        phase.setText("Этап " + Math.max(1, step) + "/" + Math.max(1, total)
                + " · " + title);
        detail.setText(explanation == null ? "" : explanation);
    }

    void finish(boolean success, String title, String explanation) {
        running = false;
        ui.removeCallbacks(tick);
        activity.setVisibility(View.GONE);
        phase.setText((success ? "✓ " : "⚠ ") + title);
        detail.setText(explanation == null ? "" : explanation);
        if (startedAt > 0)
            clock.setText("Заняло: " + duration(SystemClock.elapsedRealtime() - startedAt));
    }

    void dispose() {
        running = false;
        ui.removeCallbacks(tick);
    }

    private static String duration(long elapsed) {
        long seconds = Math.max(0, elapsed / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
