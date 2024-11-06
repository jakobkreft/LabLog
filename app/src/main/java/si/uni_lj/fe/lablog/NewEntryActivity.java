package si.uni_lj.fe.lablog;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
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
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;
public class NewEntryActivity extends AppCompatActivity {
    private Uri currentImageUri;
    private LinearLayout linearLayout;
    private LayoutInflater inflater;
    private ArrayList<String> selectedKeysList; // Store the selected keys
    private String currentKeyForImage; // To store the current key for which image is being taken
    private HashMap<String, Uri> imageUriMap = new HashMap<>(); // Store image URIs associated with their keys
    private KeyDao keyDao;
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
                            createKeyCard(keyName, keyType, null);
                        }
                    });

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            // Display the image in the corresponding ImageButton
                            ImageButton imageButton = linearLayout.findViewWithTag(currentKeyForImage);
                            if (imageButton != null && currentImageUri != null) {
                                imageButton.setImageURI(currentImageUri);

                                // Update ImageButton to match_parent width and wrap_content height
                                imageButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
                                imageButton.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                                imageButton.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                                imageButton.setAdjustViewBounds(true);
                                imageButton.setBackground(ContextCompat.getDrawable(this, R.color.black));

                                // Store the URI in the map
                                imageUriMap.put(currentKeyForImage, currentImageUri);
                                downscaleImage();
                            } else {
                                Toast.makeText(this, "Failed to display image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    private MQTTHelper mqttHelper;

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
        // Initialize the DAO
        keyDao = MyApp.getDatabase().keyDao();
        // Initialize the DAO

        // Initialize MQTT Helper
        mqttHelper = new MQTTHelper(this);

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
        String currentTimestamp = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault()).format(new Date());
        textValueTextView.setText(currentTimestamp);

        // Hide other elements that are not needed for the timestamp card
        timestampCardView.findViewById(R.id.checkBox2).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.textInputLayout).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.imageButton).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.removeButton).setVisibility(View.INVISIBLE);

        // Set an OnClickListener on the timestamp card to update the timestamp
        timestampCardView.setOnClickListener(v -> {
            // Update the timestamp to the current time
            String newTimestamp = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault()).format(new Date());
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

        // Check if a payload is passed for duplication
        String payload = getIntent().getStringExtra("payload");
        if (payload != null) {
            // Initialize the DAO

            prepopulateWithPayload(payload);  // Method to prepopulate fields
        }
    }
    private void prepopulateWithPayload(String payload) {
        try {
            JSONObject jsonObject = new JSONObject(payload);

            // Iterate through each key-value pair and prepopulate fields
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonObject.getString(key);

                // Add the key to the selected keys list
                selectedKeysList.add(key);

                // Fetch the key type asynchronously
                getKeyType(key, keyType -> {
                    Log.e("NewEntryDuplicate", "create new card with key and value: " + key + " type: " + keyType);

                    // Create a new card based on the key and value
                    createKeyCard(key, keyType, value); // Pass the value for prepopulation
                });
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing entry for duplication: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getKeyType(String keyName, KeyTypeCallback callback) {
        // Use Executors to handle background threads
        Executors.newSingleThreadExecutor().execute(() -> {
            Key key = keyDao.getKeyByName(keyName);

            // Post back to the main thread to handle the result
            runOnUiThread(() -> {
                if (key != null) {
                    callback.onKeyTypeRetrieved(key.type);
                } else {
                    Log.e("NewEntryActivity", "Key not found for name: " + keyName);
                    callback.onKeyTypeRetrieved("String"); // Default to "String" if not found
                }
            });
        });
    }

    // Create a callback interface
    public interface KeyTypeCallback {
        void onKeyTypeRetrieved(String keyType);
    }

    private void createKeyCard(String keyName, String keyType, String value) {
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
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                TextInputEditText stringEditText = keyCardView.findViewById(R.id.textInputEditText);
                stringEditText.setTag(keyName);
                if (value != null) {
                    stringEditText.setText(value);
                }
                break;
            case "Boolean":
                keyCardView.findViewById(R.id.checkBox2).setVisibility(View.VISIBLE);
                CheckBox checkBox = keyCardView.findViewById(R.id.checkBox2);
                checkBox.setTag(keyName);
                if (value != null) {
                    checkBox.setChecked(Boolean.parseBoolean(value));
                }
                break;
            case "Integer":
            case "Float":
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                TextInputEditText numberEditText = keyCardView.findViewById(R.id.textInputEditText);
                numberEditText.setTag(keyName);
                numberEditText.setInputType(keyType.equals("Integer") ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                if (value != null) {
                    numberEditText.setText(value);
                }
                break;
            case "Image":
                keyCardView.findViewById(R.id.imageButton).setVisibility(View.VISIBLE);
                ImageButton imageButton = keyCardView.findViewById(R.id.imageButton);
                imageButton.setTag(keyName);

                if (value != null) {
                    byte[] decodedString = Base64.decode(value, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imageButton.setImageBitmap(decodedByte);
                }

                imageButton.setOnClickListener(v -> {
                    currentKeyForImage = keyName;
                    launchCamera();
                });
                break;
            default:
                Log.e("NewEntryActivity", "Unknown key type: " + keyType);
                break;
        }

        // Setup the remove button functionality
        View removeButton = keyCardView.findViewById(R.id.removeButton);
        removeButton.setOnClickListener(v -> removeCard(keyCardView, keyName));


        // Add the new key card to the LinearLayout
        linearLayout.addView(keyCardView);
    }

    private void removeCard(View cardView, String keyName) {
        // Remove the card view from the LinearLayout
        linearLayout.removeView(cardView);

        // Remove the key from the selected keys list
        selectedKeysList.remove(keyName);

        // Remove any associated image URI from the map
        imageUriMap.remove(keyName);

        //Toast.makeText(this, "Card removed for key: " + keyName, Toast.LENGTH_SHORT).show();
    }

    private void launchCamera() {
        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request the camera permission
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            try {
                // Create a file to store the image
                File imageFile = createImageFile();
                if (imageFile != null) {
                    // Save the URI for later use in the launcher callback
                    currentImageUri = FileProvider.getUriForFile(this, "si.uni_lj.fe.lablog.fileprovider", imageFile);

                    // Launch the camera intent with the URI
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
                    takePictureLauncher.launch(cameraIntent);
                } else {
                    Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to create image file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name with a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void downscaleImage() {
        try {
            // Check if the URI and file are valid
            if (currentImageUri == null) {
                throw new IOException("Invalid image URI.");
            }

            // Load the full-size image from the URI
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap originalBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(currentImageUri), null, options);

            if (originalBitmap == null) {
                throw new IOException("Failed to decode image from URI.");
            }

            // Calculate aspect ratio-preserving dimensions for HD resolution
            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();
            int newWidth = 1000; // target width
            int newHeight = (originalHeight * newWidth) / originalWidth;

            // If the height exceeds 1080, downscale using height as the reference
            if (newHeight > 1000) {
                newHeight = 1000; // target height
                newWidth = (originalWidth * newHeight) / originalHeight;
            }

            // Downscale the bitmap
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

            // Save the downscaled image back to the file
            try (OutputStream out = getContentResolver().openOutputStream(currentImageUri)) {
                if (out != null) {
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out); // Use JPEG format with 85% quality
                } else {
                    throw new IOException("Failed to get output stream.");
                }
            }

            // Update the image in the ImageButton after downscaling
            ImageButton imageButton = linearLayout.findViewWithTag(currentKeyForImage);
            if (imageButton != null) {
                imageButton.setImageBitmap(scaledBitmap);

                // Adjust layout settings for the image button
                imageButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
                imageButton.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                imageButton.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                imageButton.setAdjustViewBounds(true);
                imageButton.setBackground(ContextCompat.getDrawable(this, R.color.black));
            }

            Toast.makeText(this, "Image captured and downscaled successfully", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        Uri imageUri = imageUriMap.get(key);
                        if (imageUri != null) {
                            // Convert the image URI to a Base64 encoded string
                            try {
                                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos);
                                byte[] byteArray = baos.toByteArray();
                                String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);
                                jsonPayload.put(key, base64Image);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Failed to encode image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }

            // Create a new entry with the payload and current timestamp
            String payload = jsonPayload.toString();
            long timestamp = System.currentTimeMillis();

            // Start StatusActivity and pass the fields separately
            Intent statusIntent = new Intent(this, StatusActivity.class);
            statusIntent.putExtra("payload", payload);
            statusIntent.putExtra("timestamp", timestamp);
            startActivity(statusIntent);

            // Finish NewEntryActivity so it returns to the main screen when StatusActivity finishes
            finish();

        } catch (JSONException e) {
            Toast.makeText(this, "Failed to save entry: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


}

