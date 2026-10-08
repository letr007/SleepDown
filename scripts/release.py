#!/usr/bin/env python3
"""Validate independent Android and iOS release metadata and notes."""

import argparse
from pathlib import Path
import re
import sys


SEMVER = re.compile(
    r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"
    r"(?:-((?:0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*)"
    r"(?:\.(?:0|[1-9][0-9]*|[0-9]*[A-Za-z-][0-9A-Za-z-]*))*))?"
)
PLATFORMS = {"android": ("Android", "v"), "ios": ("iOS", "ios-v")}


def single(pattern, text, label):
    values = re.findall(pattern, text, re.MULTILINE)
    if len(values) != 1:
        raise ValueError(f"Expected exactly one {label}, found {len(values)}")
    return values[0]


def semver(version, label):
    match = SEMVER.fullmatch(version)
    if not match:
        raise ValueError(f"{label} must be SemVer without build metadata")
    return match


def build_number(value, label, maximum=None):
    if not re.fullmatch(r"[1-9][0-9]*", value):
        raise ValueError(f"{label} must be a positive integer without leading zeros")
    if maximum is not None and int(value) > maximum:
        raise ValueError(f"{label} must be in 1..{maximum}")
    return value


def select_platform(platform=None, tag=None):
    if platform not in (None, "all", *PLATFORMS):
        raise ValueError(f"Unknown platform {platform!r}")
    if tag is None:
        return platform or "all"
    if tag.startswith("ios-v"):
        target, version = "ios", tag[5:]
    elif tag.startswith("v"):
        target, version = "android", tag[1:]
    else:
        raise ValueError("Tag must use v<version> for Android or ios-v<version> for iOS")
    semver(version, "Tag version")
    if platform is not None and platform != target:
        raise ValueError(f"Tag {tag!r} targets {target}, not {platform}")
    return target


def release_notes(changelog, version, platform):
    label, _ = PLATFORMS[platform]
    match = semver(version, "Release notes version")
    aliases = (version, f"v{version}", f"[{version}]", f"[v{version}]")
    headings = {f"{label} {alias}" for alias in aliases}
    # Unscoped prerelease sections belong to the historical joint beta releases.
    if match.group(4):
        headings.update(aliases)
    sections = re.split(r"^##[ \t]+(.+?)[ \t]*$", changelog, flags=re.MULTILINE)
    matches = [sections[index + 1].strip() for index in range(1, len(sections), 2)
               if sections[index].strip() in headings]
    if len(matches) != 1 or not re.sub(r"<!--.*?-->", "", matches[0], flags=re.S).strip():
        raise ValueError(f"CHANGELOG.md must contain one non-empty '## {label} {version}' section")
    return matches[0] + "\n"


def ios_settings(text, label):
    values = re.findall(rf"\b{label}\s*=\s*([^;\n]+);", text)
    if len(values) != len(re.findall(rf"\b{label}\s*=", text)):
        raise ValueError(f"Invalid iOS {label} assignment")
    if not values:
        raise ValueError(f"Missing iOS {label}")
    settings = []
    for value in values:
        value = value.strip()
        if re.fullmatch(r'"[^"\s]+"', value):
            value = value[1:-1]
        elif not re.fullmatch(r'[^"\s]+', value):
            raise ValueError(f"Invalid iOS {label}: {value!r}")
        settings.append(value)
    return settings


def validate_platform(root, platform, tag=None):
    if platform == "android":
        android = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
        version = single(r'^[ \t]*versionName\s*=\s*"([^"\n]+)"[ \t]*$', android, "versionName")
        build = single(r'^[ \t]*versionCode\s*=\s*([^\s]+)[ \t]*$', android, "versionCode")
        match = semver(version, "versionName")
        build_number(build, "versionCode", maximum=2_100_000_000)
    else:
        text = (root / "iosApp/release-version.txt").read_text(encoding="utf-8")
        version = text[:-1] if text.endswith("\n") else text
        match = semver(version, "iosApp/release-version.txt")
        ios = (root / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
        base = ".".join(match.groups()[:3])
        if set(ios_settings(ios, "MARKETING_VERSION")) != {base}:
            raise ValueError(f"Every iOS MARKETING_VERSION must equal {base}")
        builds = ios_settings(ios, "CURRENT_PROJECT_VERSION")
        for value in builds:
            build_number(value, "iOS CURRENT_PROJECT_VERSION")
        if len(set(builds)) != 1:
            raise ValueError("Every iOS CURRENT_PROJECT_VERSION must be identical")
        build = builds[0]
    expected_tag = PLATFORMS[platform][1] + version
    if tag is not None and tag != expected_tag:
        raise ValueError(f"Tag {tag!r} does not match {expected_tag}")
    notes = release_notes((root / "CHANGELOG.md").read_text(encoding="utf-8"), version, platform)
    return {"platform": platform, "version": version, "build_number": build,
            "tag": expected_tag, "prerelease": "true" if match.group(4) else "false"}, notes


def validate(root, tag=None, platform=None):
    platform = select_platform(platform, tag)
    if platform != "all":
        return validate_platform(root, platform, tag)
    values = {"platform": "all"}
    for target in PLATFORMS:
        metadata, _ = validate_platform(root, target)
        values.update({f"{target}_{key}": value for key, value in metadata.items() if key != "platform"})
    return values, None


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["validate"])
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--platform", choices=["all", "android", "ios"],
                        help="Default: validate both platforms, or route by --tag")
    parser.add_argument("--tag")
    parser.add_argument("--notes-file", type=Path)
    parser.add_argument("--github-output", type=Path)
    args = parser.parse_args(argv)
    try:
        if args.notes_file and select_platform(args.platform, args.tag) == "all":
            raise ValueError("--notes-file requires one platform or a release tag")
        values, notes = validate(args.root, args.tag, args.platform)
        if args.notes_file:
            args.notes_file.parent.mkdir(parents=True, exist_ok=True)
            args.notes_file.write_text(notes, encoding="utf-8")
        if args.github_output:
            with args.github_output.open("a", encoding="utf-8") as output:
                for key, value in values.items():
                    output.write(f"{key}={value}\n")
        if values["platform"] == "all":
            for target, (label, _) in PLATFORMS.items():
                print(f"{label} {values[target + '_version']} / build {values[target + '_build_number']} validated")
        else:
            print(f"{PLATFORMS[values['platform']][0]} {values['version']} / build {values['build_number']} validated")
    except (OSError, ValueError) as error:
        print(f"Release validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
