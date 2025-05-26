package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.helper.AppPreferenceHelper;
import com.karaik.scripteditor.helper.ClipboardHelper;
import com.karaik.scripteditor.helper.ScrollEventHandler;
import com.karaik.scripteditor.helper.StageCloseHandler;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class EditorController {

    @FXML private ScrollPane mainContentScrollPane;
    @FXML private VBox entryContainer;
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

    private int itemsPerPage = 3;
    private AtomicBoolean isRendering = new AtomicBoolean(false);
    private long lastPageChangeTime = 0;
    private static final long PAGE_CHANGE_COOLDOWN = 500;
    private boolean initializing = true;
    private final AtomicBoolean warningShown = new AtomicBoolean(false);
    private static final double KEYBOARD_SCROLL_AMOUNT = 200; // 控制每次按键滚动的像素量
    private static final double MOUSE_WHEEL_SCROLL_MULTIPLIER = 2.0; // 控制滚轮幅度

    @FXML
    public void initialize() {

        // 加载偏好设置
        loadPreferencesForStartup();

        // 滚轮相关设置
        if (mainContentScrollPane != null) {
            ScrollEventHandler.installSmartScroll(mainContentScrollPane, MOUSE_WHEEL_SCROLL_MULTIPLIER);
        }

        this.fileHandlerController = new FileHandlerController(this);
        this.paginationUIController = new PaginationUIController(this);

        setupItemsPerPageComboBox();

        if (pageInputField != null) {
            pageInputField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleJumpToPage();
                }
            });
        }

        if (pagination != null) {
            pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal != null && newVal != null && oldVal.intValue() != newVal.intValue()) {
                    if (initializing) {
                        AppPreferenceHelper.saveLastPageIndex(newVal.intValue());
                        if (modified) {
                            fileHandlerController.saveFile();
                        }
                        lastPageChangeTime = System.currentTimeMillis();
                        return;
                    }

                    if (!canChangePage()) {
                        if (warningShown.compareAndSet(false, true)) {
                            Platform.runLater(() -> {
                                pagination.setCurrentPageIndex(oldVal.intValue());
                                Alert alert = new Alert(Alert.AlertType.WARNING, "别翻页太快，会崩的(～￣(OO)￣)ブ");
                                configureAlertOnTop(alert);
                                alert.setOnCloseRequest(alertEvent -> warningShown.set(false));
                                alert.show();
                            });
                        }
                        return;
                    }

                    if (modified) {
                        fileHandlerController.saveFile();
                    }
                    AppPreferenceHelper.saveLastPageIndex(newVal.intValue());
                    lastPageChangeTime = System.currentTimeMillis();
                }
            });
        }

        Platform.runLater(() -> {
            Scene scene = Optional.ofNullable(pagination).map(Control::getScene)
                    .orElse(Optional.ofNullable(entryContainer).map(Node::getScene).orElse(null));

            if (scene != null && scene.getWindow() instanceof Stage) {
                primaryStage = (Stage) scene.getWindow();
                scene.getStylesheets().add(getClass().getResource("/com/karaik/scripteditor/css.css").toExternalForm());
                StageCloseHandler.attach(
                        primaryStage,
                        () -> this.modified,
                        () -> fileHandlerController.saveFile(),
                        () -> primaryStage.close()
                );
                updateTitle();

                if (alwaysOnTopCheckBox != null) {
                    boolean wasOnTop = AppPreferenceHelper.loadAlwaysOnTop();
                    alwaysOnTopCheckBox.setSelected(wasOnTop);
                    primaryStage.setAlwaysOnTop(wasOnTop);
                }

                if (paginationUIController != null) {
                    paginationUIController.setupPagination();
                }

                File lastFile = getLastOpenedFile();
                if (lastFile != null && lastFile.exists()) {
                    fileHandlerController.openSpecificFile(lastFile);
                } else {
                    if (paginationUIController != null) {
                        if (entries.isEmpty()) {
                            paginationUIController.updatePaginationView();
                        } else {
                            restoreLastPage();
                        }
                    }
                    setInitializing(false);
                }
            } else {
                System.err.println("Error: Scene or Stage not available during initialization.");
                setInitializing(false);
            }
        });
    }

    public void setInitializing(boolean initializing) {
        this.initializing = initializing;
    }

    public boolean canChangePage() {
        long currentTime = System.currentTimeMillis();
        return !isRendering.get() && (currentTime - lastPageChangeTime) >= PAGE_CHANGE_COOLDOWN;
    }

    public void setRendering(boolean rendering) {
        this.isRendering.set(rendering);
    }

    private void loadPreferencesForStartup() {
        this.itemsPerPage = AppPreferenceHelper.loadItemsPerPage(itemsPerPage);
    }

    private void setupItemsPerPageComboBox() {
        if (itemsPerPageComboBox != null) {
            itemsPerPageComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 20, 30, 50));
            if (!itemsPerPageComboBox.getItems().contains(this.itemsPerPage)) {
                this.itemsPerPage = 3;
            }
            itemsPerPageComboBox.setValue(this.itemsPerPage);

            itemsPerPageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.intValue() != this.itemsPerPage) {
                    this.itemsPerPage = newVal;
                    AppPreferenceHelper.saveItemsPerPage(this.itemsPerPage);
                    if (paginationUIController != null) {
                        paginationUIController.updatePaginationView();
                    }
                }
            });
        }
    }

    public void restoreLastPage() {
        if (pagination != null && !entries.isEmpty()) {
            int lastPageIndexFromPrefs = AppPreferenceHelper.loadLastPageIndex();
            int pageCount = (int) Math.ceil((double) entries.size() / itemsPerPage);
            int targetPage = 0;

            if (lastPageIndexFromPrefs >= 0 && lastPageIndexFromPrefs < pageCount) {
                targetPage = lastPageIndexFromPrefs;
            }

            if (pagination.getCurrentPageIndex() != targetPage) {
                pagination.setCurrentPageIndex(targetPage);
            } else {
                if (initializing) {
                    AppPreferenceHelper.saveLastPageIndex(targetPage);
                    if (modified) {
                        fileHandlerController.saveFile();
                    }
                }
            }
            lastPageChangeTime = System.currentTimeMillis();
        } else if (pagination != null && entries.isEmpty()) {
            if (pagination.getCurrentPageIndex() != 0) {
                pagination.setCurrentPageIndex(0);
            } else {
                if (initializing) {
                    AppPreferenceHelper.saveLastPageIndex(0);
                }
                if (paginationUIController != null) {
                    paginationUIController.createPageForEntries(0);
                }
            }
            lastPageChangeTime = System.currentTimeMillis();
        }
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
        if (primaryStage != null) {
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

    @FXML private void handleOpenFile() { fileHandlerController.openFile(); }
    @FXML private void handleSaveFile() { fileHandlerController.saveFile(); }

    @FXML
    private void handleJumpToPage() {
        if (pageInputField == null || pagination == null || pageInputField.getText().isEmpty()) {
            return;
        }
        if (!initializing && !canChangePage()) {
            if (warningShown.compareAndSet(false, true)) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "别翻页太快，会崩的(～￣(OO)￣)ブ");
                configureAlertOnTop(alert);
                alert.setOnCloseRequest(event -> warningShown.set(false));
                alert.show();
            }
            return;
        }
        try {
            int pageTarget = Integer.parseInt(pageInputField.getText()) - 1;
            if (pageTarget >= 0 && pageTarget < pagination.getPageCount()) {
                pagination.setCurrentPageIndex(pageTarget);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "页码超出范围。");
                configureAlertOnTop(alert);
                alert.showAndWait();
            }
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "输入无效，请输入有效的页码。");
            configureAlertOnTop(alert);
            alert.showAndWait();
        }
        pageInputField.clear();
    }

    @FXML
    private void handleCopyCurrentPage() {
        if (entries.isEmpty() || pagination == null) {
            ClipboardHelper.copyEntriesToClipboard(List.of(), 0, 0, primaryStage);
            return;
        }
        int currentPageIndex = pagination.getCurrentPageIndex();
        int start = currentPageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());
        ClipboardHelper.copyEntriesToClipboard(entries, start, end, primaryStage);
    }

    public void setEntries(List<SptEntry> newEntries) {
        this.entries = newEntries != null ? newEntries : new ArrayList<>();
        markModified(false);
        if (paginationUIController != null) {
            paginationUIController.updatePaginationView();
        }
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        updateTitle();
        rememberFile(file);
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
            primaryStage.setTitle((modified ? "*" : "") + fileName + " - 虫爱少女汉化文本编辑器(仅汉化组内部使用，禁止外传)");
        }
    }

    public void rememberFile(File file) {
        AppPreferenceHelper.saveLastFile(file);
    }

    public File getLastOpenedFile() {
        return AppPreferenceHelper.loadLastFile();
    }
}