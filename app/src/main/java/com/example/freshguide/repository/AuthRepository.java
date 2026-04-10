package com.example.freshguide.repository;

import android.content.Context;

import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.LoginResponse;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.example.freshguide.util.SessionManager;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(LoginResponse response);
        void onError(String message);
    }

    private final ApiService apiService;
    private final SessionManager session;

    public AuthRepository(Context context) {
        apiService = ApiClient.getInstance(context).getApiService();
        session = SessionManager.getInstance(context);
    }

    public void registerStudent(String studentId, AuthCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("student_id", studentId);

        apiService.registerStudent(body).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call,
                                   Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse data = response.body().getData();
                    session.saveSession(data.getToken(), SessionManager.ROLE_STUDENT,
                            data.getStudentId(), data.getName());
                    callback.onSuccess(data);
                } else {
                    callback.onError(extractError(response, "Registration failed"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void loginStudent(String studentId, AuthCallback callback) {
        registerStudent(studentId, callback);
    }

    public void loginAdmin(String email, String password, AuthCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        apiService.adminLogin(body).enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponse>> call,
                                   Response<ApiResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse data = response.body().getData();
                    session.saveSession(data.getToken(), SessionManager.ROLE_ADMIN,
                            null, data.getName());
                    callback.onSuccess(data);
                } else {
                    callback.onError(extractError(response, "Invalid credentials"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void logout() {
        apiService.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {}
            @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
        });
        session.clearSession();
    }

    private <T> String extractError(Response<ApiResponse<T>> response, String fallback) {
        String err = null;
        int statusCode = response.code();

        ApiResponse<T> body = response.body();
        if (body != null) {
            if (body.getError() != null && !body.getError().isBlank()) {
                err = body.getError();
            } else if (body.getMessage() != null && !body.getMessage().isBlank()) {
                err = body.getMessage();
            }
        }

        if ((err == null || err.isBlank()) && response.errorBody() != null) {
            try {
                String raw = response.errorBody().string();
                JSONObject json = new JSONObject(raw);
                String parsed = json.optString("error");
                if (parsed == null || parsed.isBlank()) {
                    parsed = json.optString("message");
                }
                if ((parsed == null || parsed.isBlank()) && json.has("errors")) {
                    JSONObject errors = json.optJSONObject("errors");
                    if (errors != null && errors.keys().hasNext()) {
                        String firstKey = errors.keys().next();
                        parsed = errors.optJSONArray(firstKey) != null
                                ? errors.optJSONArray(firstKey).optString(0)
                                : errors.optString(firstKey);
                    }
                }
                if (parsed != null && !parsed.isBlank()) {
                    err = parsed;
                } else if (raw != null && !raw.isBlank()) {
                    String compact = raw.replaceAll("\\s+", " ").trim();
                    err = compact.length() > 160 ? compact.substring(0, 160) + "..." : compact;
                }
            } catch (Exception ignored) {
                // Keep fallback if parsing fails.
            }
        }

        if (err == null || err.isBlank()) {
            return fallback + " (HTTP " + statusCode + ")";
        }
        return err + " (HTTP " + statusCode + ")";
    }

}
