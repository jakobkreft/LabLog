package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Inflater;

import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class LatestEntryActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private LayoutInflater inflater;
    private EntryDao entryDao;
    private KeyDao keyDao;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Map<String, String> keyTypeMap;  // Map to store key types
    private final int widthStroke = 8;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_latest_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the LinearLayout and LayoutInflater
        linearLayout = findViewById(R.id.LinearLayout);
        inflater = LayoutInflater.from(this);

        // Get the EntryDao and KeyDao
        entryDao = MyApp.getDatabase().entryDao();
        keyDao = MyApp.getDatabase().keyDao();

        // Initialize ExecutorService and Handler
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize the map to store key types
        keyTypeMap = new HashMap<>();

        // Load the entries and key types from the database and display them on a background thread
        loadEntriesAndKeyTypes();

        // Back button functionality
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // Add button functionality
        View addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(LatestEntryActivity.this, NewEntryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Clear the current entries in the LinearLayout
        linearLayout.removeAllViews();

        // Reload the entries and key types from the database
        loadEntriesAndKeyTypes();
    }


    private void loadEntriesAndKeyTypes() {
        executorService.execute(() -> {
            // Fetch all entries and key types in the background
            List<Entry> entries = entryDao.getAllEntries();
            List<Key> keys = keyDao.getAllKeys();

            // Populate the keyTypeMap with key names and their types
            for (Key key : keys) {
                keyTypeMap.put(key.name, key.type);
            }

            // Post the results to the main thread for UI update
            mainHandler.post(() -> displayEntries(entries));
        });
    }

    private void displayEntries(List<Entry> entries) {
        for (Entry entry : entries) {
            try {
                // Parse the JSON payload
                JSONObject jsonObject = new JSONObject(entry.payload);

                // Inflate the card layout
                View cardView = inflater.inflate(R.layout.entry_card, linearLayout, false);

                // Set the timestamp
                TextView timestampTextView = cardView.findViewById(R.id.timestampTextView);
                String formattedTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date(entry.timestamp));
                timestampTextView.setText(formattedTimestamp);

                // Get the container for key-value pairs
                LinearLayout payloadContainer = cardView.findViewById(R.id.payloadContainer);

                // Iterate through each key-value pair in the JSON object
                for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                    String key = it.next();
                    String valueStr = jsonObject.getString(key);

                    // Get the type of the key from the map
                    String type = keyTypeMap.get(key);

                    // Handle the key based on its type
                    if (type != null) {
                        // Create a TextView for the key
                        TextView keyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, payloadContainer, false);
                        keyTextView.setText(key);

                        GradientDrawable background = (GradientDrawable) keyTextView.getBackground();

                        // Set background color based on key type

                        switch (type.toLowerCase()) {

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

                        // Create a TextView or ImageView for the value based on the type
                        if ("Image".equalsIgnoreCase(type)) {
                            // Decode the Base64 image and set it in the ImageView
                            byte[] decodedString = Base64.decode(valueStr, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            // Inflate the ImageView from the XML layout
                            View imageLayout = LayoutInflater.from(LatestEntryActivity.this).inflate(R.layout.image_view_item, payloadContainer, false);
                            ImageView imageView = imageLayout.findViewById(R.id.imageView);

                            // Set the bitmap to the ImageView
                            imageView.setImageBitmap(decodedByte);

                            // Add the key and image views to the container
                            payloadContainer.addView(keyTextView);
                            payloadContainer.addView(imageView);
                        }
                        else {
                            // Create a TextView for other types of values
                            TextView valueTextView = (TextView) inflater.inflate(R.layout.value_text_view_layout, payloadContainer, false);
                            valueTextView.setText(valueStr); // Set the text

// Add the TextView to the container
                            payloadContainer.addView(keyTextView);
                            payloadContainer.addView(valueTextView);

                            Log.d("valuevalue", valueStr);
                        }
                    }
                }

                // Add the card to the LinearLayout
                linearLayout.addView(cardView);

                // back to white after its done
                TextView keyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, linearLayout, false);
                GradientDrawable background = (GradientDrawable) keyTextView.getBackground();
                background.setStroke(widthStroke, ContextCompat.getColor(this, android.R.color.white));


            } catch (Exception e) {
                Log.e("LatestEntryActivity", "Error parsing entry: " + entry.id, e);
            }
        }
    }
}
