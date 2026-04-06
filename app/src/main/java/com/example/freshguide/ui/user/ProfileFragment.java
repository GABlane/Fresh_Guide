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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.freshguide.LoginActivity;
import com.example.freshguide.R;
import com.example.freshguide.repository.AuthRepository;
import com.example.freshguide.util.SessionManager;

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

    private SessionManager session;

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

        tvName = view.findViewById(R.id.tv_name);
        tvStudentId = view.findViewById(R.id.tv_student_id);
        tvCourseSection = view.findViewById(R.id.tv_course_section);
        tvProfileDate = view.findViewById(R.id.tv_profile_date);
        imgProfilePhoto = view.findViewById(R.id.img_profile_photo);
        tvProfileInitial = view.findViewById(R.id.tv_profile_initial);

        ImageButton btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        ImageButton btnMore = view.findViewById(R.id.btn_more);

        setCurrentDate();
        bindProfileData();

        btnEditProfile.setOnClickListener(v -> {
            EditProfileBottomSheet bottomSheet = EditProfileBottomSheet.newInstance(
                    getStudentIdText(),
                    tvName.getText().toString(),
                    tvCourseSection.getText().toString(),
                    session.getProfilePhotoUri()
            );

            bottomSheet.setOnProfileSavedListener((updatedName, updatedCourseSection, photoUri) -> {
                tvName.setText(formatDisplayName(updatedName));
                tvCourseSection.setText(getCourseSectionOrFallback(updatedCourseSection));
                updateProfilePhoto(photoUri, updatedName);
            });

            bottomSheet.show(getParentFragmentManager(), "EditProfileBottomSheet");
        });

        btnMore.setOnClickListener(this::showProfileMenu);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded()) {
            bindProfileData();
        }
    }

    private void bindProfileData() {
        String savedName = session.getUserName();
        String courseSection = session.getProfileCourseSection();

        tvName.setText(formatDisplayName(savedName));
        tvStudentId.setText(getStudentIdText());
        tvCourseSection.setText(getCourseSectionOrFallback(courseSection));
        updateProfilePhoto(session.getProfilePhotoUri(), tvName.getText().toString());
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

    private void updateProfilePhoto(@Nullable String photoUri, String displayName) {
        if (!TextUtils.isEmpty(photoUri)) {
            try {
                imgProfilePhoto.setImageURI(Uri.parse(photoUri));
                imgProfilePhoto.setVisibility(View.VISIBLE);
                tvProfileInitial.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {
                session.setProfilePhotoUri(null);
            }
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
        if (rawName == null || rawName.trim().isEmpty()) {
            return "TEST STUDENT";
        }
        return rawName.trim().toUpperCase(Locale.getDefault());
    }

    private String getCourseSectionOrFallback(String courseSection) {
        if (courseSection == null || courseSection.trim().isEmpty()) {
            return "BSCS 3A";
        }
        return courseSection.trim().toUpperCase(Locale.getDefault());
    }
}
