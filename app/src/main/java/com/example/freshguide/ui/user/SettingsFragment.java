package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.freshguide.BuildConfig;
import com.example.freshguide.R;
import com.example.freshguide.util.SessionManager;
import com.example.freshguide.util.ThemePreferenceManager;

public class SettingsFragment extends Fragment {

    private SessionManager sessionManager;

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
            setupProfileHeader();
        }
    }

    private void setupNotificationControls() {
        switchScheduleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingValues) return;
            sessionManager.setScheduleNotificationsEnabled(isChecked);
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
        String displayName = sessionManager.getUserName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = "User";
        }

        String subtitle;
        if (sessionManager.isAdmin()) {
            subtitle = "Admin";
        } else {
            String studentId = sessionManager.getStudentId();
            subtitle = TextUtils.isEmpty(studentId) ? "Student" : studentId;
        }

        tvProfileName.setText(displayName);
        tvProfileInitial.setText(getInitial(displayName));
        tvProfileSubtitle.setText(subtitle);
    }

    private String getInitial(String name) {
        if (TextUtils.isEmpty(name)) {
            return "U";
        }
        return String.valueOf(Character.toUpperCase(name.trim().charAt(0)));
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
}
