import os
import re
import subprocess
import sys
from pathlib import Path

try:
    import tomllib
except ImportError:
    tomllib = None


WORKSPACE = Path.cwd()
DOCUMENTATION_SUFFIXES = (".md", ".pdf")


def fail(message):
    escaped = message.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print(f"::error::{escaped}", file=sys.stderr)
    raise SystemExit(1)


def output(name, value):
    output_file = os.environ.get("GITHUB_OUTPUT")
    if output_file:
        with open(output_file, "a", encoding="utf-8") as stream:
            stream.write(f"{name}={value}\n")


def summary(message):
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        with open(summary_file, "a", encoding="utf-8") as stream:
            stream.write(f"{message}\n")


def changed_files():
    base = os.environ.get("BASE_SHA", "")
    head = os.environ.get("HEAD_SHA", "")
    if not base or set(base) == {"0"}:
        result = subprocess.run(
            ["git", "ls-files"], check=True, capture_output=True, text=True
        )
    else:
        if not head:
            fail("Impossible de déterminer le commit de tête à comparer.")
        result = subprocess.run(
            ["git", "diff", "--name-only", base, head],
            check=True,
            capture_output=True,
            text=True,
        )
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def is_documentation(path):
    normalized = path.replace("\\", "/").lower()
    return (
        normalized.endswith(DOCUMENTATION_SUFFIXES)
        or normalized.startswith("docs/")
    )


def gradle_root():
    candidates = []
    for wrapper in WORKSPACE.rglob("gradlew"):
        relative_parts = wrapper.relative_to(WORKSPACE).parts
        if any(part in {".git", ".gradle", "build"} for part in relative_parts):
            continue
        root = wrapper.parent
        settings = any((root / name).is_file() for name in ("settings.gradle", "settings.gradle.kts"))
        wrapper_properties = root / "gradle" / "wrapper" / "gradle-wrapper.properties"
        if settings and wrapper_properties.is_file():
            candidates.append((root, wrapper, wrapper_properties))

    if not candidates:
        fail(
            "Aucun projet Android Gradle complet n'a été trouvé. "
            "Le dépôt doit contenir settings.gradle(.kts), gradlew et "
            "gradle/wrapper/gradle-wrapper.properties."
        )
    if len(candidates) != 1:
        paths = ", ".join(str(item[0].relative_to(WORKSPACE)) for item in candidates)
        fail(f"Plusieurs racines Android Gradle sont ambiguës : {paths}")
    return candidates[0]


def android_gradle_plugin_version(root):
    app_build = next(
        (root / f"app/build.gradle{suffix}" for suffix in ("", ".kts")
         if (root / f"app/build.gradle{suffix}").is_file()),
        None,
    )
    if app_build is None:
        fail(
            f"Le module Android app est absent de {root.relative_to(WORKSPACE)} "
            "(app/build.gradle ou app/build.gradle.kts introuvable)."
        )

    config_files = [
        root / name
        for name in (
            "settings.gradle",
            "settings.gradle.kts",
            "build.gradle",
            "build.gradle.kts",
            "app/build.gradle",
            "app/build.gradle.kts",
        )
        if (root / name).is_file()
    ]
    versions = set()
    patterns = (
        re.compile(
            r"""id\s*\(?\s*["']com\.android\.application["']\s*\)?\s*version\s*["']([^"']+)["']"""
        ),
        re.compile(
            r"""com\.android\.tools\.build:gradle:([0-9][A-Za-z0-9.+_-]*)"""
        ),
    )
    for config in config_files:
        contents = config.read_text(encoding="utf-8")
        for pattern in patterns:
            versions.update(pattern.findall(contents))

    catalog = root / "gradle" / "libs.versions.toml"
    if catalog.is_file():
        if tomllib is None:
            fail("Python 3.11+ est requis pour lire gradle/libs.versions.toml.")
        data = tomllib.loads(catalog.read_text(encoding="utf-8"))
        catalog_versions = data.get("versions", {})
        for plugin in data.get("plugins", {}).values():
            if not isinstance(plugin, dict) or plugin.get("id") != "com.android.application":
                continue
            version = plugin.get("version")
            if isinstance(version, str):
                versions.add(version)
            elif isinstance(version, dict) and version.get("ref") in catalog_versions:
                referenced = catalog_versions[version["ref"]]
                if isinstance(referenced, str):
                    versions.add(referenced)

    versions = {version for version in versions if re.match(r"^\d+(?:\.\d+)+", version)}
    if len(versions) != 1:
        fail(
            "Impossible de déterminer une version AGP unique. Déclarez une version "
            "explicite de com.android.application dans les plugins Gradle, "
            "le classpath com.android.tools.build:gradle ou le catalogue Gradle."
        )
    return versions.pop(), app_build


def gradle_version(wrapper_properties):
    contents = wrapper_properties.read_text(encoding="utf-8")
    match = re.search(r"gradle-([0-9]+(?:\.[0-9]+)+)(?:-[^/]+)?\.zip", contents)
    if not match:
        fail(f"Version Gradle introuvable dans {wrapper_properties}.")
    return match.group(1)


def version_pair(version):
    match = re.match(r"^(\d+)\.(\d+)", version)
    if not match:
        fail(f"Version non reconnue : {version}")
    return tuple(map(int, match.groups()))


def main():
    changed = changed_files()
    build_required = not changed or any(not is_documentation(path) for path in changed)
    if not build_required:
        message = (
            "Changements documentaires uniquement : les vérifications CI ont été "
            "exécutées, la compilation Android est ignorée."
        )
        print(message)
        summary(f"### CI Android\n\n{message}")
        output("run_android_build", "false")
        return

    root, wrapper, wrapper_properties = gradle_root()
    agp, app_build = android_gradle_plugin_version(root)
    gradle = gradle_version(wrapper_properties)

    agp_major = version_pair(agp)[0]
    java = "17" if agp_major >= 8 else "11" if agp_major >= 7 else "8"
    gradle_pair = version_pair(gradle)
    minimum_gradle = {"8": (7, 3), "11": (5, 0), "17": (7, 3)}[java]
    if gradle_pair < minimum_gradle:
        fail(
            f"Gradle {gradle} ne peut pas lancer le JDK {java} requis par AGP {agp}. "
            f"Le wrapper doit être au moins Gradle {minimum_gradle[0]}.{minimum_gradle[1]}."
        )

    relative_root = root.relative_to(WORKSPACE)
    relative_wrapper = wrapper.relative_to(WORKSPACE).as_posix()
    mode_result = subprocess.run(
        ["git", "ls-files", "-s", "--", relative_wrapper],
        check=True,
        capture_output=True,
        text=True,
    )
    modes = {line.split()[0] for line in mode_result.stdout.splitlines() if line.split()}
    if "100755" not in modes:
        fail(
            f"{relative_wrapper} n'est pas enregistré comme exécutable dans Git. "
            "Corrigez son mode de fichier (100755) avant de lancer CI."
        )

    message = (
        f"Projet Android : `{relative_root}` — module `"
        f"{app_build.parent.relative_to(WORKSPACE)}` — AGP {agp}, "
        f"Gradle {gradle}, JDK {java}."
    )
    print(message)
    summary(f"### CI Android\n\n{message}")
    output("android_root", str(relative_root) or ".")
    output("java_version", java)
    output("run_android_build", "true")


if __name__ == "__main__":
    main()
