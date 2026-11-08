package com.tinyai2;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIChatService {

    public static final String PREFS_NAME = "tiny_ai_settings";
    public static final String KEY_API_BASE_URL = "api_base_url";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_API_MODEL = "api_model";

    private Context context;
    private SharedPreferences preferences;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public CompletableFuture<String> getAIResponse(String userMessage) {
        CompletableFuture<String> future = new CompletableFuture<>();

        executorService.execute(() -> {
            try {
                String response;
                if (isRemoteConfigured()) {
                    try {
                        ChatApiClient client = new ChatApiClient(
                                getApiBaseUrl(), getApiKey(), getApiModel());
                        response = client.complete(
                                userMessage, getTemperature(), getMaxTokens());
                    } catch (Exception remoteError) {
                        response = generateLocalResponse(userMessage)
                                + "\n\n（远程接口调用失败：" + remoteError.getMessage()
                                + "，已回退到本地演示模式。）";
                    }
                } else {
                    response = generateLocalResponse(userMessage);
                }
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public boolean isRemoteConfigured() {
        return getApiKey() != null && !getApiKey().trim().isEmpty();
    }

    public String getApiBaseUrl() {
        String value = preferences != null
                ? preferences.getString(KEY_API_BASE_URL, "")
                : "";
        if (value == null || value.trim().isEmpty()) {
            value = BuildConfig.CHAT_API_BASE_URL;
        }
        return value == null || value.trim().isEmpty()
                ? ChatApiClient.DEFAULT_BASE_URL : value.trim();
    }

    public String getApiKey() {
        String value = preferences != null
                ? preferences.getString(KEY_API_KEY, "")
                : "";
        if (value == null || value.trim().isEmpty()) {
            value = BuildConfig.CHAT_API_KEY;
        }
        return value == null ? "" : value.trim();
    }

    public String getApiModel() {
        String value = preferences != null
                ? preferences.getString(KEY_API_MODEL, "")
                : "";
        if (value == null || value.trim().isEmpty()) {
            value = BuildConfig.CHAT_API_MODEL;
        }
        return value == null || value.trim().isEmpty()
                ? ChatApiClient.DEFAULT_MODEL : value.trim();
    }

    public void setApiConfig(String baseUrl, String apiKey, String model) {
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putString(KEY_API_BASE_URL, baseUrl == null ? "" : baseUrl.trim())
                .putString(KEY_API_KEY, apiKey == null ? "" : apiKey.trim())
                .putString(KEY_API_MODEL, model == null ? "" : model.trim())
                .apply();
    }

    private String generateLocalResponse(String userMessage) {
        String message = userMessage.toLowerCase(Locale.ROOT).trim();

        if (message.contains("hello") || message.contains("hi")
                || message.contains("hey") || message.contains("你好")) {
            return "你好！我是 Tiny AI 2.0，你的本地演示助手。今天有什么可以帮你？";
        }

        if (message.contains("help") || message.contains("what can you do")
                || message.contains("帮助")) {
            return "当前运行在本地演示模式，我可以：\n"
                    + "• 回应问候并进行简单对话\n"
                    + "• 回答日期与时间问题\n"
                    + "• 演示对话历史保存、导出和导入\n"
                    + "在设置中填写 API Key 后即可连接 OpenAI 兼容的 Chat API。";
        }

        if (message.contains("who are you") || message.contains("what are you")
                || message.contains("你是谁")) {
            return "我是 Tiny AI 2.0。未配置远程 API 时，我在本地生成演示回复；"
                    + "配置后会把消息发送到你指定的 OpenAI 兼容接口。";
        }

        if (message.contains("time") || message.contains("date")
                || message.contains("时间") || message.contains("日期")) {
            java.text.SimpleDateFormat dateFormat =
                    new java.text.SimpleDateFormat("yyyy-MM-dd EEEE", java.util.Locale.getDefault());
            java.text.SimpleDateFormat timeFormat =
                    new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            java.util.Date now = new java.util.Date();
            return "今天是 " + dateFormat.format(now) + "，当前时间 " + timeFormat.format(now) + "。";
        }

        if (message.contains("calculate") || message.contains("计算")
                || (message.contains("what is")
                && (message.contains("+") || message.contains("-")
                || message.contains("*") || message.contains("/")))) {
            try {
                return calculateExpression(message);
            } catch (Exception e) {
                return "我没能理解这个算式，请换一种写法。";
            }
        }

        if (message.contains("write") || message.contains("story")
                || message.contains("poem") || message.contains("写")) {
            return generateCreativeContent(message);
        }

        return "我理解你想聊的是：“" + userMessage + "”。\n\n"
                + "这是本地演示回复。若要获得真实模型的回答，请在设置中配置 "
                + "API 地址、模型名称和 API Key。";
    }

    private String calculateExpression(String message) {
        String[] parts = message.split(" ");
        double result = 0;

        for (String part : parts) {
            if (part.contains("+")) {
                String[] nums = part.split("\\+");
                result += Double.parseDouble(nums[0]) + Double.parseDouble(nums[1]);
            } else if (part.contains("-")) {
                String[] nums = part.split("-");
                result += Double.parseDouble(nums[0]) - Double.parseDouble(nums[1]);
            } else if (part.contains("*")) {
                String[] nums = part.split("\\*");
                result += Double.parseDouble(nums[0]) * Double.parseDouble(nums[1]);
            } else if (part.contains("/")) {
                String[] nums = part.split("/");
                result += Double.parseDouble(nums[0]) / Double.parseDouble(nums[1]);
            }
        }

        return "计算结果是：" + result;
    }

    private String generateCreativeContent(String message) {
        if (message.contains("story") || message.contains("故事")) {
            return "从前，有一个叫 Tiny 的小助手，他住在一部旧手机里，"
                    + "每天帮人们记录想法、整理对话，也提醒他们别忘了休息。"
                    + "虽然不能联网时只能讲本地笑话，但他从不缺席。";
        } else if (message.contains("poem") || message.contains("诗")) {
            return "数据成行夜未央，\n"
                    + "微光一点照书窗。\n"
                    + "若问此心何处去，\n"
                    + "万千问答化寻常。";
        } else {
            return "我可以帮你写一段本地演示文本。告诉我主题、语气和长度，"
                    + "或者配置远程 API 获得更完整的创作能力。";
        }
    }

    public void setAIModel(String model) {
        if (preferences != null) {
            preferences.edit().putString("ai_model", model).apply();
        }
    }

    public void setTemperature(float temperature) {
        if (preferences != null) {
            preferences.edit().putFloat("temperature", temperature).apply();
        }
    }

    public void setMaxTokens(int maxTokens) {
        if (preferences != null) {
            preferences.edit().putInt("max_tokens", maxTokens).apply();
        }
    }

    public String getAIModel() {
        return preferences != null ? preferences.getString("ai_model", "default") : "default";
    }

    public float getTemperature() {
        return preferences != null ? preferences.getFloat("temperature", 0.7f) : 0.7f;
    }

    public int getMaxTokens() {
        return preferences != null ? preferences.getInt("max_tokens", 150) : 150;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
