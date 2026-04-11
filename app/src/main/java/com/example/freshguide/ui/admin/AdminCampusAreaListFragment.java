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
import com.example.freshguide.model.dto.ApiResponse;
import com.example.freshguide.model.dto.RoomDto;
import com.example.freshguide.network.ApiClient;
import com.example.freshguide.ui.adapter.GenericListAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCampusAreaListFragment extends Fragment {

    private static final Set<String> CAMPUS_AREA_CODES =
            new HashSet<>(Arrays.asList("COURT", "REG", "LIB", "ENT", "EXIT"));

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

        ((TextView) view.findViewById(R.id.tv_admin_page_title)).setText("Campus Areas");
        ((TextView) view.findViewById(R.id.tv_admin_page_subtitle))
                .setText("Update mapped landmarks like court, registrar, library, entrance, and exit with student-facing consistency.");

        adapter = new GenericListAdapter();
        adapter.setDeleteEnabled(false);
        RecyclerView recycler = view.findViewById(R.id.recycler_items);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        view.findViewById(R.id.fab_add).setVisibility(View.GONE);

        adapter.setOnActionListener(new GenericListAdapter.OnActionListener() {
            @Override
            public void onEdit(int position, int id) {
                Bundle args = new Bundle();
                args.putInt("roomId", id);
                Navigation.findNavController(view)
                        .navigate(R.id.action_adminCampusAreaList_to_adminCampusAreaForm, args);
            }

            @Override
            public void onDelete(int position, int id) {
                Snackbar.make(view, "Delete is disabled for mapped campus areas", Snackbar.LENGTH_SHORT).show();
            }
        });

        loadCampusAreas(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadCampusAreas(getView());
        }
    }

    private void loadCampusAreas(View view) {
        ApiClient.getInstance(requireContext()).getApiService()
                .adminGetRooms().enqueue(new Callback<ApiResponse<List<RoomDto>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<RoomDto>>> call,
                                           Response<ApiResponse<List<RoomDto>>> response) {
                        if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                            Snackbar.make(view, "Failed to load campus areas", Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        List<RoomDto> allRooms = response.body().getData();
                        List<GenericListAdapter.Item> items = new ArrayList<>();

                        if (allRooms != null) {
                            for (RoomDto room : allRooms) {
                                if (room == null || room.code == null) continue;
                                String normalized = room.code.trim().toUpperCase(Locale.ROOT);
                                if (!CAMPUS_AREA_CODES.contains(normalized)) continue;

                                String subtitle = normalized;
                                if (room.location != null && !room.location.trim().isEmpty()) {
                                    subtitle = normalized + " - " + room.location.trim();
                                }
                                items.add(new GenericListAdapter.Item(room.id, room.name, subtitle));
                            }
                        }

                        items.sort((a, b) -> {
                            int left = sortRank(a.subtitle);
                            int right = sortRank(b.subtitle);
                            return Integer.compare(left, right);
                        });

                        adapter.setItems(items);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<RoomDto>>> call, Throwable t) {
                        Snackbar.make(view, "Network error: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private int sortRank(String subtitle) {
        if (subtitle == null) return 99;
        String upper = subtitle.toUpperCase(Locale.ROOT);
        if (upper.startsWith("COURT")) return 1;
        if (upper.startsWith("REG")) return 2;
        if (upper.startsWith("LIB")) return 3;
        if (upper.startsWith("ENT")) return 4;
        if (upper.startsWith("EXIT")) return 5;
        return 99;
    }
}
