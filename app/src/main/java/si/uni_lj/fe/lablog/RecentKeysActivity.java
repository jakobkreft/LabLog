package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecentKeysActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recent_keys);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find the back button by its ID
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());

        // Find the newKey TextView by its ID
        View newKeyTextView = findViewById(R.id.newKey);

        // Set an OnClickListener to the newKey TextView
        newKeyTextView.setOnClickListener(v -> {
            // Create an Intent to navigate to NewKeyActivity
            Intent intent = new Intent(RecentKeysActivity.this, NewKeyActivity.class);
            // Start the activity
            startActivity(intent);
        });
    }
}