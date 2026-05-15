import base64
import cv2
import numpy as np
import logging
import hyperlpr3 as lpr3
from flask import Flask, request, jsonify

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

catcher = lpr3.LicensePlateCatcher()
logger.info("hyperlpr3 initialized successfully")


@app.route("/recognize", methods=["POST"])
def recognize():
    data = request.get_json(silent=True)
    if not data or "image" not in data:
        return jsonify({"code": 400, "message": "缺少image字段"}), 400

    b64_str = data["image"]

    try:
        img_bytes = base64.b64decode(b64_str)
        img_array = np.frombuffer(img_bytes, dtype=np.uint8)
        img = cv2.imdecode(img_array, cv2.IMREAD_COLOR)

        if img is None:
            return jsonify({"code": 400, "message": "图片解码失败"}), 400
    except Exception as e:
        logger.error(f"Base64解码失败: {e}")
        return jsonify({"code": 400, "message": "Base64解码失败"}), 400

    try:
        results = catcher(img)
    except Exception as e:
        logger.error(f"Catcher调用异常: {e}")
        return jsonify({"code": 404, "message": "未检测到车牌"}), 404

    if not results:
        logger.info("未检测到车牌")
        return jsonify({"code": 404, "message": "未检测到车牌"}), 404

    best = max(results, key=lambda r: r[1])
    plate, confidence, *_ = best
    logger.info(f"识别成功: {plate} (置信度: {confidence})")
    return jsonify({
        "code": 200,
        "plate": str(plate),
        "confidence": round(float(confidence), 4)
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
