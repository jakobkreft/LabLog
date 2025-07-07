package si.uni_lj.fe.lablog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
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

    private static final String PREFS_NAME = "mqtt_settings";
    private static final String[] REQUIRED_FIELDS = {"mqtt_broker", "mqtt_topic", "username", "password"};

    private EditText brokerInput;
    private EditText topicInput;
    private Switch mqttSwitch;
    private Switch authSwitch;
    private EditText usernameInput;
    private EditText passwordInput;
    private ConstraintLayout usernameContainer;
    private ConstraintLayout passContainer;
    private Button saveSettingsButton;
    private EditText messageInput;
    private ImageButton sendButton;
    private Switch retainSwitch;

    private MQTTHelper mqttHelper;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mqttsettings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // bind UI
        brokerInput  = findViewById(R.id.brokerInput);
        topicInput   = findViewById(R.id.topicInput);
        mqttSwitch   = findViewById(R.id.mqttSwitch);
        authSwitch   = findViewById(R.id.authSwitch);
        usernameInput= findViewById(R.id.usernameInput);
        passwordInput= findViewById(R.id.passwordInput);
        usernameContainer = findViewById(R.id.usernameConstraintLayout);
        passContainer     = findViewById(R.id.passConstraintLayout);
        saveSettingsButton= findViewById(R.id.SaveSettingsButton);
        messageInput = findViewById(R.id.messageInput);
        sendButton   = findViewById(R.id.sendButton);
        retainSwitch = findViewById(R.id.retainSwitch);

        // helper
        mqttHelper = new MQTTHelper(this);

        loadMqttSettings();

        // show/hide auth fields
        authSwitch.setOnCheckedChangeListener((btn, checked) -> {
            usernameContainer.setVisibility(checked ? View.VISIBLE : View.GONE);
            passContainer.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // show/hide keyboard on outside tap
        findViewById(R.id.main).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) hideKeyboard();
            return false;
        });

        // reset Save button label on any input change
        TextWatcher resetWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c){
                saveSettingsButton.setText(R.string.save);
            }
            @Override public void afterTextChanged(Editable s){}
        };
        brokerInput.addTextChangedListener(resetWatcher);
        topicInput.addTextChangedListener(resetWatcher);
        usernameInput.addTextChangedListener(resetWatcher);
        passwordInput.addTextChangedListener(resetWatcher);
        retainSwitch.setOnCheckedChangeListener((b, c) -> saveSettingsButton.setText(R.string.save));
        mqttSwitch.setOnCheckedChangeListener((b, c) -> saveSettingsButton.setText(R.string.save));
        authSwitch.setOnCheckedChangeListener((b, c) -> saveSettingsButton.setText(R.string.save));

        // save action
        saveSettingsButton.setOnClickListener(v -> saveMqttSettings());

        // send test message
        sendButton.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (msg.isEmpty()) {
                messageInput.setError("Enter a message to send");
                return;
            }
            MQTTHelper.MqttStatus status = mqttHelper.publishMessage(msg, err ->
                    runOnUiThread(() -> Toast.makeText(this, err, Toast.LENGTH_LONG).show())
            );
            switch (status) {
                case SUCCESS:
                    Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show(); break;
                case DISABLED:
                    Toast.makeText(this, "MQTT disabled in settings", Toast.LENGTH_SHORT).show(); break;
                case INVALID_SETTINGS:
                    Toast.makeText(this, "Invalid settings", Toast.LENGTH_LONG).show(); break;
                case CONNECTION_FAILED:
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_LONG).show(); break;
                default:
                    Toast.makeText(this, "Unknown error", Toast.LENGTH_LONG).show();
            }
        });

        // back button
        View back = findViewById(R.id.backButton);
        back.setVisibility(View.VISIBLE);
        back.setOnClickListener(v -> finish());
    }

    private void loadMqttSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        brokerInput.setText(sp.getString("mqtt_broker", "broker.emqx.io"));
        topicInput.setText(sp.getString("mqtt_topic", "Lab/Log/data"));
        mqttSwitch.setChecked(sp.getBoolean("mqtt_enabled", false));
        authSwitch.setChecked(sp.getBoolean("auth_enabled", false));
        usernameInput.setText(sp.getString("username", ""));
        passwordInput.setText(sp.getString("password", ""));
        retainSwitch.setChecked(sp.getBoolean("retain_enabled", false));
        // toggle visibility
        usernameContainer.setVisibility(authSwitch.isChecked() ? View.VISIBLE : View.GONE);
        passContainer.setVisibility(authSwitch.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void saveMqttSettings() {
        String broker = brokerInput.getText().toString().trim();
        String topic  = topicInput.getText().toString().trim();
        boolean enabled   = mqttSwitch.isChecked();
        boolean authOn    = authSwitch.isChecked();
        String user       = usernameInput.getText().toString().trim();
        String pass       = passwordInput.getText().toString().trim();
        boolean retain    = retainSwitch.isChecked();

        // validate
        if (enabled) {
            if (broker.isEmpty()) {
                brokerInput.setError("Broker required");
                brokerInput.requestFocus();
                return;
            }
            if (topic.isEmpty()) {
                topicInput.setError("Topic required");
                topicInput.requestFocus();
                return;
            }
            if (authOn) {
                if (user.isEmpty()) {
                    usernameInput.setError("Username required");
                    usernameInput.requestFocus();
                    return;
                }
                if (pass.isEmpty()) {
                    passwordInput.setError("Password required");
                    passwordInput.requestFocus();
                    return;
                }
            }
        }
        // persist
        SharedPreferences.Editor ed = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        ed.putString("mqtt_broker", broker)
                .putString("mqtt_topic", topic)
                .putBoolean("mqtt_enabled", enabled)
                .putBoolean("auth_enabled", authOn)
                .putString("username", authOn ? user : "")
                .putString("password", authOn ? pass : "")
                .putBoolean("retain_enabled", retain)
                .apply();

        // feedback
        saveSettingsButton.setText("Saved");
        handler.postDelayed(() -> saveSettingsButton.setText(getString(R.string.save)), 1000);
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            v.clearFocus();
        }
    }
}
