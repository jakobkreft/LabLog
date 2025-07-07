package si.uni_lj.fe.lablog;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.flexbox.FlexboxLayout;

import org.eclipse.paho.android.service.BuildConfig;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.KeyDao;


public class MainActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private LayoutInflater inflater;
    private EntryDao entryDao;
    private KeyDao keyDao;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Initialize the LinearLayout and LayoutInflater
        linearLayout = findViewById(R.id.LinearLayout);
        inflater = LayoutInflater.from(this);

        // Get the EntryDao and KeyDao
        entryDao = MyApp.getDatabase().entryDao();
        keyDao = MyApp.getDatabase().keyDao();

        // Initialize ExecutorService for background tasks
        executorService = Executors.newSingleThreadExecutor();

        // Hide the back button in MainActivity
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.GONE);

        // Set an OnClickListener to the searchButton
        View searchActivityButton = findViewById(R.id.searchActivityButton);
        searchActivityButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        // Set an OnClickListener to the settingsButton
        View settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Set an OnClickListener to the addButton
        View addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NewEntryActivity.class);
            startActivity(intent);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Clear the current views in the LinearLayout to prevent duplicates
        linearLayout.removeAllViews();

        // Reload the timestamps from the database and display them in the card
        loadAndDisplayTimestamps();

        // Reload and display the latest entry
        loadAndDisplayLatestEntry();
    }

    private void loadAndDisplayLatestEntry() {
        executorService.execute(() -> {
            // Fetch the latest entry from the database
            List<Entry> entries = entryDao.getAllEntries();

            if (entries.isEmpty()) {
                runOnUiThread(() -> {
                    TextView welcomeText = findViewById(R.id.WelcomeText);
                    welcomeText.setVisibility(View.VISIBLE); // Show the welcome text
                });
                return;
            }

            // Load key types using the helper in the background
            Map<String, String> keyTypeMap = new HashMap<>();
            keyTypeMap = new EntryDisplayHelper(MainActivity.this, inflater).loadKeyTypeMap(keyDao);

            // Post to the main thread to update UI
            Map<String, String> finalKeyTypeMap = keyTypeMap;
            runOnUiThread(() -> {
                TextView welcomeText = findViewById(R.id.WelcomeText);
                welcomeText.setVisibility(View.GONE); // Hide the welcome text if there are entries

                // Add "Last entry" text before the last entry card
                TextView lastEntryTextView = new TextView(MainActivity.this);
                lastEntryTextView.setText(R.string.last_entry);
                lastEntryTextView.setTextSize(18);
                lastEntryTextView.setTextColor(ContextCompat.getColor(this, R.color.light_gray));
                lastEntryTextView.setPadding(24, 16, 16, 16);
                linearLayout.addView(lastEntryTextView);

                // Display the latest entry using the helper
                new EntryDisplayHelper(MainActivity.this, inflater).displayEntries(entries, finalKeyTypeMap, linearLayout, false);
            });
        });
    }

// MainActivity.java

    private void loadAndDisplayTimestamps() {
        executorService.execute(() -> {
            List<Entry> entries = entryDao.getAllEntries();
            Map<String, String> keyTypeMap = new EntryDisplayHelper(this, inflater)
                    .loadKeyTypeMap(keyDao);

            runOnUiThread(() -> {
                // Header
                TextView recentEntriesTextView = new TextView(MainActivity.this);
                recentEntriesTextView.setText(R.string.recent_entries);
                recentEntriesTextView.setTextSize(18);
                recentEntriesTextView.setTextColor(ContextCompat.getColor(this, R.color.light_gray));
                recentEntriesTextView.setPadding(16, 16, 16, 16);
                linearLayout.addView(recentEntriesTextView);

                // Card container
                View cardView = inflater.inflate(R.layout.entry_card, linearLayout, false);
                FlexboxLayout flexboxContainer = cardView.findViewById(R.id.flexboxContainer);
                cardView.findViewById(R.id.timestampTextView)
                        .setVisibility(View.GONE);
                flexboxContainer.removeAllViews();

                int entriesToShow = Math.min(entries.size(), 15);
                for (int i = 0; i < entriesToShow; i++) {
                    Entry entry = entries.get(i);

                    // build snippet ≤40 chars
                    StringBuilder sb = new StringBuilder();
                    int rem = 40;
                    try {
                        JSONObject obj = new JSONObject(entry.payload);
                        Iterator<String> keys = obj.keys();
                        while (keys.hasNext() && rem > 0) {
                            String key = keys.next();
                            String v = obj.getString(key);
                            String type = keyTypeMap.get(key);
                            if ("image".equalsIgnoreCase(type)) {
                                v = "(img)";
                            } else if (v.length() > 14) {
                                v = v.substring(0, 14) + "...";
                            }
                            if (sb.length() + v.length() + 2 > 40) {
                                sb.append("...");
                                break;
                            }
                            sb.append(v);
                            rem = 40 - sb.length();
                            if (keys.hasNext() && rem > 2) sb.append(", ");
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "JSON error", e);
                    }

                    // inflate & set up click listener
                    TextView tv = (TextView) inflater.inflate(
                            R.layout.quick_value_view, flexboxContainer, false);
                    final String filterTs = String.valueOf(entry.timestamp);
                    tv.setText(sb.toString());
                    String formattedTs = new SimpleDateFormat(
                            "HH:mm:ss dd-MM-yyyy",
                            Locale.getDefault()
                    ).format(new Date(entry.timestamp));
                    tv.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                        intent.putExtra(SearchActivity.EXTRA_SEARCH_QUERY, formattedTs);
                        startActivity(intent);
                    });
                    flexboxContainer.addView(tv);
                }

                // "See all…"
                if (entries.size() > 15) {
                    TextView seeAll = new TextView(MainActivity.this);
                    seeAll.setText(R.string.load_all);
                    seeAll.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                    seeAll.setTextSize(16);
                    seeAll.setPadding(16, 8, 16, 8);
                    flexboxContainer.addView(seeAll);
                }

                linearLayout.addView(cardView);
            });
        });
    }
}