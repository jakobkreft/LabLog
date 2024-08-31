package si.uni_lj.fe.lablog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.flexbox.FlexboxLayout;

import java.util.List;

import si.uni_lj.fe.lablog.data.AppDatabase;
import si.uni_lj.fe.lablog.data.Key;
import si.uni_lj.fe.lablog.data.KeyDao;

public class RecentKeysActivity extends AppCompatActivity {

    private FlexboxLayout flexboxLayout;
    private TextView newKeyTextView;
    private LayoutInflater inflater;

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

        // Find the FlexboxLayout by its ID
        flexboxLayout = findViewById(R.id.flexboxLayout);

        // Inflate the newKey TextView from the layout file
        inflater = LayoutInflater.from(this);
        newKeyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, flexboxLayout, false);
        newKeyTextView.setText(R.string.new_key); // Set the text for newKey button

        // Set an OnClickListener to the newKey TextView
        newKeyTextView.setOnClickListener(v -> {
            // Create an Intent to navigate to NewKeyActivity
            Intent intent = new Intent(RecentKeysActivity.this, NewKeyActivity.class);
            // Start the activity
            startActivity(intent);
        });

        // Find the back button by its ID
        View backButton = findViewById(R.id.backButton);
        backButton.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Fetch the keys from the database and update the UI
        new Thread(() -> {
            // Get the database and DAO
            AppDatabase db = MyApp.getDatabase();
            KeyDao keyDao = db.keyDao();

            // Retrieve all keys from the database
            List<Key> keyList = keyDao.getAllKeys();

            // Update the UI on the main thread
            runOnUiThread(() -> {
                // Clear any existing views in the FlexboxLayout
                flexboxLayout.removeAllViews();

                // Dynamically create and add TextViews for each key
                for (Key key : keyList) {
                    TextView keyTextView = (TextView) inflater.inflate(R.layout.key_text_view_layout, flexboxLayout, false);
                    keyTextView.setText(key.name.trim()); // Set the key name

                    // Add the TextView to the FlexboxLayout
                    flexboxLayout.addView(keyTextView);

                    // Optionally, set an OnClickListener if you want to add functionality
                    keyTextView.setOnClickListener(v -> Toast.makeText(RecentKeysActivity.this,
                            "Clicked on " + key.name, Toast.LENGTH_SHORT).show());
                }

                // Add the newKey TextView as the last element
                flexboxLayout.addView(newKeyTextView);
            });
        }).start();
    }
}
