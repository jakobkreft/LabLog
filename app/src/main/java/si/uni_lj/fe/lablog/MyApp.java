package si.uni_lj.fe.lablog;

import android.app.Application;

import androidx.room.Room;
import si.uni_lj.fe.lablog.data.AppDatabase;

public class MyApp extends Application {
    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the Room database
        database = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "lablog-database").build();
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}
