package com.tinyai2;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConversationHistory {
    private static final String PREFS_NAME = "tiny_ai_history";
    private static final String KEY_CONVERSATIONS = "conversations";
    private static final int MAX_CONVERSATIONS = 100;

    public static void save(Context context, String userMessage, String aiResponse) {
        JSONArray jsonArray = loadArray(context);

        try {
            JSONObject newConversation = new JSONObject();
            newConversation.put("timestamp", System.currentTimeMillis());
            newConversation.put("title", generateTitle(userMessage));

            JSONArray messages = new JSONArray();

            JSONObject userMsg = new JSONObject();
            userMsg.put("type", Message.TYPE_USER);
            userMsg.put("content", userMessage);
            userMsg.put("timestamp", System.currentTimeMillis());
            messages.put(userMsg);

            JSONObject aiMsg = new JSONObject();
            aiMsg.put("type", Message.TYPE_AI);
            aiMsg.put("content", aiResponse);
            aiMsg.put("timestamp", System.currentTimeMillis() + 1);
            messages.put(aiMsg);

            newConversation.put("messages", messages);

            jsonArray.put(0, newConversation);
            trim(jsonArray);
            persist(context, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static List<Conversation> loadAll(Context context) {
        List<Conversation> conversations = new ArrayList<>();
        JSONArray jsonArray = loadArray(context);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject conversationJson = jsonArray.optJSONObject(i);
            if (conversationJson == null) {
                continue;
            }
            try {
                conversations.add(parseConversation(conversationJson));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return conversations;
    }

    public static Conversation findByTimestamp(Context context, long timestamp) {
        for (Conversation conversation : loadAll(context)) {
            if (conversation.getTimestamp() == timestamp) {
                return conversation;
            }
        }
        return null;
    }

    public static boolean delete(Context context, long timestamp) {
        JSONArray jsonArray = loadArray(context);
        JSONArray remaining = new JSONArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.optJSONObject(i);
            if (item == null) {
                continue;
            }
            if (item.optLong("timestamp", -1L) == timestamp) {
                continue;
            }
            remaining.put(item);
        }

        if (remaining.length() == jsonArray.length()) {
            return false;
        }

        persist(context, remaining);
        return true;
    }

    public static void clearAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_CONVERSATIONS).apply();
    }

    public static String toJson(Context context) {
        return loadArray(context).toString();
    }

    public static String toPlainText(Context context) {
        StringBuilder builder = new StringBuilder();
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        List<Conversation> conversations = loadAll(context);
        builder.append("Tiny AI 2.0 对话导出\n");
        builder.append("共 ").append(conversations.size()).append(" 条对话\n\n");

        for (Conversation conversation : conversations) {
            builder.append("== ").append(conversation.getTitle())
                    .append(" (").append(dateFormat.format(new Date(conversation.getTimestamp())))
                    .append(") ==\n");
            for (Message message : conversation.getMessages()) {
                String sender = message.getType() == Message.TYPE_USER ? "你" : "AI";
                builder.append(sender).append(": ").append(message.getContent()).append('\n');
            }
            builder.append('\n');
        }

        return builder.toString();
    }

    public static void exportHistory(Context context, String filePath) throws IOException {
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            writeJson(context, output);
        }
    }

    public static void writeJson(Context context, OutputStream output) throws IOException {
        output.write(toJson(context).getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public static void writePlainText(Context context, OutputStream output) throws IOException {
        output.write(toPlainText(context).getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public static boolean importHistory(Context context, String filePath) throws IOException {
        try (FileInputStream input = new FileInputStream(filePath)) {
            return importHistory(context, input) > 0;
        }
    }

    /** Imports conversations and merges them with the existing history. */
    public static int importHistory(Context context, InputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }

        try {
            return importFromJson(context, builder.toString(), true);
        } catch (JSONException e) {
            throw new IOException("Invalid history JSON", e);
        }
    }

    static int importFromJson(Context context, String json, boolean merge) throws JSONException {
        JSONArray incoming = parseImportedArray(json);
        if (incoming.length() == 0) {
            return 0;
        }

        JSONArray result = new JSONArray();
        Set<Long> seenTimestamps = new HashSet<>();
        int imported = 0;

        for (int i = 0; i < incoming.length(); i++) {
            JSONObject item = incoming.optJSONObject(i);
            if (item == null) {
                continue;
            }
            // Validate before accepting the item.
            parseConversation(item);
            long timestamp = item.optLong("timestamp", -1L);
            if (timestamp > 0 && seenTimestamps.add(timestamp)) {
                result.put(item);
                imported++;
            }
        }

        if (merge) {
            JSONArray existing = loadArray(context);
            for (int i = 0; i < existing.length(); i++) {
                JSONObject item = existing.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                long timestamp = item.optLong("timestamp", -1L);
                if (timestamp > 0 && seenTimestamps.add(timestamp)) {
                    result.put(item);
                }
            }
        }

        trim(result);
        persist(context, result);
        return imported;
    }

    private static JSONArray parseImportedArray(String json) throws JSONException {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.isEmpty()) {
            throw new JSONException("Empty history file");
        }
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        JSONObject root = new JSONObject(trimmed);
        JSONArray array = root.optJSONArray(KEY_CONVERSATIONS);
        if (array == null) {
            array = root.optJSONArray("history");
        }
        if (array == null) {
            throw new JSONException("Missing conversations array");
        }
        return array;
    }

    private static JSONArray loadArray(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String conversationsJson = prefs.getString(KEY_CONVERSATIONS, "[]");
        try {
            return new JSONArray(conversationsJson);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static void persist(Context context, JSONArray jsonArray) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CONVERSATIONS, jsonArray.toString()).apply();
    }

    private static void trim(JSONArray jsonArray) {
        while (jsonArray.length() > MAX_CONVERSATIONS) {
            jsonArray.remove(jsonArray.length() - 1);
        }
    }

    private static Conversation parseConversation(JSONObject conversationJson) throws JSONException {
        long timestamp = conversationJson.optLong("timestamp", System.currentTimeMillis());
        String title = conversationJson.optString("title", "Conversation");

        JSONArray messagesJson = conversationJson.optJSONArray("messages");
        if (messagesJson == null) {
            throw new JSONException("Conversation is missing messages");
        }

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < messagesJson.length(); i++) {
            JSONObject messageJson = messagesJson.optJSONObject(i);
            if (messageJson == null) {
                continue;
            }
            int type = messageJson.optInt("type", Message.TYPE_AI);
            String content = messageJson.optString("content", "");
            long messageTimestamp = messageJson.optLong("timestamp", timestamp);

            Message message = new Message(content, type);
            message.setTimestamp(messageTimestamp);
            messages.add(message);
        }

        return new Conversation(timestamp, title, messages);
    }

    private static String generateTitle(String firstMessage) {
        String trimmed = firstMessage == null ? "" : firstMessage.trim();
        if (trimmed.length() > 30) {
            return trimmed.substring(0, 27) + "...";
        }
        return trimmed;
    }
}
