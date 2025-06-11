package com.karaik.scripteditor.ui;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class SptEntryListCell extends ListCell<SptEntry> {

    private SptEntryNode viewNode;
    private Runnable onModifiedCallback;

    private final EventHandler<MouseEvent> mousePressedHandler = event -> {
        if (event.isConsumed()) {
            return;
        }
        Node target = (Node) event.getTarget();
        boolean allowEventThrough = false;
        Node current = target;
        while (current != null && current != this) {
            if (current instanceof Button ||
                    current instanceof TextArea ||
                    current instanceof ChoiceBox ||
                    current instanceof ComboBox ||
                    current instanceof CheckBox ||
                    current instanceof TextField ||
                    (current.getOnMouseClicked() != null || current.getOnMousePressed() != null || current.getOnMouseReleased() != null)) {
                allowEventThrough = true;
                break;
            }
            current = current.getParent();
        }
        if (!allowEventThrough) {
            event.consume();
            this.requestFocus();
        }
    };

    public SptEntryListCell(Runnable onModifiedCallback) {
        this.onModifiedCallback = onModifiedCallback;
        this.addEventFilter(MouseEvent.MOUSE_PRESSED, mousePressedHandler);
    }

    @Override
    protected void updateItem(SptEntry entry, boolean empty) {
        super.updateItem(entry, empty);
        if (empty || entry == null) {
            setText(null);
            if (viewNode != null) {
                viewNode.updateData(null, null);
            }
            setGraphic(null);
        } else {
            if (viewNode == null) {
                viewNode = new SptEntryNode(entry, onModifiedCallback);
            } else {
                viewNode.updateData(entry, onModifiedCallback);
            }
            setGraphic(viewNode);
        }
    }
}