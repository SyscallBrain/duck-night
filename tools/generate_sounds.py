#!/usr/bin/env python3
"""Generates the small, original sound effects used by Duck Night.

All sounds are synthesized from sine tones, filtered noise and envelopes; no sample from another
game is used. The generated WAV files are committed as the assets that go in the APK.
"""
from __future__ import annotations

import math
import random
import wave
from pathlib import Path

RATE = 22_050
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/raw"


def write(name: str, samples: list[float], gain: float = 0.7) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    peak = max(1e-6, max(abs(s) for s in samples))
    data = bytearray()
    for sample in samples:
        value = int(max(-1.0, min(1.0, sample / peak * gain)) * 32767)
        data += value.to_bytes(2, "little", signed=True)
    with wave.open(str(OUT / name), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(RATE)
        handle.writeframes(bytes(data))


def tone(freq: float, seconds: float, volume: float = 0.6, decay: float = 18.0) -> list[float]:
    count = int(seconds * RATE)
    return [
        volume * math.sin(2 * math.pi * freq * i / RATE) * math.exp(-decay * i / RATE)
        for i in range(count)
    ]


def noise(seconds: float, volume: float = 0.5, decay: float = 30.0) -> list[float]:
    rnd = random.Random(7)
    count = int(seconds * RATE)
    return [rnd.uniform(-1, 1) * volume * math.exp(-decay * i / RATE) for i in range(count)]


def silence(seconds: float) -> list[float]:
    return [0.0] * int(seconds * RATE)


def mix(*parts: list[float]) -> list[float]:
    length = max(len(p) for p in parts)
    out = [0.0] * length
    for part in parts:
        for i, value in enumerate(part):
            out[i] += value
    return out


def sequence(*parts: list[float]) -> list[float]:
    out: list[float] = []
    for part in parts:
        out += part
    return out


def main() -> None:
    # Mechanical shutter: a short noise click with a low thump.
    write("shutter.wav", mix(noise(0.075, 0.9, 45), tone(180, 0.06, 0.35, 40)))
    write("shutter_flash.wav", mix(noise(0.09, 0.9, 40), tone(240, 0.07, 0.4, 35)))
    # Film lever and empty roll.
    write("advance.wav", sequence(noise(0.025, 0.5, 80), silence(0.03), noise(0.035, 0.45, 70)))
    write("empty.wav", sequence(tone(220, 0.08, 0.3, 18), tone(160, 0.12, 0.25, 12)))
    # Good and perfect photos: small bells.
    write("photo_good.wav", sequence(tone(880, 0.16, 0.5, 15), tone(1320, 0.2, 0.35, 12)))
    write("photo_perfect.wav", sequence(tone(880, 0.15, 0.45, 14), tone(1320, 0.15, 0.4, 12), tone(1760, 0.28, 0.35, 9)))
    # A bird leaving, and the dog's laugh (four short notes).
    write("fled.wav", sequence(tone(620, 0.12, 0.3, 12), tone(500, 0.12, 0.28, 12), tone(380, 0.2, 0.25, 8)))
    write("laugh.wav", sequence(tone(520, 0.09, 0.35, 20), tone(650, 0.09, 0.32, 20), tone(560, 0.09, 0.3, 20), tone(720, 0.14, 0.28, 14)))
    write("bark.wav", sequence(tone(190, 0.09, 0.5, 25), silence(0.04), tone(170, 0.12, 0.45, 18)))
    # Protected bird: a soft low note, never a reward.
    write("protected_bird.wav", sequence(tone(220, 0.25, 0.35, 8), tone(180, 0.3, 0.3, 6)))
    # Result and stars.
    write("star.wav", tone(1320, 0.22, 0.35, 10))
    write("pass.wav", sequence(tone(523, 0.12, 0.4, 12), tone(659, 0.12, 0.4, 12), tone(784, 0.12, 0.4, 12), tone(1046, 0.3, 0.35, 8)))
    write("fail.wav", sequence(tone(392, 0.16, 0.35, 10), tone(294, 0.3, 0.35, 7)))


if __name__ == "__main__":
    main()
