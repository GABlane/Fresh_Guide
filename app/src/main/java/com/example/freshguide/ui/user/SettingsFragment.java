package com.example.freshguide.ui.user;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.SwitchCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.freshguide.BuildConfig;
import com.example.freshguide.R;
import com.example.freshguide.model.entity.UserProfileEntity;
import com.example.freshguide.util.ProfilePhotoLoader;
import com.example.freshguide.util.ScheduleReminderHelper;
import com.example.freshguide.util.SessionManager;
import com.example.freshguide.util.ThemePreferenceManager;
import com.example.freshguide.viewmodel.ProfileViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsFragment extends Fragment {

    private SessionManager sessionManager;
    private ProfileViewModel profileViewModel;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted && isAdded()) {
                    Toast.makeText(requireContext(),
                            "Class reminders need notification permission to appear",
                            Toast.LENGTH_SHORT).show();
                }
                if (isAdded() && sessionManager != null && sessionManager.isScheduleNotificationsEnabled()) {
                    ScheduleReminderHelper.syncAllReminders(requireContext());
                }
            });

    private View cardNotifications;

    private SwitchCompat switchScheduleNotifications;
    private SwitchCompat switchSyncAlerts;

    private TextView tvAppVersionInfo;
    private TextView tvRoleInfo;
    private TextView tvDeveloperInfo;
    private TextView tvAboutDescription;

    private TextView tvProfileInitial;
    private TextView tvProfileName;
    private TextView tvProfileSubtitle;
    private View cardProfilePhoto;
    private ImageView imgProfilePhoto;

    private RadioGroup themeRadioGroup;

    private boolean bindingValues;

    private TextView tvAboutContact;
    private TextView tvPrivacyPolicy;
    private TextView tvTerms;
    private TextView tvWebsite;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = SessionManager.getInstance(requireContext());
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        cardNotifications = view.findViewById(R.id.card_settings_notifications);

        switchScheduleNotifications = view.findViewById(R.id.switch_schedule_notifications);
        switchSyncAlerts = view.findViewById(R.id.switch_sync_alerts);

        tvAppVersionInfo = view.findViewById(R.id.tv_app_version_info);
        tvRoleInfo = view.findViewById(R.id.tv_role_info);
        tvDeveloperInfo = view.findViewById(R.id.tv_developer_info);
        tvAboutDescription = view.findViewById(R.id.tv_about_description);

        tvProfileInitial = view.findViewById(R.id.tv_profile_initial);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileSubtitle = view.findViewById(R.id.tv_profile_subtitle);
        cardProfilePhoto = view.findViewById(R.id.card_profile_photo);
        imgProfilePhoto = view.findViewById(R.id.img_profile_photo);

        tvAboutContact = view.findViewById(R.id.tv_about_contact);
        tvPrivacyPolicy = view.findViewById(R.id.tv_privacy_policy);
        tvTerms = view.findViewById(R.id.tv_terms);
        tvWebsite = view.findViewById(R.id.tv_website);

        themeRadioGroup = view.findViewById(R.id.theme_radio_group);

        setupNotificationControls();
        setupThemeControls();
        setupProfileHeader();
        setupAboutSection();
        applyRoleVisibility();

        bindSavedValues();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            if (sessionManager.isAdmin()) {
                setupProfileHeader();
            } else {
                profileViewModel.refreshProfile();
            }
            if (sessionManager.isScheduleNotificationsEnabled()) {
                ScheduleReminderHelper.syncAllReminders(requireContext());
            }
        }
    }

    private void setupNotificationControls() {
        switchScheduleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingValues) return;
            sessionManager.setScheduleNotificationsEnabled(isChecked);
            if (isChecked) {
                maybeRequestNotificationPermission();
                maybePromptForExactAlarmAccess();
            }
            ScheduleReminderHelper.syncAllReminders(requireContext());
        });

        switchSyncAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingValues) return;
            sessionManager.setSyncAlertsEnabled(isChecked);
        });
    }

    private void setupThemeControls() {
        int currentTheme = ThemePreferenceManager.getThemeMode(requireContext());

        switch (currentTheme) {
            case ThemePreferenceManager.THEME_LIGHT:
                themeRadioGroup.check(R.id.radio_theme_light);
                break;
            case ThemePreferenceManager.THEME_DARK:
                themeRadioGroup.check(R.id.radio_theme_dark);
                break;
            case ThemePreferenceManager.THEME_SYSTEM:
            default:
                themeRadioGroup.check(R.id.radio_theme_system);
                break;
        }

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingValues) return;

            int newTheme;
            if (checkedId == R.id.radio_theme_light) {
                newTheme = ThemePreferenceManager.THEME_LIGHT;
            } else if (checkedId == R.id.radio_theme_dark) {
                newTheme = ThemePreferenceManager.THEME_DARK;
            } else {
                newTheme = ThemePreferenceManager.THEME_SYSTEM;
            }

            ThemePreferenceManager.saveThemeMode(requireContext(), newTheme);
            ThemePreferenceManager.applyTheme(newTheme);
            requireActivity().recreate();
        });
    }

    private void setupProfileHeader() {
        if (sessionManager.isAdmin()) {
            bindAdminProfileHeader();
            return;
        }

        profileViewModel.getProfile().observe(getViewLifecycleOwner(), this::bindStudentProfileHeader);
    }

    private void bindAdminProfileHeader() {
        String displayName = sessionManager.getUserName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = "User";
        }

        String subtitle = "Admin";

        tvProfileName.setText(displayName);
        tvProfileInitial.setText(getInitial(displayName));
        tvProfileSubtitle.setText(subtitle);
        updateProfilePhoto(null, displayName);
    }

    private void bindStudentProfileHeader(@Nullable UserProfileEntity profile) {
        String displayName;
        if (profile != null && !TextUtils.isEmpty(profile.fullName)) {
            displayName = profile.fullName;
        } else {
            displayName = sessionManager.getUserName();
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = "User";
        }

        String studentId = sessionManager.getStudentId();
        String subtitle = TextUtils.isEmpty(studentId) ? "Student" : studentId;

        tvProfileName.setText(displayName);
        tvProfileInitial.setText(getInitial(displayName));
        tvProfileSubtitle.setText(subtitle);

        String photoRef = null;
        String sessionPhotoRef = sessionManager.getPendingProfilePhotoRef();
        if (!TextUtils.isEmpty(sessionPhotoRef)) {
            photoRef = sessionPhotoRef;
        } else {
            sessionPhotoRef = sessionManager.getProfilePhotoUri();
        }
        if (!TextUtils.isEmpty(sessionPhotoRef)) {
            photoRef = sessionPhotoRef;
        } else if (profile != null) {
            if (!TextUtils.isEmpty(profile.photoLocalPath)) {
                photoRef = profile.photoLocalPath;
            } else if (!TextUtils.isEmpty(profile.photoRemoteUrl)) {
                photoRef = profile.photoRemoteUrl;
            }
        }
        updateProfilePhoto(photoRef, displayName);
    }

    private String getInitial(String name) {
        if (TextUtils.isEmpty(name)) {
            return "U";
        }
        return String.valueOf(Character.toUpperCase(name.trim().charAt(0)));
    }

    private void updateProfilePhoto(@Nullable String photoRef, String displayName) {
        if (ProfilePhotoLoader.loadInto(requireContext(), imgProfilePhoto, photoRef)) {
            cardProfilePhoto.setVisibility(View.VISIBLE);
            tvProfileInitial.setVisibility(View.GONE);
            return;
        }

        imgProfilePhoto.setImageDrawable(null);
        cardProfilePhoto.setVisibility(View.GONE);
        tvProfileInitial.setVisibility(View.VISIBLE);
        tvProfileInitial.setText(getInitial(displayName));
    }

    private void setupAboutSection() {
        tvAppVersionInfo.setText("Version " + BuildConfig.VERSION_NAME);
        tvRoleInfo.setText("Role: " + (sessionManager.isAdmin() ? "Admin" : "Student"));
        tvDeveloperInfo.setText("Developed by: BSCS 3A and ETC.");
        tvAboutDescription.setText(
                "FreshGuide is a mobile application built for freshmen. " +
                        "It helps students explore the campus through map guidance and stay organized with class schedule support."
        );

    }
    private void applyRoleVisibility() {
        boolean isAdmin = sessionManager.isAdmin();
        cardNotifications.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
    }

//    private void setupResetButton(View resetButton) {
//        resetButton.setOnClickListener(v -> {
//            sessionManager.resetAppPreferences();
//            bindSavedValues();
//            Toast.makeText(requireContext(), "Settings reset", Toast.LENGTH_SHORT).show();
//        });
//    }

    private void bindSavedValues() {
        bindingValues = true;

        switchScheduleNotifications.setChecked(sessionManager.isScheduleNotificationsEnabled());
        switchSyncAlerts.setChecked(sessionManager.isSyncAlertsEnabled());

        bindingValues = false;
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void maybePromptForExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        if (ScheduleReminderHelper.canScheduleExactReminder(requireContext())) {
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Allow exact class reminders")
                .setMessage("Without exact alarm access, class reminders may arrive late. Open settings to allow accurate reminder timing?")
                .setNegativeButton("Not now", null)
                .setPositiveButton("Open Settings", (dialog, which) ->
                        ScheduleReminderHelper.openExactAlarmSettings(requireContext()))
                .show();
    }
}
