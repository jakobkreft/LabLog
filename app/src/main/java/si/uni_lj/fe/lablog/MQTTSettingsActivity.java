package si.uni_lj.fe.lablog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MQTTSettingsActivity extends AppCompatActivity {

    private EditText brokerInput;
    private EditText topicInput;
    private Switch mqttSwitch;
    private Switch authSwitch;
    private EditText usernameInput;
    private EditText passwordInput;
    private ConstraintLayout usernameConstraintLayout;
    private ConstraintLayout passConstraintLayout;
    private Button saveSettingsButton;
    private EditText messageInput;
    private ImageButton sendButton;

    private MQTTHelper mqttHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mqttsettings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the UI components
        brokerInput = findViewById(R.id.brokerInput);
        topicInput = findViewById(R.id.topicInput);
        mqttSwitch = findViewById(R.id.mqttSwitch);
        authSwitch = findViewById(R.id.authSwitch);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        usernameConstraintLayout = findViewById(R.id.usernameConstraintLayout);
        passConstraintLayout = findViewById(R.id.passConstraintLayout);
        saveSettingsButton = findViewById(R.id.SaveSettingsButton);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Initialize MQTT Helper
        mqttHelper = new MQTTHelper(this);

        // Load the saved settings from SharedPreferences
        loadMqttSettings();

        // Set visibility of username/password based on authSwitch
        authSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                usernameConstraintLayout.setVisibility(View.VISIBLE);
                passConstraintLayout.setVisibility(View.VISIBLE);
            } else {
                usernameConstraintLayout.setVisibility(View.GONE);
                passConstraintLayout.setVisibility(View.GONE);
            }
        });

        // Save the settings when the "Save" button is clicked
        saveSettingsButton.setOnClickListener(v -> saveMqttSettings());

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                // Publish the message and handle the result
                MQTTHelper.MqttStatus status = mqttHelper.publishMessage(message, errorMessage ->
                        runOnUiThread(() -> Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()));

                // Display the appropriate status to the user
                if (status == MQTTHelper.MqttStatus.SUCCESS) {
                    Toast.makeText(this, "Message sent successfully!", Toast.LENGTH_SHORT).show();
                } else if (status == MQTTHelper.MqttStatus.DISABLED) {
                    Toast.makeText(this, "MQTT is disabled in the settings.", Toast.LENGTH_SHORT).show();
                } else if (status == MQTTHelper.MqttStatus.INVALID_SETTINGS) {
                    Toast.makeText(this, "Invalid MQTT settings: Check broker, topic, username, and password.", Toast.LENGTH_LONG).show();
                } else if (status == MQTTHelper.MqttStatus.CONNECTION_FAILED) {
                    Toast.makeText(this, "Failed to connect to MQTT broker. Check your network and settings.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to send message. Unknown error occurred.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Please enter a message to send", Toast.LENGTH_SHORT).show();
            }
        });


        // Back button functionality
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadMqttSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences("mqtt_settings", MODE_PRIVATE);

        // Load and display the saved settings
        String broker = sharedPreferences.getString("mqtt_broker", "broker.emqx.io");
        String topic = sharedPreferences.getString("mqtt_topic", "Lab/Log/data");
        boolean mqttEnabled = sharedPreferences.getBoolean("mqtt_enabled", false);
        boolean authEnabled = sharedPreferences.getBoolean("auth_enabled", false);
        String username = sharedPreferences.getString("username", "");
        String password = sharedPreferences.getString("password", "");

        brokerInput.setText(broker);
        topicInput.setText(topic);
        mqttSwitch.setChecked(mqttEnabled);
        authSwitch.setChecked(authEnabled);
        usernameInput.setText(username);
        passwordInput.setText(password);

        // Set visibility of username/password fields based on saved authEnabled state
        if (authEnabled) {
            usernameConstraintLayout.setVisibility(View.VISIBLE);
            passConstraintLayout.setVisibility(View.VISIBLE);
        } else {
            usernameConstraintLayout.setVisibility(View.GONE);
            passConstraintLayout.setVisibility(View.GONE);
        }
    }

    private void saveMqttSettings() {
        String broker = brokerInput.getText().toString().trim();
        String topic = topicInput.getText().toString().trim();
        boolean mqttEnabled = mqttSwitch.isChecked();
        boolean authEnabled = authSwitch.isChecked();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (mqttEnabled && (broker.isEmpty() || topic.isEmpty())) {
            Toast.makeText(this, "Please provide both broker address and topic.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (authEnabled && (username.isEmpty() || password.isEmpty())) {
            Toast.makeText(this, "Please provide both username and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("mqtt_settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save the current settings
        editor.putString("mqtt_broker", broker);
        editor.putString("mqtt_topic", topic);
        editor.putBoolean("mqtt_enabled", mqttEnabled);
        editor.putBoolean("auth_enabled", authEnabled);
        editor.putString("username", authEnabled ? username : "");
        editor.putString("password", authEnabled ? password : "");

        editor.apply();

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
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
