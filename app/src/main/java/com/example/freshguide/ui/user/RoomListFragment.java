package com.example.freshguide.ui.user;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.freshguide.R;
import com.example.freshguide.model.ui.RoomSearchResult;
import com.example.freshguide.ui.adapter.RoomAdapter;
import com.example.freshguide.viewmodel.RoomListViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomListFragment extends Fragment {

    public static final String KEY_MAP_FOCUS_REQUEST = "map_focus_request";

    private static final String PREFS_SEARCH = "room_search_state";
    private static final String KEY_RECENT_ROOM_IDS = "recent_room_ids";
    private static final int MAX_RECENT_ROOMS = 6;
    private static final String BUILDING_MAIN = "MAIN";

    private RoomListViewModel viewModel;
    private RoomAdapter adapter;
    private EditText etSearch;
    private ImageButton btnClear;
    private TextView btnClearHistory;
    private TextView tvSectionTitle;
    private TextView tvContextLabel;
    private TextView tvEmptyMessage;
    private View emptyState;
    private View sectionHeader;
    private RecyclerView recyclerView;

    private final List<RoomSearchResult> currentRooms = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RoomListViewModel.class);

        adapter = new RoomAdapter();
        recyclerView = view.findViewById(R.id.recycler_rooms);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        etSearch = view.findViewById(R.id.et_search);
        btnClear = view.findViewById(R.id.btn_clear);
        btnClearHistory = view.findViewById(R.id.btn_clear_history);
        tvSectionTitle = view.findViewById(R.id.tv_section_title);
        tvContextLabel = view.findViewById(R.id.tv_context_label);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        emptyState = view.findViewById(R.id.layout_empty_state);
        sectionHeader = view.findViewById(R.id.layout_section_header);

        NavController nav = Navigation.findNavController(view);
        adapter.setOnItemClickListener(room -> {
            saveRecentRoom(room.roomId);
            if (canOpenIndoorMap(room) && nav.getPreviousBackStackEntry() != null) {
                Bundle focusRequest = new Bundle();
                focusRequest.putInt("roomId", room.roomId);
                focusRequest.putInt("floorNumber", room.floorNumber);
                focusRequest.putString("roomName", room.getDisplayName());
                focusRequest.putString("buildingCode", room.buildingCode);
                focusRequest.putString("buildingName", room.buildingName);
                focusRequest.putBoolean("fromSearch", true);
                nav.getPreviousBackStackEntry()
                        .getSavedStateHandle()
                        .set(KEY_MAP_FOCUS_REQUEST, focusRequest);
                nav.popBackStack();
            } else {
                Bundle args = new Bundle();
                args.putInt("roomId", room.roomId);
                args.putString("roomName", room.getDisplayName());
                nav.navigate(R.id.action_roomList_to_roomDetail, args);
            }
        });
        adapter.setOnActionClickListener(room -> {
            etSearch.setText(room.getDisplayName());
            etSearch.setSelection(etSearch.getText().length());
        });

        view.findViewById(R.id.btn_back).setOnClickListener(v -> {
            v.animate().cancel();
            v.animate()
                    .alpha(0.9f)
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(90)
                    .withEndAction(() -> {
                        v.setAlpha(1f);
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        nav.navigateUp();
                    })
                    .start();
        });
        btnClear.setOnClickListener(v -> etSearch.setText(""));
        btnClearHistory.setOnClickListener(v -> {
            clearRecentRooms();
            renderState();
        });
        playEntranceAnimation(view);

        Bundle args = getArguments();
        if (args != null) {
            String buildingCode = args.getString("buildingCode", "");
            String buildingName = args.getString("buildingName", "");
            if (!buildingCode.isEmpty()) {
                viewModel.setBuilding(buildingCode);
                tvContextLabel.setText(getString(R.string.search_filtered_in, buildingName));
                tvContextLabel.setVisibility(View.VISIBLE);
            }
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setQuery(s.toString());
                updateClearButtonVisibility(s);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
        updateClearButtonVisibility(etSearch.getText());

        viewModel.getRooms().observe(getViewLifecycleOwner(), rooms -> {
            currentRooms.clear();
            if (rooms != null) {
                currentRooms.addAll(rooms);
            }
            renderState();
        });
    }

    private void renderState() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
        boolean showingRecent = query.isEmpty();

        List<RoomSearchResult> visibleItems = showingRecent
                ? getRecentRooms(currentRooms)
                : new ArrayList<>(currentRooms);

        adapter.setDisplayMode(showingRecent ? RoomAdapter.MODE_RECENT : RoomAdapter.MODE_SEARCH);
        adapter.submitList(new ArrayList<>(visibleItems));

        boolean hasItems = !visibleItems.isEmpty();
        recyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        emptyState.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        boolean showRecentHeader = showingRecent && hasItems;
        sectionHeader.setVisibility(showRecentHeader ? View.VISIBLE : View.GONE);
        tvSectionTitle.setVisibility(showRecentHeader ? View.VISIBLE : View.GONE);
        btnClearHistory.setVisibility(showRecentHeader ? View.VISIBLE : View.GONE);
        tvEmptyMessage.setText(showingRecent
                ? getString(R.string.search_empty_default)
                : getString(R.string.search_no_results));
    }

    private List<RoomSearchResult> getRecentRooms(List<RoomSearchResult> source) {
        List<Integer> recentIds = getRecentRoomIds();
        if (recentIds.isEmpty() || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<RoomSearchResult> recentRooms = new ArrayList<>();
        for (int roomId : recentIds) {
            RoomSearchResult room = findRoomById(source, roomId);
            if (room != null) {
                recentRooms.add(room);
            }
        }
        return recentRooms;
    }

    @Nullable
    private RoomSearchResult findRoomById(List<RoomSearchResult> rooms, int roomId) {
        for (RoomSearchResult room : rooms) {
            if (room != null && room.roomId == roomId) {
                return room;
            }
        }
        return null;
    }

    private void saveRecentRoom(int roomId) {
        List<Integer> ids = getRecentRoomIds();
        ids.remove(Integer.valueOf(roomId));
        ids.add(0, roomId);
        while (ids.size() > MAX_RECENT_ROOMS) {
            ids.remove(ids.size() - 1);
        }
        persistRecentRoomIds(ids);
    }

    private List<Integer> getRecentRoomIds() {
        Context context = getContext();
        if (context == null) {
            return new ArrayList<>();
        }

        String stored = context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .getString(KEY_RECENT_ROOM_IDS, "");

        List<Integer> ids = new ArrayList<>();
        if (stored == null || stored.trim().isEmpty()) {
            return ids;
        }

        String[] parts = stored.split(",");
        for (String part : parts) {
            try {
                ids.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private void persistRecentRoomIds(List<Integer> ids) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(ids.get(i));
        }

        context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RECENT_ROOM_IDS, builder.toString())
                .apply();
    }

    private void clearRecentRooms() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        context.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_RECENT_ROOM_IDS)
                .apply();
    }

    private void updateClearButtonVisibility(CharSequence text) {
        btnClear.setVisibility(text != null && text.length() > 0 ? View.VISIBLE : View.GONE);
    }

    private boolean canOpenIndoorMap(@Nullable RoomSearchResult room) {
        return room != null
                && room.floorNumber > 0
                && room.buildingCode != null
                && BUILDING_MAIN.equalsIgnoreCase(room.buildingCode.trim());
    }

    private void playEntranceAnimation(View root) {
        View searchBar = root.findViewById(R.id.search_bar_container);

        searchBar.setAlpha(0f);
        searchBar.setTranslationY(-3f);
        searchBar.setScaleX(0.996f);
        searchBar.setScaleY(0.996f);
        searchBar.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(320)
                .start();

        recyclerView.setAlpha(0f);
        recyclerView.setTranslationY(4f);
        recyclerView.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(70)
                .setDuration(280)
                .start();

        sectionHeader.setAlpha(0f);
        sectionHeader.setTranslationY(2f);
        sectionHeader.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(45)
                .setDuration(260)
                .start();

        emptyState.setAlpha(0f);
        emptyState.animate()
                .alpha(1f)
                .setStartDelay(70)
                .setDuration(280)
                .start();
    }
}
