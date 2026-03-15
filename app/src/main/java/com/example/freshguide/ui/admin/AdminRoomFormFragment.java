package com.example.freshguide.ui.admin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.freshguide.R;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.RoomDto;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRoomFormFragment extends Fragment {

    // Image handling components
    private ImageView ivRoomPreview;
    private TextView tvImagePlaceholder;
    private Button btnRemoveImage;
    private Button btnSelectImage;
    private Uri selectedImageUri;
    private File compressedImageFile;
    private int currentRoomId = -1;
    private boolean isEditMode = false;
    
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    
    // Permission launcher for image access
    private final ActivityResultLauncher<String> requestPermissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                showSnackbar("Permission required to select images");
            }
        });
    
    // Image picker launcher
    private final ActivityResultLauncher<String> imagePickerLauncher = 
        registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleImageSelected);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_room_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentRoomId = getArguments() != null ? getArguments().getInt("roomId", -1) : -1;
        isEditMode = currentRoomId != -1;

        // Standard form elements
        EditText etName = view.findViewById(R.id.et_room_name);
        EditText etCode = view.findViewById(R.id.et_room_code);
        EditText etType = view.findViewById(R.id.et_room_type);
        EditText etFloorId = view.findViewById(R.id.et_floor_id);
        EditText etDescription = view.findViewById(R.id.et_room_description);
        Button btnSave = view.findViewById(R.id.btn_save);

        // Image handling elements
        ivRoomPreview = view.findViewById(R.id.iv_room_preview);
        tvImagePlaceholder = view.findViewById(R.id.tv_image_placeholder);
        btnRemoveImage = view.findViewById(R.id.btn_remove_image);
        btnSelectImage = view.findViewById(R.id.btn_select_image);

        btnSave.setText(isEditMode ? "Update Room" : "Create Room");
        
        setupImageHandling();
        
        if (isEditMode) {
            loadRoomForEdit(currentRoomId);
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String code = etCode.getText().toString().trim().toUpperCase(Locale.ROOT);
            String type = etType.getText().toString().trim().toLowerCase(Locale.ROOT);
            String floorIdStr = etFloorId.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();

            if (name.isEmpty() || code.isEmpty() || floorIdStr.isEmpty()) {
                Snackbar.make(view, "Name, code, and floor ID are required", Snackbar.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("code", code);
            body.put("type", type);
            body.put("floor_id", Integer.parseInt(floorIdStr));
            body.put("description", desc);

            ApiService api = ApiClient.getInstance(requireContext()).getApiService();
            Call<ApiResponse<RoomDto>> call = isEditMode
                    ? api.adminUpdateRoom(currentRoomId, body)
                    : api.adminCreateRoom(body);

            call.enqueue(new Callback<ApiResponse<RoomDto>>() {
                @Override
                public void onResponse(Call<ApiResponse<RoomDto>> c, Response<ApiResponse<RoomDto>> r) {
                    if (r.isSuccessful() && r.body() != null && r.body().isSuccess()) {
                        RoomDto savedRoom = r.body().getData();
                        
                        // Handle image upload after successful room save
                        if (compressedImageFile != null && savedRoom != null) {
                            uploadRoomImage(savedRoom.id, view);
                        } else {
                            Navigation.findNavController(view).popBackStack();
                        }
                    } else {
                        showSnackbar("Failed to save room");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<RoomDto>> c, Throwable t) {
                    showSnackbar("Network error");
                }
            });
        });
    }

    private void setupImageHandling() {
        btnSelectImage.setOnClickListener(v -> checkPermissionAndSelectImage());
        btnRemoveImage.setOnClickListener(v -> removeSelectedImage());
    }
    
    private void checkPermissionAndSelectImage() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
                
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }
    
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }
    
    private void handleImageSelected(Uri imageUri) {
        if (imageUri == null) return;
        
        selectedImageUri = imageUri;
        
        imageExecutor.execute(() -> {
            try {
                // Validate landscape orientation
                if (!isLandscapeImage(imageUri)) {
                    requireActivity().runOnUiThread(() -> 
                        showSnackbar("Please select a landscape (horizontal) image"));
                    return;
                }
                
                // Compress image
                compressedImageFile = compressImage(imageUri);
                
                // Load preview on UI thread
                requireActivity().runOnUiThread(() -> loadImagePreview(imageUri));
                
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> 
                    showSnackbar("Failed to process image: " + e.getMessage()));
            }
        });
    }
    
    private boolean isLandscapeImage(Uri imageUri) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream stream = requireContext().getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(stream, null, options);
            if (stream != null) stream.close();
            
            return options.outWidth > options.outHeight;
        } catch (Exception e) {
            return false;
        }
    }
    
    private File compressImage(Uri sourceUri) throws IOException {
        // Load original bitmap
        Bitmap original = MediaStore.Images.Media.getBitmap(
                requireContext().getContentResolver(), sourceUri);
        
        // Calculate target dimensions maintaining aspect ratio
        int targetWidth = Math.min(1280, original.getWidth());
        int targetHeight = (int) (targetWidth * ((float) original.getHeight() / original.getWidth()));
        
        // Scale bitmap
        Bitmap compressed = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true);
        
        // Save to temporary file
        File tempFile = new File(requireContext().getCacheDir(), 
                "room_image_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream out = new FileOutputStream(tempFile);
        compressed.compress(Bitmap.CompressFormat.JPEG, 85, out);
        out.flush();
        out.close();
        
        // Clean up
        if (!original.isRecycled()) original.recycle();
        if (!compressed.isRecycled()) compressed.recycle();
        
        return tempFile;
    }
    
    private void loadImagePreview(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    requireContext().getContentResolver(), imageUri);
            ivRoomPreview.setImageBitmap(bitmap);
            showImagePreview();
        } catch (Exception e) {
            showSnackbar("Failed to load image preview");
        }
    }
    
    private void showImagePreview() {
        ivRoomPreview.setVisibility(View.VISIBLE);
        btnRemoveImage.setVisibility(View.VISIBLE);
        tvImagePlaceholder.setVisibility(View.GONE);
    }
    
    private void hideImagePreview() {
        ivRoomPreview.setVisibility(View.GONE);
        btnRemoveImage.setVisibility(View.GONE);
        tvImagePlaceholder.setVisibility(View.VISIBLE);
    }
    
    private void removeSelectedImage() {
        selectedImageUri = null;
        if (compressedImageFile != null && compressedImageFile.exists()) {
            compressedImageFile.delete();
        }
        compressedImageFile = null;
        hideImagePreview();
        
        // If in edit mode, delete from backend
        if (isEditMode && currentRoomId != -1) {
            deleteRoomImage(currentRoomId);
        }
    }
    
    private void uploadRoomImage(int roomId, View view) {
        if (compressedImageFile == null || !compressedImageFile.exists()) {
            Navigation.findNavController(view).popBackStack();
            return;
        }
        
        RequestBody imageBody = RequestBody.create(
                MediaType.parse("image/jpeg"), compressedImageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                "image", compressedImageFile.getName(), imageBody);
        
        ApiService api = ApiClient.getInstance(requireContext()).getApiService();
        api.adminUploadRoomImage(roomId, imagePart).enqueue(new Callback<ApiResponse<RoomDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<RoomDto>> call, Response<ApiResponse<RoomDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    showSnackbar("Room and image saved successfully");
                } else {
                    showSnackbar("Room saved, but image upload failed");
                }
                Navigation.findNavController(view).popBackStack();
            }
            
            @Override
            public void onFailure(Call<ApiResponse<RoomDto>> call, Throwable t) {
                showSnackbar("Room saved, but image upload failed: " + t.getMessage());
                Navigation.findNavController(view).popBackStack();
            }
        });
    }
    
    private void deleteRoomImage(int roomId) {
        ApiService api = ApiClient.getInstance(requireContext()).getApiService();
        api.adminDeleteRoomImage(roomId).enqueue(new Callback<ApiResponse<RoomDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<RoomDto>> call, Response<ApiResponse<RoomDto>> response) {
                if (response.isSuccessful()) {
                    showSnackbar("Image removed");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<RoomDto>> call, Throwable t) {
                showSnackbar("Failed to remove image");
            }
        });
    }
    
    private void loadRoomForEdit(int roomId) {
        ApiService api = ApiClient.getInstance(requireContext()).getApiService();
        // Use the existing user room endpoint since it returns the same data
        api.getRoom(roomId).enqueue(new Callback<ApiResponse<RoomDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<RoomDto>> call, Response<ApiResponse<RoomDto>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    RoomDto room = response.body().getData();
                    populateFormFields(room);
                    loadExistingImage(room.imageUrl);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<RoomDto>> call, Throwable t) {
                showSnackbar("Failed to load room data");
            }
        });
    }
    
    private void populateFormFields(RoomDto room) {
        if (getView() == null) return;
        
        EditText etName = getView().findViewById(R.id.et_room_name);
        EditText etCode = getView().findViewById(R.id.et_room_code);
        EditText etType = getView().findViewById(R.id.et_room_type);
        EditText etFloorId = getView().findViewById(R.id.et_floor_id);
        EditText etDescription = getView().findViewById(R.id.et_room_description);
        
        etName.setText(room.name != null ? room.name : "");
        etCode.setText(room.code != null ? room.code : "");
        etType.setText(room.type != null ? room.type : "");
        etFloorId.setText(String.valueOf(room.floorId));
        etDescription.setText(room.description != null ? room.description : "");
    }
    
    private void loadExistingImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }
        
        imageExecutor.execute(() -> {
            try {
                // Simple image loading for existing images
                // Use the same pattern as RoomDetailFragment
                java.net.URL url = new java.net.URL(imageUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                
                InputStream inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                connection.disconnect();
                
                if (bitmap != null) {
                    requireActivity().runOnUiThread(() -> {
                        ivRoomPreview.setImageBitmap(bitmap);
                        showImagePreview();
                    });
                }
            } catch (Exception e) {
                // Silently handle - existing image load is optional
            }
        });
    }
    
    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageExecutor != null && !imageExecutor.isShutdown()) {
            imageExecutor.shutdown();
        }
        
        // Clean up temporary files
        if (compressedImageFile != null && compressedImageFile.exists()) {
            compressedImageFile.delete();
        }
    }
}
