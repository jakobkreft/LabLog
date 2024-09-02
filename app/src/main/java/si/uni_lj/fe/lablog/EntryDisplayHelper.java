// EntryDisplayHelper.java
package si.uni_lj.fe.lablog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class EntryDisplayHelper {

    private final Context context;
    private final LayoutInflater inflater;
    private final int widthStroke = 8;

    public EntryDisplayHelper(Context context, LayoutInflater inflater) {
        this.context = context;
        this.inflater = inflater;
    }

    public Map<String, String> loadKeyTypeMap(KeyDao keyDao) {
        Map<String, String> keyTypeMap = new HashMap<>();
        List<Key> keys = keyDao.getAllKeys();
        for (Key key : keys) {
            keyTypeMap.put(key.name, key.type);
        }
        return keyTypeMap;
    }

    public void displayEntries(List<Entry> entries, Map<String, String> keyTypeMap, LinearLayout container, boolean showAll) {
        int entriesToShow = showAll ? entries.size() : 1;

        for (int i = 0; i < entriesToShow; i++) {
            Entry entry = entries.get(i);
            try {
                // Parse the JSON payload
                JSONObject jsonObject = new JSONObject(entry.payload);

                // Inflate the card layout
                View cardView = inflater.inflate(R.layout.entry_card, container, false);

                // Set the timestamp
                TextView timestampTextView = cardView.findViewById(R.id.timestampTextView);
                String formattedTimestamp = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                        .format(new Date(entry.timestamp));
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
                        setKeyBackgroundColor(type, background);

                        // Create a TextView or ImageView for the value based on the type
                        if ("Image".equalsIgnoreCase(type)) {
                            // Decode the Base64 image and set it in the ImageView
                            byte[] decodedString = Base64.decode(valueStr, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            // Inflate the ImageView from the XML layout
                            View imageLayout = inflater.inflate(R.layout.image_view_item, payloadContainer, false);
                            ImageView imageView = imageLayout.findViewById(R.id.imageView);

                            // Set the bitmap to the ImageView
                            imageView.setImageBitmap(decodedByte);

                            // Add the key and image views to the container
                            payloadContainer.addView(keyTextView);
                            payloadContainer.addView(imageView);
                        } else {
                            // Create a TextView for other types of values
                            TextView valueTextView = (TextView) inflater.inflate(R.layout.value_text_view_layout, payloadContainer, false);
                            valueTextView.setText(valueStr); // Set the text

                            // Add the TextView to the container
                            payloadContainer.addView(keyTextView);
                            payloadContainer.addView(valueTextView);
                        }
                    }
                }

                // Add the card to the container
                container.addView(cardView);

                // back to white after its done
                TextView keyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, payloadContainer, false);
                GradientDrawable background = (GradientDrawable) keyTextView.getBackground();
                background.setStroke(widthStroke, ContextCompat.getColor(context, android.R.color.white));



            } catch (Exception e) {
                Log.e("EntryDisplayHelper", "Error parsing entry: " + entry.id, e);
            }
        }
    }

    private void setKeyBackgroundColor(String type, GradientDrawable background) {
        switch (type.toLowerCase()) {
            case "integer":
                background.setStroke(widthStroke, ContextCompat.getColor(context, R.color.colorInteger));
                break;
            case "boolean":
                background.setStroke(widthStroke, ContextCompat.getColor(context, R.color.colorBoolean));
                break;
            case "image":
                background.setStroke(widthStroke, ContextCompat.getColor(context, R.color.colorImage));
                break;
            case "float":
                background.setStroke(widthStroke, ContextCompat.getColor(context, R.color.colorFloat));
                break;
            case "string":
                background.setStroke(widthStroke, ContextCompat.getColor(context, R.color.colorString));
                break;
            default:
                background.setStroke(widthStroke, ContextCompat.getColor(context, android.R.color.white));
                break;
        }
    }
}
