package si.uni_lj.fe.lablog;

import android.app.Application;
import android.content.SharedPreferences;

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

        // Initialize MQTT settings with default values if not set
        initializeMqttSettings();
    }

    public static AppDatabase getDatabase() {
        return database;
    }

    private void initializeMqttSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences("mqtt_settings", MODE_PRIVATE);

        // Check if the MQTT settings are already initialized
        if (!sharedPreferences.contains("mqtt_broker")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("mqtt_broker", "tcp://broker.emqx.io:1883");
            editor.putString("mqtt_topic", "Lab/Log/data");
            editor.putBoolean("mqtt_enabled", false);
            editor.apply(); // Save the default values
        }
    }
}