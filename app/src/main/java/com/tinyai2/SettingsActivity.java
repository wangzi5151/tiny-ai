package com.tinyai2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup themeGroup;
    private RadioGroup languageGroup;
    private RadioGroup aiModelGroup;
    private SeekBar temperatureSeekBar;
    private TextView temperatureValueText;
    private SeekBar maxTokensSeekBar;
    private TextView maxTokensValueText;

    private CardView themeCard;
    private CardView languageCard;
    private CardView aiCard;
    private CardView aboutCard;

    private EditText apiBaseUrlInput;
    private EditText apiModelInput;
    private EditText apiKeyInput;
    private TextView apiModeHint;
    private Button saveApiButton;

    private SharedPreferences preferences;
    private AIChatService aiService;
    private boolean isLoadingSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        aiService = new AIChatService();
        aiService.initialize(this);

        preferences = getSharedPreferences(AIChatService.PREFS_NAME, Context.MODE_PRIVATE);

        initializeViews();
        setupListeners();
        loadSettings();
    }

    private void initializeViews() {
        themeGroup = findViewById(R.id.theme_group);
        themeCard = findViewById(R.id.theme_card);

        languageGroup = findViewById(R.id.language_group);
        languageCard = findViewById(R.id.language_card);

        aiModelGroup = findViewById(R.id.ai_model_group);
        temperatureSeekBar = findViewById(R.id.temperature_seekbar);
        temperatureValueText = findViewById(R.id.temperature_value);
        maxTokensSeekBar = findViewById(R.id.max_tokens_seekbar);
        maxTokensValueText = findViewById(R.id.max_tokens_value);
        aiCard = findViewById(R.id.ai_card);

        apiBaseUrlInput = findViewById(R.id.api_base_url_input);
        apiModelInput = findViewById(R.id.api_model_input);
        apiKeyInput = findViewById(R.id.api_key_input);
        apiModeHint = findViewById(R.id.api_mode_hint);
        saveApiButton = findViewById(R.id.save_api_button);

        aboutCard = findViewById(R.id.about_card);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        setupCardClickAnimations();
    }

    private void setupListeners() {
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isLoadingSettings) {
                saveThemeSetting(checkedId);
            }
        });

        languageGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isLoadingSettings) {
                saveLanguageSetting(checkedId);
            }
        });

        aiModelGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isLoadingSettings) {
                saveAIModelSetting(checkedId);
            }
        });

        temperatureSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float temperature = 0.1f + (progress * 0.1f);
                temperatureValueText.setText(
                        String.format(Locale.getDefault(), "%.1f", temperature));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float temperature = 0.1f + (seekBar.getProgress() * 0.1f);
                aiService.setTemperature(temperature);
                preferences.edit().putFloat("temperature", temperature).apply();
                Toast.makeText(SettingsActivity.this,
                        getString(R.string.temperature_set, temperature),
                        Toast.LENGTH_SHORT).show();
            }
        });

        maxTokensSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                maxTokensValueText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int maxTokens = seekBar.getProgress();
                aiService.setMaxTokens(maxTokens);
                preferences.edit().putInt("max_tokens", maxTokens).apply();
                Toast.makeText(SettingsActivity.this,
                        getString(R.string.max_tokens_set, maxTokens),
                        Toast.LENGTH_SHORT).show();
            }
        });

        saveApiButton.setOnClickListener(v -> saveApiConfig());
    }

    private void setupCardClickAnimations() {
        View.OnClickListener cardClickListener = v -> v.animate()
                .scaleX(0.98f)
                .scaleY(0.98f)
                .setDuration(100)
                .withEndAction(() -> v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();

        themeCard.setOnClickListener(cardClickListener);
        languageCard.setOnClickListener(cardClickListener);
        aiCard.setOnClickListener(cardClickListener);
        aboutCard.setOnClickListener(cardClickListener);
    }

    private void loadSettings() {
        isLoadingSettings = true;
        try {
            int themeId = preferences.getInt("theme", R.id.theme_auto);
            themeGroup.check(themeId);

            int languageId = preferences.getInt("language", R.id.language_auto);
            languageGroup.check(languageId);

            int aiModelId = preferences.getInt("ai_model", R.id.ai_model_default);
            aiModelGroup.check(aiModelId);

            float temperature = preferences.getFloat("temperature", 0.7f);
            int temperatureProgress = Math.round((temperature - 0.1f) / 0.1f);
            temperatureSeekBar.setProgress(Math.max(0, Math.min(9, temperatureProgress)));
            temperatureValueText.setText(
                    String.format(Locale.getDefault(), "%.1f", temperature));

            int maxTokens = preferences.getInt("max_tokens", 150);
            maxTokensSeekBar.setProgress(Math.max(0, Math.min(500, maxTokens)));
            maxTokensValueText.setText(String.valueOf(maxTokens));

            apiBaseUrlInput.setText(aiService.getApiBaseUrl());
            apiModelInput.setText(aiService.getApiModel());
            apiKeyInput.setText(aiService.getApiKey());
            updateApiModeHint();
        } finally {
            isLoadingSettings = false;
        }
    }

    private void updateApiModeHint() {
        if (aiService.isRemoteConfigured()) {
            apiModeHint.setText(getString(R.string.api_remote_active, aiService.getApiBaseUrl()));
        } else {
            apiModeHint.setText(R.string.demo_mode_active);
        }
    }

    private void saveApiConfig() {
        String baseUrl = apiBaseUrlInput.getText().toString().trim();
        String model = apiModelInput.getText().toString().trim();
        String apiKey = apiKeyInput.getText().toString().trim();

        if (baseUrl.isEmpty()) {
            baseUrl = ChatApiClient.DEFAULT_BASE_URL;
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            Toast.makeText(this, R.string.api_invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }
        if (model.isEmpty()) {
            model = ChatApiClient.DEFAULT_MODEL;
        }

        aiService.setApiConfig(baseUrl, apiKey, model);
        updateApiModeHint();
        Toast.makeText(this, R.string.api_config_saved, Toast.LENGTH_SHORT).show();
    }

    private void saveThemeSetting(int checkedId) {
        preferences.edit().putInt("theme", checkedId).apply();

        if (checkedId == R.id.theme_light) {
            setTheme(R.style.Theme_TinyAI2_Light);
        } else if (checkedId == R.id.theme_dark) {
            setTheme(R.style.Theme_TinyAI2_Dark);
        } else {
            setTheme(R.style.Theme_TinyAI2);
        }

        Toast.makeText(this, R.string.theme_updated, Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void saveLanguageSetting(int checkedId) {
        preferences.edit().putInt("language", checkedId).apply();
        Toast.makeText(this, R.string.language_updated, Toast.LENGTH_SHORT).show();
    }

    private void saveAIModelSetting(int checkedId) {
        String model = "default";
        if (checkedId == R.id.ai_model_advanced) {
            model = "advanced";
        } else if (checkedId == R.id.ai_model_fast) {
            model = "fast";
        } else if (checkedId == R.id.ai_model_creative) {
            model = "creative";
        }

        aiService.setAIModel(model);
        preferences.edit().putInt("ai_model", checkedId).apply();
        Toast.makeText(this, R.string.ai_model_updated, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiService != null) {
            aiService.shutdown();
        }
    }
}
