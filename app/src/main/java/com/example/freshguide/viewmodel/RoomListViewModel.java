package com.example.freshguide.viewmodel;

import android.app.Application;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.freshguide.model.ui.RoomSearchResult;
import com.example.freshguide.repository.RoomRepository;

import java.util.List;

public class RoomListViewModel extends AndroidViewModel {

    private final RoomRepository repository;

    private final MutableLiveData<String> query        = new MutableLiveData<>("");
    private final MutableLiveData<String> buildingCode = new MutableLiveData<>("");

    /** Combines query + buildingCode changes to trigger a new room source. */
    private final MediatorLiveData<Pair<String, String>> filter = new MediatorLiveData<>();

    private final LiveData<List<RoomSearchResult>> rooms;

    public RoomListViewModel(@NonNull Application application) {
        super(application);
        repository = new RoomRepository(application);

        filter.addSource(query,        q  -> filter.setValue(new Pair<>(q,  buildingCode.getValue())));
        filter.addSource(buildingCode, bc -> filter.setValue(new Pair<>(query.getValue(), bc)));

        rooms = Transformations.switchMap(filter, pair -> {
            String q  = pair.first  != null ? pair.first.trim()  : "";
            String bc = pair.second != null ? pair.second.trim() : "";

            if (!bc.isEmpty()) {
                return repository.searchRoomResultsByBuilding(bc, q);
            } else if (!q.isEmpty()) {
                return repository.searchRoomResults(q);
            } else {
                return repository.getAllSearchResults();
            }
        });
    }

    public LiveData<List<RoomSearchResult>> getRooms() { return rooms; }

    public void setQuery(String q) {
        query.setValue(q != null ? q : "");
    }

    public void setBuilding(String code) {
        buildingCode.setValue(code != null ? code : "");
    }
}
