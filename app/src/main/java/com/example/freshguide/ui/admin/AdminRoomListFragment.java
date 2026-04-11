package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.RoomDto;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRoomListFragment extends Fragment {

    private static final class RoomRow {
        final GenericListAdapter.Item item;
        final String searchText;

        RoomRow(@NonNull GenericListAdapter.Item item, @NonNull String searchText) {
            this.item = item;
            this.searchText = searchText;
        }
    }

    private GenericListAdapter adapter;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final List<RoomRow> allRoomRows = new ArrayList<>();
    private EditText searchInput;
    private String searchQuery = "";
    private final TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            searchQuery = s == null ? "" : s.toString();
            applyRoomFilter();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AdminNavigationUtils.bindBackToDashboard(this, view);
        AdminNavigationUtils.showSearch(view, true);

        ((TextView) view.findViewById(R.id.tv_admin_page_title)).setText("Rooms");
        ((TextView) view.findViewById(R.id.tv_admin_page_subtitle))
                .setText("Manage room records students see in search, map drill-down, and room detail views.");
        searchInput = view.findViewById(R.id.et_admin_search);
        if (searchInput != null) {
            searchInput.addTextChangedListener(searchWatcher);
        }

        adapter = new GenericListAdapter();
        adapter.setIconActionsEnabled(true);
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        adapter.setOnItemClickListener((position, item) -> {
            if (item == null || item.id <= 0) {
                return;
            }
            openRoomForm(item.id);
        });
        adapter.setOnActionListener(new GenericListAdapter.OnActionListener() {
            @Override
            public void onEdit(int position, int id) {
                openRoomForm(id);
            }

            @Override
            public void onDelete(int position, int id) {
                AdminDialogUtils.showDestructiveConfirmation(
                        AdminRoomListFragment.this,
                        "Delete Room",
                        "Are you sure?",
                        "Delete",
                        () -> deleteRoom(id, view)
                );
            }
        });

        view.findViewById(R.id.fab_add).setOnClickListener(v -> openRoomForm(-1));

        loadRooms();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRooms();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchInput != null) {
            searchInput.removeTextChangedListener(searchWatcher);
            searchInput = null;
        }
    }

    private void loadRooms() {
        ApiClient.getInstance(requireContext()).getApiService()
                .adminGetRooms()
                .enqueue(new Callback<ApiResponse<List<RoomDto>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<RoomDto>>> call, Response<ApiResponse<List<RoomDto>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            List<RoomDto> rooms = response.body().getData();
                            if (rooms == null) {
                                rooms = new ArrayList<>();
                            }
                            rooms.sort(Comparator.comparingInt(room -> room.id));

                            if (!isAdded()) {
                                return;
                            }
                            replaceRoomRows(buildRowsFromDtos(rooms));
                            return;
                        }

                        loadRoomsFromLocalFallback();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<RoomDto>>> call, Throwable t) {
                        loadRoomsFromLocalFallback();
                    }
                });
    }

    private void loadRoomsFromLocalFallback() {
        final android.content.Context appContext = requireContext().getApplicationContext();
        final AppDatabase db = AppDatabase.getInstance(appContext);
        ioExecutor.execute(() -> {
            List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();
            rooms.sort(Comparator.comparingInt(room -> room.id));
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    replaceRoomRows(buildRowsFromEntities(rooms));
                }
            });
        });
    }

    private List<RoomRow> buildRowsFromDtos(@NonNull List<RoomDto> rooms) {
        List<RoomRow> rows = new ArrayList<>();
        for (RoomDto room : rooms) {
            if (room == null) {
                continue;
            }
            String name = safeText(room.name, "Room " + room.id);
            String code = safeText(room.code, "-");
            String type = safeText(room.type, "unknown");
            String subtitle = "#" + room.id + " · " + code + " · " + type;
            GenericListAdapter.Item item = new GenericListAdapter.Item(room.id, name, subtitle);
            rows.add(new RoomRow(item, buildSearchText(name, code, type)));
        }
        return rows;
    }

    private List<RoomRow> buildRowsFromEntities(@NonNull List<RoomEntity> rooms) {
        List<RoomRow> rows = new ArrayList<>();
        for (RoomEntity room : rooms) {
            if (room == null) {
                continue;
            }
            String name = safeText(room.name, "Room " + room.id);
            String code = safeText(room.code, "-");
            String type = safeText(room.type, "unknown");
            String subtitle = "#" + room.id + " · " + code + " · " + type;
            GenericListAdapter.Item item = new GenericListAdapter.Item(room.id, name, subtitle);
            rows.add(new RoomRow(item, buildSearchText(name, code, type)));
        }
        return rows;
    }

    private void replaceRoomRows(@NonNull List<RoomRow> rows) {
        allRoomRows.clear();
        allRoomRows.addAll(rows);
        applyRoomFilter();
    }

    private void applyRoomFilter() {
        if (adapter == null) {
            return;
        }
        String normalizedQuery = searchQuery == null
                ? ""
                : searchQuery.trim().toLowerCase(Locale.ROOT);
        List<GenericListAdapter.Item> filtered = new ArrayList<>();
        for (RoomRow row : allRoomRows) {
            if (normalizedQuery.isEmpty() || row.searchText.contains(normalizedQuery)) {
                filtered.add(row.item);
            }
        }
        adapter.setItems(filtered);
    }

    @NonNull
    private String safeText(@Nullable String value, @NonNull String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    @NonNull
    private String buildSearchText(@NonNull String name, @NonNull String code, @NonNull String type) {
        return (name + " " + code + " " + type).toLowerCase(Locale.ROOT);
    }

    private void openRoomForm(int roomId) {
        if (!isAdded()) {
            return;
        }

        NavController navController = NavHostFragment.findNavController(this);
        if (navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.adminRoomListFragment) {
            return;
        }

        if (roomId <= 0) {
            navController.navigate(R.id.action_adminRoomList_to_adminRoomForm);
            return;
        }

        Bundle args = new Bundle();
        args.putInt("roomId", roomId);
        navController.navigate(R.id.action_adminRoomList_to_adminRoomForm, args);
    }

    private void deleteRoom(int roomId, @NonNull View view) {
        final android.content.Context appContext = requireContext().getApplicationContext();
        final AppDatabase db = AppDatabase.getInstance(appContext);

        ApiClient.getInstance(requireContext()).getApiService()
                .adminDeleteRoom(roomId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ioExecutor.execute(() -> {
                                RoomEntity local = db.roomDao().getByIdSync(roomId);
                                if (local != null) {
                                    db.roomDao().delete(local);
                                }
                                if (!isAdded()) {
                                    return;
                                }
                                requireActivity().runOnUiThread(() -> {
                                    Snackbar.make(view, "Room deleted", Snackbar.LENGTH_SHORT).show();
                                    loadRooms();
                                });
                            });
                            return;
                        }
                        Snackbar.make(view, "Delete failed", Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Snackbar.make(view, "Network error: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }
}
