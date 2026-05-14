# CoolMusic

中国地质大学 软件工程 智能终端 课设 — 音乐播放器

## 技术栈

- Android (Java)
- Gradle (Kotlin DSL)
- SQLite

## ⚠️ 注意事项

### 修改 DBUtil.java 务必小心

`DBUtil.java` 是整个应用的核心，负责数据库的创建、表结构定义和初始数据插入。

修改该文件时请特别注意：

1. **musicCount 变量** — 控制初始加载的音频文件数量，必须与 `res/raw/` 目录下的 `music_01` ~ `music_NN` 文件数量一致
2. **数据库版本号 (version)** — 修改表结构后必须递增版本号，否则已有用户不会触发 `onUpgrade`
3. **onUpgrade** — 当前实现为 `DROP TABLE` + 重建，会**清除所有用户数据**。如需保留数据，必须写迁移逻辑
4. **初始数据插入** — `insertInitialData()` 中的所有插入操作依赖前序插入的 ID，修改顺序或数值可能导致外键约束失败
5. **单例模式** — `getInstance()` 依赖 `appContext` 在构造前赋值，不要在构造函数内做任何依赖 Context 的操作

### 音乐文件

`app/src/main/res/raw/` 下的 FLAC 文件仅保留 5 首用于演示。如需增加，同步修改 `DBUtil.java` 中的 `musicCount`。
