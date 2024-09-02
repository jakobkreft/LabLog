package si.uni_lj.fe.lablog;

import android.os.Bundle;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_key);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Spinner spinner = findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and custom spinner layouts
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.key_type_options, R.layout.spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // Find the back button by its ID
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // Find the SaveKeyButton TextView by its ID
        TextView saveKeyButton = findViewById(R.id.SaveEntryButton);

        // Set an OnClickListener on the SaveKeyButton
        saveKeyButton.setOnClickListener(v -> {
            // Retrieve the key name from the EditText
            EditText nameInput = findViewById(R.id.nameInput);
            String keyName = nameInput.getText().toString().trim();

            // Retrieve the selected key type from the Spinner
            String keyType = spinner.getSelectedItem().toString();

            // Check if the name is empty
            if (keyName.isEmpty()) {
                Toast.makeText(NewKeyActivity.this,
                        "Key name cannot be empty!",
                        Toast.LENGTH_SHORT).show();
                return; // Do not proceed with saving
            }

            // Check if a type is selected (assuming the first item is a prompt like "Select Type")
            if (spinner.getSelectedItemPosition() == 0) {
                Toast.makeText(NewKeyActivity.this,
                        "Please select a key type!",
                        Toast.LENGTH_SHORT).show();
                return; // Do not proceed with saving
            }

            // Save the key to the database in a background thread
            new Thread(() -> {
                // Get the database and DAO
                AppDatabase db = MyApp.getDatabase();
                KeyDao keyDao = db.keyDao();

                // Create a new Key object
                Key newKey = new Key();
                newKey.name = keyName;
                newKey.type = keyType;

                // Insert the new key into the database
                keyDao.insertKey(newKey);

                // Show success toast on the UI thread
                runOnUiThread(() -> Toast.makeText(NewKeyActivity.this,
                        keyName + " key of type " + keyType + " saved.",
                        Toast.LENGTH_SHORT).show());
                // Go back to the previous activity
                finish();
            }).start();
        });
    }
}
