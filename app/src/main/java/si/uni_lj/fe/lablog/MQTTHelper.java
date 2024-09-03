package si.uni_lj.fe.lablog;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTHelper {

    private MqttClient mqttClient;
    private Context context;

    public MQTTHelper(Context context) {
        this.context = context;
    }

    public void publishMessage(String message) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("mqtt_settings", Context.MODE_PRIVATE);
        String broker = sharedPreferences.getString("mqtt_broker", "");
        String topic = sharedPreferences.getString("mqtt_topic", "");
        boolean mqttEnabled = sharedPreferences.getBoolean("mqtt_enabled", false);

        // Check if MQTT is enabled
        if (!mqttEnabled) {
            Log.d("MQTTlog", "MQTT is disabled.");
            Toast.makeText(context, "MQTT is disabled. Enable it in the settings.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if broker or topic is empty
        if (broker.isEmpty() || topic.isEmpty()) {
            Toast.makeText(context, "Invalid MQTT settings. Please check broker address and topic.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Initialize the MQTT client
            mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
            Log.d("MQTTlog", "MQTT Client initialized");

            // Set the callback to handle connection loss or message delivery
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    ((AppCompatActivity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Connection lost: " + cause.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Not expecting messages, so this is not used
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    ((AppCompatActivity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Message delivered successfully", Toast.LENGTH_SHORT).show();
                    });
                }
            });

            // Connect to the MQTT broker
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            Log.d("MQTTlog", "Connecting to MQTT broker...");
            mqttClient.connect(options);
            Log.d("MQTTlog", "Connected to MQTT broker");
            Toast.makeText(context, "Connected to MQTT broker", Toast.LENGTH_SHORT).show();

            // Create and publish the message
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1); // Quality of Service level 1
            Log.d("MQTTlog", "Publishing message...");
            mqttClient.publish(topic, mqttMessage);
            Log.d("MQTTlog", "Message published");

            // Disconnect after publishing
            mqttClient.disconnect();
            Log.d("MQTTlog", "Disconnected from MQTT broker");
            ((AppCompatActivity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Disconnected from MQTT broker", Toast.LENGTH_SHORT).show();
            });

        } catch (MqttException e) {
            e.printStackTrace();
            ((AppCompatActivity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("MQTTlog", "Failed to send message", e);
            });
        }
    }
}
