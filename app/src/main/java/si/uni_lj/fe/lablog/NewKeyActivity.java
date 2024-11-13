package si.uni_lj.fe.lablog;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class NewKeyActivity extends AppCompatActivity {

    private EditText nameInput;
    private Spinner spinner;
    private TextView saveKeyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_key);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        nameInput = findViewById(R.id.nameInput);
        spinner = findViewById(R.id.spinner);
        saveKeyButton = findViewById(R.id.SaveEntryButton);

        // Initialize the SaveKeyButton as invisible
        saveKeyButton.setVisibility(View.INVISIBLE);

        // Create an ArrayAdapter using the string array and custom spinner layouts
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.key_type_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set up listeners for EditText and Spinner
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkFieldsForEmptyValues();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                checkFieldsForEmptyValues();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Set an OnClickListener on the SaveKeyButton
        saveKeyButton.setOnClickListener(v -> {
            String keyName = nameInput.getText().toString().trim();
            String keyType = spinner.getSelectedItem().toString();

            if (keyName.isEmpty()) {
                Toast.makeText(NewKeyActivity.this, "Key name cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (spinner.getSelectedItemPosition() == 0) {
                Toast.makeText(NewKeyActivity.this, "Please select a key type!", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                AppDatabase db = MyApp.getDatabase();
                KeyDao keyDao = db.keyDao();

                Key existingKey = keyDao.getKeyByName(keyName);
                if (existingKey != null) {
                    runOnUiThread(() -> Toast.makeText(NewKeyActivity.this,
                            "Key with the same name already exists!", Toast.LENGTH_SHORT).show());
                } else {
                    Key newKey = new Key();
                    newKey.name = keyName;
                    newKey.type = keyType;

                    keyDao.insertKey(newKey);

                    runOnUiThread(() -> Toast.makeText(NewKeyActivity.this,
                            keyName + " key of type " + keyType + " saved.", Toast.LENGTH_SHORT).show());
                    finish();
                }
            }).start();
        });

        // Back button functionality
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());
    }

    // Helper method to check if both fields are filled
    private void checkFieldsForEmptyValues() {
        String keyName = nameInput.getText().toString().trim();
        boolean isSpinnerValid = spinner.getSelectedItemPosition() != 0;

        if (!keyName.isEmpty() && isSpinnerValid) {
            saveKeyButton.setVisibility(View.VISIBLE);
        } else {
            saveKeyButton.setVisibility(View.INVISIBLE);
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
