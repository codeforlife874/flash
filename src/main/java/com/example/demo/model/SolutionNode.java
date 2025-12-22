package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class SolutionNode extends DecisionNode {
    public SolutionNode(String id, String text) {
        super(id, text);
    }
}
