package si.uni_lj.fe.lablog;
import android.content.Intent;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LatestEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_latest_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find the back button by its ID
        View backButton = findViewById(R.id.backButton);

        // Make the back button visible
        backButton.setVisibility(View.VISIBLE);

        // Set an OnClickListener on the back button to return to the previous activity
        backButton.setOnClickListener(v -> {
            // Finish this activity to return to the previous one
            finish();
        });


        // Find the addButton by its ID
        View addButton = findViewById(R.id.addButton);

        // Set an OnClickListener to the addButton
        addButton.setOnClickListener(v -> {
            // Create an Intent to navigate to NewEntryActivity
            Intent intent = new Intent(LatestEntryActivity.this, NewEntryActivity.class);
            // Start the activity
            startActivity(intent);
        });
    }
}
