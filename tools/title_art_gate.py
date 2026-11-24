"""Strict validation for the six intentionally bundled offline menu covers."""
import hashlib

TITLE_ART_PATHS = frozenset(f"title_art/{i}.webp" for i in range(6))


def verify_title_art(archive, assets):
    """Return only verified APK paths. No other media is exempted."""
    expected = {"assets/" + path for path in TITLE_ART_PATHS}
    actual = [name for name in archive.namelist()
              if name.startswith("assets/title_art/") and not name.endswith("/")]
    if len(actual) != len(expected) or set(actual) != expected:
        raise ValueError("Unexpected, duplicate or missing offline title artwork")
    for path in sorted(TITLE_ART_PATHS):
        apk_path = "assets/" + path
        info = archive.getinfo(apk_path)
        if not 16000 < info.file_size < 2_000_000:
            raise ValueError("Invalid title artwork size: " + apk_path)
        data = archive.read(info)
        if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
            raise ValueError("Invalid title artwork format: " + apk_path)
        source = assets / path
        if not source.is_file() or hashlib.sha256(data).digest() != hashlib.sha256(source.read_bytes()).digest():
            raise ValueError("Title artwork SHA-256 mismatch: " + apk_path)
    return expected
