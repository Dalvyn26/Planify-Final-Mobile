package com.example.planify.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.planify.databinding.ActivityLoginBinding;
import com.example.planify.utils.SoundManager;
import com.example.planify.data.local.AppDatabase;
import com.example.planify.data.local.UserDao;
import com.example.planify.data.local.entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private UserDao userDao;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SoundManager.init(this);
        userDao = AppDatabase.getInstance(this).userDao();
        executorService = Executors.newSingleThreadExecutor();

        binding.btnLogin.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            loginUser();
        });

        binding.tvGoToSignup.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            finish();
        });
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            SoundManager.getInstance().playInvalid();
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            SoundManager.getInstance().playInvalid();
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setEnabled(false);

        executorService.execute(() -> {
            User user = userDao.login(email, password);
            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnLogin.setEnabled(true);
                
                if (user != null) {
                    SoundManager.getInstance().playSuccessAuth();
                    // Save login state and name to preferences
                    SharedPreferences prefs = getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
                    prefs.edit()
                        .putInt("user_id", user.id)
                        .putString("user_name", user.name)
                        .putString("user_email", user.email)
                        .putBoolean("is_logged_in", true)
                        .apply();
                        
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    SoundManager.getInstance().playInvalid();
                    Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
