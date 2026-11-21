"""Validate complete, distinct, decodable appearance assets before an OTA is built."""
import hashlib
import json
import pathlib
import struct
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets/portraits'
RACES='nord breton dunmer khajiit imperial orc redguard altmer bosmer argonian'.split()

def dimensions(data):
    if data[:4]!=b'RIFF' or data[8:12]!=b'WEBP':raise ValueError('Not WebP')
    if struct.unpack_from('<I',data,4)[0]+8!=len(data):raise ValueError('Truncated RIFF')
    offset=12
    while offset+8<=len(data):
        kind=data[offset:offset+4];size=struct.unpack_from('<I',data,offset+4)[0]
        block=data[offset+8:offset+8+size]
        if len(block)!=size:raise ValueError('Truncated WebP chunk')
        if kind==b'VP8X':
            return (int.from_bytes(block[4:7],'little')+1,int.from_bytes(block[7:10],'little')+1)
        if kind==b'VP8 ':
            if block[3:6]!=b'\x9d\x01\x2a':raise ValueError('Not a VP8 key frame')
            w,h=struct.unpack_from('<HH',block,6);return w&16383,h&16383
        if kind==b'VP8L':
            if block[0]!=47:raise ValueError('Not VP8L')
            bits=int.from_bytes(block[1:5],'little');return (bits&16383)+1,((bits>>14)&16383)+1
        offset+=8+size+(size&1)
    raise ValueError('No image chunk')

class CharacterPortraitAssets(unittest.TestCase):
    def test_every_race_has_three_distinct_portraits_for_each_gender(self):
        expected={f'{r}_{g}_{n}.webp' for r in RACES for g in ('male','female') for n in (1,2,3)}
        self.assertEqual({p.name for p in ASSETS.glob('*.webp')},expected)
        hashes=set()
        for name in expected:
            data=(ASSETS/name).read_bytes()
            self.assertEqual(dimensions(data),(576,768),name)
            hashes.add(hashlib.sha256(data).hexdigest())
        self.assertEqual(len(hashes),60,'Every portrait must be a distinct image')

    def test_report_matches_shipped_bytes_and_quality_floor(self):
        report=json.loads((ROOT/'docs/portrait_compression_report.json').read_text())
        self.assertEqual(report['image_count'],60)
        self.assertEqual(len(report['portraits']),60)
        for entry in report['portraits']:
            data=(ASSETS/entry['file']).read_bytes()
            self.assertEqual(hashlib.sha256(data).hexdigest(),entry['sha256'])
            self.assertEqual(len(data),entry['encoded_bytes'])
            self.assertGreaterEqual(entry['full_resolution_luma_ssim'],.985)
            self.assertGreaterEqual(entry['worst_128px_luma_ssim'],.965)
            self.assertLessEqual(entry['chroma_mean_absolute_error'],2.5)
            self.assertLessEqual(entry['chroma_95th_percentile_error'],7)
        self.assertEqual(sum(e['encoded_bytes'] for e in report['portraits']),report['encoded_bytes'])
        self.assertLess(report['encoded_bytes'],12*1024*1024)

if __name__=='__main__':unittest.main()
