package com.example.freshguide.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.ProfileDto;
import com.example.freshguide.model.entity.UserProfileEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.example.freshguide.util.NetworkUtils;
import com.example.freshguide.util.SessionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class ProfileSyncRepository {

    private static final String TAG = "ProfileSyncRepository";

    public interface SaveCallback {
        void onSuccess(UserProfileEntity profile);
        void onError(String message);
    }

    private final Context appContext;
    private final AppDatabase db;
    private final ApiService api;
    private final SessionManager session;
    private final Executor ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    public ProfileSyncRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        db = AppDatabase.getInstance(appContext);
        api = ApiClient.getInstance(appContext).getApiService();
        session = SessionManager.getInstance(appContext);
    }

    public LiveData<UserProfileEntity> observeProfile() {
        return db.userProfileDao().observeByOwner(getOwnerKey());
    }

    public void bootstrap() {
        ioExecutor.execute(() -> {
            String ownerKey = requireOwnerKey();
            if (ownerKey == null) {
                return;
            }
            ensureLocalProfile(ownerKey);
            syncNowInternal(ownerKey);
        });
    }

    public void saveProfile(String fullName,
                            String courseSection,
                            @Nullable String selectedPhotoRef,
                            @NonNull SaveCallback callback) {
        ioExecutor.execute(() -> {
            String ownerKey = requireOwnerKey();
            if (ownerKey == null) {
                mainHandler.post(() -> callback.onError("No logged-in student account"));
                return;
            }

            try {
                UserProfileEntity local = ensureLocalProfile(ownerKey);
                long now = System.currentTimeMillis();

                local.fullName = normalizeName(fullName, ownerKey);
                local.courseSection = normalizeOptional(courseSection);

                String currentPhotoRef = getDisplayPhotoRef(local);
                String selected = normalizeOptional(selectedPhotoRef);

                if (!TextUtils.equals(selected, currentPhotoRef)) {
                    if (TextUtils.isEmpty(selected)) {
                        local.pendingPhotoAction = UserProfileEntity.PHOTO_ACTION_DELETE;
                        local.photoLocalPath = null;
                        local.photoRemoteUrl = null;
                    } else {
                        String copiedPath = copyUriOrFileToProfileImage(ownerKey, selected);
                        if (copiedPath == null) {
                            throw new IllegalStateException("Unable to process the selected photo");
                        }
                        local.photoLocalPath = copiedPath;
                        local.pendingPhotoAction = UserProfileEntity.PHOTO_ACTION_UPLOAD;
                    }
                }

                local.updatedAt = now;
                local.syncState = UserProfileEntity.SYNC_STATE_DIRTY;
                db.userProfileDao().upsert(local);
                updateSessionProfile(local);

                mainHandler.post(() -> callback.onSuccess(local));
                syncNowInternal(ownerKey);
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Failed to save profile";
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    public void syncNow() {
        ioExecutor.execute(() -> syncNowInternal(requireOwnerKey()));
    }

    private void syncNowInternal(@Nullable String ownerKey) {
        if (ownerKey == null || ownerKey.isBlank()) {
            return;
        }
        if (!NetworkUtils.isConnected(appContext)) {
            return;
        }
        if (!syncRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            UserProfileEntity local = ensureLocalProfile(ownerKey);

            if (local.syncState == UserProfileEntity.SYNC_STATE_DIRTY) {
                if (pushLocalProfile(local, ownerKey)) {
                    session.setProfileMigrated(ownerKey, true);
                    pullRemoteProfile(ownerKey);
                }
                return;
            }

            if (!pullRemoteProfile(ownerKey) && !session.isProfileMigrated(ownerKey) && hasLocalLegacyData(local, ownerKey)) {
                if (pushLocalProfile(local, ownerKey)) {
                    session.setProfileMigrated(ownerKey, true);
                    pullRemoteProfile(ownerKey);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Profile sync failed", e);
        } finally {
            syncRunning.set(false);
        }
    }

    private boolean pushLocalProfile(@NonNull UserProfileEntity local, @NonNull String ownerKey) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("name", normalizeName(local.fullName, ownerKey));
            body.put("course_section", normalizeOptional(local.courseSection));

            Response<ApiResponse<ProfileDto>> updateResponse = api.updateProfile(body).execute();
            if (!isSuccess(updateResponse)) {
                return false;
            }

            if (local.pendingPhotoAction == UserProfileEntity.PHOTO_ACTION_UPLOAD && !TextUtils.isEmpty(local.photoLocalPath)) {
                File photoFile = new File(local.photoLocalPath);
                if (photoFile.exists()) {
                    RequestBody imageBody = RequestBody.create(MediaType.parse("image/*"), photoFile);
                    MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", photoFile.getName(), imageBody);
                    Response<ApiResponse<ProfileDto>> uploadResponse = api.uploadProfilePhoto(imagePart).execute();
                    if (!isSuccess(uploadResponse)) {
                        return false;
                    }
                }
            } else if (local.pendingPhotoAction == UserProfileEntity.PHOTO_ACTION_DELETE) {
                Response<ApiResponse<ProfileDto>> deleteResponse = api.deleteProfilePhoto().execute();
                if (!isSuccess(deleteResponse)) {
                    return false;
                }
            }

            local.syncState = UserProfileEntity.SYNC_STATE_CLEAN;
            local.pendingPhotoAction = UserProfileEntity.PHOTO_ACTION_NONE;
            local.lastSyncedAt = System.currentTimeMillis();
            db.userProfileDao().upsert(local);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Push profile failed", e);
            return false;
        }
    }

    private boolean pullRemoteProfile(@NonNull String ownerKey) {
        try {
            Response<ApiResponse<ProfileDto>> response = api.getProfile().execute();
            if (!isSuccess(response) || response.body() == null || response.body().getData() == null) {
                return false;
            }

            ProfileDto remote = response.body().getData();
            UserProfileEntity local = ensureLocalProfile(ownerKey);
            String previousRemoteUrl = normalizeOptional(local.photoRemoteUrl);
            String nextRemoteUrl = normalizeOptional(remote.profilePhotoUrl);

            local.fullName = normalizeName(remote.name, ownerKey);
            local.courseSection = normalizeOptional(remote.courseSection);
            local.photoRemoteUrl = nextRemoteUrl;

            boolean remotePhotoChanged = !TextUtils.equals(previousRemoteUrl, nextRemoteUrl);
            if (TextUtils.isEmpty(nextRemoteUrl)) {
                local.photoLocalPath = null;
            } else if (remotePhotoChanged || TextUtils.isEmpty(local.photoLocalPath)) {
                String downloaded = downloadRemotePhoto(ownerKey, nextRemoteUrl);
                if (!TextUtils.isEmpty(downloaded)) {
                    local.photoLocalPath = downloaded;
                }
            } else if (!TextUtils.isEmpty(local.photoLocalPath)) {
                File localPhoto = new File(local.photoLocalPath);
                if (!localPhoto.exists()) {
                    String downloaded = downloadRemotePhoto(ownerKey, nextRemoteUrl);
                    if (!TextUtils.isEmpty(downloaded)) {
                        local.photoLocalPath = downloaded;
                    }
                }
            }

            local.syncState = UserProfileEntity.SYNC_STATE_CLEAN;
            local.pendingPhotoAction = UserProfileEntity.PHOTO_ACTION_NONE;
            local.lastSyncedAt = System.currentTimeMillis();
            local.updatedAt = System.currentTimeMillis();
            db.userProfileDao().upsert(local);
            updateSessionProfile(local);
            return hasMeaningfulServerData(remote, ownerKey);
        } catch (Exception e) {
            Log.w(TAG, "Pull profile failed", e);
            return false;
        }
    }

    private UserProfileEntity ensureLocalProfile(@NonNull String ownerKey) {
        UserProfileEntity local = db.userProfileDao().getByOwnerSync(ownerKey);
        if (local != null) {
            return local;
        }

        long now = System.currentTimeMillis();

        local = new UserProfileEntity();
        local.ownerStudentId = ownerKey;
        local.fullName = normalizeName(session.getUserName(), ownerKey);
        local.courseSection = normalizeOptional(session.getProfileCourseSection());
        local.photoLocalPath = copyUriOrFileToProfileImage(ownerKey, session.getProfilePhotoUri());
        local.photoRemoteUrl = null;
        local.syncState = UserProfileEntity.SYNC_STATE_CLEAN;
        local.pendingPhotoAction = UserProfileEntity.PHOTO_ACTION_NONE;
        local.updatedAt = now;
        local.lastSyncedAt = 0L;

        db.userProfileDao().upsert(local);
        return local;
    }

    private boolean hasLocalLegacyData(@NonNull UserProfileEntity local, @NonNull String ownerKey) {
        if (!TextUtils.isEmpty(local.courseSection)) {
            return true;
        }
        if (!TextUtils.isEmpty(local.photoLocalPath)) {
            return true;
        }
        String normalizedName = normalizeName(local.fullName, ownerKey);
        return !TextUtils.equals(normalizedName, ownerKey);
    }

    private boolean hasMeaningfulServerData(@NonNull ProfileDto remote, @NonNull String ownerKey) {
        String normalizedName = normalizeName(remote.name, ownerKey);
        if (!TextUtils.equals(normalizedName, ownerKey)) {
            return true;
        }
        return !TextUtils.isEmpty(normalizeOptional(remote.courseSection))
                || !TextUtils.isEmpty(normalizeOptional(remote.profilePhotoUrl));
    }

    @Nullable
    private String copyUriOrFileToProfileImage(@NonNull String ownerKey, @Nullable String photoRef) {
        String normalizedRef = normalizeOptional(photoRef);
        if (TextUtils.isEmpty(normalizedRef)) {
            return null;
        }

        File destination = new File(getProfileImageDir(), ownerKey + "_profile.jpg");

        try {
            File maybeFile = new File(normalizedRef);
            if (maybeFile.exists() && maybeFile.isFile()) {
                copyStreams(new FileInputStream(maybeFile), new FileOutputStream(destination));
                return destination.getAbsolutePath();
            }

            Uri uri = Uri.parse(normalizedRef);
            if (uri == null) {
                return null;
            }

            String scheme = uri.getScheme();
            if (scheme == null || scheme.isBlank()) {
                return null;
            }

            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return null;
            }

            InputStream input = appContext.getContentResolver().openInputStream(uri);
            if (input == null) {
                return null;
            }

            copyStreams(input, new FileOutputStream(destination));
            return destination.getAbsolutePath();
        } catch (Exception e) {
            Log.w(TAG, "Failed to cache profile photo", e);
            return null;
        }
    }

    @Nullable
    private String downloadRemotePhoto(@NonNull String ownerKey, @NonNull String remoteUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(remoteUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.connect();

            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                return null;
            }

            File target = new File(getProfileImageDir(), ownerKey + "_profile_remote.jpg");
            InputStream input = connection.getInputStream();
            if (input == null) {
                return null;
            }

            copyStreams(input, new FileOutputStream(target));
            return target.getAbsolutePath();
        } catch (Exception e) {
            Log.w(TAG, "Failed to download remote profile photo", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private File getProfileImageDir() {
        File dir = new File(appContext.getFilesDir(), "profile_images");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    private void copyStreams(@NonNull InputStream in, @NonNull FileOutputStream out) throws Exception {
        try (InputStream input = in; FileOutputStream output = out) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        }
    }

    @Nullable
    private String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @NonNull
    private String normalizeName(@Nullable String rawName, @NonNull String ownerKey) {
        String fallback = ownerKey.trim();
        if (rawName == null) {
            return fallback;
        }
        String trimmed = rawName.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    @Nullable
    private String getDisplayPhotoRef(@NonNull UserProfileEntity profile) {
        if (!TextUtils.isEmpty(profile.photoLocalPath)) {
            return profile.photoLocalPath;
        }
        return normalizeOptional(profile.photoRemoteUrl);
    }

    private String getOwnerKey() {
        String owner = requireOwnerKey();
        return owner != null ? owner : "__no_owner__";
    }

    @Nullable
    private String requireOwnerKey() {
        return normalizeOptional(session.getStudentId());
    }

    private <T> boolean isSuccess(Response<ApiResponse<T>> response) {
        return response.isSuccessful()
                && response.body() != null
                && response.body().isSuccess();
    }

    private void updateSessionProfile(@NonNull UserProfileEntity profile) {
        session.setUserName(profile.fullName);
        session.setProfileCourseSection(profile.courseSection);
        session.setProfilePhotoUri(getDisplayPhotoRef(profile));
    }
}
