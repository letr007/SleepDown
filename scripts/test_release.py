import contextlib
import io
import tempfile
from pathlib import Path
import unittest

from release import main, release_notes, select_platform, validate


class ReleaseValidationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / "app").mkdir()
        (self.root / "iosApp/iosApp.xcodeproj").mkdir(parents=True)
        self.fixture()

    def fixture(self, android_version="1.2.3", android_build="8",
                ios_version="1.1.0-beta.4", ios_build="7", marketing=None):
        (self.root / "app/build.gradle.kts").write_text(
            f'versionName = "{android_version}"\nversionCode = {android_build}\n')
        (self.root / "iosApp/release-version.txt").write_text(ios_version + "\n")
        marketing = marketing or ios_version.split("-")[0]
        (self.root / "iosApp/iosApp.xcodeproj/project.pbxproj").write_text(
            f'MARKETING_VERSION = {marketing};\nCURRENT_PROJECT_VERSION = {ios_build};\n' * 4)
        (self.root / "CHANGELOG.md").write_text(
            f'# Changes\n\n## Unreleased\nNext\n\n## Android {android_version}\n\nAndroid notes\n\n'
            f'## iOS {ios_version}\n\niOS notes\n\n## 1.0.0-beta.1\nOlder\n')

    def test_platforms_have_independent_versions_and_build_numbers(self):
        values, notes = validate(self.root)
        self.assertEqual("all", values["platform"])
        self.assertEqual("1.2.3", values["android_version"])
        self.assertEqual("8", values["android_build_number"])
        self.assertEqual("false", values["android_prerelease"])
        self.assertEqual("v1.2.3", values["android_tag"])
        self.assertEqual("1.1.0-beta.4", values["ios_version"])
        self.assertEqual("7", values["ios_build_number"])
        self.assertEqual("true", values["ios_prerelease"])
        self.assertEqual("ios-v1.1.0-beta.4", values["ios_tag"])
        self.assertIsNone(notes)

    def test_tag_routes_to_its_platform(self):
        for tag, platform, version, build, prerelease, notes in (
            ("v1.2.3", "android", "1.2.3", "8", "false", "Android notes\n"),
            ("ios-v1.1.0-beta.4", "ios", "1.1.0-beta.4", "7", "true", "iOS notes\n"),
        ):
            with self.subTest(tag=tag):
                values, body = validate(self.root, tag)
                self.assertEqual({"platform": platform, "version": version, "build_number": build,
                                  "tag": tag, "prerelease": prerelease}, values)
                self.assertEqual(notes, body)

    def test_platform_selection_without_tag(self):
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                self.assertEqual(platform, validate(self.root, platform=platform)[0]["platform"])

    def test_android_validation_does_not_read_ios_metadata_or_notes(self):
        (self.root / "iosApp/release-version.txt").unlink()
        (self.root / "iosApp/iosApp.xcodeproj/project.pbxproj").unlink()
        (self.root / "CHANGELOG.md").write_text("## Android 1.2.3\nAndroid only\n")
        self.assertEqual("Android only\n", validate(self.root, "v1.2.3")[1])
        with self.assertRaises(OSError):
            validate(self.root)

    def test_ios_validation_does_not_read_android_metadata_or_notes(self):
        (self.root / "app/build.gradle.kts").unlink()
        (self.root / "CHANGELOG.md").write_text("## iOS 1.1.0-beta.4\niOS only\n")
        self.assertEqual("iOS only\n", validate(self.root, "ios-v1.1.0-beta.4")[1])
        with self.assertRaises(OSError):
            validate(self.root)

    def test_stable_and_prerelease_for_each_platform(self):
        for version, expected in (("2.0.0", "false"), ("2.0.0-beta.1", "true"),
                                  ("2.0.0-rc.1", "true"), ("2.0.0-0", "true")):
            self.fixture(android_version=version, ios_version=version)
            for platform in ("android", "ios"):
                with self.subTest(platform=platform, version=version):
                    self.assertEqual(expected, validate(self.root, platform=platform)[0]["prerelease"])

    def test_wrong_platform_tags_are_rejected(self):
        for platform, tag in (("ios", "v1.1.0-beta.4"), ("android", "ios-v1.2.3"),
                              ("all", "v1.2.3"), ("all", "ios-v1.1.0-beta.4")):
            with self.subTest(platform=platform, tag=tag), self.assertRaisesRegex(ValueError, "targets"):
                validate(self.root, tag, platform)

    def test_mismatched_tag_version_is_rejected(self):
        for tag in ("v1.2.4", "v1.1.0-beta.4", "ios-v1.2.3", "ios-v1.1.0-beta.5"):
            with self.subTest(tag=tag), self.assertRaisesRegex(ValueError, "does not match"):
                validate(self.root, tag)

    def test_invalid_tags_and_platforms(self):
        for tag in ("1.2.3", "android-v1.2.3", "iOS-v1.1.0-beta.4", "v", "ios-v",
                    "v1.2.3+build.1", "v01.2.3", "ios-v1.1.0-beta.04", "v1.2.3\n", "v1.2.3/extra"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                select_platform(tag=tag)
        with self.assertRaisesRegex(ValueError, "Unknown platform"):
            validate(self.root, platform="windows")

    def test_notes_are_isolated_by_platform(self):
        changelog = "## Android 1.2.3\nAndroid only\n## iOS 1.2.3\niOS only\n"
        self.assertEqual("Android only\n", release_notes(changelog, "1.2.3", "android"))
        self.assertEqual("iOS only\n", release_notes(changelog, "1.2.3", "ios"))
        for platform, text in (("ios", "## Android 1.2.3\nAndroid only\n"),
                               ("android", "## iOS 1.2.3\niOS only\n"),
                               ("ios", "## Android 1.2.3-beta.1\nAndroid beta only\n")):
            version = "1.2.3-beta.1" if "beta" in text else "1.2.3"
            with self.subTest(platform=platform, text=text), self.assertRaises(ValueError):
                release_notes(text, version, platform)

    def test_legacy_unscoped_notes_only_apply_to_prereleases(self):
        for heading in ("1.2.3-beta.1", "v1.2.3-beta.1", "[1.2.3-beta.1]", "[v1.2.3-beta.1]"):
            for platform in ("android", "ios"):
                with self.subTest(heading=heading, platform=platform):
                    self.assertEqual("Legacy beta\n", release_notes(
                        f"## {heading}\nLegacy beta\n", "1.2.3-beta.1", platform))
        for heading in ("1.2.3", "v1.2.3", "[1.2.3]", "[v1.2.3]"):
            for platform in ("android", "ios"):
                with self.subTest(heading=heading, platform=platform), self.assertRaises(ValueError):
                    release_notes(f"## {heading}\nUnscoped stable\n", "1.2.3", platform)

    def test_current_ios_can_use_legacy_beta_notes(self):
        path = self.root / "CHANGELOG.md"
        path.write_text(path.read_text().replace("## iOS 1.1.0-beta.4", "## 1.1.0-beta.4"))
        self.assertEqual("iOS notes\n", validate(self.root, "ios-v1.1.0-beta.4")[1])

    def test_missing_or_empty_notes(self):
        for text in ("", "## Android 1.2.30\nWrong\n", "## Android 1.2.3\n\n## old\nPrevious\n",
                     "## Android 1.2.3\n<!-- TODO -->\n"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                release_notes(text, "1.2.3", "android")

    def test_notes_aliases_and_duplicates(self):
        self.assertEqual("Good\n", release_notes(
            "## Android [1.2.3]\nGood\n## Android 1.2.2\nOld", "1.2.3", "android"))
        for text, version in (("## Android 1.2.3\nA\n## Android [1.2.3]\nB", "1.2.3"),
                              ("## Android 1.2.3-beta.1\nA\n## 1.2.3-beta.1\nB", "1.2.3-beta.1")):
            with self.subTest(text=text), self.assertRaises(ValueError):
                release_notes(text, version, "android")

    def test_ios_marketing_versions_must_match_its_semver_base(self):
        path = self.root / "iosApp/iosApp.xcodeproj/project.pbxproj"
        for suffix in ("MARKETING_VERSION = 9.0.0;", 'MARKETING_VERSION = "1.1.0-beta.4";'):
            self.fixture()
            path.write_text(path.read_text() + suffix)
            with self.subTest(suffix=suffix), self.assertRaisesRegex(ValueError, "MARKETING_VERSION"):
                validate(self.root, platform="ios")

    def test_ios_build_numbers_must_all_match(self):
        path = self.root / "iosApp/iosApp.xcodeproj/project.pbxproj"
        path.write_text(path.read_text() + "CURRENT_PROJECT_VERSION = 8;")
        with self.assertRaisesRegex(ValueError, "must be identical"):
            validate(self.root, platform="ios")

    def test_ios_settings_allow_consistently_quoted_values(self):
        self.fixture(marketing='"1.1.0"', ios_build='"7"')
        self.assertEqual("7", validate(self.root, platform="ios")[0]["build_number"])

    def test_missing_or_malformed_ios_settings(self):
        path = self.root / "iosApp/iosApp.xcodeproj/project.pbxproj"
        for text in ("", "MARKETING_VERSION = 1.1.0;", "CURRENT_PROJECT_VERSION = 7;",
                     'MARKETING_VERSION = "1.1.0;\nCURRENT_PROJECT_VERSION = 7;',
                     'MARKETING_VERSION = 1.1.0;\nCURRENT_PROJECT_VERSION = "7;',
                     'MARKETING_VERSION = 1.1.0;\nCURRENT_PROJECT_VERSION = 7'):
            path.write_text(text)
            with self.subTest(text=text), self.assertRaises(ValueError):
                validate(self.root, platform="ios")

    def test_invalid_ios_build_numbers(self):
        for build in ("0", "-1", "07", "1.0", "1.2.3", "bad", "$(BUILD_NUMBER)", "７"):
            self.fixture(ios_build=build)
            with self.subTest(build=build), self.assertRaises(ValueError):
                validate(self.root, platform="ios")

    def test_invalid_semver_for_each_platform(self):
        for version in ("1.2", "01.2.3", "1.2.3-beta.01", "1.2.3+unsafe", "1.2.3-",
                        "1.2.3-beta..1", "1.2.3/bad", "１.2.3", "1.2.3\nattack", " 1.2.3"):
            self.fixture(android_version=version, ios_version=version)
            for platform in ("android", "ios"):
                with self.subTest(version=version, platform=platform), self.assertRaises(ValueError):
                    validate(self.root, platform=platform)

    def test_ios_version_file_is_one_plain_semver_line(self):
        for text in ("", "1.1.0-beta.4\n\n", "1.1.0-beta.4 \n", "1.1.0-beta.4\n1.1.0-beta.4\n"):
            (self.root / "iosApp/release-version.txt").write_text(text)
            with self.subTest(text=text), self.assertRaises(ValueError):
                validate(self.root, platform="ios")

    def test_invalid_android_build_numbers(self):
        for build in ("0", "-1", "08", "2100000001", "1.0", "bad", '"8"', "８"):
            self.fixture(android_build=build)
            with self.subTest(build=build), self.assertRaises(ValueError):
                validate(self.root, platform="android")
        self.fixture(android_build="2100000000")
        self.assertEqual("2100000000", validate(self.root, platform="android")[0]["build_number"])

    def test_duplicate_android_metadata(self):
        path = self.root / "app/build.gradle.kts"
        for suffix in ('versionName = "1.2.3"\n', 'versionCode = 8\n'):
            self.fixture()
            path.write_text(path.read_text() + suffix)
            with self.subTest(suffix=suffix), self.assertRaisesRegex(ValueError, "exactly one"):
                validate(self.root, platform="android")

    def test_cli_platform_outputs_and_notes(self):
        for tag, platform, version, build, prerelease, notes in (
            ("v1.2.3", "android", "1.2.3", "8", "false", "Android notes\n"),
            ("ios-v1.1.0-beta.4", "ios", "1.1.0-beta.4", "7", "true", "iOS notes\n"),
        ):
            with self.subTest(platform=platform):
                output, body = self.root / (platform + ".output"), self.root / (platform + ".md")
                stdout = io.StringIO()
                with contextlib.redirect_stdout(stdout):
                    code = main(["validate", "--root", str(self.root), "--tag", tag,
                                 "--platform", platform, "--github-output", str(output),
                                 "--notes-file", str(body)])
                self.assertEqual(0, code)
                self.assertEqual(f"platform={platform}\nversion={version}\nbuild_number={build}\n"
                                 f"tag={tag}\nprerelease={prerelease}\n", output.read_text())
                self.assertEqual(notes, body.read_text())
                self.assertIn(version + " / build " + build + " validated", stdout.getvalue())

    def test_cli_defaults_to_independent_dual_platform_outputs(self):
        output, stdout = self.root / "all.output", io.StringIO()
        output.write_text("existing=value\n")
        with contextlib.redirect_stdout(stdout):
            code = main(["validate", "--root", str(self.root), "--github-output", str(output)])
        self.assertEqual(0, code)
        self.assertEqual("existing=value\nplatform=all\nandroid_version=1.2.3\nandroid_build_number=8\n"
                         "android_tag=v1.2.3\nandroid_prerelease=false\nios_version=1.1.0-beta.4\n"
                         "ios_build_number=7\nios_tag=ios-v1.1.0-beta.4\nios_prerelease=true\n", output.read_text())
        self.assertEqual("Android 1.2.3 / build 8 validated\niOS 1.1.0-beta.4 / build 7 validated\n",
                         stdout.getvalue())

    def test_cli_rejects_joint_notes_output(self):
        notes, stderr = self.root / "notes.md", io.StringIO()
        with contextlib.redirect_stderr(stderr):
            code = main(["validate", "--root", str(self.root), "--notes-file", str(notes)])
        self.assertEqual(1, code)
        self.assertFalse(notes.exists())
        self.assertIn("requires one platform", stderr.getvalue())

    def test_cli_validation_failure_does_not_write_outputs(self):
        for arguments in (("--platform", "ios", "--tag", "v1.2.3"), ("--tag", "ios-v9.0.0")):
            output, notes, stderr = self.root / "output", self.root / "notes.md", io.StringIO()
            with self.subTest(arguments=arguments), contextlib.redirect_stderr(stderr):
                code = main(["validate", "--root", str(self.root), *arguments,
                             "--github-output", str(output), "--notes-file", str(notes)])
            self.assertEqual(1, code)
            self.assertFalse(output.exists())
            self.assertFalse(notes.exists())
            self.assertIn("Release validation failed", stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
