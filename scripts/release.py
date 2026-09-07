#!/usr/bin/env python3
"""Validate release metadata shared by Android, iOS and GitHub Releases."""

import argparse
from pathlib import Path
import re
import sys


SEMVER = re.compile(
    r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    r"(?:-((?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*)"
    r"(?:\.(?:0|[1-9]\d*|\d*[A-Za-z-][0-9A-Za-z-]*))*))?"
)


def single(pattern, text, label):
    values = re.findall(pattern, text, re.MULTILINE)
    if len(values) != 1:
        raise ValueError(f"Expected exactly one {label}, found {len(values)}")
    return values[0]


def release_notes(changelog, version):
    sections = re.split(r"^##[ \t]+(.+?)[ \t]*$", changelog, flags=re.MULTILINE)
    matches = []
    for index in range(1, len(sections), 2):
        heading = sections[index].strip()
        if heading in (version, f"v{version}", f"[{version}]", f"[v{version}]"):
            matches.append(sections[index + 1].strip())
    if len(matches) != 1 or not matches[0] or not re.sub(r"<!--.*?-->", "", matches[0], flags=re.S).strip():
        raise ValueError(f"CHANGELOG.md must contain one non-empty '## {version}' section")
    return matches[0] + "\n"


def validate(root, tag=None):
    android = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
    version = single(r'^\s*versionName\s*=\s*"([^"\n]+)"\s*$', android, "versionName")
    build = single(r'^\s*versionCode\s*=\s*(\d+)\s*$', android, "versionCode")
    match = SEMVER.fullmatch(version)
    if not match:
        raise ValueError("versionName must be SemVer without build metadata")
    if int(build) <= 0 or int(build) > 2_100_000_000:
        raise ValueError("versionCode must be in 1..2100000000")
    if tag is not None and tag != f"v{version}":
        raise ValueError(f"Tag {tag!r} does not match v{version}")

    ios = (root / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
    marketing = re.findall(r'\bMARKETING_VERSION\s*=\s*"?([^;"\s]+)"?\s*;', ios)
    builds = re.findall(r'\bCURRENT_PROJECT_VERSION\s*=\s*"?([^;"\s]+)"?\s*;', ios)
    base = ".".join(match.groups()[:3])
    if not marketing or set(marketing) != {base}:
        raise ValueError(f"Every iOS MARKETING_VERSION must equal {base}")
    if not builds or set(builds) != {build}:
        raise ValueError(f"Every iOS CURRENT_PROJECT_VERSION must equal {build}")
    notes = release_notes((root / "CHANGELOG.md").read_text(encoding="utf-8"), version)
    return {"version": version, "build_number": build,
            "prerelease": "true" if match.group(4) else "false"}, notes


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["validate"])
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--tag")
    parser.add_argument("--notes-file", type=Path)
    parser.add_argument("--github-output", type=Path)
    args = parser.parse_args(argv)
    try:
        values, notes = validate(args.root, args.tag)
        if args.notes_file:
            args.notes_file.parent.mkdir(parents=True, exist_ok=True)
            args.notes_file.write_text(notes, encoding="utf-8")
        if args.github_output:
            with args.github_output.open("a", encoding="utf-8") as output:
                for key, value in values.items():
                    output.write(f"{key}={value}\n")
        print(f"Version {values['version']} / build {values['build_number']} validated")
    except (OSError, ValueError) as error:
        print(f"Release validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
