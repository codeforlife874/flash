package com.example.demo.controller;

import com.example.demo.model.DecisionNode;
import com.example.demo.model.QuestionNode;
import com.example.demo.model.SolutionNode;
import com.example.demo.service.TreeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class TroubleshooterController {

    private final TreeService treeService;

    public TroubleshooterController(TreeService treeService) {
        this.treeService = treeService;
    }

    /**
     * Main page
     */
    @GetMapping("/")
    public String index(HttpSession session, Model model) {

        // Get currently selected tree
        String activeTree =
                (String) session.getAttribute("activeTree");

        /*
         * If no tree has been selected yet, select the first
         * available tree automatically.
         */
        if (activeTree == null ||
                !treeService.getTreeRoots().containsKey(activeTree)) {

            if (treeService.getTreeRoots().isEmpty()) {
                return "index";
            }

            activeTree = treeService.getTreeRoots()
                    .keySet()
                    .iterator()
                    .next();

            session.setAttribute(
                    "activeTree",
                    activeTree
            );
        }

        // Get current question/solution
        String currentNodeId =
                (String) session.getAttribute("currentNodeId");

        /*
         * If the user has no current node, or the node doesn't
         * exist, start from the root of the selected tree.
         */
        if (currentNodeId == null ||
                treeService.getNode(currentNodeId) == null) {

            currentNodeId =
                    treeService.getTreeRoot(activeTree);

            session.setAttribute(
                    "currentNodeId",
                    currentNodeId
            );
        }

        DecisionNode node =
                treeService.getNode(currentNodeId);

        /*
         * Safety check in case something went wrong while
         * loading the tree.
         */
        if (node == null) {

            currentNodeId =
                    treeService.getTreeRoot(activeTree);

            session.setAttribute(
                    "currentNodeId",
                    currentNodeId
            );

            node = treeService.getNode(currentNodeId);
        }

        // Send current node to Thymeleaf
        model.addAttribute("node", node);

        // Tell HTML whether this is a question or solution
        model.addAttribute(
                "isQuestion",
                node instanceof QuestionNode
        );

        model.addAttribute(
                "isSolution",
                node instanceof SolutionNode
        );

        /*
         * Available troubleshooting trees
         *
         * Example:
         * 1
         * 2
         * 3
         * ...
         * 20
         */
        model.addAttribute(
                "availableTrees",
                treeService.getTreeRoots().keySet()
        );

        // Used by JavaScript autocomplete
        model.addAttribute(
                "availableTreesList",
                treeService.getTreeRoots().keySet()
        );

        /*
         * Map tree filename -> display name
         *
         * Example:
         * "1" -> "Troubleshooting Tree 1"
         */
        model.addAttribute(
        "availableTreesMap",
        treeService.getTreeTitles()
);

        // Tell HTML which tree is currently selected
        model.addAttribute(
                "activeTree",
                activeTree
        );

        return "index";
    }


    /**
     * Handle YES / NO decisions
     */
    @PostMapping("/decision")
    public String decision(
            HttpSession session,
            @RequestParam String decision) {

        String currentNodeId =
                (String) session.getAttribute("currentNodeId");

        DecisionNode node =
                treeService.getNode(currentNodeId);

        // Current node must be a question
        if (!(node instanceof QuestionNode)) {

            String activeTree =
                    (String) session.getAttribute("activeTree");

            String rootId =
                    treeService.getTreeRoot(activeTree);

            session.setAttribute(
                    "currentNodeId",
                    rootId
            );

            return "redirect:/";
        }

        QuestionNode question =
                (QuestionNode) node;

        String nextNodeId;

        if ("no".equalsIgnoreCase(decision)) {
            nextNodeId = question.getNoNodeId();
        } else {
            nextNodeId = question.getYesNodeId();
        }

        /*
         * If the next node exists, move to it.
         * Otherwise restart the current tree.
         */
        if (nextNodeId != null &&
                treeService.getNode(nextNodeId) != null) {

            session.setAttribute(
                    "currentNodeId",
                    nextNodeId
            );

        } else {

            String activeTree =
                    (String) session.getAttribute("activeTree");

            String rootId =
                    treeService.getTreeRoot(activeTree);

            session.setAttribute(
                    "currentNodeId",
                    rootId
            );
        }

        return "redirect:/";
    }


    /**
     * Select a different troubleshooting tree
     */
    @PostMapping("/selectTree")
    public String selectTree(
            HttpSession session,
            @RequestParam String treeName) {

        String rootId =
                treeService.getTreeRoot(treeName);

        /*
         * Only switch if the requested tree actually exists.
         */
        if (rootId != null) {

            session.setAttribute(
                    "activeTree",
                    treeName
            );

            // Start from the root of the newly selected tree
            session.setAttribute(
                    "currentNodeId",
                    rootId
            );
        }

        return "redirect:/";
    }


    /**
     * Start a troubleshooting tree from the search/autocomplete
     */
    @PostMapping("/start")
    public String start(
            HttpSession session,
            @RequestParam String treeName) {

        String rootId =
                treeService.getTreeRoot(treeName);

        if (rootId != null) {

            session.setAttribute(
                    "activeTree",
                    treeName
            );

            session.setAttribute(
                    "currentNodeId",
                    rootId
            );
        }

        return "redirect:/";
    }


    /**
     * Admin: expand a solution into a new question
     */
    @PostMapping("/admin/expand")
    public String expand(
            HttpSession session,
            @RequestParam String targetLeafId,
            @RequestParam String newQuestionText,
            @RequestParam String yesSolText,
            @RequestParam String noSolText) {

        boolean success =
                treeService.expandLeaf(
                        targetLeafId,
                        newQuestionText,
                        yesSolText,
                        noSolText
                );

        if (success) {

            /*
             * The newly created question uses the same ID
             * as the previous solution.
             */
            session.setAttribute(
                    "currentNodeId",
                    targetLeafId
            );
        }

        return "redirect:/";
    }


    /**
     * Restart the current troubleshooting tree
     */
    @GetMapping("/restart")
    public String restart(HttpSession session) {

        String activeTree =
                (String) session.getAttribute("activeTree");

        String rootId =
                treeService.getTreeRoot(activeTree);

        if (rootId != null) {

            session.setAttribute(
                    "currentNodeId",
                    rootId
            );
        }

        return "redirect:/";
    }
}