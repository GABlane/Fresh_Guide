package com.example.freshguide.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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

    private RadioGroup themeRadioGroup;

    private boolean bindingValues;

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

        themeRadioGroup = view.findViewById(R.id.theme_radio_group);

        setupExpandableSection(
                view.findViewById(R.id.header_theme),
                view.findViewById(R.id.content_theme),
                view.findViewById(R.id.indicator_theme),
                true
        );

        setupExpandableSection(
                view.findViewById(R.id.header_notifications),
                view.findViewById(R.id.content_notifications),
                view.findViewById(R.id.indicator_notifications),
                false
        );

        setupExpandableSection(
                view.findViewById(R.id.header_about),
                view.findViewById(R.id.content_about),
                view.findViewById(R.id.indicator_about),
                false
        );

        setupNotificationControls();
        setupThemeControls();
        setupAboutSection();
        applyRoleVisibility();
        setupResetButton(view.findViewById(R.id.btn_reset_preferences));

        bindSavedValues();
    }

    private void setupExpandableSection(View header, View content, ImageView indicator, boolean expanded) {
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        indicator.setRotation(expanded ? 0f : -90f);

        header.setOnClickListener(v -> {
            boolean isExpanded = content.getVisibility() == View.VISIBLE;
            content.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            indicator.animate().rotation(isExpanded ? -90f : 0f).setDuration(180).start();
        });
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

    private void setupAboutSection() {
        tvAppVersionInfo.setText("App version: " + BuildConfig.VERSION_NAME);
        tvRoleInfo.setText("Role: " + (sessionManager.isAdmin() ? "admin" : "student"));
    }

    private void applyRoleVisibility() {
        boolean isAdmin = sessionManager.isAdmin();
        cardNotifications.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
    }

    private void setupResetButton(View resetButton) {
        resetButton.setOnClickListener(v -> {
            sessionManager.resetAppPreferences();
            bindSavedValues();
            Toast.makeText(requireContext(), "Settings reset", Toast.LENGTH_SHORT).show();
        });
    }

    private void bindSavedValues() {
        bindingValues = true;

        switchScheduleNotifications.setChecked(sessionManager.isScheduleNotificationsEnabled());
        switchSyncAlerts.setChecked(sessionManager.isSyncAlertsEnabled());

        bindingValues = false;
    }
}