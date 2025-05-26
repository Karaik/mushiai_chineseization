package com.karaik.scripteditor.helper;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;

public class ScrollEventHandler {

    public static void installSmartScroll(ScrollPane scrollPane, double multiplier) {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            Node target = event.getTarget() instanceof Node ? (Node) event.getTarget() : null;
            Node currentNode = target;
            boolean isInsideTextArea = false;

            while (currentNode != null) {
                if (currentNode instanceof TextArea) {
                    isInsideTextArea = true;
                    break;
                }
                currentNode = currentNode.getParent();
            }

            if (isInsideTextArea) return;

            double adjustedDeltaY = event.getDeltaY() * multiplier;
            if (scrollPane.getContent() != null && scrollPane.getContent().getBoundsInLocal().getHeight() > 0) {
                double height = scrollPane.getContent().getBoundsInLocal().getHeight();
                double newVValue = scrollPane.getVvalue() - (adjustedDeltaY / height);
                scrollPane.setVvalue(Math.max(0, Math.min(1, newVValue)));
            }

            event.consume();
        });
    }
}
