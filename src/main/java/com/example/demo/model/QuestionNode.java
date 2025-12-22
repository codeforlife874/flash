package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class QuestionNode extends DecisionNode {
    private String yesNodeId;
    private String noNodeId;

    public QuestionNode(String id, String text, String yesNodeId, String noNodeId) {
        super(id, text);
        this.yesNodeId = yesNodeId;
        this.noNodeId = noNodeId;
    }
}
