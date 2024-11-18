package si.uni_lj.fe.lablog;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class ImportExportSettingsActivity extends AppCompatActivity {

    private static final int REQUEST_IMPORT_FILE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_import_export_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        findViewById(R.id.Export).setOnClickListener(v -> exportDatabaseToJSON());
        findViewById(R.id.Import).setOnClickListener(v -> startFilePickerForImport());
        findViewById(R.id.DeleteEntries).setOnClickListener(v -> showDeleteEntriesConfirmationDialog());
    }

    private void showDeleteEntriesConfirmationDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete All Entries")
                .setMessage("Are you sure you want to delete all entries? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAllEntries())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteAllEntries() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = MyApp.getDatabase();
                EntryDao entryDao = db.entryDao();

                entryDao.deleteAllEntries();

                runOnUiThread(() -> {
                    Toast.makeText(this, "All entries deleted successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to delete entries.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void exportDatabaseToJSON() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = MyApp.getDatabase();
                EntryDao entryDao = db.entryDao();
                KeyDao keyDao = db.keyDao();

                List<Entry> entries = entryDao.getAllEntries();
                List<Key> keys = keyDao.getAllKeys();

                JSONObject databaseJson = new JSONObject();
                databaseJson.put("entries", convertEntriesToJson(entries));
                databaseJson.put("keys", convertKeysToJson(keys));

                saveJsonToFile(databaseJson.toString());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to export data.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private JSONArray convertEntriesToJson(List<Entry> entries) throws JSONException {
        JSONArray entriesArray = new JSONArray();
        for (Entry entry : entries) {
            JSONObject entryJson = new JSONObject();
            entryJson.put("payload", new JSONObject(entry.payload));
            entryJson.put("timestamp", entry.timestamp);
            entriesArray.put(entryJson);
        }
        return entriesArray;
    }

    private JSONArray convertKeysToJson(List<Key> keys) throws JSONException {
        JSONArray keysArray = new JSONArray();
        for (Key key : keys) {
            JSONObject keyJson = new JSONObject();
            keyJson.put("name", key.name);
            keyJson.put("type", key.type);
            keysArray.put(keyJson);
        }
        return keysArray;
    }

    private void saveJsonToFile(String jsonString) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, "LabLogExport.json");
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/json");

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

            if (uri == null) {
                throw new IOException("Failed to create new MediaStore record.");
            }

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Failed to open output stream.");
                }
                outputStream.write(jsonString.getBytes());
                runOnUiThread(() -> Toast.makeText(this, "Data exported successfully to Downloads.", Toast.LENGTH_LONG).show());
            }

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Failed to save file to Downloads.", Toast.LENGTH_SHORT).show());
        }
    }

    private void startFilePickerForImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/json");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_IMPORT_FILE);
    }

    private void importDatabaseFromJSON(Uri fileUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                JSONObject databaseJson = new JSONObject(stringBuilder.toString());
                AppDatabase db = MyApp.getDatabase();
                KeyDao keyDao = db.keyDao();
                EntryDao entryDao = db.entryDao();

                if (!importKeys(databaseJson.getJSONArray("keys"), keyDao)) {
                    return;
                }

                importEntries(databaseJson.getJSONArray("entries"), entryDao, keyDao);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Data imported successfully.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to import data.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private boolean importKeys(JSONArray keysJsonArray, KeyDao keyDao) throws JSONException {
        List<Key> existingKeys = keyDao.getAllKeys();
        for (int i = 0; i < keysJsonArray.length(); i++) {
            JSONObject keyJson = keysJsonArray.getJSONObject(i);
            String keyName = keyJson.getString("name");
            String keyType = keyJson.getString("type");

            Key existingKey = keyDao.getKeyByName(keyName);
            if (existingKey != null && !existingKey.type.equals(keyType)) {
                runOnUiThread(() -> Toast.makeText(this, "Conflicting key type for: " + keyName, Toast.LENGTH_SHORT).show());
                return false;
            }
            if (existingKey == null) {
                Key newKey = new Key();
                newKey.name = keyName;
                newKey.type = keyType;
                keyDao.insertKey(newKey);
            }
        }
        return true;
    }

    private void importEntries(JSONArray entriesJsonArray, EntryDao entryDao, KeyDao keyDao) throws JSONException {
        List<Entry> existingEntries = entryDao.getAllEntries();
        HashSet<String> existingEntrySignatures = new HashSet<>();
        for (Entry entry : existingEntries) {
            existingEntrySignatures.add(entry.timestamp + entry.payload);
        }

        for (int i = 0; i < entriesJsonArray.length(); i++) {
            JSONObject entryJson = entriesJsonArray.getJSONObject(i);
            String payload = entryJson.getJSONObject("payload").toString();
            long timestamp = entryJson.getLong("timestamp");

            if (existingEntrySignatures.contains(timestamp + payload)) {
                continue;
            }

            Entry newEntry = new Entry();
            newEntry.payload = payload;
            newEntry.timestamp = timestamp;
            entryDao.insertEntry(newEntry);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri fileUri = data.getData();
                if (fileUri != null) {
                    importDatabaseFromJSON(fileUri);
                }
            }
        }
    }
}
