package si.uni_lj.fe.lablog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimestampFormatSettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "lablog_prefs";
    private static final String KEY_TIMESTAMP_FORMAT = "pref_timestamp_format";

    private TextView timestampTextView;
    private TextInputEditText formatInput;
    private Button saveButton;
    private Spinner spinner;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_timestamp_format_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // prefs
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // views
        timestampTextView = findViewById(R.id.timestampTextView);
        formatInput       = findViewById(R.id.brokerInput);
        saveButton        = findViewById(R.id.SaveSettingsButton);
        spinner           = findViewById(R.id.spinner);

        // back button
        View back = findViewById(R.id.backButton);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(v -> finish());

        // load saved or default pattern
        String currentPattern = prefs.getString(KEY_TIMESTAMP_FORMAT, "HH:mm:ss dd-MM-yyyy");
        formatInput.setText(currentPattern);

        // spinner: when user selects, overwrite input and update preview
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selected = parent.getItemAtPosition(pos).toString();
                // strip off the " (Description)" suffix if present
                int idx = selected.indexOf(" (");
                String pattern = idx > 0 ? selected.substring(0, idx) : selected;
                formatInput.setText(pattern);
                updatePreview(pattern);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // set spinner initial position to match the saved pattern
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            String item = adapter.getItem(i).toString();
            int idx = item.indexOf(" (");
            String pattern = idx > 0 ? item.substring(0, idx) : item;
            if (pattern.equals(currentPattern)) {
                spinner.setSelection(i);
                break;
            }
        }

        // initial preview
        updatePreview(currentPattern);

        // save button: validate, persist, and refresh preview
        saveButton.setOnClickListener(v -> {
            String newPattern = formatInput.getText().toString().trim();
            if (newPattern.isEmpty()) {
                formatInput.setError("Format cannot be empty");
                return;
            }
            // validate pattern
            try {
                new SimpleDateFormat(newPattern, Locale.getDefault()).format(new Date());
            } catch (IllegalArgumentException ex) {
                formatInput.setError("Invalid pattern");
                return;
            }
            // save
            prefs.edit()
                    .putString(KEY_TIMESTAMP_FORMAT, newPattern)
                    .apply();
            updatePreview(newPattern);
            saveButton.setText("Saved");
            saveButton.postDelayed(() -> saveButton.setText("Save"), 1000);
        });
    }

    /** Formats the current time using `pattern` and displays it. */
    private void updatePreview(String pattern) {
        String formatted = new SimpleDateFormat(pattern, Locale.getDefault())
                .format(new Date());
        timestampTextView.setText(formatted);
    }
}
