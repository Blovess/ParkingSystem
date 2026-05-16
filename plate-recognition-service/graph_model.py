"""
Parking lot graph model with A* pathfinding.
Loads graph_vertex / graph_edge from MySQL, builds adjacency list,
and provides A* shortest-path queries.
"""

import math
import heapq
import logging

import pymysql

logger = logging.getLogger(__name__)


class GraphModel:
    """Loaded once from DB, then used for repeated path-plan queries."""

    def __init__(self):
        self.vertices = {}      # id -> dict
        self.adj = {}           # id -> list of (neighbor_id, weight)

    # ----------------------------------------------------------------
    #  DB loader
    # ----------------------------------------------------------------
    def load_from_db(self, host="localhost", port=3306, user="root",
                     password="0322", database="parking_db"):
        conn = pymysql.connect(
            host=host, port=port, user=user, password=password,
            database=database, charset="utf8mb4",
        )
        try:
            with conn.cursor(pymysql.cursors.DictCursor) as cur:
                cur.execute("SELECT id, name, type, x, y FROM graph_vertex")
                for row in cur.fetchall():
                    vid = row["id"]
                    self.vertices[vid] = {
                        "id": vid,
                        "name": row["name"],
                        "type": row["type"],
                        "x": float(row["x"]),
                        "y": float(row["y"]),
                    }

                cur.execute(
                    "SELECT from_vertex_id, to_vertex_id, weight FROM graph_edge"
                )
                self.adj = {vid: [] for vid in self.vertices}
                for row in cur.fetchall():
                    f, t, w = row["from_vertex_id"], row["to_vertex_id"], float(row["weight"])
                    if f in self.adj and t in self.adj:
                        self.adj[f].append((t, w))
                        self.adj[t].append((f, w))

            logger.info(
                "Graph loaded: %d vertices, %d edge-directions (undirected)",
                len(self.vertices),
                sum(len(n) for n in self.adj.values()),
            )
        finally:
            conn.close()

    # ----------------------------------------------------------------
    #  Helpers
    # ----------------------------------------------------------------
    def find_nearest_vertex(self, x, y, vertex_type=None):
        """Return (id, distance) of the nearest vertex, optionally filtered by type."""
        best_id = None
        best_d2 = float("inf")
        for vid, v in self.vertices.items():
            if vertex_type and v["type"] != vertex_type:
                continue
            d2 = (v["x"] - x) ** 2 + (v["y"] - y) ** 2
            if d2 < best_d2:
                best_d2 = d2
                best_id = vid
        if best_id is None:
            return None, None
        return best_id, math.sqrt(best_d2)

    def find_entry_vertex(self):
        """Return the ENTRY vertex dict, or None."""
        for v in self.vertices.values():
            if v["type"] == "ENTRY":
                return v
        return None

    def find_exit_vertex(self):
        """Return the EXIT vertex dict, or None."""
        for v in self.vertices.values():
            if v["type"] == "EXIT":
                return v
        return None

    def find_elevator_vertices(self):
        """Return list of ELEVATOR1 / ELEVATOR2 vertex dicts."""
        return [
            v for v in self.vertices.values()
            if v["type"] in ("ELEVATOR1", "ELEVATOR2")
        ]

    @staticmethod
    def euclidean(a, b):
        return math.sqrt((a["x"] - b["x"]) ** 2 + (a["y"] - b["y"]) ** 2)

    def path_to_coords(self, path_ids):
        """Convert a list of vertex IDs into [{x, y}, ...]."""
        return [
            {"x": self.vertices[vid]["x"], "y": self.vertices[vid]["y"]}
            for vid in path_ids
            if vid in self.vertices
        ]

    # ----------------------------------------------------------------
    #  A*
    # ----------------------------------------------------------------
    def astar(self, start_id, end_id):
        """
        A* shortest path.

        Parameters
        ----------
        start_id : int
        end_id   : int

        Returns
        -------
        path : list[int]   — vertex IDs from start to end (inclusive)
        dist : float       — total path distance, or float('inf') if unreachable
        """
        if start_id not in self.vertices or end_id not in self.vertices:
            return [], float("inf")
        if start_id == end_id:
            return [start_id], 0.0

        goal = self.vertices[end_id]

        # g_score[node_id]
        g = {start_id: 0.0}
        # f = g + h
        h0 = self.euclidean(self.vertices[start_id], goal)
        f = {start_id: h0}

        prev = {}

        # priority queue: (f_score, node_id)
        open_set = [(f[start_id], start_id)]
        visited = set()

        while open_set:
            _, cur = heapq.heappop(open_set)
            if cur in visited:
                continue
            visited.add(cur)

            if cur == end_id:
                break

            for neighbor, weight in self.adj.get(cur, []):
                if neighbor in visited:
                    continue
                tentative_g = g[cur] + weight
                if tentative_g < g.get(neighbor, float("inf")):
                    prev[neighbor] = cur
                    g[neighbor] = tentative_g
                    h = self.euclidean(self.vertices[neighbor], goal)
                    f[neighbor] = tentative_g + h
                    heapq.heappush(open_set, (f[neighbor], neighbor))

        # Reconstruct
        if end_id not in g or g[end_id] == float("inf"):
            return [], float("inf")

        path = []
        cur = end_id
        while cur != start_id:
            path.append(cur)
            cur = prev[cur]
        path.append(start_id)
        path.reverse()
        return path, g[end_id]

    # ----------------------------------------------------------------
    #  High-level: plan path from entry to a target (x, y) coordinate
    # ----------------------------------------------------------------
    def plan_path_to(self, target_x, target_y):
        """
        Plan a path from the ENTRY vertex to the nearest road vertex
        near (target_x, target_y).

        Returns
        -------
        dict with keys: path (coords), total_distance, entry, target_road
        or error dict if path not found.
        """
        entry = self.find_entry_vertex()
        if entry is None:
            return {"error": "未找到入口顶点"}

        target_vid, _ = self.find_nearest_vertex(target_x, target_y, vertex_type="ROAD")
        if target_vid is None:
            return {"error": "目标附近无道路顶点"}

        path_ids, total_dist = self.astar(entry["id"], target_vid)
        if not path_ids:
            return {"error": "无法规划路径"}

        coords = self.path_to_coords(path_ids)
        return {
            "path": coords,
            "total_distance": round(total_dist, 2),
            "entry": {"x": entry["x"], "y": entry["y"]},
            "target_road": {"x": self.vertices[target_vid]["x"],
                            "y": self.vertices[target_vid]["y"]},
        }
