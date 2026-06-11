package com.example.planify.ui.activity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.planify.R;
import com.example.planify.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved dark/light theme before super.onCreate
        int themeMode = com.example.planify.utils.PreferenceManager.getInstance(this).getThemeMode();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(themeMode);

        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.example.planify.utils.SoundManager.init(this);

        // Start animation sequence
        runSplashAnimation();

        // Play Splash Sound
        android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(this, R.raw.splash_screen);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(android.media.MediaPlayer::release);
        }

        // Navigate after 3 seconds
        handler.postDelayed(this::navigateNext, 3000);
    }

    private void runSplashAnimation() {
        // ========== PHASE 1 (0ms): Logo drops in with bounce ==========
        binding.ivSplashLogo.setScaleX(0.3f);
        binding.ivSplashLogo.setScaleY(0.3f);
        binding.ivSplashLogo.setTranslationY(-60f);
        binding.ivSplashLogo.animate()
            .alpha(1f)
            .scaleX(1f).scaleY(1f)
            .translationY(0f)
            .setDuration(700)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();

        // ========== PHASE 2 (500ms): Title slides up + shimmer starts ==========
        binding.tvSplashTitle.setTranslationY(30f);
        handler.postDelayed(() -> {
            binding.tvSplashTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();

            // Start shimmer on title after it appears
            handler.postDelayed(() -> binding.shimmerTitle.startShimmer(), 300);
        }, 500);

        // ========== PHASE 3 (800ms): Divider line expands from center ==========
        handler.postDelayed(() -> {
            ValueAnimator dividerAnim = ValueAnimator.ofInt(0, dpToPx(180));
            dividerAnim.setDuration(400);
            dividerAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            dividerAnim.addUpdateListener(animation -> {
                int width = (int) animation.getAnimatedValue();
                android.view.ViewGroup.LayoutParams params = binding.viewDivider.getLayoutParams();
                params.width = width;
                binding.viewDivider.setLayoutParams(params);
            });
            dividerAnim.start();
        }, 800);

        // ========== PHASE 4 (1100ms): Subtitle fades in ==========
        binding.tvSplashSubtitle.setTranslationY(15f);
        handler.postDelayed(() -> {
            binding.tvSplashSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }, 1100);

        // ========== PHASE 5 (1400ms): Version + loading bar + credits ==========
        handler.postDelayed(() -> {
            binding.tvVersion.animate().alpha(1f).setDuration(400).start();
            binding.tvLoadingText.animate().alpha(0.7f).setDuration(400).start();
            binding.tvCredits.animate().alpha(0.6f).setDuration(600).start();

            // Start shimmer on loading bar
            binding.shimmerLoading.startShimmer();

            // Animate loading bar fill
            animateLoadingBar();
        }, 1400);

        // ========== PHASE 6 (1800ms): Loading text changes ==========
        handler.postDelayed(() -> {
            binding.tvLoadingText.setText("LOADING MODULES...");
        }, 1800);

        handler.postDelayed(() -> {
            binding.tvLoadingText.setText("SYSTEM READY ✓");
        }, 2600);
    }

    private void animateLoadingBar() {
        ValueAnimator loadAnim = ValueAnimator.ofFloat(0f, 1f);
        loadAnim.setDuration(1400);
        loadAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        loadAnim.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            int parentWidth = ((View) binding.viewLoadingFill.getParent()).getWidth();
            if (parentWidth > 0) {
                android.view.ViewGroup.LayoutParams params = binding.viewLoadingFill.getLayoutParams();
                params.width = (int) (parentWidth * progress);
                binding.viewLoadingFill.setLayoutParams(params);
            }
        });
        loadAnim.start();
    }

    private void navigateNext() {
        android.content.SharedPreferences prefs = getSharedPreferences("PlanifyPrefs", android.content.Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
        if (isLoggedIn) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
