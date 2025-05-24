package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

@Data
public class EditorController {

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

    private Preferences preferences = Preferences.userNodeForPackage(EditorController.class);
    private static final String PREF_KEY_LAST_FILE = "lastOpenedFile";
    private static final String PREF_KEY_ITEMS_PER_PAGE = "itemsPerPage";
    private static final String PREF_KEY_LAST_PAGE_INDEX = "lastPageIndex";
    private static final String PREF_KEY_ALWAYS_ON_TOP = "alwaysOnTop";

    private int itemsPerPage = 50;
    private AtomicBoolean isRendering = new AtomicBoolean(false);
    private long lastPageChangeTime = 0;
    private static final long PAGE_CHANGE_COOLDOWN = 500;
    private boolean initializing = true;
    private final AtomicBoolean warningShown = new AtomicBoolean(false);

    @FXML
    public void initialize() {
        loadPreferencesForStartup();

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
                        lastPageChangeTime = System.currentTimeMillis();
                        preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, newVal.intValue());
                        if (modified) {
                            fileHandlerController.saveFile();
                        }
                        return;
                    }

                    if (!canChangePage()) {
                        if (warningShown.compareAndSet(false, true)) {
                            Platform.runLater(() -> {
                                pagination.setCurrentPageIndex(oldVal.intValue());
                                Alert alert = new Alert(Alert.AlertType.WARNING, "页面正在渲染中或操作过于频繁，请稍后再尝试翻页。");
                                configureAlertOnTop(alert);
                                alert.setOnCloseRequest(event -> warningShown.set(false));
                                alert.show();
                            });
                        }
                        return;
                    }

                    if (modified) {
                        fileHandlerController.saveFile();
                    }
                    preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, newVal.intValue());
                    lastPageChangeTime = System.currentTimeMillis();
                }
            });
        }

        Platform.runLater(() -> {
            Scene scene = (pagination != null && pagination.getScene() != null) ? pagination.getScene() :
                    (entryContainer != null && entryContainer.getScene() != null) ? entryContainer.getScene() : null;

            if (scene != null && scene.getWindow() instanceof Stage) {
                primaryStage = (Stage) scene.getWindow();
                setupPrimaryStageEventHandlers();
                updateTitle();

                if (alwaysOnTopCheckBox != null) {
                    boolean wasOnTop = preferences.getBoolean(PREF_KEY_ALWAYS_ON_TOP, false);
                    alwaysOnTopCheckBox.setSelected(wasOnTop);
                    primaryStage.setAlwaysOnTop(wasOnTop);
                }

                if (paginationUIController != null) {
                    paginationUIController.setupPagination();
                }

                File lastFile = getLastOpenedFile();
                if (lastFile != null && lastFile.exists()) {
                    fileHandlerController.openSpecificFile(lastFile);
                    // restoreLastPage 和 initializing = false 将在 openSpecificFile 的回调中处理
                } else {
                    // 如果没有上次打开的文件，也需要确保初始化完成
                    if (paginationUIController != null && entries.isEmpty()) {
                        paginationUIController.updatePaginationView();
                    } else if (paginationUIController != null) {
                        restoreLastPage();
                    }
                    // 如果没有文件打开，直接设置 initializing 为 false
                    initializing = false;
                }
            } else {
                System.err.println("Error: Scene or Stage not available during initialization.");
                // 如果Stage都不可用，直接设为false，否则可能一直处于initializing状态
                initializing = false;
            }
        });
    }

    public boolean canChangePage() {
        long currentTime = System.currentTimeMillis();
        return !isRendering.get() && (currentTime - lastPageChangeTime) >= PAGE_CHANGE_COOLDOWN;
    }

    public void setRendering(boolean rendering) {
        this.isRendering.set(rendering);
    }

    private void loadPreferencesForStartup() {
        this.itemsPerPage = preferences.getInt(PREF_KEY_ITEMS_PER_PAGE, itemsPerPage);
    }

    private void setupItemsPerPageComboBox() {
        if (itemsPerPageComboBox != null) {
            itemsPerPageComboBox.setItems(FXCollections.observableArrayList(20, 30, 50));
            if (!itemsPerPageComboBox.getItems().contains(this.itemsPerPage)) {
                this.itemsPerPage = 20;
            }
            itemsPerPageComboBox.setValue(this.itemsPerPage);

            itemsPerPageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal != this.itemsPerPage) {
                    this.itemsPerPage = newVal;
                    preferences.putInt(PREF_KEY_ITEMS_PER_PAGE, this.itemsPerPage);
                    if (paginationUIController != null) {
                        paginationUIController.updatePaginationView();
                    }
                }
            });
        }
    }

    public void restoreLastPage() {
        if (pagination != null && !entries.isEmpty()) {
            int lastPageIndex = preferences.getInt(PREF_KEY_LAST_PAGE_INDEX, 0);
            int pageCount = (int) Math.ceil((double) entries.size() / itemsPerPage);
            if (lastPageIndex >= 0 && lastPageIndex < pageCount) {
                pagination.setCurrentPageIndex(lastPageIndex);
                if (paginationUIController != null) {
                    paginationUIController.createPageForEntries(lastPageIndex);
                }
                lastPageChangeTime = System.currentTimeMillis();
            } else {
                pagination.setCurrentPageIndex(0);
                if (paginationUIController != null) {
                    paginationUIController.createPageForEntries(0);
                }
                lastPageChangeTime = System.currentTimeMillis();
            }
        }
        // 在页面恢复和渲染逻辑完成后，才将 initializing 设置为 false
        // 这需要确保 createPageForEntries 内部的 Platform.runLater 也执行完毕
        // 实际上，createPageForEntries 已经是 Platform.runLater 调用的，
        // 所以这里可以放心地设置。
        Platform.runLater(() -> initializing = false);
    }

    private void setupPrimaryStageEventHandlers() {
        primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                fileHandlerController.saveFile();
                event.consume();
            }
        });

        primaryStage.setOnCloseRequest(event -> {
            rememberFile(currentFile);
            preferences.putInt(PREF_KEY_ITEMS_PER_PAGE, this.itemsPerPage);
            if (pagination != null && !entries.isEmpty() && pagination.getPageCount() > 0) {
                preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, pagination.getCurrentPageIndex());
            } else {
                preferences.remove(PREF_KEY_LAST_PAGE_INDEX);
            }
            if (alwaysOnTopCheckBox != null) {
                preferences.putBoolean(PREF_KEY_ALWAYS_ON_TOP, alwaysOnTopCheckBox.isSelected());
            }

            if (modified) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前文件有未保存的更改，确定要退出吗？");
                configureAlertOnTop(alert);
                Optional<ButtonType> result = alert.showAndWait();
                if (!result.isPresent() || (result.get() != ButtonType.OK && result.get() != ButtonType.YES)) {
                    event.consume();
                }
            }
        });
    }

    @FXML
    private void handleAlwaysOnTopToggle() {
        if (primaryStage != null) {
            primaryStage.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected());
        }
    }

    public void configureAlertOnTop(Alert alert) {
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
            if (primaryStage.isAlwaysOnTop()) {
                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                if (alertStage != null) {
                    alertStage.setAlwaysOnTop(true);
                } else {
                    alert.setOnShown(e -> {
                        Stage s = (Stage) alert.getDialogPane().getScene().getWindow();
                        if (s != null) s.setAlwaysOnTop(true);
                    });
                }
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
                Alert alert = new Alert(Alert.AlertType.WARNING, "页面正在渲染中或操作过于频繁，请稍后再尝试跳转。");
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
                lastPageChangeTime = System.currentTimeMillis();
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

    public void setEntries(List<SptEntry> newEntries) {
        this.entries = newEntries != null ? newEntries : new ArrayList<>();
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
            primaryStage.setTitle((modified ? "*" : "") + fileName + " - 虫爱少女汉化文本编辑器");
        }
    }

    public void rememberFile(File file) {
        if (file != null) {
            preferences.put(PREF_KEY_LAST_FILE, file.getAbsolutePath());
        } else {
            preferences.remove(PREF_KEY_LAST_FILE);
        }
    }

    public File getLastOpenedFile() {
        String path = preferences.get(PREF_KEY_LAST_FILE, null);
        return path != null ? new File(path) : null;
    }
}