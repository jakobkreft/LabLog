package si.uni_lj.fe.lablog;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
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

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.textfield.TextInputEditText;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    private FlexboxLayout flexboxLayout;
    private TextView newKeyTextView;
    private final int widthStroke = 8;
    private EntryDao entryDao;

    private static final String PREFS_NAME = "lablog_prefs";
    private static final String KEY_TIMESTAMP_FORMAT = "pref_timestamp_format";
    private static final String DEFAULT_TIMESTAMP_FORMAT = "HH:mm:ss dd-MM-yyyy";

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
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        currentImageUri = selectedImageUri;

                        // Downscale the selected image
                        downscaleImage();
                    } else {
                        Toast.makeText(this, "Failed to load image from gallery.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private String formatTimestamp(long ts) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String pattern = prefs.getString(KEY_TIMESTAMP_FORMAT, DEFAULT_TIMESTAMP_FORMAT);
        if ("RAW".equals(pattern)) {
            return String.valueOf(ts);
        }
        try {
            return new SimpleDateFormat(pattern, Locale.getDefault())
                    .format(new Date(ts));
        } catch (IllegalArgumentException e) {
            // fallback to default if pattern invalid
            return new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT, Locale.getDefault())
                    .format(new Date(ts));
        }
    }

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

        // DAOs
        keyDao   = MyApp.getDatabase().keyDao();
        entryDao = MyApp.getDatabase().entryDao();

        // Init selected-keys list
        selectedKeysList = new ArrayList<>();

        // Timestamp card container
        linearLayout = findViewById(R.id.timestampLayout);
        inflater     = LayoutInflater.from(this);

        // Inflate & configure timestamp card
        View timestampCardView = inflater.inflate(R.layout.key_value_card, linearLayout, false);
        TextView keyNameTextView  = timestampCardView.findViewById(R.id.keyNameText);
        TextView textValueTextView= timestampCardView.findViewById(R.id.textValue);

        keyNameTextView.setText("Timestamp");
        // Use user’s saved format
        textValueTextView.setText(formatTimestamp(System.currentTimeMillis()));

        // Hide all other inputs on this card
        timestampCardView.findViewById(R.id.checkBox2)      .setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.textInputLayout).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.imageButton)    .setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.removeButton)   .setVisibility(View.INVISIBLE);

        // Refresh timestamp on tap
        timestampCardView.setOnClickListener(v ->
                textValueTextView.setText(formatTimestamp(System.currentTimeMillis()))
        );

        linearLayout.addView(timestampCardView);

        // Back button
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // “Add Key” launcher
        View addKey = findViewById(R.id.AddKey);
        addKey.setOnClickListener(v -> {
            Intent i = new Intent(NewEntryActivity.this, RecentKeysActivity.class);
            i.putStringArrayListExtra("selectedKeys", selectedKeysList);
            selectKeyLauncher.launch(i);
        });

        // Save entry button
        View saveBtn = findViewById(R.id.SaveEntryButton);
        saveBtn.setOnClickListener(v -> saveEntry());

        // Duplicate payload?
        String payload = getIntent().getStringExtra("payload");
        if (payload != null) {
            prepopulateWithPayload(payload);
        }

        // Flexbox for additional keys
        flexboxLayout = findViewById(R.id.flexboxLayout);
        newKeyTextView = (TextView) inflater.inflate(
                R.layout.key_text_view_layout, flexboxLayout, false);
        newKeyTextView.setText(R.string.new_key);
        newKeyTextView.setOnClickListener(v -> {
            startActivity(new Intent(NewEntryActivity.this, NewKeyActivity.class));
        });

        // Image & gallery launchers already registered above
        // (no changes needed there)

        // Dismiss keyboard on outside touch
        findViewById(R.id.main).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) hideKeyboard();
            return false;
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Fetch the keys from the database and update the UI
        new Thread(() -> {
            List<Key> keyList = keyDao.getAllKeys();

            runOnUiThread(() -> {
                // Clear existing views in FlexboxLayout
                flexboxLayout.removeAllViews();

                // Add key TextViews dynamically
                for (Key key : keyList) {
                    if (selectedKeysList.contains(key.name)) continue;

                    TextView keyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, flexboxLayout, false);
                    keyTextView.setText(key.name.trim());

                    // Set stroke color based on key type
                    GradientDrawable background = (GradientDrawable) keyTextView.getBackground();
                    setStrokeColorByType(key, background);

                    // Add TextView to FlexboxLayout
                    flexboxLayout.addView(keyTextView);

                    // Handle key click
                    keyTextView.setOnClickListener(v -> {
                        selectedKeysList.add(key.name);
                        createKeyCard(key.name, key.type, null);
                        Toast.makeText(this, key.name + " key selected.", Toast.LENGTH_SHORT).show();
                    });

                    // Handle long click for rename/delete
                    keyTextView.setOnLongClickListener(v -> {
                        showEditOrDeleteDialog(key);
                        return true;
                    });
                }

                // Add the "New Key" TextView at the end
                flexboxLayout.addView(newKeyTextView);
            });
        }).start();
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

    private void setStrokeColorByType(Key key, GradientDrawable background) {
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
    }

    private void showEditOrDeleteDialog(Key key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit or Delete Key");
        builder.setMessage("What would you like to do with this key?");

        builder.setPositiveButton("Rename", (dialog, which) -> showRenameDialog(key));
        builder.setNegativeButton("Delete", (dialog, which) -> checkIfKeyCanBeDeleted(key));
        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showRenameDialog(Key key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Key");

        final EditText input = new EditText(this);
        input.setText(key.name);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newKeyName = input.getText().toString().trim();
            if (!newKeyName.isEmpty() && !newKeyName.equals(key.name)) {
                new Thread(() -> {
                    Key existingKey = keyDao.getKeyByName(newKeyName);
                    runOnUiThread(() -> {
                        if (existingKey != null) {
                            Toast.makeText(this, "Key with this name already exists.", Toast.LENGTH_SHORT).show();
                        } else {
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

    private void renameKeyInEntries(Key key, String newKeyName) {
        new Thread(() -> {
            List<Entry> entries = entryDao.getAllEntries();
            for (Entry entry : entries) {
                try {
                    JSONObject jsonObject = new JSONObject(entry.payload);
                    if (jsonObject.has(key.name)) {
                        Object value = jsonObject.get(key.name);
                        jsonObject.remove(key.name);
                        jsonObject.put(newKeyName, value);
                        entry.payload = jsonObject.toString();
                        entryDao.updateEntry(entry);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            key.name = newKeyName;
            keyDao.updateKey(key);

            runOnUiThread(() -> {
                Toast.makeText(this, "Key renamed successfully.", Toast.LENGTH_SHORT).show();
                onResume();
            });
        }).start();
    }

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
                keyDao.deleteKey(key);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Key deleted successfully.", Toast.LENGTH_SHORT).show();
                    onResume();
                });
            }
        }).start();
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
                    showImageSourceDialog();
                });


                break;
            default:
                Log.e("NewEntryActivity", "Unknown key type: " + keyType);
                break;
        }

        // Setup the remove button functionality
        View removeButton = keyCardView.findViewById(R.id.removeButton);
        removeButton.setOnClickListener(v -> {
            removeCard(keyCardView, keyName);
            onResume(); // Update FlexboxLayout after removing a key card
        });

        // Add the new key card to the LinearLayout
        linearLayout.addView(keyCardView);

        // Update FlexboxLayout after adding a key card
        onResume();
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setMessage("Choose an option.")
                .setPositiveButton("Take Photo", (dialog, which) -> launchCamera())
                .setNegativeButton("Choose from Gallery", (dialog, which) -> openGallery())
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }



    private void removeCard(View cardView, String keyName) {
        // Remove the card view from the LinearLayout
        linearLayout.removeView(cardView);

        // Remove the key from the selected keys list
        selectedKeysList.remove(keyName);

        // Remove any associated image URI from the map
        imageUriMap.remove(keyName);

        // Update FlexboxLayout to show the key again
        onResume();
    }

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            try {
                File imageFile = createImageFile();
                if (imageFile != null) {
                    currentImageUri = FileProvider.getUriForFile(this, "si.uni_lj.fe.lablog.fileprovider", imageFile);

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
            if (currentImageUri == null) {
                throw new IOException("Invalid image URI.");
            }

            // Copy the URI to a private file location if it's from gallery
            File imageFile = copyUriToFile(currentImageUri);

            if (imageFile == null || !imageFile.exists()) {
                throw new IOException("Failed to copy image to private storage.");
            }

            // Decode the image from the file
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            if (originalBitmap == null) {
                throw new IOException("Failed to decode image from file.");
            }

            // Calculate aspect ratio-preserving dimensions
            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();
            int newWidth = 1000; // Target width
            int newHeight = (originalHeight * newWidth) / originalWidth;

            if (newHeight > 1000) {
                newHeight = 1000; // Target height
                newWidth = (originalWidth * newHeight) / originalHeight;
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

            // Save the scaled image to a temporary file
            File scaledImageFile = createTempImageFile();
            try (OutputStream out = new FileOutputStream(scaledImageFile)) {
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
            }

            // Update the ImageButton with the scaled image
            ImageButton imageButton = linearLayout.findViewWithTag(currentKeyForImage);
            if (imageButton != null) {
                imageButton.setImageBitmap(scaledBitmap);
                imageButton.setScaleType(ImageButton.ScaleType.FIT_CENTER);
                imageButton.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
                imageButton.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                imageButton.setAdjustViewBounds(true);
                imageButton.setBackground(ContextCompat.getDrawable(this, R.color.black));

                // Store the URI of the scaled image
                imageUriMap.put(currentKeyForImage, Uri.fromFile(scaledImageFile));
            }

            Toast.makeText(this, "Image processed successfully.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File copyUriToFile(Uri uri) throws IOException {
        File destFile = createTempImageFile();
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(destFile)) {
            if (in == null) throw new IOException("Failed to open input stream for URI.");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return destFile;
    }



    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "TEMP_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
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