package com.example.freshguide;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.annotation.IdRes;

import com.example.freshguide.receiver.NetworkChangeReceiver;
import com.example.freshguide.util.SessionManager;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements NetworkChangeReceiver.NetworkListener {

    private NavController navController;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootView = findViewById(R.id.main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        View navContainer = findViewById(R.id.nav_bar_container);
        View navHome = findViewById(R.id.nav_item_home);
        View navSchedule = findViewById(R.id.nav_item_schedule);
        View navSettings = findViewById(R.id.nav_item_settings);
        View navProfile = findViewById(R.id.nav_item_profile);

        // Route to correct start destination based on role
        SessionManager session = SessionManager.getInstance(this);
        if (session.isAdmin()) {
            if (navContainer != null) navContainer.setVisibility(View.GONE);
            navController.navigate(R.id.adminDashboardFragment);
        } else {
            setupCustomNav(navHome, navSchedule, navSettings, navProfile);
            updateNavSelection(R.id.homeFragment);
            navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                    updateNavSelection(destination.getId()));
        }

        // Network change receiver (checklist 3.2)
        NetworkChangeReceiver.setListener(this);
    }

    private void setupCustomNav(View navHome, View navSchedule, View navSettings, View navProfile) {
        if (navHome != null) {
            navHome.setOnClickListener(v -> navigateTo(R.id.homeFragment));
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> navigateTo(R.id.profileFragment));
        }
        if (navSchedule != null) {
            navSchedule.setOnClickListener(v -> showComingSoon());
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> showComingSoon());
        }
    }

    private void navigateTo(@IdRes int destinationId) {
        if (navController.getCurrentDestination() == null) return;
        if (navController.getCurrentDestination().getId() == destinationId) return;
        navController.navigate(destinationId);
    }

    private void showComingSoon() {
        if (rootView != null) {
            Snackbar.make(rootView, "Coming soon", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void updateNavSelection(@IdRes int destinationId) {
        View navHome = findViewById(R.id.nav_item_home);
        View navSchedule = findViewById(R.id.nav_item_schedule);
        View navSettings = findViewById(R.id.nav_item_settings);
        View navProfile = findViewById(R.id.nav_item_profile);

        boolean homeSelected = destinationId == R.id.homeFragment;
        boolean profileSelected = destinationId == R.id.profileFragment;

        setNavItemSelected(navHome, R.id.nav_icon_home, R.id.nav_text_home, homeSelected);
        setNavItemSelected(navSchedule, R.id.nav_icon_schedule, R.id.nav_text_schedule, false);
        setNavItemSelected(navSettings, R.id.nav_icon_settings, R.id.nav_text_settings, false);
        setNavItemSelected(navProfile, R.id.nav_icon_profile, R.id.nav_text_profile, profileSelected);
    }

    private void setNavItemSelected(View item, int iconId, int textId, boolean selected) {
        if (item == null) return;
        int green = getColor(R.color.green_primary);
        int gray = getColor(R.color.text_hint);

        item.setBackgroundResource(selected ? R.drawable.bg_nav_item_selected : android.R.color.transparent);

        View iconView = item.findViewById(iconId);
        View textView = item.findViewById(textId);
        if (iconView instanceof android.widget.ImageView) {
            ((android.widget.ImageView) iconView).setColorFilter(selected ? green : gray);
        }
        if (textView instanceof android.widget.TextView) {
            ((android.widget.TextView) textView).setTextColor(selected ? green : gray);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkChangeReceiver.setListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkChangeReceiver.clearListener();
    }

    @Override
    public void onNetworkChanged(boolean isConnected) {
        if (!isConnected && rootView != null) {
            Snackbar.make(rootView, "No internet connection", Snackbar.LENGTH_LONG).show();
        }
    }

    // Checklist 4.1-4.3: Options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SessionManager session = SessionManager.getInstance(this);
        menu.findItem(R.id.action_logout).setVisible(session.isLoggedIn());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            new com.example.freshguide.repository.AuthRepository(this).logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_search) {
            return true;
        } else if (id == R.id.action_filter) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
