# Changelog

## 2.0.0 - 2026-11-08

### Added

- 可构建的 Gradle Android 工程（JDK 17 / compileSdk 34 / minSdk 24）
- OpenAI 兼容 Chat Completions HTTP 客户端（`ChatApiClient`）
- `local.properties` / BuildConfig / 应用内设置三级 API 配置
- 历史记录 JSON / TXT 导出与 JSON 导入（合并、去重）
- 单条对话删除与分享
- JUnit 单元测试与 Android CI

### Changed

- 默认回复明确标注为“本地演示模式”，不再宣称固定云端 API
- README、主题、字符串与启动图标整理

### Fixed

- SettingsActivity 非法字段、未声明变量、匿名监听器 `this` 与 ThemeCard 引用
- HistoryAdapter 调用私有 `showEmptyState`
- 缺失的字符串、动画、备份规则和 mipmap 资源
- ChatActivity / HistoryActivity 控件类型与布局问题
