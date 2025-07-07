// EntryDisplayHelper.java
package si.uni_lj.fe.lablog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class EntryDisplayHelper {

    private static final String PREFS_NAME = "lablog_prefs";
    private static final String KEY_TIMESTAMP_FORMAT = "pref_timestamp_format";
    private static final String DEFAULT_TIMESTAMP_FORMAT = "HH:mm:ss dd-MM-yyyy";

    private final Context context;
    private final LayoutInflater inflater;
    private final int widthStroke = 8;

    public EntryDisplayHelper(Context context, LayoutInflater inflater) {
        this.context = context;
        this.inflater = inflater;
    }

    /** Load the key→type mapping from the DB */
    public Map<String, String> loadKeyTypeMap(KeyDao keyDao) {
        Map<String, String> keyTypeMap = new HashMap<>();
        List<Key> keys = keyDao.getAllKeys();
        for (Key key : keys) {
            keyTypeMap.put(key.name, key.type);
        }
        return keyTypeMap;
    }

    /**
     * Display each Entry as a card, using the user’s preferred timestamp format.
     */
    public void displayEntries(List<Entry> entries,
                               Map<String, String> keyTypeMap,
                               LinearLayout container,
                               boolean showAll) {

        int entriesToShow = showAll ? entries.size() : 1;

        for (int i = 0; i < entriesToShow; i++) {
            Entry entry = entries.get(i);
            try {
                JSONObject jsonObject = new JSONObject(entry.payload);
                View cardView = inflater.inflate(R.layout.entry_card, container, false);

                // ——— FORMAT & SET TIMESTAMP ———
                TextView timestampTextView = cardView.findViewById(R.id.timestampTextView);
                timestampTextView.setText(formatTimestamp(entry.timestamp));

                // ——— FLEXBOX TOGGLE BUTTONS ———
                FlexboxLayout flexboxLayout = cardView.findViewById(R.id.flexboxLayout);
                flexboxLayout.setVisibility(View.GONE);
                cardView.setOnClickListener(v -> {
                    flexboxLayout.setVisibility(
                            flexboxLayout.getVisibility() == View.VISIBLE
                                    ? View.GONE : View.VISIBLE
                    );
                });
                setupButtons(flexboxLayout, entry, cardView, container);

                // ——— PAYLOAD KEY/VALUES ———
                LinearLayout payloadContainer = cardView.findViewById(R.id.payloadContainer);
                for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                    String key = it.next();
                    String valueStr = jsonObject.getString(key);
                    String type = keyTypeMap.get(key);

                    if (type != null) {
                        // key label
                        TextView keyTv = (TextView) inflater
                                .inflate(R.layout.key_text_view_layout, payloadContainer, false);
                        keyTv.setText(key);
                        GradientDrawable bg = (GradientDrawable) keyTv.getBackground();
                        setKeyBackgroundColor(type, bg);

                        // value
                        if ("Image".equalsIgnoreCase(type)) {
                            byte[] decoded = Base64.decode(valueStr, Base64.DEFAULT);
                            Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            View imgLayout = inflater.inflate(R.layout.image_view_item, payloadContainer, false);
                            ImageView iv = imgLayout.findViewById(R.id.imageView);
                            iv.setImageBitmap(bmp);
                            payloadContainer.addView(keyTv);
                            payloadContainer.addView(iv);

                        } else {
                            TextView valTv = (TextView) inflater
                                    .inflate(R.layout.value_text_view_layout, payloadContainer, false);
                            valTv.setText(valueStr);
                            payloadContainer.addView(keyTv);
                            payloadContainer.addView(valTv);
                        }
                    }
                }

                container.addView(cardView);

                // reset stroke color for next iteration
                TextView ktv = (TextView) inflater
                        .inflate(R.layout.key_text_view_layout, payloadContainer, false);
                GradientDrawable resetBg = (GradientDrawable) ktv.getBackground();
                resetBg.setStroke(widthStroke,
                        ContextCompat.getColor(context, android.R.color.white));

            } catch (Exception e) {
                Log.e("EntryDisplayHelper", "Error parsing entry: " + entry.id, e);
            }
        }
    }

    /** Read the user’s chosen format and apply it (or RAW). */
    private String formatTimestamp(long ts) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String pattern = prefs.getString(KEY_TIMESTAMP_FORMAT, DEFAULT_TIMESTAMP_FORMAT);

        if ("RAW".equals(pattern)) {
            return String.valueOf(ts);
        }

        try {
            return new SimpleDateFormat(pattern, Locale.getDefault())
                    .format(new Date(ts));
        } catch (IllegalArgumentException ex) {
            // fallback to default on invalid pattern
            return new SimpleDateFormat(DEFAULT_TIMESTAMP_FORMAT, Locale.getDefault())
                    .format(new Date(ts));
        }
    }

    // … (setupButtons, archiveEntry, editEntry, deleteEntry, duplicateEntry, resendEntry) …

    private void setKeyBackgroundColor(String type, GradientDrawable background) {
        switch (type.toLowerCase(Locale.ROOT)) {
            case "integer":
                background.setStroke(widthStroke,
                        ContextCompat.getColor(context, R.color.colorInteger));
                break;
            case "boolean":
                background.setStroke(widthStroke,
                        ContextCompat.getColor(context, R.color.colorBoolean));
                break;
            case "image":
                background.setStroke(widthStroke,
                        ContextCompat.getColor(context, R.color.colorImage));
                break;
            case "float":
                background.setStroke(widthStroke,
                        ContextCompat.getColor(context, R.color.colorFloat));
                break;
            case "string":
                background.setStroke(widthStroke,
                        ContextCompat.getColor(context, R.color.colorString));
                break;
            default:
                background.setStroke(widthStroke,
                        ContextCompat.getColor(context, android.R.color.white));
        }
    }



