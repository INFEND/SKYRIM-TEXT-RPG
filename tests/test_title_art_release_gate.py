"""Exercise the APK gate, including missing, changed and additional menu assets."""
import io
import pathlib
import tempfile
import unittest
import zipfile

from tools.title_art_gate import TITLE_ART_PATHS, verify_title_art


class TitleArtReleaseGateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.assets = pathlib.Path(self.temp.name)
        self.frames = {}
        for i, path in enumerate(sorted(TITLE_ART_PATHS)):
            data = b"RIFF" + b"0000" + b"WEBP" + bytes([i]) * 16001
            target = self.assets / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            self.frames["assets/" + path] = data

    def check(self, frames):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w") as archive:
            for path, content in frames.items():
                archive.writestr(path, content)
        with zipfile.ZipFile(stream) as archive:
            return verify_title_art(archive, self.assets)

    def test_approved_exact_frames_only(self):
        frames = dict(self.frames, **{"assets/gallery/extra.webp": b"other"})
        self.assertEqual(self.check(frames), set(self.frames))

    def test_reject_missing_or_unapproved_cover(self):
        frames = dict(self.frames)
        frames.pop("assets/title_art/0.webp")
        with self.assertRaises(ValueError):
            self.check(frames)
        frames = dict(self.frames, **{"assets/title_art/6.webp": b"extra"})
        with self.assertRaises(ValueError):
            self.check(frames)

    def test_reject_tampering_format_and_size(self):
        path = "assets/title_art/0.webp"
        original = self.frames[path]
        for bad in (original[:-1] + b"x", b"x" + original[1:], original[:100]):
            with self.subTest(size=len(bad)):
                with self.assertRaises(ValueError):
                    self.check(dict(self.frames, **{path: bad}))
