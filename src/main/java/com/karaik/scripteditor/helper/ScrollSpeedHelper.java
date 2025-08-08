package com.karaik.scripteditor.helper;

import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;

/**
 * 调整 ListView 鼠标滚轮滚动速度的辅助工具类 (Helper)。
 * 本方案将一次鼠标滚轮事件转换为一次或多次【按条目】的滚动。
 * 它使用 ListView 的公共 API scrollTo()，是目前最稳定可靠的方案。
 */
public final class ScrollSpeedHelper {

    /*
     * 为什么需要这个值？
     * JavaFX 的 ScrollEvent 没有提供一个公共API来明确区分事件源是“鼠标滚轮”还是“触摸板”。
     * 但是，这两种设备产生的滚动事件在 `deltaY` 的值上存在数量级的差异，这为我们提供了
     * 一种可靠的、基于行为的区分方法。
     *
     * 原理：
     * 1. 鼠标滚轮 (Mouse Wheel): 滚动是离散的。每滚动一“格”(tick)，系统会生成一个
     *    `ScrollEvent`，其 `deltaY` 通常是一个固定的、相对较大的值。在大多数系统
     *    (如Windows)上，这个值通常是 40.0 或 -40.0 的倍数。它几乎总是远大于10。
     *
     * 2. 触摸板 (Trackpad): 滚动是连续的、像素级的。为了实现平滑滚动，触摸板会高频
     *    率地发送 `ScrollEvent`，但每一次的 `deltaY` 值都非常小，通常在 -5.0 到 5.0
     *    之间，很少会超过10。
     *
     * 为什么选择 20.0？
     * 这个值是一个“安全”的中间地带，它：
     *  - 足够大，可以有效地过滤掉几乎所有由触摸板产生的平滑滚动事件。
     *  - 足够小，可以确保捕获到几乎所有标准鼠标滚轮产生的滚动事件。
     *
     * 这是一个基于广泛经验和普遍观察得出的工程实践值，而非官方标准。
     * 它的可靠性非常高，足以应对绝大多数硬件和操作系统环境。
     */
    static final double MOUSE_WHEEL_DELTA_THRESHOLD = 20.0;

    private ScrollSpeedHelper() {}

    /**
     * 为指定的 ListView 安装滚动速度调整器。
     * @param listView   要应用调整的 ListView
     * @param multiplier 每次滚轮事件滚动的【条目数】。例如，1.0 表示一次滚动1个条目。
     */
    public static void install(ListView<?> listView, double multiplier) {
        if (listView == null) {
            return;
        }

        // 计算每次滚动多少个条目，至少为1
        final int itemsToScroll = Math.max(1, (int) Math.round(multiplier));

        listView.addEventFilter(ScrollEvent.SCROLL, event -> {
            // 1. 忽略触摸板的平滑滚动、水平滚动和已被处理的事件
            if (event.isInertia() || event.getDeltaY() == 0 || event.isConsumed()) {
                return;
            }

            if (event.isInertia() || Math.abs(event.getDeltaY()) < MOUSE_WHEEL_DELTA_THRESHOLD) {
                return;
            }

            // 2. 安全地检查事件目标，如果是在可滚动的 TextArea 内，则不处理
            EventTarget eventTarget = event.getTarget();
            if (eventTarget instanceof Node) {
                if (isTargetInsideScrollableTextArea((Node) eventTarget, listView)) {
                    return;
                }
            }

            // 3. 消费事件，由我们接管
            event.consume();

            // 4. 查找 VirtualFlow 以获取当前视图信息
            Node node = listView.lookup(".virtual-flow");
            if (!(node instanceof VirtualFlow)) {
                return; // 找不到 VirtualFlow，无法计算
            }
            VirtualFlow<?> flow = (VirtualFlow<?>) node;

            // 5. 如果没有可见的单元格，也无法计算
            if (flow.getFirstVisibleCell() == null) {
                return;
            }

            // 6. 获取当前可见的第一个条目的索引
            int firstVisibleIndex = flow.getFirstVisibleCell().getIndex();

            // 7. 根据滚动方向（向上/向下）计算目标索引
            // event.getDeltaY() > 0 是向上滚，索引应减小
            int direction = (event.getDeltaY() > 0) ? -1 : 1;
            int targetIndex = firstVisibleIndex + (direction * itemsToScroll);

            // 8. 确保目标索引在列表的有效范围内
            int maxIndex = listView.getItems().size() - 1;
            if (maxIndex < 0) return; // 列表为空
            targetIndex = Math.max(0, Math.min(targetIndex, maxIndex));

            // 9. 使用稳定、公开的 API 执行滚动
            listView.scrollTo(targetIndex);
        });
    }

    private static boolean isTargetInsideScrollableTextArea(Node target, ListView<?> listView) {
        Node current = target;
        while (current != null && current != listView) {
            if (current instanceof TextArea) {
                // 为简化逻辑，只要光标在任何 TextArea 上，都让其优先处理滚动
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}