package com.example.planify.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.planify.R;
import com.example.planify.data.local.AppDatabase;
import com.example.planify.data.local.TaskDao;
import com.example.planify.data.local.UserDao;
import com.example.planify.data.local.entity.User;
import com.example.planify.utils.SoundManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etUserName, etEmail, etPassword;
    private MaterialButton btnSaveProfile, btnCancel, btnChangeAvatar;
    private TextView btnDeleteAccount;
    private ImageView ivAvatar;
    private int currentUserId = 0;
    private UserDao userDao;
    private TaskDao taskDao;
    private ExecutorService executorService;
    private User currentUserObj;

    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && currentUserId != 0) {
                    executorService.execute(() -> {
                        String newPath = com.example.planify.utils.AvatarUtils.saveAvatarToInternalStorage(this, uri, currentUserId);
                        if (newPath != null) {
                            runOnUiThread(() -> {
                                SharedPreferences prefs = getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
                                prefs.edit().putString("user_avatar_path_" + currentUserId, newPath).apply();
                                loadAvatarImage(newPath);
                                Toast.makeText(this, "Avatar updated", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Failed to save avatar", Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        SoundManager.init(this);
        AppDatabase db = AppDatabase.getInstance(this);
        userDao = db.userDao();
        taskDao = db.taskDao();
        executorService = Executors.newSingleThreadExecutor();

        etUserName = findViewById(R.id.etUserName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        ivAvatar = findViewById(R.id.ivAvatar);
        
        TextView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                SoundManager.getInstance().playClick();
                finish();
            });
        }

        loadCurrentData();

        btnSaveProfile.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            saveProfile();
        });
        
        btnCancel.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            finish();
        });
        
        btnChangeAvatar.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            pickImageLauncher.launch("image/*");
        });
        
        btnDeleteAccount.setOnClickListener(v -> {
            SoundManager.getInstance().playClick();
            showDeleteConfirmation();
        });
    }

    private void loadCurrentData() {
        SharedPreferences prefs = getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
        String savedEmail = prefs.getString("user_email", "");
        currentUserId = prefs.getInt("user_id", 0);
        String avatarPath = prefs.getString("user_avatar_path_" + currentUserId, null);
        loadAvatarImage(avatarPath);

        if (!savedEmail.isEmpty()) {
            executorService.execute(() -> {
                currentUserObj = userDao.getUserByEmail(savedEmail);
                runOnUiThread(() -> {
                    if (currentUserObj != null) {
                        etUserName.setText(currentUserObj.name);
                        etEmail.setText(currentUserObj.email);
                        etPassword.setText(currentUserObj.password);
                    } else {
                        // If user is null but email is in SharedPreferences, data might be corrupted.
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            });
        }
    }

    private void loadAvatarImage(String path) {
        if (path != null && !path.isEmpty()) {
            android.graphics.Bitmap bitmap = com.example.planify.utils.AvatarUtils.loadAvatar(path);
            if (bitmap != null) {
                ivAvatar.setImageBitmap(bitmap);
                ivAvatar.clearColorFilter();
                return;
            }
        }
        // Fallback
        ivAvatar.setImageResource(R.drawable.ic_profile);
        ivAvatar.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.on_surface));
    }

    private void saveProfile() {
        String newName = etUserName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            SoundManager.getInstance().playInvalid();
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserObj != null) {
            if (newPassword.length() > 0 && newPassword.length() < 6) {
                SoundManager.getInstance().playInvalid();
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            
            executorService.execute(() -> {
                currentUserObj.name = newName;
                currentUserObj.email = newEmail;
                if (!newPassword.isEmpty()) {
                    currentUserObj.password = newPassword;
                }
                userDao.updateUser(currentUserObj);
                
                runOnUiThread(() -> {
                    SharedPreferences prefs = getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
                    prefs.edit()
                        .putString("user_name", newName)
                        .putString("user_email", newEmail)
                        .apply();
                    
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } else {
            Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("DELETE_ACCOUNT")
            .setMessage("WARNING: This will permanently delete your account and cannot be undone. Are you sure?")
            .setPositiveButton("DELETE", (dialog, which) -> deleteAccount())
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void deleteAccount() {
        executorService.execute(() -> {
            // Delete all user's tasks and data
            if (currentUserObj != null) {
                taskDao.deleteAllTasks(currentUserObj.id);
                userDao.deleteUser(currentUserObj);
            }
            
            runOnUiThread(() -> {
                // Clear Current Session and User Specific Data
                SharedPreferences prefs = getSharedPreferences("PlanifyPrefs", Context.MODE_PRIVATE);
                int userId = prefs.getInt("user_id", 0);
                prefs.edit()
                    .putBoolean("is_logged_in", false)
                    .remove("user_id")
                    .remove("user_name")
                    .remove("user_email")
                    .remove("user_xp_" + userId)
                    .apply();
                
                // Google Sign out just in case
                GoogleSignInClient client = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
                client.signOut().addOnCompleteListener(task -> {
                    Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
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
