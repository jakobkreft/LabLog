package si.uni_lj.fe.lablog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;

public class NewEntryActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private LayoutInflater inflater;
    private ArrayList<String> selectedKeysList; // Store the selected keys
    private String currentKeyForImage; // To store the current key for which image is being taken
    private HashMap<String, Bitmap> imageBitmapMap = new HashMap<>(); // Store images associated with their keys

    // Launcher to start RecentKeysActivity and handle the result
    private final ActivityResultLauncher<Intent> selectKeyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // Get the returned key name and type
                            String keyName = result.getData().getStringExtra("keyName");
                            String keyType = result.getData().getStringExtra("keyType");

                            // Add the key to the selected keys list
                            selectedKeysList.add(keyName);

                            // Create a new card based on the selected key
                            createKeyCard(keyName, keyType);
                        }
                    });

    // Launcher to handle the camera result
    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // Retrieve the captured image as a Bitmap
                            Bitmap imageBitmap = (Bitmap) result.getData().getExtras().get("data");

                            // Store the Bitmap in the HashMap using the key
                            imageBitmapMap.put(currentKeyForImage, imageBitmap);

                            // Display the image in the corresponding ImageButton
                            ImageButton imageButton = linearLayout.findViewWithTag(currentKeyForImage); // Find ImageButton by tag
                            if (imageButton != null && imageBitmap != null) {
                                imageButton.setImageBitmap(imageBitmap);

                                // Update ImageButton to match_parent width and wrap_content height
                                imageButton.setScaleType(ImageButton.ScaleType.FIT_CENTER); // Optional, to make the image scale properly
                                imageButton.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                                imageButton.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                                imageButton.setAdjustViewBounds(true); // Ensure the image scales within the new bounds
                                imageButton.setBackground(ContextCompat.getDrawable(this, R.color.black));
                            } else {
                                Toast.makeText(this, "Failed to display image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the selected keys list
        selectedKeysList = new ArrayList<>();

        // Find the LinearLayout inside the ScrollView where we will add the card
        linearLayout = findViewById(R.id.timestampLayout);

        // Inflate the key_value_card.xml layout
        inflater = LayoutInflater.from(this);
        View timestampCardView = inflater.inflate(R.layout.key_value_card, linearLayout, false);

        // Set the label "Timestamp" in the keyNameText TextView
        TextView keyNameTextView = timestampCardView.findViewById(R.id.keyNameText);
        keyNameTextView.setText("Timestamp");

        // Set the current timestamp in the textValue TextView
        TextView textValueTextView = timestampCardView.findViewById(R.id.textValue);
        String currentTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        textValueTextView.setText(currentTimestamp);

        // Hide other elements that are not needed for the timestamp card
        timestampCardView.findViewById(R.id.checkBox2).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.textInputLayout).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.imageButton).setVisibility(View.GONE);

        // Set an OnClickListener on the timestamp card to update the timestamp
        timestampCardView.setOnClickListener(v -> {
            // Update the timestamp to the current time
            String newTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
            textValueTextView.setText(newTimestamp);
        });

        // Add the inflated card view to the LinearLayout
        linearLayout.addView(timestampCardView);

        // Back button functionality
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // Add key functionality
        View addKeyTextView = findViewById(R.id.AddKey);
        addKeyTextView.setOnClickListener(v -> {
            Intent intent = new Intent(NewEntryActivity.this, RecentKeysActivity.class);
            // Pass the selected keys list to the RecentKeysActivity
            intent.putStringArrayListExtra("selectedKeys", selectedKeysList);
            selectKeyLauncher.launch(intent);
        });

        // Set up the Save button functionality
        View saveButton = findViewById(R.id.SaveEntryButton);
        saveButton.setOnClickListener(v -> saveEntry());
    }

    private void createKeyCard(String keyName, String keyType) {
        // Inflate the key_value_card.xml layout
        View keyCardView = inflater.inflate(R.layout.key_value_card, linearLayout, false);

        // Set the key name in the keyNameText TextView
        TextView keyNameTextView = keyCardView.findViewById(R.id.keyNameText);
        keyNameTextView.setText(keyName);

        // Hide all possible views initially
        keyCardView.findViewById(R.id.checkBox2).setVisibility(View.GONE);
        keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.GONE);
        keyCardView.findViewById(R.id.textValue).setVisibility(View.GONE);
        keyCardView.findViewById(R.id.imageButton).setVisibility(View.GONE);

        // Set up the input based on the key type
        switch (keyType) {
            case "String":
                // Show the TextInput for String
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                TextInputEditText stringEditText = keyCardView.findViewById(R.id.textInputEditText);
                stringEditText.setTag(keyName); // Tag with the key name
                break;
            case "Boolean":
                // Show the CheckBox for Boolean
                keyCardView.findViewById(R.id.checkBox2).setVisibility(View.VISIBLE);
                CheckBox checkBox = keyCardView.findViewById(R.id.checkBox2);
                checkBox.setTag(keyName); // Tag with the key name
                break;
            case "Integer":
                // Show the TextInput for Int and set input type to number
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                TextInputEditText intEditText = keyCardView.findViewById(R.id.textInputEditText);
                intEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                intEditText.setTag(keyName); // Tag with the key name
                break;
            case "Float":
                // Show the TextInput for Float and set input type to decimal number
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                TextInputEditText floatEditText = keyCardView.findViewById(R.id.textInputEditText);
                floatEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                floatEditText.setTag(keyName); // Tag with the key name
                break;
            case "Image":
                // Show the ImageButton for Image capture
                keyCardView.findViewById(R.id.imageButton).setVisibility(View.VISIBLE);
                ImageButton imageButton = keyCardView.findViewById(R.id.imageButton);
                imageButton.setTag(keyName); // Tag with the key name

                // Set OnClickListener to launch the camera
                imageButton.setOnClickListener(v -> {
                    currentKeyForImage = keyName; // Set the current key
                    launchCamera();
                });
                break;
            default:
                // Handle any other types if necessary
                break;
        }

        // Add the new key card to the LinearLayout
        linearLayout.addView(keyCardView);
    }

    private void launchCamera() {
        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request the camera permission
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            // Launch the camera intent
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureLauncher.launch(cameraIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Ensure the superclass method is called
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveEntry() {
        try {
            JSONObject jsonPayload = new JSONObject();

            // Loop through each selected key and retrieve the corresponding value
            for (String key : selectedKeysList) {
                View keyCardView = linearLayout.findViewWithTag(key);

                if (keyCardView != null) {
                    if (keyCardView.findViewById(R.id.textInputEditText) != null) {
                        TextInputEditText editText = keyCardView.findViewById(R.id.textInputEditText);
                        String value = editText.getText().toString();
                        jsonPayload.put(key, value);
                    } else if (keyCardView.findViewById(R.id.checkBox2) != null) {
                        CheckBox checkBox = keyCardView.findViewById(R.id.checkBox2);
                        boolean value = checkBox.isChecked();
                        jsonPayload.put(key, value);
                    } else if (keyCardView.findViewById(R.id.imageButton) != null) {
                        Bitmap bitmap = imageBitmapMap.get(key);
                        if (bitmap != null) {
                            // Convert the Bitmap to base64
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] byteArray = baos.toByteArray();
                            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);
                            jsonPayload.put(key, base64Image);
                        }
                    }
                }
            }

            // Create a new entry with the payload and current timestamp
            Entry entry = new Entry();
            entry.payload = jsonPayload.toString();
            entry.timestamp = System.currentTimeMillis();

            // Insert the entry into the database
            AppDatabase db = MyApp.getDatabase();
            EntryDao entryDao = db.entryDao();
            new Thread(() -> entryDao.insertEntry(entry)).start();

            // Notify the user of success
            Toast.makeText(this, "Entry saved successfully!", Toast.LENGTH_SHORT).show();

            // Clear the form or close the activity as needed
            finish();

        } catch (JSONException e) {
            Toast.makeText(this, "Failed to save entry: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
