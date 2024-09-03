package si.uni_lj.fe.lablog;

import android.content.Intent;
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

import org.json.JSONObject;

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
        View searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
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
                Log.d("MainActivity", "No entries found in the database.");
                return;
            }

            // Load key types using the helper
            EntryDisplayHelper helper = new EntryDisplayHelper(this, inflater);
            Map<String, String> keyTypeMap = helper.loadKeyTypeMap(keyDao);

            // Post to the main thread to update UI
            runOnUiThread(() -> {
                // Add "Last entry" text before the last entry card
                TextView lastEntryTextView = new TextView(MainActivity.this);
                lastEntryTextView.setText(R.string.last_entry);
                lastEntryTextView.setTextSize(18);
                lastEntryTextView.setTextColor(ContextCompat.getColor(this, R.color.light_gray));
                lastEntryTextView.setPadding(24, 16, 16, 16);
                linearLayout.addView(lastEntryTextView);

                // Display the latest entry
                Log.d("MainActivity", "Displaying the latest entry.");
                helper.displayEntries(entries, keyTypeMap, linearLayout, false);
            });
        });
    }
    private void loadAndDisplayTimestamps() {
        executorService.execute(() -> {
            // Fetch all entries from the database
            List<Entry> entries = entryDao.getAllEntries();
            Map<String, String> keyTypeMap = new HashMap<>();

            // Load key types using the helper
            EntryDisplayHelper helper = new EntryDisplayHelper(this, inflater);
            keyTypeMap = helper.loadKeyTypeMap(keyDao);

            // Post to the main thread to update UI
            Map<String, String> finalKeyTypeMap = keyTypeMap;
            runOnUiThread(() -> {
                // Add "Recent entries" text outside the card
                TextView recentEntriesTextView = new TextView(MainActivity.this);
                recentEntriesTextView.setText(R.string.recent_entries);
                recentEntriesTextView.setTextSize(18);
                recentEntriesTextView.setTextColor(ContextCompat.getColor(this, R.color.light_gray));
                recentEntriesTextView.setPadding(16, 16, 16, 16);
                linearLayout.addView(recentEntriesTextView);

                // Inflate the entry card layout
                View cardView = inflater.inflate(R.layout.entry_card, linearLayout, false);

                // Set an OnClickListener to the card to open LatestEntryActivity
                cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, LatestEntryActivity.class);
                    startActivity(intent);
                });

                // Get the FlexboxLayout where payloads will be added
                FlexboxLayout flexboxContainer = cardView.findViewById(R.id.flexboxContainer);

                // Clear any existing views in the container
                flexboxContainer.removeAllViews();

                // Hide the timestampTextView specifically for this card
                TextView timestampTextView = cardView.findViewById(R.id.timestampTextView);
                if (timestampTextView != null) {
                    timestampTextView.setVisibility(View.GONE);
                }

                // Determine how many entries to show (up to 15)
                int entriesToShow = Math.min(entries.size(), 15);

                // Iterate through the latest 5 entries and create a TextView for each payload snippet
                for (int i = 0; i < entriesToShow; i++) {
                    Entry entry = entries.get(i);

                    // Inflate the TextView from the entry_card layout
                    TextView payloadTextView = (TextView) inflater.inflate(R.layout.quick_value_view, flexboxContainer, false);

                    // Create a snippet of the payload with a maximum of 40 characters
                    StringBuilder snippetBuilder = new StringBuilder();
                    int remainingChars = 40;

                    try {
                        JSONObject jsonObject = new JSONObject(entry.payload);
                        Iterator<String> keys = jsonObject.keys();

                        while (keys.hasNext() && remainingChars > 0) {
                            String key = keys.next();
                            String value = jsonObject.getString(key);

                            // Check if the key is of type "image"
                            String type = finalKeyTypeMap.get(key);
                            if ("image".equalsIgnoreCase(type)) {
                                value = "(img)";
                            } else {
                                // Truncate the value to 10 characters max
                                if (value.length() > 14) {
                                    value = value.substring(0, 14) + "...";
                                }
                            }

                            // Ensure the total length does not exceed 40 characters
                            if (snippetBuilder.length() + value.length() + 2 > 40) { // +2 for ", " after each value
                                snippetBuilder.append("...");
                                break;
                            }

                            // Append the truncated value or "(img)" to the snippet
                            snippetBuilder.append(value);

                            remainingChars = 40 - snippetBuilder.length();

                            if (keys.hasNext() && remainingChars > 2) {
                                snippetBuilder.append(", ");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error parsing JSON for entry " + entry.id, e);
                    }

                    // Set the text to the TextView
                    payloadTextView.setText(snippetBuilder.toString());

                    // Add the TextView to the FlexboxLayout
                    flexboxContainer.addView(payloadTextView);
                }

                if (entries.size() > 15) {
                    // Create the "See all..." TextView programmatically
                    TextView seeAllTextView = new TextView(MainActivity.this);

                    // Set the text to "See all..."
                    seeAllTextView.setText(R.string.load_all);

                    // Set the text color to white
                    seeAllTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                    // Set the text size (optional)
                    seeAllTextView.setTextSize(16);

                    // Set padding (optional)
                    seeAllTextView.setPadding(16, 8, 16, 8);

                    // Set an OnClickListener to navigate to LatestEntryActivity when clicked
                    seeAllTextView.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, LatestEntryActivity.class);
                        startActivity(intent);
                    });

                    // Add the "See all..." TextView to the FlexboxLayout
                    flexboxContainer.addView(seeAllTextView);
                }

                // Add the populated card to the LinearLayout
                linearLayout.addView(cardView);
            });
        });
    }

}
