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

public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_SEARCH_QUERY = "search_query";

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

        // Back button
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // Hide search‐nav button
        findViewById(R.id.searchActivityButton).setVisibility(View.INVISIBLE);

        // Settings and add buttons
        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, SettingsActivity.class));
        });
        findViewById(R.id.addButton).setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, NewEntryActivity.class));
        });

        // Load key types map
        loadKeyTypeMap();

        // Date toggle
        dateButton.setOnClickListener(v -> toggleDateLayoutVisibility());

        // Date pickers
        startDateButton.setOnClickListener(v -> showDateTimePicker(startDateButton, true));
        endDateButton.setOnClickListener(v -> showDateTimePicker(endDateButton, false));

        // Search actions
        searchButton.setOnClickListener(v -> performSearch());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performSearch();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // If launched from MainActivity snippet click, prefill & search
        String initialQuery = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);
        if (initialQuery != null && !initialQuery.isEmpty()) {
            searchInput.setText(initialQuery);
            performSearch();
        } else {
            // otherwise, load all
            performSearch();
        }
    }

    private void showDateTimePicker(Button button, boolean isStartDate) {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            date.set(year, month, day);
            new TimePickerDialog(this, (view1, hour, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hour);
                date.set(Calendar.MINUTE, minute);
                String formatted = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                        .format(date.getTime());
                button.setText(formatted);
                if (isStartDate) startTimestamp = date.getTimeInMillis();
                else endTimestamp = date.getTimeInMillis();
            }, currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE), false).show();
        }, currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadKeyTypeMap() {
        executorService.execute(() -> {
            keyTypeMap = new EntryDisplayHelper(this, inflater).loadKeyTypeMap(keyDao);
        });
    }

    private void performSearch() {
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);

        final String query = searchInput.getText().toString().trim().toLowerCase();
        executorService.execute(() -> {
            List<Entry> all = entryDao.getAllEntries();
            List<Entry> matched = new ArrayList<>();
            boolean noFilters = query.isEmpty() && startTimestamp == 0 && endTimestamp == Long.MAX_VALUE;

            for (Entry e : all) {
                try {
                    boolean inRange = (e.timestamp >= startTimestamp && e.timestamp <= endTimestamp);
                    if (noFilters || inRange) {
                        boolean match = noFilters;
                        if (!query.isEmpty()) {
                            String tsStr = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy",
                                    Locale.getDefault()).format(new Date(e.timestamp))
                                    .toLowerCase();
                            if (tsStr.contains(query)) match = true;
                            JSONObject obj = new JSONObject(e.payload);
                            Iterator<String> it = obj.keys();
                            while (!match && it.hasNext()) {
                                String k = it.next();
                                String v = obj.getString(k);
                                String type = keyTypeMap.get(k);
                                if (k.toLowerCase().contains(query)) { match = true; break; }
                                if (type != null && !"image".equalsIgnoreCase(type)
                                        && v.toLowerCase().contains(query)) {
                                    match = true; break;
                                }
                            }
                        }
                        if (match) matched.add(e);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            runOnUiThread(() -> displaySearchResults(matched));
        });
    }

    private void displaySearchResults(List<Entry> matchedEntries) {
        searchResultLayout.removeAllViews();
        if (matchedEntries.isEmpty()) {
            Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show();
            return;
        }
        new EntryDisplayHelper(this, inflater)
                .displayEntries(matchedEntries, keyTypeMap, searchResultLayout, true);
    }

    private void toggleDateLayoutVisibility() {
        if (dateLayout.getVisibility() == View.GONE) {
            dateLayout.setVisibility(View.VISIBLE);
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(300);
            dateLayout.startAnimation(fadeIn);
        } else {
            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(300);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    dateLayout.setVisibility(View.GONE);
                }
            });
            dateLayout.startAnimation(fadeOut);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View focus = getCurrentFocus();
            if (focus instanceof EditText) {
                int[] loc = new int[2];
                focus.getLocationOnScreen(loc);
                float x = event.getRawX(), y = event.getRawY();
                if (x < loc[0] || x > loc[0] + focus.getWidth()
                        || y < loc[1] || y > loc[1] + focus.getHeight()) {
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                    focus.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}