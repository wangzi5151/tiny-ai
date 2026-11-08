package com.tinyai2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateTitle;
    private TextView emptyStateSubtitle;
    private ImageButton backButton;
    private ImageButton fabClear;
    private ImageButton fabExport;
    private ImageButton fabImport;

    private String pendingExportFormat = "json";
    private ActivityResultLauncher<Intent> createDocumentLauncher;
    private ActivityResultLauncher<Intent> openDocumentLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        registerFileLaunchers();
        initializeViews();
        setupHistoryAdapter();
        setupClickListeners();
        loadHistory();
    }

    private void registerFileLaunchers() {
        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        exportToUri(result.getData().getData());
                    }
                });

        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        importFromUri(result.getData().getData());
                    }
                });
    }

    private void initializeViews() {
        historyRecyclerView = findViewById(R.id.history_recycler_view);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        emptyStateTitle = findViewById(R.id.empty_state_title);
        emptyStateSubtitle = findViewById(R.id.empty_state_subtitle);
        backButton = findViewById(R.id.back_button);
        fabClear = findViewById(R.id.fab_clear);
        fabExport = findViewById(R.id.fab_export);
        fabImport = findViewById(R.id.fab_import);
    }

    private void setupHistoryAdapter() {
        historyAdapter = new HistoryAdapter(this, conversation -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversation_id", conversation.getTimestamp());
            startActivity(intent);
        });

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        fabClear.setOnClickListener(v -> showClearConfirmationDialog());
        fabImport.setOnClickListener(v -> launchImportPicker());
        fabExport.setOnClickListener(v -> showExportOptions());
    }

    private void loadHistory() {
        List<Conversation> conversations = ConversationHistory.loadAll(this);

        if (conversations.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
            historyAdapter.setConversations(conversations);
        }
    }

    public void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        historyRecyclerView.setVisibility(View.GONE);

        emptyStateTitle.setText(R.string.no_messages);
        emptyStateSubtitle.setText(R.string.empty_state_subtitle);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        historyRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showClearConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_clear_title)
                .setMessage(R.string.confirm_clear)
                .setPositiveButton(R.string.yes, (dialog, which) -> clearAllHistory())
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void clearAllHistory() {
        ConversationHistory.clearAll(this);
        historyAdapter.clearConversations();
        showEmptyState();
        Toast.makeText(this, R.string.clear_history, Toast.LENGTH_SHORT).show();
    }

    private void showExportOptions() {
        String[] formats = {
                getString(R.string.export_format_json),
                getString(R.string.export_format_txt)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.export_format)
                .setItems(formats, (dialog, which) -> {
                    pendingExportFormat = which == 0 ? "json" : "txt";
                    launchExportPicker();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void launchExportPicker() {
        boolean json = "json".equals(pendingExportFormat);
        String extension = json ? "json" : "txt";
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                .format(new Date());

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(json ? "application/json" : "text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "tinyai-history-" + timestamp + "." + extension);
        createDocumentLauncher.launch(intent);
    }

    private void exportToUri(Uri uri) {
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("Unable to open output stream");
            }
            if ("json".equals(pendingExportFormat)) {
                ConversationHistory.writeJson(this, output);
            } else {
                ConversationHistory.writePlainText(this, output);
            }
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        openDocumentLauncher.launch(intent);
    }

    private void importFromUri(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Unable to open input stream");
            }
            int imported = ConversationHistory.importHistory(this, input);
            if (imported <= 0) {
                Toast.makeText(this, R.string.import_empty, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.imported_count, imported),
                        Toast.LENGTH_SHORT).show();
                loadHistory();
            }
        } catch (IOException e) {
            Toast.makeText(this, R.string.import_invalid, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }
}
