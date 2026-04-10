package com.example.freshguide.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.freshguide.database.dao.BuildingDao;
import com.example.freshguide.database.dao.FacilityDao;
import com.example.freshguide.database.dao.FloorDao;
import com.example.freshguide.database.dao.OriginDao;
import com.example.freshguide.database.dao.RoomDao;
import com.example.freshguide.database.dao.RouteDao;
import com.example.freshguide.database.dao.SavedRoomDao;
import com.example.freshguide.database.dao.ScheduleDao;
import com.example.freshguide.database.dao.SyncMetaDao;
import com.example.freshguide.database.dao.UserProfileDao;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.RoomFacilityCrossRef;
import com.example.freshguide.model.entity.RouteEntity;
import com.example.freshguide.model.entity.RouteStepEntity;
import com.example.freshguide.model.entity.SavedRoomEntity;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.model.entity.SyncMetaEntity;
import com.example.freshguide.model.entity.UserProfileEntity;

@Database(
    entities = {
        BuildingEntity.class,
        FloorEntity.class,
        RoomEntity.class,
        FacilityEntity.class,
        RoomFacilityCrossRef.class,
        SavedRoomEntity.class,
        OriginEntity.class,
        RouteEntity.class,
        RouteStepEntity.class,
        ScheduleEntryEntity.class,
        SyncMetaEntity.class,
        UserProfileEntity.class
    },
    version = 9,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "fresh_guide_db";
    private static volatile AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE rooms ADD COLUMN image_url TEXT");
            database.execSQL("ALTER TABLE rooms ADD COLUMN location TEXT");
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS schedule_entries ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "title TEXT, "
                    + "course_code TEXT, "
                    + "instructor TEXT, "
                    + "notes TEXT, "
                    + "color_hex TEXT, "
                    + "day_of_week INTEGER NOT NULL, "
                    + "start_minutes INTEGER NOT NULL, "
                    + "end_minutes INTEGER NOT NULL, "
                    + "is_online INTEGER NOT NULL, "
                    + "room_id INTEGER, "
                    + "online_platform TEXT, "
                    + "reminder_minutes INTEGER NOT NULL, "
                    + "created_at INTEGER NOT NULL, "
                    + "updated_at INTEGER NOT NULL, "
                    + "FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE SET NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_day_of_week ON schedule_entries(day_of_week)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_room_id ON schedule_entries(room_id)");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE rooms ADD COLUMN cached_image_path TEXT");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE schedule_entries ADD COLUMN remote_id INTEGER");
            database.execSQL("ALTER TABLE schedule_entries ADD COLUMN client_uuid TEXT");
            database.execSQL("ALTER TABLE schedule_entries ADD COLUMN owner_student_id TEXT");
            database.execSQL("ALTER TABLE schedule_entries ADD COLUMN sync_state INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE schedule_entries ADD COLUMN pending_delete INTEGER NOT NULL DEFAULT 0");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_owner_student_id ON schedule_entries(owner_student_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_remote_id ON schedule_entries(remote_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_client_uuid ON schedule_entries(client_uuid)");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS schedule_entries");
            database.execSQL("CREATE TABLE IF NOT EXISTS schedule_entries ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "title TEXT, "
                    + "course_code TEXT, "
                    + "instructor TEXT, "
                    + "notes TEXT, "
                    + "color_hex TEXT, "
                    + "day_of_week INTEGER NOT NULL, "
                    + "start_minutes INTEGER NOT NULL, "
                    + "end_minutes INTEGER NOT NULL, "
                    + "is_online INTEGER NOT NULL, "
                    + "room_id INTEGER, "
                    + "online_platform TEXT, "
                    + "reminder_minutes INTEGER NOT NULL, "
                    + "created_at INTEGER NOT NULL, "
                    + "updated_at INTEGER NOT NULL, "
                    + "remote_id INTEGER, "
                    + "client_uuid TEXT, "
                    + "owner_student_id TEXT, "
                    + "sync_state INTEGER NOT NULL DEFAULT 0, "
                    + "pending_delete INTEGER NOT NULL DEFAULT 0, "
                    + "FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE SET NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_day_of_week ON schedule_entries(day_of_week)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_room_id ON schedule_entries(room_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_owner_student_id ON schedule_entries(owner_student_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_remote_id ON schedule_entries(remote_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_client_uuid ON schedule_entries(client_uuid)");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("PRAGMA foreign_keys=OFF");
            database.execSQL("CREATE TABLE IF NOT EXISTS routes_new ("
                    + "id INTEGER NOT NULL, "
                    + "origin_id INTEGER NOT NULL, "
                    + "destination_room_id INTEGER NOT NULL, "
                    + "description TEXT, "
                    + "instruction TEXT, "
                    + "PRIMARY KEY(id), "
                    + "FOREIGN KEY(origin_id) REFERENCES origins(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY(destination_room_id) REFERENCES rooms(id) ON DELETE CASCADE)");
            database.execSQL("INSERT INTO routes_new (id, origin_id, destination_room_id, description, instruction) "
                    + "SELECT id, origin_id, destination_room_id, description, NULL FROM routes");
            database.execSQL("DROP TABLE routes");
            database.execSQL("ALTER TABLE routes_new RENAME TO routes");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_routes_origin_id ON routes(origin_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_routes_destination_room_id ON routes(destination_room_id)");
            database.execSQL("PRAGMA foreign_keys=ON");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS user_profiles ("
                    + "owner_student_id TEXT NOT NULL, "
                    + "full_name TEXT, "
                    + "course_section TEXT, "
                    + "photo_local_path TEXT, "
                    + "photo_remote_url TEXT, "
                    + "sync_state INTEGER NOT NULL DEFAULT 1, "
                    + "pending_photo_action INTEGER NOT NULL DEFAULT 0, "
                    + "updated_at INTEGER NOT NULL DEFAULT 0, "
                    + "last_synced_at INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(owner_student_id))");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_user_profiles_sync_state ON user_profiles(sync_state)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_user_profiles_updated_at ON user_profiles(updated_at)");
        }
    };

    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS saved_rooms ("
                    + "owner_student_id TEXT NOT NULL, "
                    + "room_id INTEGER NOT NULL, "
                    + "saved_at INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(owner_student_id, room_id), "
                    + "FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_saved_rooms_owner_student_id ON saved_rooms(owner_student_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_saved_rooms_room_id ON saved_rooms(room_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_saved_rooms_owner_student_id_saved_at ON saved_rooms(owner_student_id, saved_at)");
        }
    };

    public abstract BuildingDao buildingDao();
    public abstract FloorDao floorDao();
    public abstract RoomDao roomDao();
    public abstract SavedRoomDao savedRoomDao();
    public abstract FacilityDao facilityDao();
    public abstract OriginDao originDao();
    public abstract RouteDao routeDao();
    public abstract ScheduleDao scheduleDao();
    public abstract SyncMetaDao syncMetaDao();
    public abstract UserProfileDao userProfileDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DB_NAME
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
             .build();
        }
        return instance;
    }
}
