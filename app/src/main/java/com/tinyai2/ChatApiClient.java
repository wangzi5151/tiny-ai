package com.tinyai2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal OpenAI-compatible chat completions client built on HttpURLConnection.
 * No third-party networking library is required.
 */
public class ChatApiClient {

    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public ChatApiClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl == null || baseUrl.trim().isEmpty() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
    }

    public String complete(String userMessage, float temperature, int maxTokens) throws IOException {
        if (apiKey.isEmpty()) {
            throw new IOException("API key is empty");
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(buildEndpoint(baseUrl));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            JSONObject payload = buildPayload(userMessage, temperature, maxTokens, model);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            InputStream responseStream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = readAll(responseStream);

            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP " + statusCode + (body.isEmpty() ? "" : ": " + body));
            }

            return parseContent(body);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static String buildEndpoint(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.isEmpty()) {
            normalized = DEFAULT_BASE_URL;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        return normalized + "/chat/completions";
    }

    static JSONObject buildPayload(String userMessage, float temperature, int maxTokens, String model)
            throws IOException {
        try {
            JSONObject payload = new JSONObject();
            payload.put("model", model);
            payload.put("temperature", temperature);
            payload.put("max_tokens", maxTokens);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", userMessage);
            messages.put(message);
            payload.put("messages", messages);

            return payload;
        } catch (JSONException e) {
            throw new IOException("Failed to build request body", e);
        }
    }

    static String parseContent(String body) throws IOException {
        try {
            JSONObject root = new JSONObject(body);
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                throw new IOException("Response does not contain choices");
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.optJSONObject("message");
            if (message == null) {
                throw new IOException("Response does not contain a message");
            }
            String content = message.optString("content", "");
            if (content.isEmpty()) {
                throw new IOException("Response message is empty");
            }
            return content;
        } catch (JSONException e) {
            throw new IOException("Failed to parse response", e);
        }
    }

    static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }
}
