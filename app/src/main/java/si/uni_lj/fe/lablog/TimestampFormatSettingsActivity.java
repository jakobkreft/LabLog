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

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        timestampTextView = findViewById(R.id.timestampTextView);
        formatInput       = findViewById(R.id.brokerInput);
        saveButton        = findViewById(R.id.SaveSettingsButton);
        spinner           = findViewById(R.id.spinner);

        View back = findViewById(R.id.backButton);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(v -> finish());

        // load saved or default
        String currentPattern = prefs.getString(KEY_TIMESTAMP_FORMAT, "HH:mm:ss dd-MM-yyyy");
        formatInput.setText(currentPattern);

        // spinner selection → overwrite input & preview
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selected = parent.getItemAtPosition(pos).toString();
                int idx = selected.indexOf(" (");
                String pattern = idx>0 ? selected.substring(0,idx) : selected;

                // overwrite input
                formatInput.setText(pattern);
                // preview
                updatePreview(pattern);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // set spinner to currentPattern
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i=0; i<adapter.getCount(); i++){
            String item = adapter.getItem(i).toString();
            int idx = item.indexOf(" (");
            String pattern = idx>0? item.substring(0,idx): item;
            if (pattern.equals(currentPattern)){
                spinner.setSelection(i);
                break;
            }
        }

        updatePreview(currentPattern);

        saveButton.setOnClickListener(v -> {
            String newPattern = formatInput.getText().toString().trim();
            if (newPattern.isEmpty()) {
                formatInput.setError("Format cannot be empty");
                return;
            }
            // if Raw, no format-validate, otherwise test SimpleDateFormat
            if (!"RAW".equals(newPattern)) {
                try {
                    new SimpleDateFormat(newPattern, Locale.getDefault())
                            .format(new Date());
                } catch (IllegalArgumentException ex) {
                    formatInput.setError("Invalid pattern");
                    return;
                }
            }
            prefs.edit()
                    .putString(KEY_TIMESTAMP_FORMAT, newPattern)
                    .apply();
            updatePreview(newPattern);
            saveButton.setText("Saved");
            saveButton.postDelayed(() -> saveButton.setText("Save"), 1000);
        });
    }

    private void updatePreview(String pattern) {
        String out;
        if ("RAW".equals(pattern)) {
            out = String.valueOf(new Date().getTime());
        } else {
            out = new SimpleDateFormat(pattern, Locale.getDefault())
                    .format(new Date());
        }
        timestampTextView.setText(out);
    }
}
