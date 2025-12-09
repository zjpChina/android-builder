# URI Loader Android App

一个简单的 Android 应用，通过配置文件加载指定的 URI。

## 🚀 功能特性

- 📱 **WebView 加载器** - 使用 WebView 加载配置的网址
- ⚙️ **灵活配置** - 支持通过配置文件指定 URI
- 🔄 **热更新** - 支持运行时更换配置文件
- 🎨 **现代 UI** - 渐变色主题和加载进度条
- 🔐 **安全** - 支持 HTTPS 和混合内容

## 📁 项目结构

```
apk-builder/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── config.json          # 默认配置文件
│   │   ├── java/.../MainActivity.kt  # 主界面
│   │   ├── res/
│   │   │   ├── layout/              # 布局文件
│   │   │   ├── values/              # 主题和字符串
│   │   │   └── drawable/            # 图标资源
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/
│   └── workflows/
│       └── build-apk.yml            # GitHub Actions 工作流
└── README.md
```

## ⚙️ 配置文件

### 默认配置 (内置)

配置文件位于 `app/src/main/assets/config.json`：

```json
{
    "uri": "https://www.example.com",
    "description": "这是默认配置文件，请修改uri字段为你想要加载的网址"
}
```

### 运行时配置 (可选)

应用支持多种配置方式，优先级从高到低：

1. **手动配置**：长按屏幕 5 秒，在弹出的对话框中输入 URI 并选择屏幕方向（横屏/竖屏）。此配置会持久化保存。
2. **外部文件配置**：`/sdcard/Android/data/com.example.uriloader/files/config.json`
3. **默认配置**：内置的 `assets/config.json`

你可以在安装应用后，将自定义的 `config.json` 放到上述路径来覆盖默认配置。

## 🔨 本地构建

### 前提条件

- JDK 17+
- Android SDK
- Gradle 8.4+

### 构建步骤

```bash
# 克隆项目
git clone <your-repo-url>
cd apk-builder

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

APK 输出路径：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 🤖 GitHub Actions 自动构建

### 自动触发

- 推送到 `main` 或 `master` 分支时自动构建
- 创建以 `v` 开头的 tag 时自动发布 Release

### 手动触发（支持自定义 URI）

1. 进入 GitHub 仓库的 **Actions** 页面
2. 选择 **Build Android APK** 工作流
3. 点击 **Run workflow**
4. 可选：输入自定义的 URI
5. 点击 **Run workflow** 开始构建

### 下载 APK

构建完成后：
1. 进入对应的 workflow run
2. 在 **Artifacts** 区域下载 `app-debug` 或 `app-release`

## 🔐 签名配置（可选）

如需构建签名的 Release APK，请在 GitHub 仓库设置以下 Secrets：

| Secret 名称 | 说明 |
|------------|------|
| `KEYSTORE_BASE64` | keystore 文件的 Base64 编码 |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

生成 Base64 编码的 keystore：

```bash
base64 -i your-keystore.jks | tr -d '\n'
```

## 📱 安装使用

1. 下载 APK 文件
2. 在 Android 设备上安装（需要允许安装未知来源应用）
3. 打开应用，会自动加载配置的 URI
4. 如需更改 URI，修改配置文件后重新安装或使用运行时配置

## 🛠️ 自定义

### 修改默认 URI

编辑 `app/src/main/assets/config.json`：

```json
{
    "uri": "https://your-website.com"
}
```

### 修改应用名称

编辑 `app/src/main/res/values/strings.xml`：

```xml
<string name="app_name">Your App Name</string>
```

### 修改包名

在 `app/build.gradle.kts` 中修改：

```kotlin
applicationId = "com.yourcompany.yourapp"
```

## 📄 License

MIT License
