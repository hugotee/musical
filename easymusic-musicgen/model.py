"""
MusicGen 模型封装 — 基于 Meta MusicGen (facebook/musicgen-small)
论文: Simple and Controllable Music Generation (Copet et al., 2023)
"""
import os
# 国内网络使用镜像站，海外直连可删除此行
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")

import torch
import torchaudio
import scipy.io.wavfile
import numpy as np
import uuid
import time
import os
import warnings
from pathlib import Path
from transformers import AutoProcessor, MusicgenForConditionalGeneration

warnings.filterwarnings("ignore")

OUTPUT_DIR = Path(__file__).parent / "output"
OUTPUT_DIR.mkdir(exist_ok=True)


class MusicGenModel:
    def __init__(self, model_name="facebook/musicgen-small"):
        self.device = torch.device("mps" if torch.backends.mps.is_available() else "cpu")
        print(f"[MusicGen] Loading model {model_name} on {self.device}...")

        self.processor = AutoProcessor.from_pretrained(model_name)
        self.model = MusicgenForConditionalGeneration.from_pretrained(model_name)
        self.model = self.model.to(self.device)
        self.model.eval()

        self.sample_rate = self.model.config.audio_encoder.sampling_rate
        print(f"[MusicGen] Ready. Sample rate: {self.sample_rate}")

    def generate(self, prompt: str, duration: float = 15.0,
                 guidance_scale: float = 3.0, temperature: float = 1.0) -> dict:
        """生成音乐，返回文件路径和元信息"""
        task_id = uuid.uuid4().hex[:12]
        start_time = time.time()

        # 1. 处理输入
        inputs = self.processor(
            text=[prompt],
            padding=True,
            return_tensors="pt"
        ).to(self.device)

        # 2. 计算 token 长度 (MusicGen 每 token 约 0.02 秒对应 50Hz)
        max_new_tokens = int(duration * 50)
        max_new_tokens = max(max_new_tokens, 256)  # 最少生成 5 秒

        # 3. 推理
        with torch.no_grad():
            audio_values = self.model.generate(
                **inputs,
                max_new_tokens=max_new_tokens,
                do_sample=True,
                guidance_scale=guidance_scale,
                temperature=temperature,
            )

        # 4. 保存为 WAV
        audio = audio_values[0, 0].cpu().numpy()
        filename = f"{task_id}.wav"
        filepath = OUTPUT_DIR / filename
        scipy.io.wavfile.write(str(filepath), self.sample_rate, audio.astype(np.float32))

        actual_duration = len(audio) / self.sample_rate
        elapsed = time.time() - start_time

        print(f"[MusicGen] Generated {task_id}: {actual_duration:.1f}s in {elapsed:.1f}s | {prompt[:50]}")

        return {
            "taskId": task_id,
            "filename": filename,
            "duration": round(actual_duration, 1),
            "sampleRate": self.sample_rate,
            "prompt": prompt,
            "elapsed": round(elapsed, 1),
        }


# 全局单例
_model = None


def get_model():
    global _model
    if _model is None:
        _model = MusicGenModel()
    return _model
