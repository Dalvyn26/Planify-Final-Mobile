package com.example.planify.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.planify.databinding.ActivitySignupBinding;
import com.example.planify.utils.SoundManager;
import com.example.planify.data.local.AppDatabase;
import com.example.planify.data.local.UserDao;
import com.example.planify.data.local.entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;
    private UserDao userDao;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SoundManager.init(this);
        userDao = AppDatabase.getInstance(this).userDao();
        executorService = Executors.newSingleThreadExecutor();

        binding.btnSignup.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            registerUser();
        });

        binding.tvGoToLogin.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            SoundManager.getInstance().playInvalid();
            binding.etName.setError("Name is required");
            binding.etName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            SoundManager.getInstance().playInvalid();
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            SoundManager.getInstance().playInvalid();
            binding.etPassword.setError("Minimum password length is 6 characters");
            binding.etPassword.requestFocus();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSignup.setEnabled(false);

        executorService.execute(() -> {
            User existingUser = userDao.getUserByEmail(email);
            if (existingUser != null) {
                runOnUiThread(() -> {
                    SoundManager.getInstance().playInvalid();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSignup.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "Email is already registered", Toast.LENGTH_SHORT).show();
                });
            } else {
                User newUser = new User(name, email, password);
                long id = userDao.insertUser(newUser);

                runOnUiThread(() -> {
                    SoundManager.getInstance().playSuccessAuth();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSignup.setEnabled(true);
                    
                    // Save login state and name to preferences
                    SharedPreferences prefs = getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
                    prefs.edit()
                        .putInt("user_id", (int) id)
                        .putString("user_name", name)
                        .putString("user_email", email)
                        .putBoolean("is_logged_in", true)
                        .apply();
                    
                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                    finish();
                });
            }
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
