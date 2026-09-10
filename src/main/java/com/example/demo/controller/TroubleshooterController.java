package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.DecisionNode;
import com.example.demo.model.QuestionNode;
import com.example.demo.model.SolutionNode;
import com.example.demo.service.TreeService;

import jakarta.servlet.http.HttpSession;

@Controller
public class TroubleshooterController {

    private final TreeService treeService;

    public TroubleshooterController(TreeService treeService) {
        this.treeService = treeService;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        Boolean started = (Boolean) session.getAttribute("started");
        if (started == null || !started) {
            model.addAttribute("isStarted", false);
            model.addAttribute("availableTrees", treeService.getAvailableTreeNames());
            model.addAttribute("availableTreesMap", treeService.getAvailableTreeLabels());
            model.addAttribute("availableTreesList", treeService.getAvailableTreeNames());
            model.addAttribute("activeTree", treeService.getActiveTreeName());
            return "index";
        }

        String current = (String) session.getAttribute("currentNodeId");
        if (current == null || treeService.getNode(current) == null) {
            session.setAttribute("currentNodeId", treeService.getRootNodeId());
            current = treeService.getRootNodeId();
        }

        DecisionNode node = treeService.getNode(current);
        if (node == null) {
            session.setAttribute("currentNodeId", treeService.getRootNodeId());
            node = treeService.getNode(treeService.getRootNodeId());
        }

        model.addAttribute("isStarted", true);
        model.addAttribute("node", node);
        model.addAttribute("isQuestion", node instanceof QuestionNode);
        model.addAttribute("isSolution", node instanceof SolutionNode);
        model.addAttribute("activeTree", treeService.getActiveTreeName());
        model.addAttribute("availableTrees", treeService.getAvailableTreeNames());
        model.addAttribute("availableTreesMap", treeService.getAvailableTreeLabels());
        model.addAttribute("availableTreesList", treeService.getAvailableTreeNames());
        return "index";
    }

    @PostMapping("/selectTree")
    public String selectTree(@RequestParam String treeName, HttpSession session) {
        boolean ok = treeService.selectTree(treeName);
        if (ok) {
            session.setAttribute("currentNodeId", treeService.getRootNodeId());
        }
        return "redirect:/";
    }

    @PostMapping("/start")
    public String start(@RequestParam String treeName, HttpSession session) {
        boolean ok = treeService.selectTree(treeName);
        if (ok) {
            session.setAttribute("started", true);
            session.setAttribute("currentNodeId", treeService.getRootNodeId());
        }
        return "redirect:/";
    }

    @PostMapping("/decision")
    public String decision(HttpSession session, @RequestParam String decision) {
        String current = (String) session.getAttribute("currentNodeId");
        DecisionNode node = treeService.getNode(current);
        if (!(node instanceof QuestionNode)) {
            // reset to root
            session.setAttribute("currentNodeId", treeService.getRootNodeId());
            return "redirect:/";
        }

        QuestionNode q = (QuestionNode) node;
        String nextId = "no".equalsIgnoreCase(decision) ? q.getNoNodeId() : q.getYesNodeId();
        if (treeService.getNode(nextId) == null) {
            session.setAttribute("currentNodeId", treeService.getRootNodeId());
        } else {
            session.setAttribute("currentNodeId", nextId);
        }

        return "redirect:/";
    }

    @PostMapping("/admin/expand")
    public String expand(HttpSession session,
                         @RequestParam String targetLeafId,
                         @RequestParam String newQuestionText,
                         @RequestParam String yesSolText,
                         @RequestParam String noSolText) {
        boolean ok = treeService.expandLeaf(targetLeafId, newQuestionText, yesSolText, noSolText);
        if (ok) {
            // set session to the newly created question (same id)
            session.setAttribute("currentNodeId", targetLeafId);
        }
        return "redirect:/";
    }

    @GetMapping("/restart")
    public String restart(HttpSession session) {
        session.setAttribute("currentNodeId", treeService.getRootNodeId());
        return "redirect:/";
    }
}
