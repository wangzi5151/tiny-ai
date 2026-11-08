package com.tinyai2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

public class ChatApiClientTest {

    @Test
    public void buildEndpointAddsChatCompletionsPath() {
        assertEquals(
                "https://api.openai.com/v1/chat/completions",
                ChatApiClient.buildEndpoint("https://api.openai.com/v1"));
        assertEquals(
                "https://api.openai.com/v1/chat/completions",
                ChatApiClient.buildEndpoint("https://api.openai.com/v1/"));
    }

    @Test
    public void buildEndpointKeepsFullEndpoint() {
        assertEquals(
                "https://example.com/v1/chat/completions",
                ChatApiClient.buildEndpoint("https://example.com/v1/chat/completions"));
    }

    @Test
    public void buildEndpointUsesDefaultForBlankValue() {
        assertEquals(
                ChatApiClient.DEFAULT_BASE_URL + "/chat/completions",
                ChatApiClient.buildEndpoint("   "));
    }

    @Test
    public void buildPayloadContainsModelAndUserMessage() throws Exception {
        JSONObject payload = ChatApiClient.buildPayload("hello", 0.5f, 128, "test-model");

        assertEquals("test-model", payload.getString("model"));
        assertEquals(0.5, payload.getDouble("temperature"), 0.0001);
        assertEquals(128, payload.getInt("max_tokens"));

        JSONArray messages = payload.getJSONArray("messages");
        assertEquals(1, messages.length());
        assertEquals("user", messages.getJSONObject(0).getString("role"));
        assertEquals("hello", messages.getJSONObject(0).getString("content"));
    }

    @Test
    public void parseContentExtractsAssistantMessage() throws IOException {
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"content\":\"Hi there\"}}]}";

        assertEquals("Hi there", ChatApiClient.parseContent(body));
    }

    @Test(expected = IOException.class)
    public void parseContentRejectsResponseWithoutChoices() throws IOException {
        ChatApiClient.parseContent("{\"error\":\"bad request\"}");
    }

    @Test
    public void defaultConstructorFallsBackToSafeValues() {
        ChatApiClient client = new ChatApiClient("", "", "");
        assertTrue(client != null);
        assertEquals("https://api.openai.com/v1/chat/completions",
                ChatApiClient.buildEndpoint(ChatApiClient.DEFAULT_BASE_URL));
    }
}
