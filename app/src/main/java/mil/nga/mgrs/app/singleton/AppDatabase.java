package mil.nga.mgrs.app.singleton;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import mil.nga.mgrs.app.dao.ObservationDAO;
import mil.nga.mgrs.app.entities.Observation;

@Database(entities = {Observation.class}, version = 1, exportSchema = false)
public  abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    public abstract ObservationDAO observationDAO();

    public static synchronized AppDatabase getInstance(Context context){
        if (instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "LAM_DB")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
