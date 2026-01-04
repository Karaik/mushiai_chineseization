package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.helper.*; // 假设这些 helper 类存在且路径正确
import com.karaik.scripteditor.ui.SptEntryListCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class EditorController {

    @FXML private ListView<SptEntry> entryListView; // 确保 fx:id="entryListView" 在 FXML 中
    @FXML private Pagination pagination;
    @FXML private TextField pageInputField;
    @FXML private ComboBox<Integer> itemsPerPageComboBox;
    @FXML private CheckBox alwaysOnTopCheckBox;

    private File currentFile;
    private List<SptEntry> entries = new ArrayList<>();
    private boolean modified = false;
    private Stage primaryStage;

    private FileHandlerController fileHandlerController;
    private PaginationUIController paginationUIController;

    private int itemsPerPage = 3; // 默认值
    private AtomicBoolean isRendering = new AtomicBoolean(false);
    private long lastPageChangeTime = 0;
    public static final long PAGE_CHANGE_COOLDOWN = 150;
    private boolean initializing = true;
    private final AtomicBoolean warningShown = new AtomicBoolean(false);
    public static final double KEYBOARD_SCROLL_AMOUNT = 200;
    public static final double MOUSE_WHEEL_SCROLL_MULTIPLIER = 0.7;

    @FXML
    public void initialize() {
        loadPreferencesForStartup();

        if (entryListView != null) {
            entryListView.setCellFactory(listView -> new SptEntryListCell(() -> markModified(true)));
            entryListView.setPlaceholder(new Label("正在加载或无内容可显示..."));
            ScrollSpeedHelper.install(entryListView, MOUSE_WHEEL_SCROLL_MULTIPLIER);
        } else {
            // 这个错误非常严重，意味着FXML加载或注入失败
            System.err.println("CRITICAL ERROR: entryListView is null after FXML loading. Check fx:id in EditorView.fxml and controller field name.");
            // 此时应用可能无法正常工作，可以考虑抛出异常或显示错误对话框
        }

        setupControllers();
        setupItemsPerPageComboBox();
        setupPageJumpField();
        setupPaginationListener(); // 确保在 paginationUIController.setupPagination() 之前
        setupUIAfterLoad(); // 这个方法会调用 paginationUIController.setupPagination()
    }

    private void setupControllers() {
        // 确保在 EditorController 的成员变量被 FXML 注入（如 pagination, entryListView）之后再创建这些控制器
        this.fileHandlerController = new FileHandlerController(this);
        this.paginationUIController = new PaginationUIController(this);
    }

    private void setupItemsPerPageComboBox() {
        if (itemsPerPageComboBox != null) {
            itemsPerPageComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 5, 10, 20, 30, 50, 100, 200, 500, 1000));
            if (!itemsPerPageComboBox.getItems().contains(this.itemsPerPage)) {
                this.itemsPerPage = itemsPerPageComboBox.getItems().get(0); // 默认选择第一个
            }
            itemsPerPageComboBox.setValue(this.itemsPerPage);

            itemsPerPageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.intValue() != this.itemsPerPage && newVal.intValue() > 0) { // 确保新值有效
                    this.itemsPerPage = newVal;
                    AppPreferenceHelper.saveItemsPerPage(this.itemsPerPage);
                    if (paginationUIController != null) {
                        paginationUIController.updatePaginationView();
                    }
                }
            });
        }
    }

    private void setupPageJumpField() {
        if (pageInputField != null) {
            pageInputField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleJumpToPage();
                }
            });
        }
    }

    private void setupPaginationListener() {
        if (pagination == null) {
            System.err.println("Pagination control is null. Cannot attach listener.");
            return;
        }
        PageChangeListener.attach(
                pagination,
                () -> initializing,
                () -> modified,
                () -> fileHandlerController.saveFile(), // 假设 FileHandlerController 已初始化
                AppPreferenceHelper::saveLastPageIndex,
                this::canChangePage,
                warningShown,
                this
        );
    }

    private void setupUIAfterLoad() {
        Platform.runLater(() -> {
            // 获取 Scene 和 Stage 的方式保持不变，但现在依赖 entryListView (如果 pagination 为 null)
            Scene scene = Optional.ofNullable(pagination).map(Control::getScene)
                    .orElse(Optional.ofNullable(entryListView).map(Node::getScene).orElse(null));

            if (scene != null && scene.getWindow() instanceof Stage) {
                primaryStage = (Stage) scene.getWindow();
                String cssPath = "/com/karaik/scripteditor/css.css"; // CSS路径
                try {
                    scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                } catch (Exception e) { // 捕获更广泛的异常
                    System.err.println("Failed to load CSS from path: " + cssPath + ". Error: " + e.getMessage());
                }

                KeyboardNavigationHelper.setupKeyboardShortcuts(primaryStage, this);
                StageCloseHandler.attach(
                        primaryStage,
                        () -> this.modified,
                        () -> fileHandlerController.saveFile(),
                        () -> primaryStage.close(),
                        this
                );
                updateTitle();

                if (alwaysOnTopCheckBox != null) {
                    boolean wasOnTop = AppPreferenceHelper.loadAlwaysOnTop();
                    alwaysOnTopCheckBox.setSelected(wasOnTop);
                    primaryStage.setAlwaysOnTop(wasOnTop);
                }

                // 在这里调用 setupPagination，确保 pagination 控件已注入并可用
                if (paginationUIController != null) {
                    paginationUIController.setupPagination();
                }

                File lastFile = getLastOpenedFile();
                if (lastFile != null && lastFile.exists()) {
                    fileHandlerController.openSpecificFile(lastFile); // 这会触发 updatePaginationView
                } else {
                    // 如果没有上次的文件，也需要初始化分页视图（例如显示空列表提示）
                    if (paginationUIController != null) {
                        paginationUIController.updatePaginationView();
                    }
                    setInitializing(false); // 确保在没有文件加载时也设置
                }
            } else {
                System.err.println("Error: Scene or Stage not available during setupUIAfterLoad.");
                setInitializing(false); // 确保在错误情况下也设置
            }
        });
    }

    public void setInitializing(boolean initializing) {
        this.initializing = initializing;
    }

    public boolean canChangePage() {
        long currentTime = System.currentTimeMillis();
        boolean rendering = (isRendering != null) && isRendering.get();
        return !rendering && (currentTime - lastPageChangeTime) >= PAGE_CHANGE_COOLDOWN;
    }

    public void setRendering(boolean rendering) {
        if (isRendering != null) {
            this.isRendering.set(rendering);
        }
        if (pagination != null) {
            pagination.setDisable(rendering);
        }
    }

    private void loadPreferencesForStartup() {
        this.itemsPerPage = AppPreferenceHelper.loadItemsPerPage(this.itemsPerPage > 0 ? this.itemsPerPage : 3); // 确保 itemsPerPage > 0
    }

    @FXML
    private void handleAlwaysOnTopToggle() {
        if (primaryStage != null && alwaysOnTopCheckBox != null) {
            boolean selected = alwaysOnTopCheckBox.isSelected();
            primaryStage.setAlwaysOnTop(selected);
            AppPreferenceHelper.saveAlwaysOnTop(selected);
        }
    }

    public void configureAlertOnTop(Alert alert) {
        if (primaryStage != null && alert != null) { // 添加 alert null 检查
            alert.initOwner(primaryStage);
            if (primaryStage.isAlwaysOnTop()) {
                alert.setOnShown(e -> {
                    Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                    if (alertStage != null) {
                        alertStage.setAlwaysOnTop(true);
                    }
                });
            }
        }
    }

    @FXML private void handleOpenFile() { if(fileHandlerController != null) fileHandlerController.openFile(); }
    @FXML private void handleSaveFile() { if(fileHandlerController != null) fileHandlerController.saveFile(); }

    @FXML
    private void handleJumpToPage() {
        if (pagination == null || pageInputField == null) return;
        PageJumpHandler.handle(
                pageInputField,
                pagination,
                primaryStage,
                this::canChangePage,
                this
        );
    }

    @FXML
    private void handleCopyCurrentPage() {
        if (pagination == null) { // 确保 pagination 不是 null
            ClipboardHelper.copyEntriesToClipboard(entries, 0, Math.min(itemsPerPage, entries.size()), primaryStage, this);
            return;
        }
        if (entries.isEmpty()) {
            ClipboardHelper.copyEntriesToClipboard(List.of(), 0, 0, primaryStage, this);
            return;
        }
        int currentPageIndex = pagination.getCurrentPageIndex();
        int start = currentPageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());
        ClipboardHelper.copyEntriesToClipboard(entries, start, end, primaryStage, this);
    }

    public void setEntries(List<SptEntry> newEntries) {
        this.entries = newEntries != null ? new ArrayList<>(newEntries) : new ArrayList<>();
        markModified(false);
        if (paginationUIController != null) {
            paginationUIController.updatePaginationView();
        } else {
            System.err.println("paginationUIController is null in setEntries, cannot update view.");
        }
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        updateTitle();
        if (file != null) { // 只在文件非空时记住
            rememberFile(file);
        }
    }

    public void markModified(boolean modifiedState) {
        if (this.modified != modifiedState) {
            this.modified = modifiedState;
            updateTitle();
        }
    }

    private void updateTitle() {
        if (primaryStage != null) {
            String fileName = (currentFile != null) ? currentFile.getName() : "请选择文件";
            primaryStage.setTitle((modified ? "*" : "") + fileName + " - 虫爱少女汉化文本编辑器");
        }
    }

    public void rememberFile(File file) { AppPreferenceHelper.saveLastFile(file); }
    public File getLastOpenedFile() { return AppPreferenceHelper.loadLastFile(); }

    public void restoreLastPage() {
        if (pagination == null || paginationUIController == null) return;

        if (!entries.isEmpty()) {
            int lastPageIndexFromPrefs = AppPreferenceHelper.loadLastPageIndex();
            int pageCount = pagination.getPageCount();
            if (pageCount <= 0) pageCount = 1; // 确保至少有1页

            int targetPage = Math.max(0, Math.min(lastPageIndexFromPrefs, pageCount - 1));

            if (pagination.getCurrentPageIndex() != targetPage) {
                pagination.setCurrentPageIndex(targetPage); // 这会触发 pageFactory -> loadDataForPage
            } else {
                // 如果页码未变，但可能需要加载数据（例如初始化，或数据已清除再加载）
                paginationUIController.loadDataForPage(targetPage); // 直接调用加载
            }
        } else { // entries is empty
            if (pagination.getCurrentPageIndex() != 0) {
                pagination.setCurrentPageIndex(0);
            } else {
                paginationUIController.loadDataForPage(0); // 加载空页（显示占位符）
            }
        }
        lastPageChangeTime = System.currentTimeMillis();
    }

    public ListView<SptEntry> getEntryListView() { return entryListView; }
    public Pagination getPagination() { return pagination; }
    public List<SptEntry> getEntries() { return entries; }
    public int getItemsPerPage() { return itemsPerPage > 0 ? itemsPerPage : 3; } // 防御
}