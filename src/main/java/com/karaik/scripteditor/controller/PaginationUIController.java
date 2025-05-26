package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.ui.EntryUIFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PaginationUIController {

    private final EditorController editorController;

    public void setupPagination() {
        // 将分页控件的工厂函数设为当前类的方法
        Pagination pagination = editorController.getPagination();
        if (pagination != null) {
            pagination.setPageFactory(this::createPageForEntries);
        }
    }

    public void updatePaginationView() {
        Pagination pagination = editorController.getPagination();
        VBox container = editorController.getEntryContainer();
        if (pagination == null || container == null) return;

        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();

        // 计算新的总页数
        int newPageCount = entries.isEmpty() ? 1
                : Math.max(1, (int) Math.ceil((double) entries.size() / itemsPerPage));

        int oldIndex = pagination.getCurrentPageIndex();
        pagination.setPageCount(newPageCount);
        int newIndex = pagination.getCurrentPageIndex();

        // 当页码或数据变化时刷新当前页
        if (oldIndex != newIndex || newPageCount == 1 || !entries.isEmpty()) {
            createPageForEntries(newIndex);
        } else if (entries.isEmpty() && newIndex == 0) {
            createPageForEntries(0);
        }
    }

    public VBox createPageForEntries(int pageIndex) {
        VBox container = editorController.getEntryContainer();
        if (container == null) {
            return new VBox(new Label("Error: UI container not found."));
        }

        // 渲染过程中禁止用户操作分页
        editorController.setRendering(true);
        Pagination pagination = editorController.getPagination();
        if (pagination != null) pagination.setDisable(true);

        List<SptEntry> entries = editorController.getEntries();
        int itemsPerPage = editorController.getItemsPerPage();
        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        List<Node> nodes = new ArrayList<>();

        if (entries.isEmpty() && pageIndex == 0) {
            // 没有数据时的提示
            Label lbl = new Label("无内容可显示。");
            lbl.setPadding(new Insets(10));
            nodes.add(lbl);
        } else if (start >= entries.size() && !entries.isEmpty()) {
            // 页码越界时的提示
            Label lbl = new Label("页码超出范围或无条目。");
            lbl.setPadding(new Insets(10));
            nodes.add(lbl);
        } else {
            // 正常加载条目
            for (int i = start; i < end; i++) {
                nodes.add(
                        EntryUIFactory.createEntryNode(
                                entries.get(i),
                                () -> editorController.markModified(true)
                        )
                );
                if (i < end - 1) nodes.add(new Separator());
            }
        }

        // 更新 UI 并恢复控件状态
        Platform.runLater(() -> {
            container.getChildren().setAll(nodes);
            editorController.setRendering(false);
            Pagination pg = editorController.getPagination();
            if (pg != null) pg.setDisable(false);
            if (editorController.getMainContentScrollPane() != null) {
                editorController.getMainContentScrollPane().setVvalue(0.0);
            }
        });

        // 返回一个空 VBox 作为占位符
        return new VBox();
    }
}
