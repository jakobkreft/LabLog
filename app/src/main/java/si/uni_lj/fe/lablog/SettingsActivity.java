package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button functionality
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        View MQTTCommunicationLayout = findViewById(R.id.MQTTCommunicationLayout);
        MQTTCommunicationLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MQTTSettingsActivity.class);
            startActivity(intent);
        });

        View ImportExportLayout = findViewById(R.id.ImportExportLayout);
        ImportExportLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ImportExportSettingsActivity.class);
            startActivity(intent);
        });

        View TimestampLayout = findViewById(R.id.TimestampLayout);
        TimestampLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, TimestampFormatSettingsActivity.class);
            startActivity(intent);
        });

        View AboutLayout = findViewById(R.id.AboutLayout);
        AboutLayout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AboutSettingsActivity.class);
            startActivity(intent);
        });

        // Set an OnClickListener to the searchButton
        View searchActivityButton = findViewById(R.id.searchActivityButton);
        searchActivityButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        // Set an OnClickListener to the searchButton
        View settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setVisibility(View.INVISIBLE);

        // Set an OnClickListener to the addButton
        View addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, NewEntryActivity.class);
            startActivity(intent);
        });


    }
}