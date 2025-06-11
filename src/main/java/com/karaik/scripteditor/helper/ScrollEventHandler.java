package com.karaik.scripteditor.helper;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;

import java.util.Set;

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

    /**
     * 为 ListView 安装智能滚动处理，允许通过 multiplier 控制滚动幅度。
     * 它会查找 ListView 内部的垂直滚动条并操作它。
     *
     * @param listView   要应用滚动的 ListView
     * @param multiplier 滚动幅度乘数
     */
    public static void installSmartScrollForListView(ListView<?> listView, double multiplier) {
        if (listView == null || multiplier == 1.0) { // 如果乘数为1，则使用默认滚动
            return;
        }

        listView.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0 || event.isConsumed()) {
                return;
            }

            Node target = event.getTarget() instanceof Node ? (Node) event.getTarget() : null;
            boolean allowListViewScroll = true;

            // 检查事件目标是否是 ListView 内部的 TextArea
            // 如果是，并且 TextArea 本身可以滚动，则让 TextArea 处理
            Node current = target;
            while (current != null && current != listView) {
                if (current instanceof TextArea) {
                    TextArea ta = (TextArea) current;
                    // 检查 TextArea 是否真的可以滚动 (内容超出其可视范围)
                    // 这是一个简化的检查，更精确的需要看 TextArea 内部的 ScrollPane (如果有)
                    Node taContent = ta.lookup(".content");
                    if (taContent != null && taContent.getBoundsInLocal().getHeight() > ta.getHeight() - ta.getPadding().getTop() - ta.getPadding().getBottom()) {
                        allowListViewScroll = false; // TextArea 自己滚动
                    }
                    break;
                }
                current = current.getParent();
            }

            if (!allowListViewScroll) {
                return; // 让 TextArea 处理自己的滚动
            }

            ScrollBar vBar = findVerticalScrollBarInternal(listView);

            if (vBar != null && vBar.isVisible() && vBar.getMin() < vBar.getMax()) {
                double delta = -event.getDeltaY(); // 反转符号，因为ScrollBar的value增加是向下
                double adjustedDelta = delta * multiplier;

                double currentValue = vBar.getValue();
                double min = vBar.getMin();
                double max = vBar.getMax();

                double newValue = currentValue + adjustedDelta;
                newValue = Math.max(min, Math.min(max, newValue));

                if (Math.abs(newValue - currentValue) > 0.001) {
                    vBar.setValue(newValue);
                }
                event.consume(); // 我们处理了滚动，消费事件
            }
        });
    }

    /**
     * 辅助方法：查找 ListView 内部的垂直滚动条。
     * 注意：依赖 JavaFX 内部实现。
     */
    private static ScrollBar findVerticalScrollBarInternal(ListView<?> listView) {
        Node virtualFlow = listView.lookup(".virtual-flow");
        if (virtualFlow != null) {
            Set<Node> scrollBars = virtualFlow.lookupAll(".scroll-bar");
            for (Node node : scrollBars) {
                if (node instanceof ScrollBar) {
                    ScrollBar sb = (ScrollBar) node;
                    if (sb.getOrientation() == Orientation.VERTICAL) {
                        return sb;
                    }
                }
            }
        }
        return null;
    }

}
