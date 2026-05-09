"""
MusicGen REST API 服务
对接 easymusic-java 后端的 MusicCreateApi 接口

注意: MusicGen Small 仅生成纯器乐/背景音乐，不支持人声歌唱。
如需带人声的歌曲，应走天谱乐 API。
"""
from flask import Flask, request, jsonify, send_file
from model import get_model
import os

app = Flask(__name__)

print("Starting MusicGen server...")
model = get_model()

# 中文音乐描述 → 英文音乐术语映射，提升 MusicGen 生成质量
GENRE_MAP = {
    "流行": "pop",
    "摇滚": "rock",
    "电子": "electronic",
    "古典": "classical",
    "爵士": "jazz",
    "嘻哈": "hip-hop",
    "R&B": "R&B",
    "民谣": "folk",
    "乡村": "country",
    "金属": "metal",
    "雷鬼": "reggae",
    "放克": "funk",
    "古风": "traditional Chinese folk",
    "国风": "Chinese traditional",
    "轻音乐": "easy listening instrumental",
    "交响": "symphonic orchestral",
    "世界音乐": "world music",
}

EMOTION_MAP = {
    "放松": "relaxing calm peaceful",
    "快乐": "happy cheerful upbeat",
    "悲伤": "sad melancholic emotional",
    "浪漫": "romantic tender",
    "激昂": "epic powerful energetic",
    "安静": "quiet gentle ambient",
    "温暖": "warm cozy",
    "梦幻": "dreamy ethereal",
    "黑暗": "dark mysterious",
    "紧张": "tense suspenseful",
    "愤怒": "angry intense",
    "感动": "touching emotional",
    "欢快": "lively joyful bright",
    "忧愁": "melancholic wistful",
    "空灵": "ethereal atmospheric",
}

VOICE_STYLE = {
    "男声": "male vocal",
    "女声": "female vocal",
}


def build_music_prompt(prompt: str, duration: float, music_type: int,
                       music_gener: str = None, music_emotion: str = None,
                       music_sex: str = None) -> str:
    """将中文音乐描述拼接为 MusicGen 易于理解的英文 prompt。

    MusicGen Small 是纯器乐模型，不生成人声。voice 参数会被忽略并追加
    instrumental 提示，引导模型生成乐器旋律线来替代人声部分。
    """
    parts = []

    # 曲风
    if music_gener:
        gener_en = GENRE_MAP.get(music_gener, music_gener)
        parts.append(gener_en)

    # 情绪
    if music_emotion:
        emotions = music_emotion.split(",")
        emotion_parts = []
        for e in emotions:
            e = e.strip()
            emotion_parts.append(EMOTION_MAP.get(e, e))
        parts.append(", ".join(emotion_parts))

    # 用户原始描述
    if prompt and prompt.strip():
        parts.append(prompt.strip())

    # MusicGen 是纯器乐模型，追加器乐引导
    if not any(w in str(parts).lower() for w in ["instrumental", "orchestral", "symphony"]):
        parts.append("instrumental music")
    else:
        parts.append("instrumental")

    return ", ".join(parts)


@app.route("/api/health", methods=["GET", "POST"])
def health():
    return jsonify({"status": "ok", "model": "musicgen-small"})


@app.route("/api/generate", methods=["POST"])
def generate():
    """生成音乐
    POST JSON: {
        "prompt": "...",
        "duration": 15,
        "guidance_scale": 3.0,
        "musicType": 0,
        "musicGener": "流行",
        "musicEmotion": "放松,快乐",
        "musicSex": "女声"
    }
    """
    data = request.get_json(force=True, silent=True) or {}
    prompt = data.get("prompt", "").strip()

    duration = float(data.get("duration", 15))
    duration = max(10, min(duration, 30))

    guidance = float(data.get("guidance_scale", 3.0))
    guidance = max(1.0, min(guidance, 10.0))

    temperature = float(data.get("temperature", 1.0))
    music_type = int(data.get("musicType", 1))

    # 构建 MusicGen 友好的英文 prompt
    music_prompt = build_music_prompt(
        prompt=prompt,
        duration=duration,
        music_type=music_type,
        music_gener=data.get("musicGener"),
        music_emotion=data.get("musicEmotion"),
        music_sex=data.get("musicSex"),
    )

    if not music_prompt:
        return jsonify({"error": "prompt is required"}), 400

    try:
        result = model.generate(
            prompt=music_prompt,
            duration=duration,
            guidance_scale=guidance,
            temperature=temperature,
        )
        return jsonify({
            "createSuccess": True,
            "taskId": result["taskId"],
            "title": (prompt or music_prompt)[:20],
            "duration": result["duration"],
            "audioUrl": f"/api/audio/{result['filename']}",
            "elapsed": result["elapsed"],
            "promptUsed": music_prompt,
        })
    except Exception as e:
        return jsonify({
            "createSuccess": False,
            "error": str(e),
        }), 500


@app.route("/api/audio/<filename>", methods=["GET"])
def audio(filename):
    filepath = os.path.join(os.path.dirname(__file__), "output", filename)
    if not os.path.exists(filepath):
        return jsonify({"error": "file not found"}), 404
    return send_file(filepath, mimetype="audio/wav")


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8092, debug=False)
