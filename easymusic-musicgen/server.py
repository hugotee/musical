"""
MusicGen REST API 服务
对接 easymusic-java 后端的 MusicCreateApi 接口
"""
from flask import Flask, request, jsonify, send_file
from model import get_model
import os

app = Flask(__name__)

# 启动时加载模型
print("Starting MusicGen server...")
model = get_model()


@app.route("/api/health", methods=["GET", "POST"])
def health():
    return jsonify({"status": "ok", "model": "musicgen-small"})


@app.route("/api/generate", methods=["POST"])
def generate():
    """生成音乐
    POST JSON: { "prompt": "...", "duration": 15, "guidance_scale": 3.0 }
    """
    data = request.get_json(force=True, silent=True) or {}
    prompt = data.get("prompt", "").strip()
    if not prompt:
        return jsonify({"error": "prompt is required"}), 400

    duration = float(data.get("duration", 15))
    duration = max(10, min(duration, 30))  # 限制 10-30 秒

    guidance = float(data.get("guidance_scale", 3.0))
    guidance = max(1.0, min(guidance, 10.0))

    temperature = float(data.get("temperature", 1.0))

    try:
        result = model.generate(
            prompt=prompt,
            duration=duration,
            guidance_scale=guidance,
            temperature=temperature,
        )
        # 返回格式对齐 MusicCreateApi 的 MusicCreationResultDTO
        return jsonify({
            "createSuccess": True,
            "taskId": result["taskId"],
            "title": prompt[:20],
            "duration": result["duration"],
            "audioUrl": f"/api/audio/{result['filename']}",
            "elapsed": result["elapsed"],
        })
    except Exception as e:
        return jsonify({
            "createSuccess": False,
            "error": str(e),
        }), 500


@app.route("/api/audio/<filename>", methods=["GET"])
def audio(filename):
    """下载生成的音频文件"""
    filepath = os.path.join(os.path.dirname(__file__), "output", filename)
    if not os.path.exists(filepath):
        return jsonify({"error": "file not found"}), 404
    return send_file(filepath, mimetype="audio/wav")


if __name__ == "__main__":
    # 启动 Flask 服务，端口 8092
    app.run(host="0.0.0.0", port=8092, debug=False)
