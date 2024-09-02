package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.KeyDao;

public class LatestEntryActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private LayoutInflater inflater;
    private EntryDao entryDao;
    private KeyDao keyDao;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Map<String, String> keyTypeMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_latest_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the LinearLayout and LayoutInflater
        linearLayout = findViewById(R.id.LinearLayout);
        inflater = LayoutInflater.from(this);

        // Get the EntryDao and KeyDao
        entryDao = MyApp.getDatabase().entryDao();
        keyDao = MyApp.getDatabase().keyDao();

        // Initialize ExecutorService and Handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize the map to store key types
        keyTypeMap = new HashMap<>();

        // Load the entries and key types from the database and display them
        loadEntriesAndKeyTypes();

        // Back button functionality
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // Add button functionality
        View addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(LatestEntryActivity.this, NewEntryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Clear the current entries in the LinearLayout
        linearLayout.removeAllViews();

        // Reload the entries and key types from the database
        loadEntriesAndKeyTypes();
    }

    private void loadEntriesAndKeyTypes() {
        executorService.execute(() -> {
            // Fetch all entries and key types in the background
            List<Entry> entries = entryDao.getAllEntries();

            // Load key types using the helper
            EntryDisplayHelper helper = new EntryDisplayHelper(this, inflater);
            keyTypeMap = helper.loadKeyTypeMap(keyDao);

            // Post the results to the main thread for UI update
            mainHandler.post(() -> {
                linearLayout.removeAllViews(); // Ensure the LinearLayout is cleared before adding new views
                helper.displayEntries(entries, keyTypeMap, linearLayout, true);
            });
        });
    }
}