private void setupButtons(FlexboxLayout flexboxLayout, Entry entry, View cardView, LinearLayout container) {
        // Find buttons inside the flexbox layout
        Button archiveButton = flexboxLayout.findViewById(R.id.ArchiveButton);
        Button editButton = flexboxLayout.findViewById(R.id.EditButton);
        Button deleteButton = flexboxLayout.findViewById(R.id.DeleteButton);
        Button duplicateButton = flexboxLayout.findViewById(R.id.DuplicateButton);
        Button resendButton = flexboxLayout.findViewById(R.id.ResendButton);

        // Implement the functionality for each button
        archiveButton.setOnClickListener(v -> {
            // Archive logic
            archiveEntry(entry);
        });

        editButton.setOnClickListener(v -> {
            // Edit logic
            editEntry(entry);
        });

        deleteButton.setOnClickListener(v -> {
            // Delete logic
            deleteEntry(entry, cardView, container);
        });

        duplicateButton.setOnClickListener(v -> {
            // Duplicate logic
            duplicateEntry(entry);
        });

        resendButton.setOnClickListener(v -> {
            // Resend logic
            resendEntry(entry);
        });
    }

    private void archiveEntry(Entry entry) {
        // Implement archive functionality
        Log.d("EntryDisplayHelper", "Archiving entry: " + entry.id);
    }

    private void editEntry(Entry entry) {
        // Implement edit functionality
        Log.d("EntryDisplayHelper", "Editing entry: " + entry.id);
    }


    private void deleteEntry(Entry entry, View cardView, LinearLayout container) {
        // Create a confirmation dialog
        new AlertDialog.Builder(context)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry? This will delete it from the local database but will not affect any data already sent over MQTT.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Execute the delete operation
                    new Thread(() -> {
                        try {
                            // Delete the entry from the database
                            AppDatabase db = MyApp.getDatabase();
                            EntryDao entryDao = db.entryDao();
                            entryDao.deleteEntry(entry);

                            // Run on the UI thread to update the UI (remove card)
                            ((AppCompatActivity) context).runOnUiThread(() -> {
                                container.removeView(cardView); // Remove the card from the layout
                                Toast.makeText(context, "Entry deleted successfully.", Toast.LENGTH_SHORT).show();
                            });

                        } catch (Exception e) {
                            // Handle any error during deletion
                            Log.e("EntryDisplayHelper", "Error deleting entry: " + entry.id, e);
                            ((AppCompatActivity) context).runOnUiThread(() -> {
                                Toast.makeText(context, "Failed to delete entry.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .show();
    }

    private void duplicateEntry(Entry entry) {
        try {
            // Get the payload (key-value data) from the entry
            String payload = entry.payload;

            // Create an Intent to start NewEntryActivity
            Intent duplicateIntent = new Intent(context, NewEntryActivity.class);
            duplicateIntent.putExtra("payload", payload);  // Pass the payload

            // Start NewEntryActivity
            context.startActivity(duplicateIntent);
        } catch (Exception e) {
            Log.e("EntryDisplayHelperDuplicate", "Error duplicating entry: " + entry.id, e);
            Toast.makeText(context, "Error occurred while duplicating entry.", Toast.LENGTH_SHORT).show();
        }
    }
    private void resendEntry(Entry entry) {
        // Create confirmation dialog
        new AlertDialog.Builder(context)
                .setTitle("Resend Entry")
                .setMessage("Are you sure you want to resend this entry? This will republish the data over MQTT.")
                .setPositiveButton("Resend", (dialog, which) -> {
                    try {
                        // Start StatusActivity for resending
                        Intent intent = new Intent(context, StatusActivity.class);
                        intent.putExtra("payload", entry.payload);
                        intent.putExtra("timestamp", entry.timestamp);
                        intent.putExtra("isResend", true); // Add a flag to indicate resending
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Log.e("EntryDisplayHelper", "Error starting StatusActivity for resending entry: " + entry.id, e);
                        Toast.makeText(context, "Failed to resend entry: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}
