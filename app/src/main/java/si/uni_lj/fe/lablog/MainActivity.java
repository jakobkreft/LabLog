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
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

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

        // Initialize ExecutorService for background tasks
        executorService = Executors.newSingleThreadExecutor();

        // Load the timestamps from the database and display them in the card
        loadAndDisplayTimestamps();

        // Load and display the latest entry
        loadAndDisplayLatestEntry();

        // Hide the back button in MainActivity
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.GONE);

        // Find the addButton by its ID
        View addButton = findViewById(R.id.addButton);

        // Set an OnClickListener to the addButton
        addButton.setOnClickListener(v -> {
            // Create an Intent to navigate to NewEntryActivity
            Intent intent = new Intent(MainActivity.this, NewEntryActivity.class);
            // Start the activity
            startActivity(intent);
        });
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

                // Get the container where payloads will be added
                LinearLayout payloadContainer = cardView.findViewById(R.id.payloadContainer);

                // Clear any existing views in the container
                payloadContainer.removeAllViews();

                // Determine how many entries to show (up to 5)
                int entriesToShow = Math.min(entries.size(), 5);

                // Iterate through the latest 5 entries and create a TextView for each payload snippet
                for (int i = 0; i < entriesToShow; i++) {
                    Entry entry = entries.get(i);

                    // Inflate the TextView from the entry_card layout
                    TextView payloadTextView = (TextView) inflater.inflate(R.layout.quick_value_view, payloadContainer, false);

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
                                if (value.length() > 40) {
                                    value = value.substring(0, 30);
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

                    // Add the TextView to the container
                    payloadContainer.addView(payloadTextView);
                }

                if (entries.size() > 5) {
                    // Create the "See all..." TextView programmatically
                    TextView seeAllTextView = new TextView(MainActivity.this);

                    // Set the text to "See all..."
                    seeAllTextView.setText(R.string.load_all);

                    // Set the text color to white
                    seeAllTextView.setTextColor(ContextCompat.getColor(this, android.R.color.white));

                    // Set the text size (optional)
                    seeAllTextView.setTextSize(16); // You can adjust the size as needed

                    // Set padding (optional)
                    seeAllTextView.setPadding(16, 8, 16, 8);

                    // Set an OnClickListener to navigate to LatestEntryActivity when clicked
                    seeAllTextView.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, LatestEntryActivity.class);
                        startActivity(intent);
                    });

                    // Add the "See all..." TextView to the container
                    payloadContainer.addView(seeAllTextView);
                }


                // Add the populated card to the LinearLayout
                linearLayout.addView(cardView);
            });
        });
    }


}
