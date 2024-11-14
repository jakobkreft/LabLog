package si.uni_lj.fe.lablog;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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

    // Define an interface for reporting MQTT errors
    public interface MqttErrorCallback {
        void onError(String errorMessage);
    }

    public MqttStatus publishMessage(String message, MqttErrorCallback errorCallback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("mqtt_settings", Context.MODE_PRIVATE);
        String broker = sharedPreferences.getString("mqtt_broker", "");
        String topic = sharedPreferences.getString("mqtt_topic", "");
        boolean mqttEnabled = sharedPreferences.getBoolean("mqtt_enabled", false);
        boolean authEnabled = sharedPreferences.getBoolean("auth_enabled", false);
        String username = sharedPreferences.getString("username", "");
        String password = sharedPreferences.getString("password", "");

        // Check if MQTT is disabled
        if (!mqttEnabled) {
            errorCallback.onError("MQTT is disabled.");
            return MqttStatus.DISABLED;
        }

        // Check if broker or topic is empty
        if (broker.isEmpty() || topic.isEmpty()) {
            errorCallback.onError("Invalid MQTT settings: Broker or topic is empty.");
            return MqttStatus.INVALID_SETTINGS;
        }

        try {
            // Initialize the MQTT client
            MqttClient mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            // Set username and password if authentication is enabled
            if (authEnabled) {
                if (!username.isEmpty() && !password.isEmpty()) {
                    options.setUserName(username);
                    options.setPassword(password.toCharArray());
                } else {
                    errorCallback.onError("Authentication enabled but username or password is empty.");
                    return MqttStatus.INVALID_SETTINGS;
                }
            }

            // Connect to the MQTT broker
            mqttClient.connect(options);
            errorCallback.onError("Connected to MQTT broker: " + broker);

            // Create and publish the message
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);

            // Disconnect the client
            mqttClient.disconnect();
            errorCallback.onError("Message published successfully to topic: " + topic);
            return MqttStatus.SUCCESS;

        } catch (MqttException e) {
            // Report the error using the callback
            errorCallback.onError("MQTT publish failed: " + e.getMessage());
            errorCallback.onError("Reason Code: " + e.getReasonCode());
            errorCallback.onError("Cause: " + e.getCause());
            return e.getReasonCode() == MqttException.REASON_CODE_SERVER_CONNECT_ERROR ?
                    MqttStatus.CONNECTION_FAILED : MqttStatus.PUBLISH_FAILED;
        }
    }
}
