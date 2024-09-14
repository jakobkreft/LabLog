package si.uni_lj.fe.lablog;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.MqttException;

public class MQTTSettingsActivity extends AppCompatActivity {

    private EditText brokerInput;
    private EditText topicInput;
    private Switch mqttSwitch;
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
        saveSettingsButton = findViewById(R.id.SaveSettingsButton);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Initialize MQTT Helper
        mqttHelper = new MQTTHelper(this);

        // Load the saved settings from SharedPreferences
        loadMqttSettings();

        // Save the settings when the "Save" button is clicked
        saveSettingsButton.setOnClickListener(v -> saveMqttSettings());

        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                mqttHelper.publishMessage(message); // No try-catch needed if MqttException is not thrown
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

        brokerInput.setText(broker);
        topicInput.setText(topic);
        mqttSwitch.setChecked(mqttEnabled);
    }

    private void saveMqttSettings() {
        String broker = brokerInput.getText().toString().trim();
        String topic = topicInput.getText().toString().trim();
        boolean mqttEnabled = mqttSwitch.isChecked();

        if (mqttEnabled && (broker.isEmpty() || topic.isEmpty())) {
            // If MQTT is enabled but broker or topic is empty, warn the user and don't save
            Toast.makeText(this, "Please provide both broker address and topic.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("mqtt_settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save the current settings
        editor.putString("mqtt_broker", broker);
        editor.putString("mqtt_topic", topic);
        editor.putBoolean("mqtt_enabled", mqttEnabled);

        // Apply the changes
        editor.apply();

        // Notify the user that the settings were saved
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }
}
