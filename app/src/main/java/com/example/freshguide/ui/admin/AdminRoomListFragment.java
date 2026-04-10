package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRoomListFragment extends Fragment {

    private GenericListAdapter adapter;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private List<RoomEntity> currentRooms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.tv_admin_page_title)).setText("Rooms");
        ((TextView) view.findViewById(R.id.tv_admin_page_subtitle))
                .setText("Manage room records students see in search, map drill-down, and room detail views.");

        adapter = new GenericListAdapter();
        adapter.setIconActionsEnabled(true);
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        adapter.setOnItemClickListener((position, item) -> {
            if (position < 0 || position >= currentRooms.size()) {
                return;
            }
            openRoomForm(currentRooms.get(position).id);
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

    private void loadRooms() {
        final android.content.Context appContext = requireContext().getApplicationContext();
        final AppDatabase db = AppDatabase.getInstance(appContext);
        ioExecutor.execute(() -> {
            List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();
            currentRooms = rooms;
            List<GenericListAdapter.Item> items = new ArrayList<>();
            for (RoomEntity r : rooms) {
                items.add(new GenericListAdapter.Item(r.id, r.name, r.code + " · " + r.type));
            }
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (isAdded()) {
                    adapter.setItems(items);
                }
            });
        });
    }

    private void openRoomForm(int roomId) {
        if (!isAdded()) {
            return;
        }

        if (roomId <= 0) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_adminRoomList_to_adminRoomForm);
            return;
        }

        Bundle args = new Bundle();
        args.putInt("roomId", roomId);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_adminRoomList_to_adminRoomForm, args);
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
