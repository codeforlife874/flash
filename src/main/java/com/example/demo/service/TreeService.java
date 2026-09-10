package com.example.demo.service;

import com.example.demo.model.DecisionNode;
import com.example.demo.model.QuestionNode;
import com.example.demo.model.SolutionNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TreeService {

    /*
     * Stores every question/solution node from all trees.
     *
     * Example:
     *
     * 1-abc123 -> QuestionNode
     * 1-def456 -> SolutionNode
     * 2-xyz789 -> QuestionNode
     */
    private final ConcurrentHashMap<String, DecisionNode> nodeRegistry =
            new ConcurrentHashMap<>();


    /*
     * Tree name -> root node ID
     *
     * Example:
     *
     * "1" -> "1-abc123"
     * "2" -> "2-xyz789"
     */
    private final Map<String, String> treeRoots =
            new LinkedHashMap<>();


    /*
     * Tree name -> user-friendly title
     *
     * Example:
     *
     * "1" -> "Is the router powered on?"
     * "2" -> "Is the printer turning on?"
     */
    private final Map<String, String> treeTitles =
            new LinkedHashMap<>();


    private final ObjectMapper mapper =
            new ObjectMapper();


    /**
     * Returns all nodes.
     */
    public ConcurrentHashMap<String, DecisionNode> getNodeRegistry() {
        return nodeRegistry;
    }


    /**
     * Returns the root node of the first loaded tree.
     */
    public String getRootNodeId() {

        if (treeRoots.isEmpty()) {
            return null;
        }

        return treeRoots.values()
                .iterator()
                .next();
    }


    /**
     * Returns:
     *
     * tree name -> root node ID
     */
    public Map<String, String> getTreeRoots() {
        return Collections.unmodifiableMap(treeRoots);
    }


    /**
     * Returns:
     *
     * tree name -> user-friendly title
     */
    public Map<String, String> getTreeTitles() {
        return Collections.unmodifiableMap(treeTitles);
    }


    /**
     * Load all JSON troubleshooting trees from:
     *
     * src/main/resources/data/*.json
     */
    @PostConstruct
    public void init() throws IOException {

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver();


        Resource[] resources =
                resolver.getResources(
                        "classpath*:data/*.json"
                );


        /*
         * Sort files numerically.
         *
         * Without this:
         *
         * 1.json
         * 10.json
         * 11.json
         * 2.json
         *
         * With this:
         *
         * 1.json
         * 2.json
         * 3.json
         * ...
         * 20.json
         */
        Arrays.sort(
                resources,
                Comparator.comparingInt(resource -> {

                    String filename =
                            resource.getFilename();

                    if (filename == null) {
                        return Integer.MAX_VALUE;
                    }

                    try {

                        return Integer.parseInt(
                                filename.replace(
                                        ".json",
                                        ""
                                )
                        );

                    } catch (NumberFormatException e) {

                        return Integer.MAX_VALUE;
                    }
                })
        );


        /*
         * Load every JSON file.
         */
        for (Resource resource : resources) {

            String filename =
                    resource.getFilename();


            if (filename == null ||
                    !filename.endsWith(".json")) {

                continue;
            }


            /*
             * Example:
             *
             * 1.json -> 1
             * 2.json -> 2
             */
            String treeName =
                    filename.substring(
                            0,
                            filename.length() - 5
                    );


            /*
             * Read the nested JSON.
             */
            JsonNode rootJson =
                    mapper.readTree(
                            resource.getInputStream()
                    );


            if (rootJson == null ||
                    rootJson.isEmpty()) {

                continue;
            }


            /*
             * Convert the nested JSON into
             * QuestionNode / SolutionNode objects.
             */
            String rootId =
                    buildTree(
                            treeName,
                            rootJson
                    );


            /*
             * Store the root ID.
             */
            treeRoots.put(
                    treeName,
                    rootId
            );


            /*
             * Use the root question as the
             * default title.
             *
             * Example:
             *
             * JSON:
             *
             * {
             *   "text": "Is the router powered on?",
             *   ...
             * }
             *
             * Dropdown:
             *
             * Is the router powered on?
             */
            String title =
                    rootJson
                            .path("text")
                            .asText(
                                    "Troubleshooting Problem"
                            );


            treeTitles.put(
                    treeName,
                    title
            );
        }


        System.out.println(
                "======================================"
        );

        System.out.println(
                "Loaded troubleshooting trees: " +
                treeRoots.keySet()
        );

        System.out.println(
                "Loaded tree titles: " +
                treeTitles
        );

        System.out.println(
                "Total loaded nodes: " +
                nodeRegistry.size()
        );

        System.out.println(
                "======================================"
        );
    }


    /**
     * Converts the nested JSON structure into
     * QuestionNode and SolutionNode objects.
     *
     * Example JSON:
     *
     * {
     *   "text": "Is the router powered on?",
     *
     *   "yesChild": {
     *       "text": "Is the power LED stable?"
     *   },
     *
     *   "noChild": {
     *       "text": "Turn router ON."
     *   }
     * }
     *
     * becomes:
     *
     * QuestionNode
     *      |
     *      +---- YES -> QuestionNode
     *      |
     *      +---- NO  -> SolutionNode
     */
    private String buildTree(
            String treeName,
            JsonNode jsonNode) {


        /*
         * Give every node a unique ID.
         */
        String id =
                treeName +
                "-" +
                UUID.randomUUID();


        /*
         * Get question/solution text.
         */
        String text =
                jsonNode
                        .path("text")
                        .asText("");


        /*
         * Check whether this node has children.
         */
        boolean hasYesChild =
                jsonNode.has("yesChild");


        boolean hasNoChild =
                jsonNode.has("noChild");


        /*
         * No children means this is a solution.
         */
        if (!hasYesChild &&
                !hasNoChild) {

            SolutionNode solution =
                    new SolutionNode(
                            id,
                            text
                    );


            nodeRegistry.put(
                    id,
                    solution
            );


            return id;
        }


        /*
         * Otherwise this is a question.
         */
        String yesId = null;

        String noId = null;


        /*
         * Build YES branch.
         */
        if (hasYesChild) {

            yesId =
                    buildTree(
                            treeName,
                            jsonNode.get("yesChild")
                    );
        }


        /*
         * Build NO branch.
         */
        if (hasNoChild) {

            noId =
                    buildTree(
                            treeName,
                            jsonNode.get("noChild")
                    );
        }


        /*
         * Create the question node.
         */
        QuestionNode question =
                new QuestionNode(
                        id,
                        text,
                        yesId,
                        noId
                );


        /*
         * Store it.
         */
        nodeRegistry.put(
                id,
                question
        );


        return id;
    }


    /**
     * Find a node by ID.
     */
    public DecisionNode getNode(
            String id) {

        if (id == null) {
            return null;
        }

        return nodeRegistry.get(id);
    }


    /**
     * Get the root node ID for a specific tree.
     *
     * Example:
     *
     * getTreeRoot("1")
     */
    public String getTreeRoot(
            String treeName) {

        if (treeName == null) {
            return null;
        }

        return treeRoots.get(treeName);
    }


    /**
     * Get the user-friendly title of a tree.
     */
    public String getTreeTitle(
            String treeName) {

        return treeTitles.get(treeName);
    }


    /**
     * Expand a solution into a new question.
     *
     * IMPORTANT:
     *
     * This currently changes the tree only in memory.
     * It does NOT save the modification back to JSON.
     */
    public synchronized boolean expandLeaf(
            String targetLeafId,
            String newQuestionText,
            String yesSolText,
            String noSolText) {


        /*
         * Find the existing solution.
         */
        DecisionNode target =
                nodeRegistry.get(
                        targetLeafId
                );


        /*
         * Make sure it really is a solution.
         */
        if (target == null ||
                !(target instanceof SolutionNode)) {

            return false;
        }


        /*
         * Create YES solution.
         */
        String yesId =
                UUID.randomUUID().toString();


        SolutionNode yesNode =
                new SolutionNode(
                        yesId,
                        yesSolText
                );


        /*
         * Create NO solution.
         */
        String noId =
                UUID.randomUUID().toString();


        SolutionNode noNode =
                new SolutionNode(
                        noId,
                        noSolText
                );


        /*
         * Replace the old solution with
         * a new question.
         *
         * The ID remains the same so that
         * the parent doesn't need to change.
         */
        QuestionNode questionNode =
                new QuestionNode(
                        targetLeafId,
                        newQuestionText,
                        yesId,
                        noId
                );


        /*
         * Add the new nodes.
         */
        nodeRegistry.put(
                yesId,
                yesNode
        );


        nodeRegistry.put(
                noId,
                noNode
        );


        /*
         * Replace the solution.
         */
        nodeRegistry.put(
                targetLeafId,
                questionNode
        );


        return true;
    }
}