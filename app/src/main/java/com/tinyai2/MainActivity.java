package com.tinyai2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private CardView chatCard, historyCard, settingsCard;
    private Button getStartedButton;
    private TextView welcomeTitle, welcomeSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Apply theme
        applyTheme();
    }

    private void initializeViews() {
        welcomeTitle = findViewById(R.id.welcome_title);
        welcomeSubtitle = findViewById(R.id.welcome_subtitle);
        chatCard = findViewById(R.id.chat_card);
        historyCard = findViewById(R.id.history_card);
        settingsCard = findViewById(R.id.settings_card);
        getStartedButton = findViewById(R.id.get_started_button);
    }

    private void setupClickListeners() {
        chatCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        historyCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        settingsCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void applyTheme() {
        // Apply theme colors based on user preference
        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        int secondaryColor = ContextCompat.getColor(this, R.color.secondary);
        int backgroundCard = ContextCompat.getColor(this, R.color.card_background);

        chatCard.setCardBackgroundColor(backgroundCard);
        historyCard.setCardBackgroundColor(backgroundCard);
        settingsCard.setCardBackgroundColor(backgroundCard);

        getStartedButton.setBackgroundColor(primaryColor);
        getStartedButton.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update UI based on theme changes
        applyTheme();
    }
}