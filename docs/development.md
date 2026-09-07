# 开发文档

[返回 README](../README.md) · [CI 与自动发版](ci-release.md)

## 构建环境

| 工具 | 要求 |
| --- | --- |
| JDK | 17 或 21；构建验证使用 21 |
| Android SDK | Platform 36，以及对应构建工具 |
| Gradle | 使用仓库的 Wrapper，版本 8.9 |
| ADB | 安装 APK 与运行设备测试时使用 |

Android 客户端最低支持 API 26。首次构建需要下载依赖。

Android Studio 可配置 SDK，也可在项目根目录创建 `local.properties`：

```properties
sdk.dir=/path/to/Android/sdk
```

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Windows 使用 `gradlew.bat`。APK 分别输出到 `app/build/outputs/apk/debug/` 和 `app/build/outputs/apk/release/`。

## Release 签名

没有本地签名配置时，release 构建输出未签名 APK。项目发布私钥不随源码分发；自行发布时须使用自己的证书。

将 keystore 放入 `release-signing/`，创建 `release-signing/keystore.properties`：

```properties
storeFile=release-signing/your-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

`storeFile` 相对于项目根目录。Gradle 会读取此文件并为 release 构建签名。`local.properties` 与整个 `release-signing/` 目录均被 Git 忽略。

请离线备份私钥，不要提交真实密码或密钥。后续发布应使用同一证书并递增 `versionCode`；debug 证书或其他发布证书签署的 APK 不能直接覆盖安装。

## 设备测试

**使用专用测试设备或模拟器。当前设备测试会重建应用数据库，不要在存有个人课表的实例上运行。**

运行 Android 设备测试：

```sh
./gradlew :app:connectedDebugAndroidTest
```

运行指定测试类：

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.letr.sleepdown.widget.ScheduleWidgetTest
```

报告位于 `app/build/reports/androidTests/connected/debug/`。

修改交互时，除测试外还应在设备上检查入口、保存、取消和重新进入页面后的结果。小组件还需检查桌面上的真实显示与更新。性能测量应使用 release 构建，不把 debug 时序当作发布包性能结论。

### iOS 构建与签名

使用 macOS、Xcode 26 和 JDK 21 打开 `iosApp/iosApp.xcodeproj`。主 App 最低支持 iOS 15，Widget 扩展最低支持 iOS 17。Xcode 构建阶段会通过 Gradle 编译对应架构的共享 framework。

构建未签名的真机归档：

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' \
  -archivePath iosApp/build/iosApp.xcarchive \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO archive
```

将归档中 `Products/Applications/iosApp.app` 放入 `Payload/` 后压缩为 IPA。安装前须签名主 App 和内嵌 Widget 扩展，并为二者配置同一开发团队有权使用的 App Group。工程中的默认组名为 `group.com.letr.sleepdown`；自定义组名时，须同步修改两个 entitlements 和 `WidgetSharedData.appGroupID`。证书、描述文件和私钥不随源码分发。

`iosApp` scheme 包含单元测试，`iosAppUI` 包含界面测试。UI 测试会创建测试课表、修改偏好并添加桌面小组件，应使用专用测试模拟器。`testExistingWidgetBindingAndTap` 要求桌面已有中尺寸 SleepDown 组件，图库用例包含添加流程。AppEntity 的独立课表配置测试需要有 Team ID 的签名环境。

## 目录结构

| 路径 | 内容 |
| --- | --- |
| `app/` | Android 客户端：Compose 界面、Room 数据库、本地提醒和 RemoteViews 小组件 |
| `app/src/androidTest/` | Android 设备测试 |
| `shared/` | Kotlin Multiplatform 课程模型、命令、计算与文件交换逻辑 |
| `shared/src/commonTest/` | 共享模块测试 |
| `iosApp/` | SwiftUI 客户端、Widget 扩展及 XCTest 测试 |
| `gradle/` | 依赖版本与 Wrapper |

## 提交修改

保持修改范围明确，运行相关测试。界面变更请附截图或录屏；新功能和大范围调整建议先通过 Issue 讨论。

不要提交个人课表、设备日志、本地调试产物、签名密钥或参考资料。引入第三方代码及资源时，注明来源并保留许可证。文件兼容不代表与对应第三方应用存在隶属或合作关系，也不授予其商标或资源的使用权。
