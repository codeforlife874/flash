package com.example.demo.service;

import com.example.demo.model.DecisionNode;
import com.example.demo.model.QuestionNode;
import com.example.demo.model.SolutionNode;
import com.example.demo.model.StoredTree;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TreeService {

    private final ConcurrentHashMap<String, DecisionNode> nodeRegistry = new ConcurrentHashMap<>();
    private String rootNodeId;
    private final ObjectMapper mapper = new ObjectMapper();

    public ConcurrentHashMap<String, DecisionNode> getNodeRegistry() {
        return nodeRegistry;
    }

    public String getRootNodeId() {
        return rootNodeId;
    }

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource r = new ClassPathResource("tree.json");
        StoredTree stored = mapper.readValue(r.getInputStream(), StoredTree.class);
        if (stored == null) return;
        rootNodeId = stored.getRootId();

        Map<String, DecisionNode> temp = new HashMap<>();
        if (stored.getNodes() != null) {
            for (DecisionNode n : stored.getNodes()) {
                temp.put(n.getId(), n);
            }
        }

        // recursively traverse from root and populate registry
        Set<String> visited = new HashSet<>();
        traverseAndRegister(rootNodeId, temp, visited);
    }

    private void traverseAndRegister(String nodeId, Map<String, DecisionNode> temp, Set<String> visited) {
        if (nodeId == null || visited.contains(nodeId)) return;
        DecisionNode node = temp.get(nodeId);
        if (node == null) return;
        visited.add(nodeId);
        nodeRegistry.put(nodeId, node);
        if (node instanceof QuestionNode) {
            QuestionNode q = (QuestionNode) node;
            traverseAndRegister(q.getYesNodeId(), temp, visited);
            traverseAndRegister(q.getNoNodeId(), temp, visited);
        }
    }

    public DecisionNode getNode(String id) {
        return nodeRegistry.get(id);
    }

    public synchronized boolean expandLeaf(String targetLeafId, String newQuestionText, String yesSolText, String noSolText) {
        DecisionNode target = nodeRegistry.get(targetLeafId);
        if (target == null || !(target instanceof SolutionNode)) {
            return false;
        }

        // create new solution nodes
        String yesId = UUID.randomUUID().toString();
        String noId = UUID.randomUUID().toString();
        SolutionNode yesNode = new SolutionNode(yesId, yesSolText);
        SolutionNode noNode = new SolutionNode(noId, noSolText);

        // create new question node with same id as the target leaf
        QuestionNode questionNode = new QuestionNode(targetLeafId, newQuestionText, yesId, noId);

        // atomic-ish update
        nodeRegistry.put(yesId, yesNode);
        nodeRegistry.put(noId, noNode);
        nodeRegistry.put(targetLeafId, questionNode);

        return true;
    }
}
