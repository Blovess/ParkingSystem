package com.parking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.entity.GraphEdge;
import com.parking.entity.GraphVertex;
import com.parking.mapper.GraphEdgeMapper;
import com.parking.mapper.GraphVertexMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphService {

    @Autowired
    private GraphEdgeMapper graphEdgeMapper;

    @Autowired
    private GraphVertexMapper graphVertexMapper;

    /**
     * Dijkstra shortest path from startVertexId to endVertexId.
     * Returns ordered list of {x, y} coordinate maps, or empty list if no path.
     */
    public List<Map<String, Object>> shortestPath(Long startVertexId, Long endVertexId) {
        List<GraphEdge> allEdges = graphEdgeMapper.selectList(null);

        Map<Long, List<Map.Entry<Long, Double>>> adj = new HashMap<>();
        for (GraphEdge e : allEdges) {
            Long fv = e.getFromVertexId();
            Long tv = e.getToVertexId();
            Double w = e.getWeight();
            adj.computeIfAbsent(fv, k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(tv, w));
            adj.computeIfAbsent(tv, k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(fv, w));
        }

        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        for (Long v : adj.keySet()) dist.put(v, Double.MAX_VALUE);
        dist.put(startVertexId, 0.0);

        PriorityQueue<Map.Entry<Long, Double>> pq = new PriorityQueue<>(Comparator.comparing(Map.Entry::getValue));
        pq.add(new AbstractMap.SimpleEntry<>(startVertexId, 0.0));

        while (!pq.isEmpty()) {
            Map.Entry<Long, Double> cur = pq.poll();
            Long u = cur.getKey();
            if (visited.contains(u)) continue;
            visited.add(u);
            if (u.equals(endVertexId)) break;
            for (Map.Entry<Long, Double> nb : adj.getOrDefault(u, Collections.emptyList())) {
                Long v = nb.getKey();
                Double w = nb.getValue();
                if (visited.contains(v)) continue;
                double nd = dist.get(u) + w;
                if (nd < dist.get(v)) {
                    dist.put(v, nd);
                    prev.put(v, u);
                    pq.add(new AbstractMap.SimpleEntry<>(v, nd));
                }
            }
        }

        if (!dist.containsKey(endVertexId) || dist.get(endVertexId) == Double.MAX_VALUE) {
            return Collections.emptyList();
        }

        List<Long> pathIds = new ArrayList<>();
        Long cur = endVertexId;
        while (cur != null && !cur.equals(startVertexId)) {
            pathIds.add(cur);
            cur = prev.get(cur);
        }
        pathIds.add(startVertexId);
        Collections.reverse(pathIds);

        List<Map<String, Object>> route = new ArrayList<>();
        Map<Long, GraphVertex> vertexMap = new HashMap<>();
        graphVertexMapper.selectList(null).forEach(v -> vertexMap.put(v.getId(), v));

        for (Long id : pathIds) {
            GraphVertex v = vertexMap.get(id);
            if (v != null) {
                Map<String, Object> point = new HashMap<>();
                point.put("x", v.getX());
                point.put("y", v.getY());
                route.add(point);
            }
        }
        return route;
    }

    public Long findNearestRoadVertex(double x, double y) {
        LambdaQueryWrapper<GraphVertex> qw = new LambdaQueryWrapper<>();
        qw.eq(GraphVertex::getType, "ROAD");
        List<GraphVertex> roads = graphVertexMapper.selectList(qw);
        Long nearest = null;
        double minDist = Double.MAX_VALUE;
        for (GraphVertex v : roads) {
            double d = Math.pow(v.getX() - x, 2) + Math.pow(v.getY() - y, 2);
            if (d < minDist) {
                minDist = d;
                nearest = v.getId();
            }
        }
        return nearest;
    }

    public Long findEntryVertexId() {
        LambdaQueryWrapper<GraphVertex> qw = new LambdaQueryWrapper<>();
        qw.eq(GraphVertex::getType, "ENTRY").last("LIMIT 1");
        GraphVertex v = graphVertexMapper.selectOne(qw);
        return v != null ? v.getId() : null;
    }
}
