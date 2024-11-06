package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class RecentKeysActivity extends AppCompatActivity {

    private FlexboxLayout flexboxLayout;
    private TextView newKeyTextView;
    private LayoutInflater inflater;
    private ArrayList<String> selectedKeysList; // List to hold selected keys
    private final int widthStroke = 8;
    private KeyDao keyDao;
    private EntryDao entryDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recent_keys);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the list of selected keys from the intent
        selectedKeysList = getIntent().getStringArrayListExtra("selectedKeys");

        // Find the FlexboxLayout by its ID
        flexboxLayout = findViewById(R.id.flexboxLayout);

        // Inflate the newKey TextView from the layout file
        inflater = LayoutInflater.from(this);
        newKeyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, flexboxLayout, false);
        newKeyTextView.setText(R.string.new_key); // Set the text for newKey button

        // Set an OnClickListener to the newKey TextView
        newKeyTextView.setOnClickListener(v -> {
            // Create an Intent to navigate to NewKeyActivity
            Intent intent = new Intent(RecentKeysActivity.this, NewKeyActivity.class);
            // Start the activity
            startActivity(intent);
        });

        // Initialize database access objects
        AppDatabase db = MyApp.getDatabase();
        keyDao = db.keyDao();
        entryDao = db.entryDao();

        // Find the back button by its ID
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Fetch the keys from the database and update the UI
        new Thread(() -> {
            // Retrieve all keys from the database
            List<Key> keyList = keyDao.getAllKeys();

            // Update the UI on the main thread
            runOnUiThread(() -> {
                // Clear any existing views in the FlexboxLayout
                flexboxLayout.removeAllViews();

                // Dynamically create and add TextViews for each key
                for (Key key : keyList) {
                    // Skip already selected keys
                    if (selectedKeysList.contains(key.name)) {
                        continue;
                    }

                    TextView keyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, flexboxLayout, false);
                    keyTextView.setText(key.name.trim()); // Set the key name

                    // Change the stroke color based on the key type
                    GradientDrawable background = (GradientDrawable) keyTextView.getBackground();
                    switch (key.type.toLowerCase()) {
                        case "integer":
                            background.setStroke(widthStroke, ContextCompat.getColor(this, R.color.colorInteger));
                            break;
                        case "boolean":
                            background.setStroke(widthStroke, ContextCompat.getColor(this, R.color.colorBoolean));
                            break;
                        case "image":
                            background.setStroke(widthStroke, ContextCompat.getColor(this, R.color.colorImage));
                            break;
                        case "float":
                            background.setStroke(widthStroke, ContextCompat.getColor(this, R.color.colorFloat));
                            break;
                        case "string":
                            background.setStroke(widthStroke, ContextCompat.getColor(this, R.color.colorString));
                            break;
                        default:
                            background.setStroke(widthStroke, ContextCompat.getColor(this, android.R.color.white));
                            break;
                    }

                    // Add the TextView to the FlexboxLayout
                    flexboxLayout.addView(keyTextView);

                    // Set an OnClickListener to return the selected key's details
                    keyTextView.setOnClickListener(v -> {
                        // Create an Intent to pass data back
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("keyName", key.name);
                        resultIntent.putExtra("keyType", key.type);

                        // Set the result with the Intent
                        setResult(RESULT_OK, resultIntent);

                        // Show a Toast message
                        Toast.makeText(RecentKeysActivity.this,
                                key.name + " key selected.", Toast.LENGTH_SHORT).show();

                        // Finish the activity and return to NewEntryActivity
                        finish();
                    });

                    // Set a LongClickListener to rename or delete the key
                    keyTextView.setOnLongClickListener(v -> {
                        showEditOrDeleteDialog(key);
                        return true;
                    });
                }

                // Add the newKey TextView as the last element
                flexboxLayout.addView(newKeyTextView);
            });
        }).start();
    }

    // Method to display a dialog for renaming or deleting a key
    private void showEditOrDeleteDialog(Key key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit or Delete Key");
        builder.setMessage("What would you like to do with this key?");

        builder.setPositiveButton("Rename", (dialog, which) -> {
            showRenameDialog(key);
        });

        builder.setNegativeButton("Delete", (dialog, which) -> {
            checkIfKeyCanBeDeleted(key);
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }


    // Method to show a dialog for renaming a key
    private void showRenameDialog(Key key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Key");

        final EditText input = new EditText(this);
        input.setText(key.name);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newKeyName = input.getText().toString().trim();
            if (!newKeyName.isEmpty() && !newKeyName.equals(key.name)) {
                // Check if a key with the new name already exists
                new Thread(() -> {
                    Key existingKey = keyDao.getKeyByName(newKeyName);
                    runOnUiThread(() -> {
                        if (existingKey != null) {
                            // Show a Toast message if the key name already exists
                            Toast.makeText(this, "A key with this name already exists. Choose a different name.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Proceed with renaming if no duplicate key is found
                            renameKeyInEntries(key, newKeyName);
                        }
                    });
                }).start();
            } else {
                Toast.makeText(this, "Invalid name or no change made.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }



    // Method to rename a key and update entries that use it
    private void renameKeyInEntries(Key key, String newKeyName) {
        new Thread(() -> {
            List<Entry> entries = entryDao.getAllEntries();
            for (Entry entry : entries) {
                try {
                    JSONObject jsonObject = new JSONObject(entry.payload);
                    if (jsonObject.has(key.name)) {
                        Object value = jsonObject.get(key.name);
                        jsonObject.remove(key.name); // Remove the old key
                        jsonObject.put(newKeyName, value); // Add the new key
                        entry.payload = jsonObject.toString(); // Update the payload
                        entryDao.updateEntry(entry);  // Update the entry in the database
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // After updating all entries, rename the key in the Key table
            key.name = newKeyName;
            keyDao.updateKey(key);  // Update the key in the Key table

            runOnUiThread(() -> {
                Toast.makeText(this, "Key renamed successfully.", Toast.LENGTH_SHORT).show();
                onResume();  // Refresh the list
            });
        }).start();
    }

    // Method to check if a key can be deleted
    private void checkIfKeyCanBeDeleted(Key key) {
        new Thread(() -> {
            boolean isUsed = false;
            List<Entry> entries = entryDao.getAllEntries();
            for (Entry entry : entries) {
                try {
                    JSONObject jsonObject = new JSONObject(entry.payload);
                    if (jsonObject.has(key.name)) {
                        isUsed = true;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (isUsed) {
                runOnUiThread(() -> Toast.makeText(this, "Cannot delete. Key is in use.", Toast.LENGTH_SHORT).show());
            } else {
                keyDao.deleteKey(key);  // Delete the key if not used
                runOnUiThread(() -> {
                    Toast.makeText(this, "Key deleted successfully.", Toast.LENGTH_SHORT).show();
                    onResume();  // Refresh the list
                });
            }
        }).start();
    }
}
