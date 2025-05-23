package com.karaik.scripteditor.controller.child;

import com.karaik.scripteditor.controller.EditorController;
import com.karaik.scripteditor.entry.SptEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView; // 导入 ListView
import javafx.scene.control.Pagination;
import javafx.scene.layout.VBox; // 作为 PageFactory 的占位符返回

import java.util.List;

public class PaginationController {

    private EditorController mainController;
    private ListView<SptEntry> entryListView; // 直接操作 ListView
    private Pagination pagination;
    private int itemsPerPage;

    // 构造函数更改：不再需要 VBox entryContainer 和 Function<SptEntry, VBox> entryUIGenerator
    public PaginationController(EditorController mainController, ListView<SptEntry> entryListView, Pagination pagination, int initialItemsPerPage) {
        this.mainController = mainController;
        this.entryListView = entryListView;
        this.pagination = pagination;
        this.itemsPerPage = initialItemsPerPage;

        this.pagination.setPageFactory(this::createPage);
    }

    /**
     * 设置每页显示的条目数并重新刷新分页。
     * @param newItemsPerPage 新的每页条目数
     */
    public void setItemsPerPage(int newItemsPerPage) {
        this.itemsPerPage = newItemsPerPage;
        // updatePagination 会在 EditorController 中被调用，无需在这里立即调用
    }

    /**
     * 更新 Pagination 的页数和 PageFactory，并根据当前页刷新 ListView 的内容。
     * @param totalEntriesCount 总条目数
     */
    public void updatePagination(int totalEntriesCount) {
        if (totalEntriesCount == 0) {
            pagination.setPageCount(1); // 至少一页
            entryListView.setItems(FXCollections.observableArrayList()); // 清空 ListView
            return;
        }

        int pageCount = (int) Math.ceil(totalEntriesCount / (double) itemsPerPage);
        pagination.setPageCount(Math.max(1, pageCount));
        // pagination.setPageFactory(this::createPage); // 仅在 pagination 初始化时设置一次即可

        // 确保当前页面在有效范围内，如果超出则重置到第一页
        int currentPage = pagination.getCurrentPageIndex();
        if (currentPage >= pageCount) {
            pagination.setCurrentPageIndex(0);
        } else {
            // 强制重新生成当前页面，以更新 ListView
            createPage(pagination.getCurrentPageIndex());
        }
    }

    /**
     * 刷新当前显示的页面。
     * 这会触发 createPage 重新获取数据并设置给 ListView。
     */
    public void refreshCurrentPage() {
        createPage(pagination.getCurrentPageIndex());
    }

    /**
     * Pagination 的 PageFactory 回调方法。
     * 它现在只负责从主数据列表中提取当前页的条目，并设置给 ListView。
     * ListView 的 CellFactory 会负责实际的 UI 渲染。
     * @param pageIndex 当前页码
     * @return 返回一个占位符 VBox，实际内容由 ListView 渲染
     */
    private VBox createPage(int pageIndex) {
        // 清空 ListView 的显示数据，然后加载当前页的数据
        entryListView.getItems().clear(); // 清空 ListView
        List<SptEntry> allEntries = mainController.getEntries(); // 获取所有数据

        int start = pageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allEntries.size());

        if (start < allEntries.size()) {
            // 获取当前页的子列表，并设置给 ListView
            ObservableList<SptEntry> currentPageEntries = FXCollections.observableArrayList(allEntries.subList(start, end));
            entryListView.setItems(currentPageEntries);
        } else if (allEntries.isEmpty()){
            entryListView.setItems(FXCollections.observableArrayList()); // 如果没有数据，清空ListView
        } else {
            // 如果计算出的起始索引超出了总条目数，但在第一页，可能仍然需要显示
            entryListView.setItems(FXCollections.emptyObservableList()); // 使用 emptyObservableList 更明确
        }

        // 返回一个空的 VBox 作为 Pagination 的 PageFactory 的返回值，
        // 因为实际的内容现在由 ListView 负责渲染。
        return new VBox();
    }
}