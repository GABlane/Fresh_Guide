        package com.example.freshguide;

        import android.content.Intent;
        import android.content.IntentFilter;
        import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
        import androidx.navigation.fragment.NavHostFragment;
        import androidx.annotation.IdRes;

        import com.example.freshguide.receiver.NetworkChangeReceiver;
        import com.example.freshguide.util.SessionManager;
        import com.google.android.material.snackbar.Snackbar;

        public class MainActivity extends AppCompatActivity implements NetworkChangeReceiver.NetworkListener {

            private NavController navController;
            private View rootView;
            private View headerBack;
            private TextView headerTitle;
            private View headerBar;
            private boolean isAdmin;

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
                setContentView(R.layout.activity_main);

                rootView = findViewById(R.id.main);
                View navHostView = findViewById(R.id.nav_host_fragment);

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
                isAdmin = session.isAdmin();

                if (isAdmin) {
                    if (navContainer != null) navContainer.setVisibility(View.GONE);
                    if (savedInstanceState == null) {
                        NavOptions options = new NavOptions.Builder()
                                .setPopUpTo(R.id.homeFragment, true)
                                .setLaunchSingleTop(true)
                                .build();
                        navController.navigate(R.id.adminDashboardFragment, null, options);
                    }
                } else {
                    setupCustomNav(navHome, navSchedule, navSettings, navProfile);
                    updateNavSelection(R.id.homeFragment);
                    if (savedInstanceState == null) {
                        String openTab = getIntent() != null ? getIntent().getStringExtra("open_tab") : null;
                        if ("schedule".equalsIgnoreCase(openTab)) {
                            navigateTo(R.id.scheduleFragment);
                        }
                    }
                }

                applySystemBarInsets(rootView, navHostView, navContainer);

                if (headerBack != null) {
                    headerBack.setOnClickListener(v -> handleBackTap());
                }

                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    if (!isAdmin) {
                        updateNavSelection(destination.getId());
                    }
                    updateHeader(destination);
                });
                updateHeader(navController.getCurrentDestination());

                // Network change receiver (checklist 3.2)
                NetworkChangeReceiver.setListener(this);
            }

            private void handleBackTap() {
                if (navController == null || navController.getCurrentDestination() == null) return;
                if (navController.popBackStack()) return;

                if (isAdmin) {
                    navigateTo(R.id.adminDashboardFragment);
                } else {
                    navigateTo(R.id.homeFragment);
                }
            }

            private void updateHeader(NavDestination destination) {
                if (destination == null) return;

                boolean hideHeader =
                        destination.getId() == R.id.homeFragment ||
                                destination.getId() == R.id.adminDashboardFragment;

                if (headerBar != null) {
                    headerBar.setVisibility(hideHeader ? View.GONE : View.VISIBLE);
                }

                if (headerTitle != null) {
                    if (destination.getId() == R.id.homeFragment) {
                        headerTitle.setText(R.string.dashboard_header);
                    } else if (destination.getLabel() != null && destination.getLabel().length() > 0) {
                        headerTitle.setText(destination.getLabel());
                    } else {
                        headerTitle.setText(R.string.app_name);
                    }
                }

                if (headerBack != null) {
                    headerBack.setVisibility(isTopLevelDestination(destination.getId()) ? View.GONE : View.VISIBLE);
                }
            }

            private boolean isTopLevelDestination(@IdRes int destinationId) {
                if (isAdmin) {
                    return destinationId == R.id.adminDashboardFragment;
                }
                return destinationId == R.id.homeFragment;
            }

            private void setupCustomNav(View navHome, View navSchedule, View navSettings, View navProfile) {
                if (navHome != null) {
                    navHome.setOnClickListener(v -> navigateTo(R.id.homeFragment));
                }
                if (navProfile != null) {
                    navProfile.setOnClickListener(v -> navigateTo(R.id.profileFragment));
                }
                if (navSchedule != null) {
                    navSchedule.setOnClickListener(v -> navigateTo(R.id.scheduleFragment));
                }
                if (navSettings != null) {
                    navSettings.setOnClickListener(v -> navigateTo(R.id.settingsFragment));
                }
            }

            private void applySystemBarInsets(View root, View navHostView, View navContainer) {
                if (root == null) {
                    return;
                }

                final int extraTopMargin = 0;

                final int rootPaddingLeft = root.getPaddingLeft();
                final int rootPaddingTop = root.getPaddingTop();
                final int rootPaddingRight = root.getPaddingRight();
                final int rootPaddingBottom = root.getPaddingBottom();

                final int navHostPaddingLeft = navHostView != null ? navHostView.getPaddingLeft() : 0;
                final int navHostPaddingTop = navHostView != null ? navHostView.getPaddingTop() : 0;
                final int navHostPaddingRight = navHostView != null ? navHostView.getPaddingRight() : 0;
                final int navHostPaddingBottom = navHostView != null ? navHostView.getPaddingBottom() : 0;

                final int navContainerPaddingLeft = navContainer != null ? navContainer.getPaddingLeft() : 0;
                final int navContainerPaddingTop = navContainer != null ? navContainer.getPaddingTop() : 0;
                final int navContainerPaddingRight = navContainer != null ? navContainer.getPaddingRight() : 0;
                final int navContainerPaddingBottom = navContainer != null ? navContainer.getPaddingBottom() : 0;

                ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                    Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            rootPaddingLeft,
                            rootPaddingTop,
                            rootPaddingRight,
                            rootPaddingBottom
                    );

                    if (navHostView != null) {
                        int hostTopInset = systemBars.top + extraTopMargin;
                        int hostBottomInset = isAdmin ? systemBars.bottom : 0;
                        navHostView.setPadding(
                                navHostPaddingLeft,
                                navHostPaddingTop + hostTopInset,
                                navHostPaddingRight,
                                navHostPaddingBottom + hostBottomInset
                        );
                    }

                    if (navContainer != null) {
                        navContainer.setPadding(
                                navContainerPaddingLeft,
                                navContainerPaddingTop,
                                navContainerPaddingRight,
                                navContainerPaddingBottom + systemBars.bottom
                        );
                    }

                    return windowInsets;
                });

                ViewCompat.requestApplyInsets(root);
            }

            private void navigateTo(@IdRes int destinationId) {
                if (navController.getCurrentDestination() == null) return;
                if (navController.getCurrentDestination().getId() == destinationId) return;
                navController.navigate(destinationId);
            }

            private void updateNavSelection(@IdRes int destinationId) {
                View navHome = findViewById(R.id.nav_item_home);
                View navSchedule = findViewById(R.id.nav_item_schedule);
                View navSettings = findViewById(R.id.nav_item_settings);
                View navProfile = findViewById(R.id.nav_item_profile);

                boolean homeSelected = destinationId == R.id.homeFragment;
                boolean scheduleSelected = destinationId == R.id.scheduleFragment;
                boolean settingsSelected = destinationId == R.id.settingsFragment;
                boolean profileSelected = destinationId == R.id.profileFragment;

                setNavItemSelected(navHome, R.id.nav_icon_home, R.id.nav_text_home, homeSelected);
                setNavItemSelected(navSchedule, R.id.nav_icon_schedule, R.id.nav_text_schedule, scheduleSelected);
                setNavItemSelected(navSettings, R.id.nav_icon_settings, R.id.nav_text_settings, settingsSelected);
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
