package si.uni_lj.fe.lablog;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.KeyDao;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private ImageButton searchButton;
    private LinearLayout searchResultLayout;
    private LayoutInflater inflater;
    private EntryDao entryDao;
    private KeyDao keyDao;
    private ExecutorService executorService;
    private Map<String, String> keyTypeMap;
    private ImageButton dateButton;
    private ConstraintLayout dateLayout;
    private Button startDateButton;
    private Button endDateButton;

    // Variables to store the selected timestamps
    private long startTimestamp = 0;
    private long endTimestamp = Long.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        dateButton = findViewById(R.id.dateButton);
        dateLayout = findViewById(R.id.dateLayout);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);
        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        searchResultLayout = findViewById(R.id.searchResultLayout);
        inflater = LayoutInflater.from(this);

        // Initialize the DAOs and ExecutorService
        entryDao = MyApp.getDatabase().entryDao();
        keyDao = MyApp.getDatabase().keyDao();
        executorService = Executors.newSingleThreadExecutor();


        // Find the back button by its ID
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());


        // Load key types map
        loadKeyTypeMap();

        // Set search button click listener
        searchButton.setOnClickListener(v -> performSearch());

        // Set dateButton click listener to toggle dataLayout visibility
        dateButton.setOnClickListener(v -> toggleDateLayoutVisibility());


        // Set date pickers
        startDateButton.setOnClickListener(v -> showDateTimePicker(startDateButton, true));
        endDateButton.setOnClickListener(v -> showDateTimePicker(endDateButton, false));
    }

    private void showDateTimePicker(Button button, boolean isStartDate) {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);
            new TimePickerDialog(SearchActivity.this, (view1, hourOfDay, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);
                // Set the selected date and time on the button
                String formattedDate = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(date.getTime());
                button.setText(formattedDate);

                // Save the timestamp based on the button clicked
                if (isStartDate) {
                    startTimestamp = date.getTimeInMillis();
                } else {
                    endTimestamp = date.getTimeInMillis();
                }
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadKeyTypeMap() {
        executorService.execute(() -> {
            EntryDisplayHelper helper = new EntryDisplayHelper(this, inflater);
            keyTypeMap = helper.loadKeyTypeMap(keyDao);
        });
    }

    private void performSearch() {
        String searchQuery = searchInput.getText().toString().trim();

        if (searchQuery.isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        executorService.execute(() -> {
            // Fetch all entries from the database
            List<Entry> allEntries = entryDao.getAllEntries();
            List<Entry> matchedEntries = new ArrayList<>();

            // Iterate through all entries and check for matches
            for (Entry entry : allEntries) {
                try {
                    // Check if the entry's timestamp is within the selected range
                    if (entry.timestamp >= startTimestamp && entry.timestamp <= endTimestamp) {
                        JSONObject jsonObject = new JSONObject(entry.payload);
                        boolean matchFound = false;

                        // Iterate through each key-value pair in the JSON object
                        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                            String key = it.next();
                            String valueStr = jsonObject.getString(key);

                            // Get the type of the key from the map
                            String type = keyTypeMap.get(key);

                            // Skip image types for searching
                            if (type != null && !"Image".equalsIgnoreCase(type)) {
                                if (valueStr.contains(searchQuery)) {
                                    matchFound = true;
                                    break;
                                }
                            }
                        }

                        // If a match is found, add the entry to the matchedEntries list
                        if (matchFound) {
                            matchedEntries.add(entry);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Post the results to the main thread for UI update
            runOnUiThread(() -> displaySearchResults(matchedEntries));
        });
    }


    private void displaySearchResults(List<Entry> matchedEntries) {
        // Clear previous search results
        searchResultLayout.removeAllViews();

        if (matchedEntries.isEmpty()) {
            Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show();
            return;
        }

        EntryDisplayHelper helper = new EntryDisplayHelper(this, inflater);
        helper.displayEntries(matchedEntries, keyTypeMap, searchResultLayout, true);
    }


    private void toggleDateLayoutVisibility() {
        if (dateLayout.getVisibility() == View.GONE) {
            // Fade in
            dateLayout.setVisibility(View.VISIBLE);
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(300); // Duration in milliseconds
            dateLayout.startAnimation(fadeIn);
        } else {
            // Fade out
            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(300); // Duration in milliseconds
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // No need to do anything here
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    dateLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // No need to do anything here
                }
            });
            dateLayout.startAnimation(fadeOut);
        }
    }
}
