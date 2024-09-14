package si.uni_lj.fe.lablog;

import android.content.Context;
import android.content.SharedPreferences;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTHelper {

    private Context context;

    public MQTTHelper(Context context) {
        this.context = context;
    }

    // Define an enum for MQTT status outcomes
    public enum MqttStatus {
        SUCCESS,
        DISABLED,
        INVALID_SETTINGS,
        CONNECTION_FAILED,
        PUBLISH_FAILED
    }

    public MqttStatus publishMessage(String message) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("mqtt_settings", Context.MODE_PRIVATE);
        String broker = sharedPreferences.getString("mqtt_broker", "");
        String topic = sharedPreferences.getString("mqtt_topic", "");
        boolean mqttEnabled = sharedPreferences.getBoolean("mqtt_enabled", false);

        // Check if MQTT is disabled
        if (!mqttEnabled) {
            return MqttStatus.DISABLED; // MQTT disabled
        }

        // Check if broker or topic is empty
        if (broker.isEmpty() || topic.isEmpty()) {
            return MqttStatus.INVALID_SETTINGS; // Invalid settings
        }

        try {
            // Initialize the MQTT client
            MqttClient mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqttClient.connect(options);

            // Create and publish the message
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1); // Quality of Service level 1
            mqttClient.publish(topic, mqttMessage);

            mqttClient.disconnect();
            return MqttStatus.SUCCESS; // Successfully published
        } catch (MqttException e) {
            e.printStackTrace();
            if (e.getReasonCode() == MqttException.REASON_CODE_SERVER_CONNECT_ERROR) {
                return MqttStatus.CONNECTION_FAILED; // Could not connect to broker
            }
            return MqttStatus.PUBLISH_FAILED; // Failed to publish message
        }
    }
}
