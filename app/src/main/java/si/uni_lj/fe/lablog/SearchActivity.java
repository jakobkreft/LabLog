package si.uni_lj.fe.lablog;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

import com.google.android.material.textfield.TextInputEditText;

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


        // Set an OnClickListener to the searchButton
        View searchActivityButton = findViewById(R.id.searchActivityButton);
        searchActivityButton.setVisibility(View.INVISIBLE);

        // Set an OnClickListener to the searchButton
        View settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Set an OnClickListener to the addButton
        View addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, NewEntryActivity.class);
            startActivity(intent);
        });


        // Load key types map
        loadKeyTypeMap();

        // Set search button click listener
        searchButton.setOnClickListener(v -> performSearch());

        // Set dateButton click listener to toggle dataLayout visibility
        dateButton.setOnClickListener(v -> toggleDateLayoutVisibility());


        // Set date pickers
        startDateButton.setOnClickListener(v -> showDateTimePicker(startDateButton, true));
        endDateButton.setOnClickListener(v -> showDateTimePicker(endDateButton, false));

        TextInputEditText searchInput = findViewById(R.id.searchInput);

        // Set up the listener for the Done action on the keyboard
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    performSearch();  // Call your search function here

                    // Hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    return true;  // Consume the event
                }
                return false;  // Don't consume the event if it's not the Done action
            }
        });

        performSearch();
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
        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);

        String searchQuery = searchInput.getText().toString().trim().toLowerCase();

        executorService.execute(() -> {
            // Fetch all entries from the database
            List<Entry> allEntries = entryDao.getAllEntries();
            List<Entry> matchedEntries = new ArrayList<>();

            // If no search query and no date range is specified, show all entries
            boolean noFiltersApplied = searchQuery.isEmpty() && startTimestamp == 0 && endTimestamp == Long.MAX_VALUE;

            // Iterate through all entries and check for matches
            for (Entry entry : allEntries) {
                try {
                    // Check if the entry's timestamp is within the selected range
                    if (noFiltersApplied || (entry.timestamp >= startTimestamp && entry.timestamp <= endTimestamp)) {
                        boolean matchFound = false;

                        // If searchQuery is not empty, perform search within the entry's data
                        if (!searchQuery.isEmpty()) {
                            JSONObject jsonObject = new JSONObject(entry.payload);

                            // Check if the timestamp matches the search query
                            String formattedTimestamp = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault()).format(new Date(entry.timestamp));
                            if (formattedTimestamp.toLowerCase().contains(searchQuery)) {
                                matchFound = true;
                            }

                            // Iterate through each key-value pair in the JSON object
                            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                                String key = it.next();
                                String valueStr = jsonObject.getString(key);

                                // Get the type of the key from the map
                                String type = keyTypeMap.get(key);

                                // Key names should always be searchable, regardless of the type
                                if (key.toLowerCase().contains(searchQuery)) {
                                    matchFound = true;
                                    break;
                                }

                                // Skip image values for searching but include key names
                                if (type != null && !"Image".equalsIgnoreCase(type)) {
                                    // Check if the value matches the search query
                                    if (valueStr.toLowerCase().contains(searchQuery)) {
                                        matchFound = true;
                                        break;
                                    }
                                }
                            }
                        } else {
                            // If no search query, match by time range or no filters applied
                            matchFound = true;
                        }

                        // If a match is found (based on time or search query), add the entry to the matchedEntries list
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


    // Helper method to hide the keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    // Override the dispatchTouchEvent to detect taps outside the input fields
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View currentFocus = getCurrentFocus();
            if (currentFocus != null && (currentFocus instanceof EditText)) {
                int[] location = new int[2];
                currentFocus.getLocationOnScreen(location);
                float x = event.getRawX() + currentFocus.getLeft() - location[0];
                float y = event.getRawY() + currentFocus.getTop() - location[1];

                if (x < currentFocus.getLeft() || x > currentFocus.getRight() ||
                        y < currentFocus.getTop() || y > currentFocus.getBottom()) {
                    hideKeyboard();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
