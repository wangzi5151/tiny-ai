# Tiny AI 2.0（W.ai）

一个轻量级 Android 对话应用，默认使用**本地演示回复**，也可以配置任意
**OpenAI 兼容的 Chat Completions HTTP 接口**。

> 重要：仓库中的 APK 是旧版本构建产物。当前源码默认不会调用云端 API；
> 只有你自行填写 API Key 后，消息才会发送到你配置的远程接口。

## 当前能力

- Material 3 聊天、历史记录和设置界面
- 本地演示模式：无需网络和 API Key 即可体验完整交互
- 可选远程模式：OpenAI 兼容接口（`POST {baseUrl}/chat/completions`）
- 对话历史本地保存（SharedPreferences），最多保留 100 条
- 历史记录 JSON / TXT 导出，JSON 导入（自动合并、去重）
- 单条对话删除、分享和文本导出
- 主题切换、温度与最大 token 设置

## 配置远程 API

推荐把密钥放在 **`local.properties`**（已被 `.gitignore` 忽略），不要提交到仓库：

```properties
sdk.dir=/path/to/android-sdk
CHAT_API_BASE_URL=https://api.openai.com/v1
CHAT_API_MODEL=gpt-4o-mini
CHAT_API_KEY=sk-your-key
```

也可以在应用内“设置 → 远程 Chat API”中填写接口地址、模型和 API Key。
API Key 留空时自动回退到本地演示模式；远程请求失败时同样会回退并提示。

可参考 `local.properties.example`。环境变量 `CHAT_API_BASE_URL`、
`CHAT_API_MODEL`、`CHAT_API_KEY` 也可作为构建时默认值。

## 构建

要求：

- JDK 17
- Android SDK，`compileSdk 34`、`minSdk 24`
- Gradle 8.2.1（仓库自带 wrapper）

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Debug APK 输出在 `app/build/outputs/apk/debug/`。

## 目录结构

```text
tinyAI/
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/tinyai2/
│       │   └── res/
│       └── test/java/com/tinyai2/
├── gradle/wrapper/
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 验证与 CI

`.github/workflows/android.yml` 在 push / pull request 时执行：

```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```

## 已知限制

- 没有配置 API Key 时，回复来自本地规则，不代表真实模型能力。
- 语言设置目前只保存偏好，界面文案仍以中文为主。
- 历史记录保存在应用私有 SharedPreferences 中，卸载应用会丢失。
- 仓库中的旧 APK 不会随源码更新，请以自行构建的 APK 为准。

## 开源协议

本项目采用 [MIT License](LICENSE)。

## 联系方式

- 作者：wangzi5151
- GitHub：[@wangzi5151](https://github.com/wangzi5151)
