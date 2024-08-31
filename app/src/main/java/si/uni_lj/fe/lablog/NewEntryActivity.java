package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NewEntryActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private LayoutInflater inflater;
    private ArrayList<String> selectedKeysList; // Store the selected keys

    // Launcher to start RecentKeysActivity and handle the result
    private final ActivityResultLauncher<Intent> selectKeyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            // Get the returned key name and type
                            String keyName = result.getData().getStringExtra("keyName");
                            String keyType = result.getData().getStringExtra("keyType");

                            // Add the key to the selected keys list
                            selectedKeysList.add(keyName);

                            // Create a new card based on the selected key
                            createKeyCard(keyName, keyType);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the selected keys list
        selectedKeysList = new ArrayList<>();

        // Find the LinearLayout inside the ScrollView where we will add the card
        linearLayout = findViewById(R.id.timestampLayout);

        // Inflate the key_value_card.xml layout
        inflater = LayoutInflater.from(this);
        View timestampCardView = inflater.inflate(R.layout.key_value_card, linearLayout, false);

        // Set the label "Timestamp" in the keyNameText TextView
        TextView keyNameTextView = timestampCardView.findViewById(R.id.keyNameText);
        keyNameTextView.setText("Timestamp");

        // Set the current timestamp in the textValue TextView
        TextView textValueTextView = timestampCardView.findViewById(R.id.textValue);
        String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        textValueTextView.setText(currentTimestamp);

        // Hide other elements that are not needed for the timestamp card
        timestampCardView.findViewById(R.id.checkBox2).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.textInputLayout).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.imageButton).setVisibility(View.GONE);
        timestampCardView.findViewById(R.id.imageView).setVisibility(View.GONE);

        // Add the inflated card view to the LinearLayout
        linearLayout.addView(timestampCardView);

        // Back button functionality
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // Add key functionality
        View addKeyTextView = findViewById(R.id.AddKey);
        addKeyTextView.setOnClickListener(v -> {
            Intent intent = new Intent(NewEntryActivity.this, RecentKeysActivity.class);
            // Pass the selected keys list to the RecentKeysActivity
            intent.putStringArrayListExtra("selectedKeys", selectedKeysList);
            selectKeyLauncher.launch(intent);
        });
    }

    private void createKeyCard(String keyName, String keyType) {
        // Inflate the key_value_card.xml layout
        View keyCardView = inflater.inflate(R.layout.key_value_card, linearLayout, false);

        // Set the key name in the keyNameText TextView
        TextView keyNameTextView = keyCardView.findViewById(R.id.keyNameText);
        keyNameTextView.setText(keyName);

        // Hide all possible views initially
        keyCardView.findViewById(R.id.checkBox2).setVisibility(View.GONE);
        keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.GONE);
        keyCardView.findViewById(R.id.textValue).setVisibility(View.GONE);
        keyCardView.findViewById(R.id.imageButton).setVisibility(View.GONE);
        keyCardView.findViewById(R.id.imageView).setVisibility(View.GONE);

        // Set up the input based on the key type
        switch (keyType) {
            case "String":
                // Show the TextInput for String
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                break;
            case "Boolean":
                // Show the CheckBox for Boolean
                keyCardView.findViewById(R.id.checkBox2).setVisibility(View.VISIBLE);
                break;
            case "Integer":
                // Show the TextInput for Int and set input type to number
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                TextInputEditText intEditText = keyCardView.findViewById(R.id.textInputEditText);
                intEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case "Float":
                // Show the TextInput for Float and set input type to decimal number
                keyCardView.findViewById(R.id.textInputLayout).setVisibility(View.VISIBLE);
                TextInputEditText floatEditText = keyCardView.findViewById(R.id.textInputEditText);
                floatEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case "Image":
                // Show the ImageButton for Image capture
                keyCardView.findViewById(R.id.imageButton).setVisibility(View.VISIBLE);
                keyCardView.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
                break;
            default:
                // Handle any other types if necessary
                break;
        }

        // Add the new key card to the LinearLayout
        linearLayout.addView(keyCardView);
    }

}
