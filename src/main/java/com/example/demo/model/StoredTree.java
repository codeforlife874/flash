package com.example.demo.model;

import lombok.Data;

import java.util.List;

@Data
public class StoredTree {
    private String rootId;
    private List<DecisionNode> nodes;
}
