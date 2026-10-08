# 开发文档

[返回 README](../README.md)

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

## 版本与发布

Android 当前为 `0.1.0`，构建号 `6`。iOS 保留 `0.1.0-beta.4`，构建号 `5`。两个平台独立维护版本和构建号。

| 平台 | 完整版本 | 构建号 | 发布标签 |
| --- | --- | --- | --- |
| Android | `app/build.gradle.kts` 的 `versionName` | 同文件的 `versionCode` | `v<version>` |
| iOS | `iosApp/release-version.txt` | Xcode 工程中的 `CURRENT_PROJECT_VERSION` | `ios-v<version>` |

完整版本须为 SemVer，不含 `+` 构建元数据。构建号须为无前导零的正整数，Android 上限为 `2,100,000,000`。iOS 各处 `MARKETING_VERSION` 须等于完整版本的基础版本，例如 `0.1.0-beta.4` 对应 `0.1.0`。各处 `CURRENT_PROJECT_VERSION` 须相同，不要求与 Android 相同。

发布说明使用 `CHANGELOG.md` 中的二级标题，例如 `## Android 0.1.0` 或 `## iOS 0.1.0-beta.4`。同一平台和版本须有且只有一个非空章节。旧 beta 的无平台标题仍可使用；无平台正式版章节和另一平台章节不能用于当前平台发布。

本地校验不读取签名配置，也不创建标签或 Release：

```sh
python3 -m unittest discover -s scripts -p 'test_*.py' -v
python3 scripts/release.py validate
python3 scripts/release.py validate --platform android --tag v0.1.0
python3 scripts/release.py validate --platform ios --tag ios-v0.1.0-beta.4
```

未指定平台和标签时，脚本独立校验双端。`--platform` 支持 `all`、`android` 和 `ios`。只指定 `--tag` 时按标签前缀选择平台；显式平台须与标签一致。

`--notes-file <path>` 只支持单平台，将该平台说明写入文件。`--github-output <path>` 追加 `platform`、`version`、`build_number`、`tag` 和 `prerelease` 字段。双端模式保留 `platform=all`，其他字段添加 `android_` 或 `ios_` 前缀。

PR、main 和合并队列的 CI 覆盖双端。平台发布调用 CI 时只运行选定平台的构建与测试。Android 发布不等待 iOS，iOS 发布不运行 Android 设备测试或签名任务。门禁要求所选平台任务全部成功，只有未选平台的任务可以跳过。

Android 发布 APK 按处理器架构分为两种：

| ABI | 适用设备 | 发布文件名 |
| --- | --- | --- |
| `armeabi-v7a` | 支持 32 位 ARM 应用的设备 | `SleepDown-<version>-android-armeabi-v7a.apk` |
| `arm64-v8a` | 支持 64 位 ARM 应用的设备 | `SleepDown-<version>-android-arm64-v8a.apk` |

使用以下命令生成两个独立 APK，不额外生成通用包：

```sh
./gradlew :app:assembleRelease -PsplitApks=true
```

不传 `-PsplitApks=true` 时，构建仍生成通用 APK。调试构建和设备测试保持默认配置，兼容 CI 的 x86_64 模拟器。两个架构包使用相同的应用标识、版本和构建号；正式发布时均使用原发布证书签名。

CI 的 `android-arm-release-unsigned` artifact 包含两个未签名架构包和 `SHA256SUMS.txt`，文件名增加 `-unsigned` 后缀。未签名包须自行签名后安装。iOS 测试包的打包方式保持不变。

发布标签须指向 main 的祖先提交，并与该平台完整版本一致。Android Release 上传两个架构 APK 和 `SHA256SUMS.txt`，共 3 个附件；iOS Release 仍为 IPA 和校验和，共 2 个附件。每个 Android APK 须通过发布证书校验。流程拒绝覆盖同标签已有的 Release 或草稿，先创建草稿，再上传并核对全部附件，最后公开发布。

含 SemVer 预发布标识的版本始终标记为 prerelease。只有 Android 正式版更新 GitHub 的 latest；iOS 发布保留现有 latest。证书及发布密钥的配置由发布环境管理，本地校验无需这些密钥。

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
