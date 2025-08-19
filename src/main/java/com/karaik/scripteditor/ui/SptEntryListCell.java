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
            // 如果 viewNode 存在，必须在 setGraphic(null) 之前或之后清理它，
            // 以断开它与上一个 SptEntry 的所有连接。
            if (viewNode != null) {
                viewNode.dispose();
            }
            setGraphic(null);
        } else {
            if (viewNode == null) {
                // 使用无参构造函数创建一次
                viewNode = new SptEntryNode();
            }
            // 每次都调用 updateData
            viewNode.updateData(entry, onModifiedCallback);
            setGraphic(viewNode);
        }
    }
}