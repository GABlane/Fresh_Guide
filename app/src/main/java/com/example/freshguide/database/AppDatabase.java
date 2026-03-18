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
import com.example.freshguide.database.dao.ScheduleDao;
import com.example.freshguide.database.dao.SyncMetaDao;
import com.example.freshguide.model.entity.BuildingEntity;
import com.example.freshguide.model.entity.FacilityEntity;
import com.example.freshguide.model.entity.FloorEntity;
import com.example.freshguide.model.entity.OriginEntity;
import com.example.freshguide.model.entity.RoomEntity;
import com.example.freshguide.model.entity.RoomFacilityCrossRef;
import com.example.freshguide.model.entity.RouteEntity;
import com.example.freshguide.model.entity.RouteStepEntity;
import com.example.freshguide.model.entity.ScheduleEntryEntity;
import com.example.freshguide.model.entity.SyncMetaEntity;

@Database(
    entities = {
        BuildingEntity.class,
        FloorEntity.class,
        RoomEntity.class,
        FacilityEntity.class,
        RoomFacilityCrossRef.class,
        OriginEntity.class,
        RouteEntity.class,
        RouteStepEntity.class,
        ScheduleEntryEntity.class,
        SyncMetaEntity.class
    },
    version = 6,
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

    public abstract BuildingDao buildingDao();
    public abstract FloorDao floorDao();
    public abstract RoomDao roomDao();
    public abstract FacilityDao facilityDao();
    public abstract OriginDao originDao();
    public abstract RouteDao routeDao();
    public abstract ScheduleDao scheduleDao();
    public abstract SyncMetaDao syncMetaDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    DB_NAME
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
             .build();
        }
        return instance;
    }
}
