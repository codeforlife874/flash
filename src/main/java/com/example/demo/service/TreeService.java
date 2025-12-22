package com.example.demo.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.example.demo.model.DecisionNode;
import com.example.demo.model.QuestionNode;
import com.example.demo.model.SolutionNode;
import com.example.demo.model.StoredTree;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class TreeService {

    private final ConcurrentHashMap<String, DecisionNode> nodeRegistry = new ConcurrentHashMap<>();
    private String rootNodeId;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, StoredTree> storedTrees = new LinkedHashMap<>();
    private String activeTreeName;

    public ConcurrentHashMap<String, DecisionNode> getNodeRegistry() {
        return nodeRegistry;
    }

    public String getRootNodeId() {
        return rootNodeId;
    }

    @PostConstruct
    public void init() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:data/*.json");

        for (Resource res : resources) {
            try {
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(res.getInputStream());
                StoredTree stored = null;

                // If file already in StoredTree shape
                if (root.has("rootId") && root.has("nodes")) {
                    stored = mapper.treeToValue(root, StoredTree.class);
                } else {
                    // attempt to convert nested yesChild/noChild tree into StoredTree
                    stored = convertNestedTree(root);
                }

                if (stored != null) {
                    String name = res.getFilename();
                    storedTrees.put(name, stored);
                }
            } catch (Exception ex) {
                // ignore malformed files for now
            }
        }

        // fallback to single tree.json if no folder-based trees found
        if (storedTrees.isEmpty()) {
            ClassPathResource r = new ClassPathResource("tree.json");
            if (r.exists()) {
                StoredTree stored = mapper.readValue(r.getInputStream(), StoredTree.class);
                if (stored != null) {
                    storedTrees.put("tree.json", stored);
                }
            }
        }

        // choose first available tree as active
        Optional<String> first = storedTrees.keySet().stream().findFirst();
        if (first.isPresent()) {
            selectTree(first.get());
        }
    }

    /**
     * Selects a loaded tree by resource filename (e.g. mytree.json) and rebuilds the node registry.
     */
    public synchronized boolean selectTree(String treeResourceName) {
        StoredTree stored = storedTrees.get(treeResourceName);
        if (stored == null) return false;
        // clear existing registry
        nodeRegistry.clear();
        rootNodeId = stored.getRootId();

        Map<String, DecisionNode> temp = new HashMap<>();
        if (stored.getNodes() != null) {
            for (DecisionNode n : stored.getNodes()) {
                temp.put(n.getId(), n);
            }
        }

        Set<String> visited = new HashSet<>();
        traverseAndRegister(rootNodeId, temp, visited);
        activeTreeName = treeResourceName;
        return true;
    }

    private StoredTree convertNestedTree(com.fasterxml.jackson.databind.JsonNode root) {
        if (root == null || !root.has("text")) return null;

        Map<String, DecisionNode> temp = new HashMap<>();

        // recursive builder returns id of created node
        java.util.function.BiFunction<com.fasterxml.jackson.databind.JsonNode, Map<String, DecisionNode>, String> build = new java.util.function.BiFunction<>() {
            @Override
            public String apply(com.fasterxml.jackson.databind.JsonNode node, Map<String, DecisionNode> map) {
                String id = UUID.randomUUID().toString();
                String text = node.has("text") ? node.get("text").asText() : "";
                // leaf if no yesChild and noChild
                if (!node.has("yesChild") && !node.has("noChild")) {
                    SolutionNode s = new SolutionNode(id, text);
                    map.put(id, s);
                    return id;
                }

                String yesId = null;
                String noId = null;
                if (node.has("yesChild")) yesId = this.apply(node.get("yesChild"), map);
                if (node.has("noChild")) noId = this.apply(node.get("noChild"), map);

                QuestionNode q = new QuestionNode(id, text, yesId, noId);
                map.put(id, q);
                return id;
            }
        };

        String rootId = build.apply(root, temp);

        StoredTree st = new StoredTree();
        st.setRootId(rootId);
        st.setNodes(new ArrayList<>(temp.values()));
        return st;
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

    public String getActiveTreeName() {
        return activeTreeName;
    }

    public Set<String> getAvailableTreeNames() {
        return Collections.unmodifiableSet(storedTrees.keySet());
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
