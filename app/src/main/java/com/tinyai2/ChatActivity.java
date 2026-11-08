package com.tinyai2;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton fabSettings;
    private ScrollView scrollView;
    private LinearLayout typingIndicator;
    private TextView typingText;

    private final List<Message> messages = new ArrayList<>();
    private boolean isTyping = false;
    private AIChatService aiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        aiService = new AIChatService();
        aiService.initialize(this);

        initializeViews();
        setupMessageAdapter();
        setupClickListeners();
        loadConversationHistory();
        scrollToBottom();
    }

    private void initializeViews() {
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        fabSettings = findViewById(R.id.fab_settings);
        scrollView = findViewById(R.id.scroll_view);
        typingIndicator = findViewById(R.id.typing_indicator);
        typingText = findViewById(R.id.typing_text);
    }

    private void setupMessageAdapter() {
        messageAdapter = new MessageAdapter(messages, this);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        fabSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.fab_paste).setOnClickListener(v -> pasteFromClipboard());
        findViewById(R.id.back_button).setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, R.string.error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        Message userMessage = new Message(message, Message.TYPE_USER);
        messages.add(userMessage);
        messageAdapter.notifyItemInserted(messages.size() - 1);

        messageInput.setText("");
        hideKeyboard();
        scrollToBottom();
        showTypingIndicator();

        CompletableFuture<String> responseFuture = aiService.getAIResponse(message);
        responseFuture.whenComplete((aiResponse, error) -> runOnUiThread(() -> {
            hideTypingIndicator();

            String content = error == null
                    ? aiResponse
                    : "请求失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage());

            Message aiMessage = new Message(content, Message.TYPE_AI);
            messages.add(aiMessage);
            messageAdapter.notifyItemInserted(messages.size() - 1);
            scrollToBottom();
            saveToHistory(message, content);
        }));
    }

    private void showTypingIndicator() {
        isTyping = true;
        typingIndicator.setVisibility(LinearLayout.VISIBLE);
        typingText.setText(R.string.typing_indicator);
    }

    private void hideTypingIndicator() {
        isTyping = false;
        typingIndicator.setVisibility(LinearLayout.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0);
        }
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()
                && clipboard.getPrimaryClip() != null
                && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence pasteData = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (pasteData != null) {
                messageInput.setText(pasteData);
                messageInput.setSelection(messageInput.getText().length());
                showSnackbar(getString(R.string.paste));
            }
        }
    }

    private void saveToHistory(String userMessage, String aiResponse) {
        ConversationHistory.save(this, userMessage, aiResponse);
    }

    private void loadConversationHistory() {
        long conversationId = getIntent().getLongExtra("conversation_id", -1L);
        Conversation conversation = conversationId >= 0
                ? ConversationHistory.findByTimestamp(this, conversationId)
                : null;

        if (conversation == null) {
            List<Conversation> conversations = ConversationHistory.loadAll(this);
            if (!conversations.isEmpty()) {
                conversation = conversations.get(0);
            }
        }

        if (conversation != null) {
            messages.clear();
            messages.addAll(conversation.getMessages());
            messageAdapter.notifyDataSetChanged();
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.chat_container), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiService != null) {
            aiService.shutdown();
        }
    }
}
