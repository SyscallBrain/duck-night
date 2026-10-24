#!/usr/bin/env python3
"""Generates the fourteen ambient loops used by Duck Night.

The music is synthesized from slow sine chords and filtered noise. The committed OGG files are
the ones that go in the APK; the build never regenerates them. No sample from another game is used.
"""
from __future__ import annotations

import math
import random
import subprocess
import tempfile
import wave
from pathlib import Path

RATE = 22_050
SECONDS = 60
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"

# Home, twelve places, fair. Slow roots in a low register.
ROOTS = [110.0, 98.0, 123.47, 87.31, 130.81, 116.54, 103.83, 92.5, 146.83, 82.41, 110.0, 98.0, 123.47, 130.81]
NAMES = ["home"] + [f"p{i:02d}" for i in range(1, 13)] + ["fair"]


def ambient(root: float, seed: int) -> list[float]:
    rnd = random.Random(seed)
    count = SECONDS * RATE
    samples = [0.0] * count
    # Two slow chords, an octave apart, with a gentle noise bed.
    intervals = [1.0, 1.25, 1.5, 2.0]
    phases = [rnd.uniform(0, 2 * math.pi) for _ in intervals]
    for i in range(count):
        t = i / RATE
        value = 0.0
        for index, ratio in enumerate(intervals):
            lfo = 0.55 + 0.45 * math.sin(2 * math.pi * (0.025 + index * 0.007) * t + phases[index])
            value += math.sin(2 * math.pi * root * ratio * t + phases[index]) * lfo / len(intervals)
        value += rnd.uniform(-0.02, 0.02)
        # Long fade at both ends makes the loop click-free.
        fade = min(1.0, t / 2.0, (SECONDS - t) / 2.0)
        samples[i] = value * 0.22 * max(0.0, fade)
    return samples


def write_wav(path: Path, samples: list[float]) -> None:
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(RATE)
        data = bytearray()
        for sample in samples:
            value = int(max(-1.0, min(1.0, sample)) * 32767)
            data += value.to_bytes(2, "little", signed=True)
        handle.writeframes(bytes(data))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temp:
        for index, (name, root) in enumerate(zip(NAMES, ROOTS)):
            wav = Path(temp) / f"{name}.wav"
            ogg = OUT / f"ambient_{name}.ogg"
            write_wav(wav, ambient(root, 100 + index))
            subprocess.run(
                ["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "1", str(ogg)],
                check=True,
            )
            print(f"{ogg.name}: {ogg.stat().st_size} bytes")


if __name__ == "__main__":
    main()
