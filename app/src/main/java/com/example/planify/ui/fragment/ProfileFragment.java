package com.example.planify.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.planify.databinding.FragmentProfileBinding;
import com.example.planify.ui.activity.SettingsActivity;
import com.example.planify.ui.viewmodel.TaskViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private TaskViewModel taskViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use activity scope so it shares the same data as HomeFragment
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

        setupUserInfo();
        setupStats();
        setupButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupUserInfo();
        setupStats(); // Refresh stats every time user opens Profile tab
    }

    private void setupUserInfo() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("PlanifyPrefs", android.content.Context.MODE_PRIVATE);
        String savedName = prefs.getString("user_name", "Planify User");
        String savedEmail = prefs.getString("user_email", "Not signed in");
        int userId = prefs.getInt("user_id", 0);
        String avatarPath = prefs.getString("user_avatar_path_" + userId, null);
        
        binding.tvUserName.setText(savedName);
        binding.tvUserEmail.setText(savedEmail);
        
        if (avatarPath != null && !avatarPath.isEmpty()) {
            android.graphics.Bitmap bitmap = com.example.planify.utils.AvatarUtils.loadAvatar(avatarPath);
            if (bitmap != null) {
                binding.ivAvatarDisplay.setImageBitmap(bitmap);
                binding.ivAvatarDisplay.setVisibility(View.VISIBLE);
                binding.tvAvatar.setVisibility(View.GONE);
                return;
            }
        }
        
        String initials = savedName.length() > 0 ? String.valueOf(savedName.charAt(0)).toUpperCase() : "P";
        binding.tvAvatar.setText(initials);
        binding.tvAvatar.setVisibility(View.VISIBLE);
        binding.ivAvatarDisplay.setVisibility(View.GONE);
    }

    private void setupStats() {
        // Read total pomodoros from SharedPreferences (most reliable — incremented on every session)
        android.content.SharedPreferences prefs = requireContext()
            .getSharedPreferences("PlanifyPrefs", android.content.Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", 0);
        int totalPomodorosFromPrefs = prefs.getInt("total_pomodoros_" + userId, 0);

        taskViewModel.getStats((total, completed, pomodorosFromDb) -> {
            if (binding == null) return;
            // Use the higher value between DB sum and SharedPrefs counter
            int finalPomodoros = Math.max(pomodorosFromDb, totalPomodorosFromPrefs);
            binding.tvTotalTasks.setText(String.valueOf(total));
            binding.tvCompletedTasks.setText(String.valueOf(completed));
            binding.tvTotalPomodoros.setText(String.valueOf(finalPomodoros));
            int rate = total > 0 ? (int) ((completed * 100f) / total) : 0;
            binding.tvCompletionRate.setText(rate + "%");
        });

        taskViewModel.getStreakDays(days -> {
            if (binding == null) return;
            binding.tvStreak.setText("🔥 " + days + " day streak");
        });
    }

    private void setupButtons() {
        binding.btnOpenSettings.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });

        binding.tvEditProfile.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            startActivity(new Intent(requireContext(), com.example.planify.ui.activity.EditProfileActivity.class));
        });

        binding.btnViewMissionLog.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            startActivity(new Intent(requireContext(), com.example.planify.ui.activity.MissionLogActivity.class));
        });

        binding.btnSignOut.setOnClickListener(v -> {
            com.example.planify.utils.SoundManager.getInstance().playClick();
            
            // Show confirmation dialog
            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.setContentView(com.example.planify.R.layout.dialog_brutalist_confirm);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            android.widget.TextView tvTitle = dialog.findViewById(com.example.planify.R.id.tvDialogTitle);
            android.widget.TextView tvMsg = dialog.findViewById(com.example.planify.R.id.tvDialogMessage);
            android.widget.TextView btnCancel = dialog.findViewById(com.example.planify.R.id.btnDialogCancel);
            android.widget.TextView btnConfirm = dialog.findViewById(com.example.planify.R.id.btnDialogConfirm);

            tvTitle.setText("TERMINATE SESSION?");
            tvMsg.setText("Are you sure you want to log out of Planify?");

            btnCancel.setOnClickListener(cv -> dialog.dismiss());
            btnConfirm.setOnClickListener(cv -> {
                dialog.dismiss();
                performSignOut();
            });

            dialog.show();
        });
    }

    private void performSignOut() {
        // Clear SharedPreferences session
        requireContext().getSharedPreferences("PlanifyPrefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_logged_in", false)
            .remove("user_id")
            .remove("user_name")
            .remove("user_email")
            .apply();
        
        // Sign out from Google
        GoogleSignInClient client = GoogleSignIn.getClient(requireContext(),
            GoogleSignInOptions.DEFAULT_SIGN_IN);
        client.signOut().addOnCompleteListener(task -> {
            // Navigate to LoginActivity
            Intent intent = new Intent(requireContext(), com.example.planify.ui.activity.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
