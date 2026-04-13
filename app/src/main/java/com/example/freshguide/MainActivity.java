        package com.example.freshguide;

        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.res.Configuration;
        import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
        import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
        import androidx.navigation.fragment.NavHostFragment;
        import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

        import com.example.freshguide.repository.ProfileSyncRepository;
        import com.example.freshguide.repository.SavedRoomRepository;
        import com.example.freshguide.repository.SyncRepository;
        import com.example.freshguide.receiver.NetworkChangeReceiver;
        import com.example.freshguide.util.SessionManager;
        import com.example.freshguide.util.ThemePreferenceManager;
        import com.google.android.material.snackbar.Snackbar;

        public class MainActivity extends AppCompatActivity implements NetworkChangeReceiver.NetworkListener {

            private NavController navController;
            private View rootView;
            private boolean isAdmin;
            private static final long NAV_ITEM_PRESS_DURATION_MS = 55L;
            private static final long NAV_ITEM_RECOVERY_DURATION_MS = 145L;
            private Runnable pendingNavAction;
            private View pendingNavActionView;
            private ProfileSyncRepository profileSyncRepository;
            private SavedRoomRepository savedRoomRepository;
            private SyncRepository coreSyncRepository;

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                // Apply saved theme preference
                ThemePreferenceManager.applyTheme(ThemePreferenceManager.getThemeMode(this));

                WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

                // Control system bar appearance based on current theme
                WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

                if (windowInsetsController != null) {
                    // Check if dark mode is active
                    int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    boolean isDarkMode = (nightMode == Configuration.UI_MODE_NIGHT_YES);

                    // Set light or dark appearance for system bars
                    windowInsetsController.setAppearanceLightStatusBars(!isDarkMode);
                    windowInsetsController.setAppearanceLightNavigationBars(!isDarkMode);
                }

                // Set system bar colors to match theme
                getWindow().setStatusBarColor(
                    ContextCompat.getColor(this, R.color.background_default));
                getWindow().setNavigationBarColor(
                    ContextCompat.getColor(this, R.color.background_default));

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
                    coreSyncRepository = new SyncRepository(this);
                    setupAdminNav(navHome, navSchedule, navSettings, navProfile);
                    updateNavSelection(R.id.adminDashboardFragment);
                    syncCoreDataIfNeeded();
                    if (savedInstanceState == null) {
                        NavOptions options = new NavOptions.Builder()
                                .setPopUpTo(R.id.homeFragment, true)
                                .setLaunchSingleTop(true)
                                .build();
                        navController.navigate(R.id.adminDashboardFragment, null, options);
                    }
                } else {
                    profileSyncRepository = new ProfileSyncRepository(this);
                    savedRoomRepository = new SavedRoomRepository(this);
                    setupCustomNav(navHome, navSchedule, navSettings, navProfile);
                    updateNavSelection(R.id.homeFragment);
                    savedRoomRepository.syncNow();
                    if (savedInstanceState == null) {
                        String openTab = getIntent() != null ? getIntent().getStringExtra("open_tab") : null;
                        if ("schedule".equalsIgnoreCase(openTab)) {
                            navigateTo(R.id.scheduleFragment);
                        }
                    }
                }

                applySystemBarInsets(rootView, navHostView, navContainer);

                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    updateNavSelection(destination.getId());
                    updateBottomNavVisibility(destination);
                });

                // Network change receiver (checklist 3.2)
                NetworkChangeReceiver.setListener(this);
            }

            private void setupCustomNav(View navHome, View navSchedule, View navSettings, View navProfile) {
                bindNavItem(navHome, R.id.nav_icon_home, R.id.nav_text_home,
                        this::openHomeTab);
                bindNavItem(navSchedule, R.id.nav_icon_schedule, R.id.nav_text_schedule,
                        () -> navigateTo(R.id.scheduleFragment));
                bindNavItem(navSettings, R.id.nav_icon_settings, R.id.nav_text_settings,
                        () -> navigateTo(R.id.settingsFragment));
                bindNavItem(navProfile, R.id.nav_icon_profile, R.id.nav_text_profile,
                        () -> navigateTo(R.id.profileFragment));
            }

            private void setupAdminNav(View navHome, View navSchedule, View navSettings, View navProfile) {
                configureNavItem(navHome, R.id.nav_icon_home, R.id.nav_text_home,
                        R.drawable.ic_nav_home, R.string.nav_admin_dashboard);
                configureNavItem(navSchedule, R.id.nav_icon_schedule, R.id.nav_text_schedule,
                        R.drawable.ic_find_room, R.string.nav_admin_campus);
                configureNavItem(navSettings, R.id.nav_icon_settings, R.id.nav_text_settings,
                        R.drawable.ic_directions, R.string.nav_admin_routes);
                configureNavItem(navProfile, R.id.nav_icon_profile, R.id.nav_text_profile,
                        R.drawable.ic_nav_settings, R.string.nav_admin_settings);

                bindNavItem(navHome, R.id.nav_icon_home, R.id.nav_text_home,
                        this::openAdminHomeTab);
                bindNavItem(navSchedule, R.id.nav_icon_schedule, R.id.nav_text_schedule,
                        () -> navigateTo(R.id.adminRoomListFragment));
                bindNavItem(navSettings, R.id.nav_icon_settings, R.id.nav_text_settings,
                        () -> navigateTo(R.id.adminRouteListFragment));
                bindNavItem(navProfile, R.id.nav_icon_profile, R.id.nav_text_profile,
                        () -> navigateTo(R.id.settingsFragment));
            }

            private void configureNavItem(View item, @IdRes int iconId, @IdRes int textId,
                                          int iconResId, int labelResId) {
                if (item == null) {
                    return;
                }
                View iconView = item.findViewById(iconId);
                if (iconView instanceof android.widget.ImageView) {
                    ((android.widget.ImageView) iconView).setImageResource(iconResId);
                }
                View textView = item.findViewById(textId);
                if (textView instanceof TextView) {
                    ((TextView) textView).setText(labelResId);
                }
            }

            private void bindNavItem(View item, @IdRes int iconId, @IdRes int textId, Runnable action) {
                if (item == null || action == null) {
                    return;
                }
                View.OnClickListener clickListener = v -> animateNavTap(item, action);
                item.setOnClickListener(clickListener);

                View icon = item.findViewById(iconId);
                if (icon != null) {
                    icon.setOnClickListener(clickListener);
                }

                View label = item.findViewById(textId);
                if (label != null) {
                    label.setOnClickListener(clickListener);
                }
            }

            private void openHomeTab() {
                if (navController == null || navController.getCurrentDestination() == null) {
                    return;
                }
                if (navController.getCurrentDestination().getId() == R.id.homeFragment) {
                    return;
                }
                if (navController.popBackStack(R.id.homeFragment, false)) {
                    updateNavSelection(R.id.homeFragment);
                    return;
                }
                navigateTo(R.id.homeFragment);
            }

            private void openAdminHomeTab() {
                if (navController == null || navController.getCurrentDestination() == null) {
                    return;
                }
                if (navController.getCurrentDestination().getId() == R.id.adminDashboardFragment) {
                    return;
                }
                if (navController.popBackStack(R.id.adminDashboardFragment, false)) {
                    updateNavSelection(R.id.adminDashboardFragment);
                    return;
                }
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .build();
                navController.navigate(R.id.adminDashboardFragment, null, options);
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

                final Runnable[] applyNavHostOverlayInset = new Runnable[1];
                applyNavHostOverlayInset[0] = () -> {
                    if (navHostView == null) {
                        return;
                    }
                    int overlayHeight = 0;
                    if (navContainer != null && navContainer.getVisibility() == View.VISIBLE) {
                        overlayHeight = navContainer.getHeight();
                    }
                    navHostView.setPadding(
                            navHostPaddingLeft,
                            navHostView.getPaddingTop(),
                            navHostPaddingRight,
                            navHostPaddingBottom + overlayHeight
                    );
                };

                if (navContainer != null) {
                    navContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                            applyNavHostOverlayInset[0].run());
                }

                ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
                    Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    int rootTopInset = systemBars.top + extraTopMargin;

                    v.setPadding(
                            rootPaddingLeft,
                            rootPaddingTop + rootTopInset,
                            rootPaddingRight,
                            rootPaddingBottom
                    );

                    if (navHostView != null) {
                        navHostView.setPadding(
                                navHostPaddingLeft,
                                navHostPaddingTop,
                                navHostPaddingRight,
                                navHostPaddingBottom
                        );
                    }

                    if (navContainer != null) {
                        navContainer.setPadding(
                                navContainerPaddingLeft,
                                navContainerPaddingTop,
                                navContainerPaddingRight,
                                navContainerPaddingBottom + systemBars.bottom
                        );
                        navContainer.post(applyNavHostOverlayInset[0]);
                    } else {
                        applyNavHostOverlayInset[0].run();
                    }

                    return windowInsets;
                });

                ViewCompat.requestApplyInsets(root);
            }

            private void navigateTo(@IdRes int destinationId) {
                if (navController.getCurrentDestination() == null) return;
                if (navController.getCurrentDestination().getId() == destinationId) return;

                int currentDestinationId = navController.getCurrentDestination().getId();
                NavOptions navOptions = buildNavigationOptions(currentDestinationId, destinationId);
                navController.navigate(destinationId, null, navOptions);
            }

            private void animateNavTap(View view, Runnable action) {
                if (view == null) {
                    action.run();
                    return;
                }

                cancelPendingNavAction();

                view.animate()
                        .cancel();
                view.setPivotX(view.getWidth() / 2f);
                view.setPivotY(view.getHeight() / 2f);
                view.animate()
                        .scaleX(0.986f)
                        .scaleY(0.986f)
                        .alpha(0.975f)
                        .setDuration(NAV_ITEM_PRESS_DURATION_MS)
                        .withEndAction(() -> {
                            view.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .alpha(1f)
                                    .setDuration(NAV_ITEM_RECOVERY_DURATION_MS)
                                    .start();
                        })
                        .start();
                action.run();
            }

            private void cancelPendingNavAction() {
                if (pendingNavActionView != null && pendingNavAction != null) {
                    pendingNavActionView.removeCallbacks(pendingNavAction);
                }
                pendingNavAction = null;
                pendingNavActionView = null;
            }

            private NavOptions buildNavigationOptions(@IdRes int currentDestinationId, @IdRes int destinationId) {
                NavOptions.Builder builder = new NavOptions.Builder()
                        .setLaunchSingleTop(true);

                if (!isAdmin && isUserTopLevelDestination(currentDestinationId) && isUserTopLevelDestination(destinationId)) {
                    boolean movingForward = getTopLevelIndex(destinationId) > getTopLevelIndex(currentDestinationId);
                    int startDestinationId = navController.getGraph().getStartDestinationId();
                    boolean navigatingHome = destinationId == R.id.homeFragment;
                    builder
                            .setEnterAnim(navigatingHome
                                    ? R.anim.home_tab_enter_from_left
                                    : (movingForward ? R.anim.tab_screen_enter_from_right : R.anim.tab_screen_enter_from_left))
                            .setExitAnim(navigatingHome
                                    ? R.anim.home_tab_exit_to_right
                                    : (movingForward ? R.anim.tab_screen_exit_to_left : R.anim.tab_screen_exit_to_right))
                            .setPopEnterAnim(navigatingHome
                                    ? R.anim.home_tab_enter_from_left
                                    : (movingForward ? R.anim.tab_screen_enter_from_left : R.anim.tab_screen_enter_from_right))
                            .setPopExitAnim(navigatingHome
                                    ? R.anim.home_tab_exit_to_right
                                    : (movingForward ? R.anim.tab_screen_exit_to_right : R.anim.tab_screen_exit_to_left))
                            .setRestoreState(true)
                            .setPopUpTo(startDestinationId, false, true);
                } else if (isAdmin && isAdminTopLevelDestination(currentDestinationId)
                        && isAdminTopLevelDestination(destinationId)) {
                    boolean movingForward = getAdminTopLevelIndex(destinationId) > getAdminTopLevelIndex(currentDestinationId);
                    boolean navigatingDashboard = destinationId == R.id.adminDashboardFragment;
                    builder
                            .setEnterAnim(navigatingDashboard
                                    ? R.anim.home_tab_enter_from_left
                                    : (movingForward ? R.anim.tab_screen_enter_from_right : R.anim.tab_screen_enter_from_left))
                            .setExitAnim(navigatingDashboard
                                    ? R.anim.home_tab_exit_to_right
                                    : (movingForward ? R.anim.tab_screen_exit_to_left : R.anim.tab_screen_exit_to_right))
                            .setPopEnterAnim(navigatingDashboard
                                    ? R.anim.home_tab_enter_from_left
                                    : (movingForward ? R.anim.tab_screen_enter_from_left : R.anim.tab_screen_enter_from_right))
                            .setPopExitAnim(navigatingDashboard
                                    ? R.anim.home_tab_exit_to_right
                                    : (movingForward ? R.anim.tab_screen_exit_to_right : R.anim.tab_screen_exit_to_left))
                            .setRestoreState(true)
                            .setPopUpTo(R.id.adminDashboardFragment, false, true);
                }

                return builder.build();
            }

            private boolean isUserTopLevelDestination(@IdRes int destinationId) {
                return destinationId == R.id.homeFragment
                        || destinationId == R.id.scheduleFragment
                        || destinationId == R.id.settingsFragment
                        || destinationId == R.id.profileFragment;
            }

            private int getTopLevelIndex(@IdRes int destinationId) {
                if (destinationId == R.id.homeFragment) {
                    return 0;
                }
                if (destinationId == R.id.scheduleFragment) {
                    return 1;
                }
                if (destinationId == R.id.settingsFragment) {
                    return 2;
                }
                if (destinationId == R.id.profileFragment) {
                    return 3;
                }
                return 0;
            }

            private boolean isAdminTopLevelDestination(@IdRes int destinationId) {
                return destinationId == R.id.adminDashboardFragment
                        || destinationId == R.id.adminRoomListFragment
                        || destinationId == R.id.adminRouteListFragment
                        || destinationId == R.id.settingsFragment;
            }

            private int getAdminTopLevelIndex(@IdRes int destinationId) {
                if (destinationId == R.id.adminDashboardFragment) {
                    return 0;
                }
                if (destinationId == R.id.adminRoomListFragment) {
                    return 1;
                }
                if (destinationId == R.id.adminRouteListFragment) {
                    return 2;
                }
                if (destinationId == R.id.settingsFragment) {
                    return 3;
                }
                return 0;
            }

            private void syncCoreDataIfNeeded() {
                if (coreSyncRepository == null) {
                    return;
                }
                coreSyncRepository.syncIfNeeded(new SyncRepository.SyncCallback() {
                    @Override
                    public void onSyncComplete() {
                        // Local floor and room views depend on this bootstrap cache.
                    }

                    @Override
                    public void onSyncSkipped() {
                        // Already up to date.
                    }

                    @Override
                    public void onSyncError(String message) {
                        // Admin screens fall back to local data when available.
                    }
                });
            }

            private void updateNavSelection(@IdRes int destinationId) {
                View navHome = findViewById(R.id.nav_item_home);
                View navSchedule = findViewById(R.id.nav_item_schedule);
                View navSettings = findViewById(R.id.nav_item_settings);
                View navProfile = findViewById(R.id.nav_item_profile);

                if (isAdmin) {
                    int adminBucket = getAdminNavigationBucket(destinationId);
                    setNavItemSelected(navHome, R.id.nav_icon_home, R.id.nav_text_home, adminBucket == 0);
                    setNavItemSelected(navSchedule, R.id.nav_icon_schedule, R.id.nav_text_schedule, adminBucket == 1);
                    setNavItemSelected(navSettings, R.id.nav_icon_settings, R.id.nav_text_settings, adminBucket == 2);
                    setNavItemSelected(navProfile, R.id.nav_icon_profile, R.id.nav_text_profile, adminBucket == 3);
                    return;
                }

                boolean homeSelected = destinationId == R.id.homeFragment;
                boolean scheduleSelected = destinationId == R.id.scheduleFragment;
                boolean settingsSelected = destinationId == R.id.settingsFragment;
                boolean profileSelected = destinationId == R.id.profileFragment;

                setNavItemSelected(navHome, R.id.nav_icon_home, R.id.nav_text_home, homeSelected);
                setNavItemSelected(navSchedule, R.id.nav_icon_schedule, R.id.nav_text_schedule, scheduleSelected);
                setNavItemSelected(navSettings, R.id.nav_icon_settings, R.id.nav_text_settings, settingsSelected);
                setNavItemSelected(navProfile, R.id.nav_icon_profile, R.id.nav_text_profile, profileSelected);
            }

            private void updateBottomNavVisibility(@NonNull NavDestination destination) {
                View navContainer = findViewById(R.id.nav_bar_container);
                if (navContainer == null) {
                    return;
                }
                navContainer.setVisibility(isDialogDestination(destination.getId()) ? View.GONE : View.VISIBLE);
            }

            private boolean isDialogDestination(@IdRes int destinationId) {
                return destinationId == R.id.roomDetailFragment
                        || destinationId == R.id.adminBuildingFormFragment
                        || destinationId == R.id.adminFloorFormFragment
                        || destinationId == R.id.adminRoomFormFragment
                        || destinationId == R.id.adminRouteFormFragment
                        || destinationId == R.id.adminCampusAreaFormFragment;
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

            private int getAdminNavigationBucket(@IdRes int destinationId) {
                if (destinationId == R.id.adminDashboardFragment || destinationId == R.id.adminPublishFragment) {
                    return 0;
                }
                if (destinationId == R.id.adminRoomListFragment
                        || destinationId == R.id.adminRoomFormFragment
                        || destinationId == R.id.adminBuildingListFragment
                        || destinationId == R.id.adminBuildingFormFragment
                        || destinationId == R.id.adminFloorListFragment
                        || destinationId == R.id.adminFloorFormFragment
                        || destinationId == R.id.adminCampusAreaListFragment
                        || destinationId == R.id.adminCampusAreaFormFragment) {
                    return 1;
                }
                if (destinationId == R.id.adminRouteListFragment
                        || destinationId == R.id.adminRouteFormFragment
                        || destinationId == R.id.adminFacilityListFragment
                        || destinationId == R.id.adminOriginListFragment) {
                    return 2;
                }
                if (destinationId == R.id.settingsFragment) {
                    return 3;
                }
                return 0;
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
                if (!isConnected) {
                    if (rootView != null) {
                        Snackbar.make(rootView, "No internet connection", Snackbar.LENGTH_LONG).show();
                    }
                    return;
                }

                if (!isAdmin && profileSyncRepository != null) {
                    profileSyncRepository.syncNow();
                }
                if (!isAdmin && savedRoomRepository != null) {
                    savedRoomRepository.syncNow();
                }
                if (isAdmin && coreSyncRepository != null) {
                    syncCoreDataIfNeeded();
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
