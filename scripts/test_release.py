import tempfile
from pathlib import Path
import unittest

from release import release_notes, validate, main


class ReleaseValidationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / "app").mkdir()
        (self.root / "iosApp/iosApp.xcodeproj").mkdir(parents=True)
        self.fixture()

    def fixture(self, version="1.2.3-beta.1", build="7", ios="1.2.3"):
        (self.root / "app/build.gradle.kts").write_text(
            f'versionName = "{version}"\nversionCode = {build}\n')
        (self.root / "iosApp/iosApp.xcodeproj/project.pbxproj").write_text(
            f'MARKETING_VERSION = {ios};\nCURRENT_PROJECT_VERSION = {build};\n' * 4)
        (self.root / "CHANGELOG.md").write_text(
            f'# Changes\n\n## Unreleased\nNext\n\n## {version}\n\nRelease notes\n\n## 1.2.2\nOlder\n')

    def test_prerelease_and_exact_notes(self):
        values, notes = validate(self.root, "v1.2.3-beta.1")
        self.assertEqual("true", values["prerelease"])
        self.assertEqual("7", values["build_number"])
        self.assertEqual("Release notes\n", notes)

    def test_stable(self):
        self.fixture(version="1.2.3")
        self.assertEqual("false", validate(self.root)[0]["prerelease"])

    def test_tag_mismatch(self):
        with self.assertRaisesRegex(ValueError, "does not match"):
            validate(self.root, "v1.2.3")

    def test_missing_or_empty_notes(self):
        for text in ("", "## 1.2.3-beta.10\nWrong\n", "## 1.2.3-beta.1\n\n## old\nPrevious\n",
                     "## 1.2.3-beta.1\n<!-- TODO -->\n"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                release_notes(text, "1.2.3-beta.1")

    def test_bracketed_notes_and_duplicates(self):
        self.assertEqual("Good\n", release_notes("## [1.2.3]\nGood\n## 1.2.2\nOld", "1.2.3"))
        with self.assertRaises(ValueError):
            release_notes("## 1.2.3\nA\n## [1.2.3]\nB", "1.2.3")

    def test_ios_version_and_build_must_all_match(self):
        path = self.root / "iosApp/iosApp.xcodeproj/project.pbxproj"
        for suffix in ("MARKETING_VERSION = 9.0.0;", "CURRENT_PROJECT_VERSION = 8;"):
            self.fixture()
            path.write_text(path.read_text() + suffix)
            with self.subTest(suffix=suffix), self.assertRaises(ValueError):
                validate(self.root)

    def test_missing_ios_versions(self):
        (self.root / "iosApp/iosApp.xcodeproj/project.pbxproj").write_text("")
        with self.assertRaises(ValueError):
            validate(self.root)

    def test_invalid_semver_and_build(self):
        for version in ("1.2", "01.2.3", "1.2.3-beta.01", "1.2.3+unsafe", "1.2.3\nattack"):
            self.fixture(version=version)
            with self.subTest(version=version), self.assertRaises(ValueError):
                validate(self.root)
        self.fixture(build="0")
        with self.assertRaises(ValueError):
            validate(self.root)

    def test_duplicate_android_version(self):
        path = self.root / "app/build.gradle.kts"
        path.write_text(path.read_text() + 'versionName = "1.2.3"\n')
        with self.assertRaises(ValueError):
            validate(self.root)

    def test_cli_outputs(self):
        output, notes = self.root / "output", self.root / "notes.md"
        self.assertEqual(0, main(["validate", "--root", str(self.root), "--tag", "v1.2.3-beta.1",
                                  "--github-output", str(output), "--notes-file", str(notes)]))
        self.assertEqual("version=1.2.3-beta.1\nbuild_number=7\nprerelease=true\n", output.read_text())
        self.assertEqual("Release notes\n", notes.read_text())


if __name__ == "__main__":
    unittest.main()
