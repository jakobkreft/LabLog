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
        boolean retainEnabled = sharedPreferences.getBoolean("retain_enabled", false);


        if (!mqttEnabled) {
            errorCallback.onError("MQTT is disabled.");
            return MqttStatus.DISABLED;
        }

        if (broker.isEmpty() || topic.isEmpty()) {
            errorCallback.onError("Invalid MQTT settings: Broker or topic is empty.");
            return MqttStatus.INVALID_SETTINGS;
        }

        try {
            // Validate the broker URI
            if (!broker.startsWith("tcp://") && !broker.startsWith("ssl://")) {
                errorCallback.onError("Invalid broker URI. It must start with 'tcp://' or 'ssl://'.");
                return MqttStatus.INVALID_SETTINGS;
            }

            MqttClient mqttClient = new MqttClient(broker, MqttClient.generateClientId(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(6); // Set a 10-second timeout for connection attempts
            options.setKeepAliveInterval(12); // Optional: Set a keep-alive interval

            if (authEnabled) {
                if (!username.isEmpty() && !password.isEmpty()) {
                    options.setUserName(username);
                    options.setPassword(password.toCharArray());
                } else {
                    errorCallback.onError("Authentication enabled but username or password is empty.");
                    return MqttStatus.INVALID_SETTINGS;
                }
            }

            try {
                mqttClient.connect(options);
            } catch (MqttException e) {
                switch (e.getReasonCode()) {
                    case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                        errorCallback.onError("Connection to the broker failed: Port might be incorrect or broker unreachable.");
                        return MqttStatus.CONNECTION_FAILED;
                    case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                        errorCallback.onError("Broker unavailable: Check if the broker is running.");
                        return MqttStatus.CONNECTION_FAILED;
                    default:
                        errorCallback.onError("Connection failed: " + e.getMessage());
                        return MqttStatus.CONNECTION_FAILED;
                }
            }

            errorCallback.onError("INFO: Connected to MQTT broker: " + broker);

            // Publish the message
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttMessage.setRetained(retainEnabled);
            mqttClient.publish(topic, mqttMessage);

            mqttClient.disconnect();
            errorCallback.onError("INFO: Message published successfully to topic: " + topic);
            return MqttStatus.SUCCESS;

        } catch (IllegalArgumentException e) {
            errorCallback.onError("Invalid broker URI: " + e.getMessage());
            return MqttStatus.INVALID_SETTINGS;
        } catch (MqttException e) {
            errorCallback.onError("MQTT publish failed: " + e.getMessage());
            return e.getReasonCode() == MqttException.REASON_CODE_SERVER_CONNECT_ERROR
                    ? MqttStatus.CONNECTION_FAILED
                    : MqttStatus.PUBLISH_FAILED;
        }
    }

}
