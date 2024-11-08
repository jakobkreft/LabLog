package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Entry;
import si.uni_lj.fe.lablog.data.EntryDao;

public class StatusActivity extends AppCompatActivity {

    private TextView statusTextView;
    private Button confirmButton;
    private boolean hasErrors = false; // Flag to track errors

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_status);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        statusTextView = findViewById(R.id.statusText);
        confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(v -> finishProcess());
        confirmButton.setVisibility(View.INVISIBLE); // Hide until task is done

        // Retrieve the individual fields from the Intent
        String payload = getIntent().getStringExtra("payload");
        long timestamp = getIntent().getLongExtra("timestamp", 0);

        // Start the background process to save entry and publish to MQTT
        handleEntryProcess(payload, timestamp);
    }

    private void handleEntryProcess(String payload, long timestamp) {
        appendStatus("Saving entry to database...", false, false);

        new Thread(() -> {
            boolean dbSuccess = saveEntryToDatabase(payload, timestamp);

            runOnUiThread(() -> appendStatus(dbSuccess ? "Database save successful." : "Database save failed.", !dbSuccess, dbSuccess));

            runOnUiThread(() -> appendStatus("Checking MQTT settings...", false, false));

            boolean mqttUsed = false;
            MQTTHelper.MqttStatus mqttStatus = null;

            try {
                // Format payload with timestamp for MQTT
                String formattedMessage = createFormattedMessage(payload, timestamp);

                // Publish formatted message to MQTT
                mqttStatus = publishToMqtt(formattedMessage);
                mqttUsed = mqttStatus == MQTTHelper.MqttStatus.SUCCESS;
            } catch (Exception e) {
                mqttStatus = MQTTHelper.MqttStatus.PUBLISH_FAILED;
            }

            boolean finalDbSuccess = dbSuccess;
            MQTTHelper.MqttStatus finalMqttStatus = mqttStatus;

            runOnUiThread(() -> {
                if (finalDbSuccess) {
                    if (finalMqttStatus == MQTTHelper.MqttStatus.SUCCESS) {
                        appendStatus("Entry published to MQTT successfully!", false, true);
                    } else if (finalMqttStatus == MQTTHelper.MqttStatus.DISABLED) {
                        appendStatus("MQTT was disabled in the settings.", false, false);
                    } else if (finalMqttStatus == MQTTHelper.MqttStatus.INVALID_SETTINGS) {
                        appendStatus("MQTT publish failed: Invalid broker or topic settings.", true, false);
                    } else if (finalMqttStatus == MQTTHelper.MqttStatus.CONNECTION_FAILED) {
                        appendStatus("MQTT publish failed: Connection to broker failed.", true, false);
                    } else {
                        appendStatus("MQTT publish failed: Unknown error. Check your internet connection.", true, false);
                    }
                } else {
                    appendStatus("Database save failed.", true, false);
                }

                // Automatically finish the activity if there are no errors
                if (hasErrors) {
                    confirmButton.setVisibility(View.VISIBLE);
                } else {
                    finishProcess();
                }
            });
        }).start();
    }

    private String createFormattedMessage(String payload, long timestamp) {
        try {
            // Create JSON object with timestamp and payload as "data"
            JSONObject jsonMessage = new JSONObject();
            jsonMessage.put("timestamp", timestamp);

            // Parse payload into JSON and add it as the "data" field
            JSONObject dataObject = new JSONObject(payload);
            jsonMessage.put("data", dataObject);

            return jsonMessage.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}"; // Return an empty JSON on error
        }
    }

    private boolean saveEntryToDatabase(String payload, long timestamp) {
        try {
            Entry entry = new Entry();
            entry.payload = payload;
            entry.timestamp = timestamp;

            // Simulate saving to the database
            AppDatabase db = MyApp.getDatabase();
            EntryDao entryDao = db.entryDao();
            entryDao.insertEntry(entry);  // Save entry to the database
            return true;  // Success
        } catch (Exception e) {
            e.printStackTrace();
            return false;  // Failure
        }
    }

    private MQTTHelper.MqttStatus publishToMqtt(String message) throws MqttException {
        MQTTHelper mqttHelper = new MQTTHelper(this);
        return mqttHelper.publishMessage(message); // Returns the result of the MQTT operation
    }

    // Utility method to append new status messages with color
    private void appendStatus(String newStatus, boolean isError, boolean isSuccess) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(statusTextView.getText());

        SpannableString spannableString = new SpannableString(newStatus + "\n");
        if (isError) {
            spannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, spannableString.length(), 0);
            hasErrors = true; // Set the flag if there is an error
        } else if (isSuccess) {
            spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), 0, spannableString.length(), 0);
        } else {
            spannableString.setSpan(new ForegroundColorSpan(Color.WHITE), 0, spannableString.length(), 0);
        }

        spannableStringBuilder.append(spannableString);
        statusTextView.setText(spannableStringBuilder);
    }

    private void finishProcess() {
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();  // Finish StatusActivity and go back to NewEntryActivity
    }
}
