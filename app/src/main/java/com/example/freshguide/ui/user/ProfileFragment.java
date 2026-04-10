package com.example.freshguide.ui.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.LoginActivity;
import com.example.freshguide.R;
import com.example.freshguide.model.entity.UserProfileEntity;
import com.example.freshguide.repository.AuthRepository;
import com.example.freshguide.repository.SavedRoomRepository;
import com.example.freshguide.ui.adapter.RoomAdapter;
import com.example.freshguide.util.ProfilePhotoLoader;
import com.example.freshguide.util.SessionManager;
import com.example.freshguide.viewmodel.ProfileViewModel;
import com.example.freshguide.viewmodel.SavedRoomsViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvName;
    private TextView tvStudentId;
    private TextView tvCourseSection;
    private TextView tvProfileDate;
    private ImageView imgProfilePhoto;
    private TextView tvProfileInitial;
    private TextView tvSavedLocationsEmpty;
    private RecyclerView recyclerSavedLocations;
    private View layoutSavedLocationsEmpty;

    private SessionManager session;
    private ProfileViewModel profileViewModel;
    private SavedRoomsViewModel savedRoomsViewModel;
    private UserProfileEntity currentProfile;
    private RoomAdapter savedRoomAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = SessionManager.getInstance(requireContext());
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        savedRoomsViewModel = new ViewModelProvider(this).get(SavedRoomsViewModel.class);

        tvName = view.findViewById(R.id.tv_name);
        tvStudentId = view.findViewById(R.id.tv_student_id);
        tvCourseSection = view.findViewById(R.id.tv_course_section);
        tvProfileDate = view.findViewById(R.id.tv_profile_date);
        imgProfilePhoto = view.findViewById(R.id.img_profile_photo);
        tvProfileInitial = view.findViewById(R.id.tv_profile_initial);
        tvSavedLocationsEmpty = view.findViewById(R.id.tv_saved_locations_empty);
        recyclerSavedLocations = view.findViewById(R.id.recycler_saved_locations);
        layoutSavedLocationsEmpty = view.findViewById(R.id.layout_saved_locations_empty);

        ImageButton btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        ImageButton btnMore = view.findViewById(R.id.btn_more);
        NavController navController = Navigation.findNavController(view);

        savedRoomAdapter = new RoomAdapter();
        savedRoomAdapter.setDisplayMode(RoomAdapter.MODE_SAVED);
        savedRoomAdapter.setOnItemClickListener(room -> {
            Bundle args = new Bundle();
            args.putInt("roomId", room.roomId);
            args.putString("roomName", room.getDisplayName());
            args.putBoolean("showGoTo", true);
            navController.navigate(R.id.roomDetailFragment, args);
        });
        savedRoomAdapter.setOnActionClickListener(room ->
                savedRoomsViewModel.toggleSaved(room.roomId, new SavedRoomRepository.ToggleCallback() {
                    @Override
                    public void onComplete(boolean isSaved) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(
                                requireContext(),
                                isSaved ? R.string.saved_location_added : R.string.saved_location_removed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                }));
        recyclerSavedLocations.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSavedLocations.setNestedScrollingEnabled(false);
        recyclerSavedLocations.setAdapter(savedRoomAdapter);

        setCurrentDate();
        bindProfileData(null);

        profileViewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            currentProfile = profile;
            bindProfileData(profile);
        });

        savedRoomsViewModel.getSavedRooms().observe(getViewLifecycleOwner(), rooms -> {
            boolean hasSavedRooms = rooms != null && !rooms.isEmpty();
            savedRoomAdapter.submitList(rooms);
            recyclerSavedLocations.setVisibility(hasSavedRooms ? View.VISIBLE : View.GONE);
            layoutSavedLocationsEmpty.setVisibility(hasSavedRooms ? View.GONE : View.VISIBLE);
        });

        btnEditProfile.setOnClickListener(v -> {
            String currentName = currentProfile != null
                    ? normalizeName(currentProfile.fullName)
                    : normalizeName(session.getUserName());
            String currentCourseSection = currentProfile != null
                    ? normalizeCourseSection(currentProfile.courseSection)
                    : normalizeCourseSection(session.getProfileCourseSection());
            String previousSessionName = session.getUserName();
            String previousSessionCourseSection = session.getProfileCourseSection();
            String previousPendingPhotoRef = session.getPendingProfilePhotoRef();

            EditProfileBottomSheet bottomSheet = EditProfileBottomSheet.newInstance(
                    getStudentIdText(),
                    currentName,
                    currentCourseSection,
                    getCurrentPhotoRef()
            );

            bottomSheet.setOnProfileSavedListener((updatedName, updatedCourseSection, photoUri) ->
                    {
                        UserProfileEntity previousProfile = currentProfile;
                        applyOptimisticProfileUpdate(updatedName, updatedCourseSection, photoUri);

                        profileViewModel.saveProfile(updatedName, updatedCourseSection, photoUri,
                                new ProfileViewModel.SaveCallback() {
                                    @Override
                                    public void onSuccess(UserProfileEntity profile) {
                                    if (!isAdded()) {
                                        return;
                                    }
                                        session.clearPendingProfilePhotoRef();
                                        currentProfile = profile;
                                        bindProfileData(profile);
                                        Toast.makeText(requireContext(), "Profile updated.", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                public void onError(String message) {
                                    if (!isAdded()) {
                                        return;
                                    }
                                    session.setUserName(previousSessionName);
                                    session.setProfileCourseSection(previousSessionCourseSection);
                                    session.setPendingProfilePhotoRef(previousPendingPhotoRef);
                                    currentProfile = previousProfile;
                                    bindProfileData(previousProfile);
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
            );

            bottomSheet.show(getParentFragmentManager(), "EditProfileBottomSheet");
        });

        btnMore.setOnClickListener(this::showProfileMenu);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            profileViewModel.refreshProfile();
        }
    }

    private void bindProfileData(@Nullable UserProfileEntity profile) {
        String savedName = profile != null ? profile.fullName : session.getUserName();
        String courseSection = profile != null ? profile.courseSection : session.getProfileCourseSection();

        tvName.setText(formatDisplayName(savedName));
        tvStudentId.setText(getStudentIdText());
        tvCourseSection.setText(getCourseSectionOrFallback(courseSection));
        updateProfilePhoto(getPhotoRefFromProfile(profile), tvName.getText().toString());
    }

    private void applyOptimisticProfileUpdate(@NonNull String updatedName,
                                              @NonNull String updatedCourseSection,
                                              @Nullable String photoRef) {
        session.setUserName(updatedName);
        session.setProfileCourseSection(updatedCourseSection);
        session.setPendingProfilePhotoRef(photoRef);
        tvName.setText(formatDisplayName(updatedName));
        tvStudentId.setText(getStudentIdText());
        tvCourseSection.setText(getCourseSectionOrFallback(updatedCourseSection));
        updateProfilePhoto(photoRef, tvName.getText().toString());
    }

    private void updateProfileInitial(String displayName) {
        String initial = "U";

        if (displayName != null) {
            String trimmed = displayName.trim();
            if (!trimmed.isEmpty() && !trimmed.equals("—")) {
                initial = String.valueOf(trimmed.charAt(0)).toUpperCase(Locale.getDefault());
            }
        }

        tvProfileInitial.setText(initial);
    }

    private void updateProfilePhoto(@Nullable String photoRef, String displayName) {
        if (ProfilePhotoLoader.loadInto(requireContext(), imgProfilePhoto, photoRef)) {
            imgProfilePhoto.setVisibility(View.VISIBLE);
            tvProfileInitial.setVisibility(View.GONE);
            return;
        }

        imgProfilePhoto.setImageDrawable(null);
        imgProfilePhoto.setVisibility(View.GONE);
        tvProfileInitial.setVisibility(View.VISIBLE);
        updateProfileInitial(displayName);
    }

    private void setCurrentDate() {
        Calendar calendar = Calendar.getInstance();

        String dayName = new SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.getTime());
        String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String formattedDate = dayName + ", " + monthName + " " + day;
        tvProfileDate.setText(formattedDate);
    }

    private void showProfileMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this::onMenuItemClicked);
        popupMenu.show();
    }

    private boolean onMenuItemClicked(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AuthRepository(requireContext()).logout();

            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
            return true;
        }
        return false;
    }

    private String getStudentIdText() {
        String studentId = session.getStudentId();
        return studentId != null ? studentId : "—";
    }

    private String formatDisplayName(String rawName) {
        return normalizeName(rawName).toUpperCase(Locale.getDefault());
    }

    private String getCourseSectionOrFallback(String courseSection) {
        return normalizeCourseSection(courseSection).toUpperCase(Locale.getDefault());
    }

    private String normalizeName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "TEST STUDENT";
        }
        return rawName.trim();
    }

    private String normalizeCourseSection(String courseSection) {
        if (courseSection == null || courseSection.trim().isEmpty()) {
            return "BSCS 3A";
        }
        return courseSection.trim();
    }

    @Nullable
    private String getPhotoRefFromProfile(@Nullable UserProfileEntity profile) {
        String sessionPhotoRef = session.getPendingProfilePhotoRef();
        if (!TextUtils.isEmpty(sessionPhotoRef)) {
            return sessionPhotoRef;
        }
        sessionPhotoRef = session.getProfilePhotoUri();
        if (!TextUtils.isEmpty(sessionPhotoRef)) {
            return sessionPhotoRef;
        }
        if (profile == null) {
            return null;
        }
        if (!TextUtils.isEmpty(profile.photoLocalPath)) {
            return profile.photoLocalPath;
        }
        if (!TextUtils.isEmpty(profile.photoRemoteUrl)) {
            return profile.photoRemoteUrl;
        }
        return null;
    }

    @Nullable
    private String getCurrentPhotoRef() {
        return getPhotoRefFromProfile(currentProfile);
    }
}
