package com.example.freshguide.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.database.AppDatabase;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.network.ApiService;
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRoomListFragment extends Fragment {

    private GenericListAdapter adapter;

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
                .setText("Manage the records students see in search, map drill-down, and room detail views.");

        adapter = new GenericListAdapter();
        adapter.setIconActionsEnabled(true);
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminRoomList_to_adminRoomForm));

        adapter.setOnActionListener(new GenericListAdapter.OnActionListener() {
            @Override
            public void onEdit(int position, int id) {
                Bundle args = new Bundle();
                args.putInt("roomId", id);
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminRoomList_to_adminRoomForm, args);
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

        loadRooms();
    }

    private void loadRooms() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<RoomEntity> rooms = AppDatabase.getInstance(requireContext())
                    .roomDao().getAllRoomsSync();
            List<GenericListAdapter.Item> items = new ArrayList<>();
            for (RoomEntity r : rooms) {
                items.add(new GenericListAdapter.Item(r.id, r.name, r.code + " · " + r.type));
            }
            requireActivity().runOnUiThread(() -> adapter.setItems(items));
        });
    }

    private void deleteRoom(int id, View view) {
        ApiClient.getInstance(requireContext()).getApiService()
                .adminDeleteRoom(id).enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> r) {
                        Snackbar.make(view, "Room deleted", Snackbar.LENGTH_SHORT).show();
                        loadRooms();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Snackbar.make(view, "Delete failed", Snackbar.LENGTH_LONG).show();
                    }
                });
    }
}
