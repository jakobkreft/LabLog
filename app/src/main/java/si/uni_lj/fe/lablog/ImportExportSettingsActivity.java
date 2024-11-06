package si.uni_lj.fe.lablog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class ImportExportSettingsActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 1;
    private static final int REQUEST_IMPORT_FILE = 2;
    private File exportedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_import_export_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.Export).setOnClickListener(v -> checkStoragePermissionAndExport());
        findViewById(R.id.Import).setOnClickListener(v -> startFilePickerForImport());
    }

    private void checkStoragePermissionAndExport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            } else {
                exportDatabaseToJSON();
            }
        } else {
            exportDatabaseToJSON();
        }
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
        File exportDir = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? new File(getExternalFilesDir(null), "LabLogExports")
                : new File(Environment.getExternalStorageDirectory(), "LabLogExports");

        if (!exportDir.exists() && !exportDir.mkdirs()) {
            runOnUiThread(() -> Toast.makeText(this, "Failed to create export directory.", Toast.LENGTH_SHORT).show());
            return;
        }

        exportedFile = new File(exportDir, "LabLogExport.json");
        try (FileWriter writer = new FileWriter(exportedFile)) {
            writer.write(jsonString);
            runOnUiThread(() -> {
                Toast.makeText(this, "Data exported successfully", Toast.LENGTH_SHORT).show();
                shareExportedFile();
            });
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Failed to write export file.", Toast.LENGTH_SHORT).show());
        }
    }

    private void shareExportedFile() {
        if (exportedFile != null && exportedFile.exists()) {
            Uri fileUri = FileProvider.getUriForFile(this, "si.uni_lj.fe.lablog.fileprovider", exportedFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share exported file"));
        } else {
            Toast.makeText(this, "No file available to share. Please export first.", Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> Toast.makeText(this, "Data imported successfully.", Toast.LENGTH_SHORT).show());

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDatabaseToJSON();
            } else {
                Toast.makeText(this, "Permission required to export data.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
