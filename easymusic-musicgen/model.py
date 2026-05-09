"""
MusicGen 模型封装 — 基于 Meta MusicGen (facebook/musicgen-small)
论文: Simple and Controllable Music Generation (Copet et al., 2023)

注意: MusicGen Small 仅生成纯器乐，不支持人声/歌唱。
"""
import os
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")

import torch
import scipy.io.wavfile
import numpy as np
import uuid
import time
import warnings
from pathlib import Path
from transformers import AutoProcessor, MusicgenForConditionalGeneration

warnings.filterwarnings("ignore")

OUTPUT_DIR = Path(__file__).parent / "output"
OUTPUT_DIR.mkdir(exist_ok=True)

# 半精度模式：float16 内存占用约为 float32 的 50%
USE_HALF_PRECISION = True


class MusicGenModel:
    def __init__(self, model_name="facebook/musicgen-small"):
        self.device = torch.device(
            "mps" if torch.backends.mps.is_available() else "cpu"
        )
        self.use_half = USE_HALF_PRECISION and self.device.type == "mps"

        dtype = torch.float16 if self.use_half else torch.float32
        dtype_str = "float16" if self.use_half else "float32"
        print(f"[MusicGen] Loading {model_name} on {self.device} ({dtype_str})...")

        self.processor = AutoProcessor.from_pretrained(model_name)
        self.model = MusicgenForConditionalGeneration.from_pretrained(
            model_name,
            torch_dtype=dtype,
        )
        self.model = self.model.to(self.device)
        self.model.eval()

        self.sample_rate = self.model.config.audio_encoder.sampling_rate
        print(f"[MusicGen] Ready. Sample rate: {self.sample_rate}, "
              f"dtype: {dtype_str}, "
              f"device: {self.device}")

    def generate(self, prompt: str, duration: float = 15.0,
                 guidance_scale: float = 3.0, temperature: float = 1.0) -> dict:
        task_id = uuid.uuid4().hex[:12]
        start_time = time.time()

        inputs = self.processor(
            text=[prompt],
            padding=True,
            return_tensors="pt"
        ).to(self.device)

        # 每 token 约 0.02 秒 (50Hz)，最少 256 token (~5秒)
        max_new_tokens = max(int(duration * 50), 256)

        with torch.no_grad():
            audio_values = self.model.generate(
                **inputs,
                max_new_tokens=max_new_tokens,
                do_sample=True,
                guidance_scale=guidance_scale,
                temperature=temperature,
            )

        audio = audio_values[0, 0].cpu().numpy()
        filename = f"{task_id}.wav"
        filepath = OUTPUT_DIR / filename
        scipy.io.wavfile.write(
            str(filepath), self.sample_rate, audio.astype(np.float32)
        )

        actual_duration = len(audio) / self.sample_rate
        elapsed = time.time() - start_time

        print(f"[MusicGen] {task_id}: {actual_duration:.1f}s "
              f"in {elapsed:.1f}s | {prompt[:60]}")

        return {
            "taskId": task_id,
            "filename": filename,
            "duration": round(actual_duration, 1),
            "sampleRate": self.sample_rate,
            "prompt": prompt,
            "elapsed": round(elapsed, 1),
        }


_model = None


def get_model():
    global _model
    if _model is None:
        _model = MusicGenModel()
    return _model
