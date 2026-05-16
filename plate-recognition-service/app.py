import base64
import cv2
import numpy as np
import logging
import hyperlpr3 as lpr3
from flask import Flask, request, jsonify

from graph_model import GraphModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# CORS: allow frontend calls from any origin
@app.after_request
def add_cors(response):
    response.headers["Access-Control-Allow-Origin"] = "*"
    response.headers["Access-Control-Allow-Headers"] = "Content-Type"
    response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS"
    return response

catcher = lpr3.LicensePlateCatcher()
logger.info("hyperlpr3 initialized successfully")

# ----------------------------------------------------------------
#  Load graph model at startup
# ----------------------------------------------------------------
graph = GraphModel()
try:
    graph.load_from_db()
    logger.info("Graph model loaded, vertices=%d", len(graph.vertices))
except Exception as e:
    logger.error("Failed to load graph model: %s", e)
# ----------------------------------------------------------------


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


@app.route("/api/path-plan", methods=["POST"])
def path_plan():
    """
    Path planning endpoint.

    Request JSON:
        { "target_x": float, "target_y": float }

    Response:
        {
            "code": 200,
            "data": {
                "path": [{"x": ..., "y": ...}, ...],
                "total_distance": float,
                "entry": {"x": ..., "y": ...},
                "target_road": {"x": ..., "y": ...}
            }
        }
    """
    data = request.get_json(silent=True)
    if not data or "target_x" not in data or "target_y" not in data:
        return jsonify({"code": 400, "message": "缺少target_x或target_y"}), 400

    try:
        tx = float(data["target_x"])
        ty = float(data["target_y"])
    except (TypeError, ValueError):
        return jsonify({"code": 400, "message": "坐标格式错误"}), 400

    if not graph.vertices:
        return jsonify({"code": 500, "message": "图模型未加载"}), 500

    result = graph.plan_path_to(tx, ty)
    if "error" in result:
        return jsonify({"code": 404, "message": result["error"]}), 404

    return jsonify({
        "code": 200,
        "data": {
            "path": result["path"],
            "total_distance": result["total_distance"],
            "entry": result["entry"],
            "target_road": result["target_road"],
        }
    })


@app.route("/api/path-plan/by-vertex", methods=["POST"])
def path_plan_by_vertex():
    """
    Path planning by vertex IDs.

    Request JSON:
        { "start_vertex_id": int, "end_vertex_id": int }

    Response:
        { "code": 200, "data": { "path": [...], "total_distance": float } }
    """
    data = request.get_json(silent=True)
    if not data or "start_vertex_id" not in data or "end_vertex_id" not in data:
        return jsonify({"code": 400, "message": "缺少start_vertex_id或end_vertex_id"}), 400

    try:
        sid = int(data["start_vertex_id"])
        eid = int(data["end_vertex_id"])
    except (TypeError, ValueError):
        return jsonify({"code": 400, "message": "顶点ID格式错误"}), 400

    if not graph.vertices:
        return jsonify({"code": 500, "message": "图模型未加载"}), 500

    path_ids, total_dist = graph.astar(sid, eid)
    if not path_ids:
        return jsonify({"code": 404, "message": "无法规划路径"}), 404

    coords = graph.path_to_coords(path_ids)
    return jsonify({
        "code": 200,
        "data": {
            "path": coords,
            "path_vertex_ids": path_ids,
            "total_distance": round(total_dist, 2),
        }
    })


@app.route("/api/graph-info", methods=["GET"])
def graph_info():
    """Return summary info about the loaded graph model."""
    if not graph.vertices:
        return jsonify({"code": 500, "message": "图模型未加载"}), 500

    entry = graph.find_entry_vertex()
    exit_v = graph.find_exit_vertex()
    elevators = graph.find_elevator_vertices()
    road_count = sum(1 for v in graph.vertices.values() if v["type"] == "ROAD")
    edge_count = sum(len(n) for n in graph.adj.values()) // 2

    return jsonify({
        "code": 200,
        "data": {
            "total_vertices": len(graph.vertices),
            "total_edges": edge_count,
            "entry": entry,
            "exit": exit_v,
            "elevators": elevators,
            "road_vertices": road_count,
        }
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
