# CI 与自动发版

## 持续集成

`.github/workflows/ci.yml` 在 Pull Request、`main` 提交、合并队列及手动运行时执行，也由 Release 工作流复用。

| 检查 | 内容 |
| --- | --- |
| Version and release checks | Android/iOS 版本一致、当前版本 CHANGELOG、发布脚本单测、Gradle Wrapper 校验 |
| Android build and lint | Debug/Release 构建、Android 测试包编译、lint、共享模块单测 |
| Android tests (API 26 / 36) | 在独立临时模拟器上运行全部 instrumentation tests |
| iOS tests and release archive | `iosApp` scheme 单元测试、主 App 与 Widget 真机 Release 归档、未签名 IPA |
| **CI Gate** | 以上任务全部成功才通过；失败、取消或跳过都不能通过 |

CI 不使用发布密钥。Pull Request 无写权限，第三方 Actions 固定到提交 SHA。调试 APK、未签名 IPA 和测试报告保留 7 天。iOS 桌面小组件的交互测试依赖预置桌面与签名环境，不属于自动门禁；单元测试和主 App、Widget 编译属于门禁。

### 启用合并门禁

推送工作流并让 CI 至少运行一次后，在 GitHub **Settings → Rules → Rulesets** 为 `main` 建立规则：

1. 要求通过 Pull Request 合并。
2. 启用 **Require status checks to pass**，添加 **CI Gate**。
3. 要求分支与目标分支保持最新，或启用合并队列（工作流已支持 `merge_group`）。
4. 禁止强制推送和删除；按团队需要限制 bypass 权限。

工作流文件本身不能开启服务端分支保护。未完成这一步，失败的检查仍不会阻止管理员直接推送。另建议为 `v*` 标签配置 ruleset，限制创建者并禁止移动、删除已发布标签。

## 配置 Android 发布签名

在 GitHub **Settings → Environments** 创建 `release` 环境，允许来自 `v*` 标签的部署，并在环境中配置以下 Secrets：

| Secret | 内容 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 原发布 keystore 的 Base64 内容 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 发布密钥别名 |
| `ANDROID_KEY_PASSWORD` | 发布密钥密码 |
| `ANDROID_SIGNING_CERT_SHA256` | 原发布证书 SHA-256 指纹，支持带冒号或连续十六进制 |

必须使用与已发布 APK 相同的密钥。可在本机用 `keytool -list -v -keystore <keystore>` 查看证书指纹，不要把密钥或密码提交到仓库或聊天记录。

需要无人值守发版时，不设置环境的人工审批人；若配置审批，自动流程将在签名任务前等待批准。仓库须允许工作流申请 `contents: write`，该权限只授予最后的发布任务。

Gradle 支持 `SLEEPDOWN_KEYSTORE_FILE`、`SLEEPDOWN_KEYSTORE_PASSWORD`、`SLEEPDOWN_KEY_ALIAS`、`SLEEPDOWN_KEY_PASSWORD` 四个环境变量，配置任一个就必须配置齐全。未配置时继续使用本地 `release-signing/keystore.properties`；本地两者都未配置时构建未签名 APK。**Release CI 不允许缺密钥降级**，会验证 APK 签名及证书指纹，临时密钥在任务结束时删除。

## 发版步骤

1. 修改 `app/build.gradle.kts` 的 `versionName` 和递增的 `versionCode`。
2. 同步 `iosApp/iosApp.xcodeproj/project.pbxproj` 中主 App、Widget 的所有 `MARKETING_VERSION`、`CURRENT_PROJECT_VERSION`。iOS 的营销版本不带 prerelease 后缀，例如 Android `0.1.0-beta.2` 对应 iOS `0.1.0`；构建号与 Android 一致。
3. 将本次修改归入 `CHANGELOG.md` 的 `## 0.1.0-beta.2` 等精确版本标题下，不能留空。
4. 运行 `python3 scripts/release.py validate`，提交并通过门禁合入 `main`。
5. 对该提交创建并推送匹配标签，例如：

```sh
git tag v0.1.0-beta.2
git push origin v0.1.0-beta.2
```

以上是维护者主动执行的发版操作，不是普通代码提交的一部分。不要复用或移动现有 `v0.1.0-beta.1` 标签。

标签推送触发 `.github/workflows/release.yml`：

- 验证标签与源码版本、CHANGELOG 一致，且提交属于 `main` 历史。
- 对标签所指提交重新运行全部 CI 门禁，不能靠其他提交的绿色检查发版。
- 构建并验证签名 Android APK；复用同次门禁产出的 iOS IPA。
- 生成版本说明与 `SHA256SUMS.txt`，将全部附件上传至草稿 Release，成功后才公开。
- `-beta.*` 等 prerelease 版本标记为预发布，不覆盖 latest；正式版本标记为 latest。

发布资产：

```text
SleepDown-<version>-android.apk
SleepDown-<version>-ios-unsigned.ipa
SHA256SUMS.txt
```

iOS IPA **未签名**，不意味着可以直接安装或已发布到 App Store。安装仍需签名主 App 和 Widget，并配置有权限的 App Group。

自动发版失败不会创建新的公开版本。若上传失败留下草稿，维护者核对后删除该草稿，再对原标签重跑工作流；工作流不会覆盖已经公开的同名 Release。手动运行 Release 时也必须选择标签，不能选分支。

## 本地验证

```sh
python3 -m unittest discover -s scripts -p 'test_*.py' -v
python3 scripts/release.py validate
actionlint .github/workflows/ci.yml .github/workflows/release.yml
./gradlew :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest :app:lintDebug :shared:testDebugUnitTest
```

Android 设备测试会重建测试实例数据库，仅在专用设备运行 `:app:connectedDebugAndroidTest`。iOS 测试与归档命令见工作流及[开发文档](development.md)。本地通过不代表 GitHub 托管 runner 已实跑；首次推送后应查看完整 CI 结果。
