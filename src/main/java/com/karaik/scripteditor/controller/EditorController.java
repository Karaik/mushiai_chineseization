package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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

        // 监听页码变化以自动保存文件
        if (pagination != null) {
            pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
                if (oldVal != null && newVal != null && oldVal.intValue() != newVal.intValue()) {
                    // 如果是初始化阶段，不触发冷却和警告
                    if (initializing) {
                        lastPageChangeTime = System.currentTimeMillis(); // 更新时间戳，确保后续操作不受初始化的影响
                        preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, newVal.intValue()); // 保存当前页码
                        if (modified) {
                            fileHandlerController.saveFile(); // 如果有修改则保存
                        }
                        return; // 初始化期间直接返回
                    }

                    if (!canChangePage()) { // 检查是否可以翻页
                        if (warningShown.compareAndSet(false, true)) { // 避免重复弹出警告
                            Platform.runLater(() -> {
                                pagination.setCurrentPageIndex(oldVal.intValue()); // 翻回旧页面
                                Alert alert = new Alert(Alert.AlertType.WARNING, "别翻页太快，会崩的(～￣(OO)￣)ブ");
                                configureAlertOnTop(alert);
                                alert.setOnCloseRequest(event -> warningShown.set(false)); // 弹窗关闭后重置警告标志
                                alert.show();
                            });
                        }
                        return; // 阻止翻页
                    }

                    // 在翻页前保存文件（如果有修改）
                    if (modified) {
                        fileHandlerController.saveFile();
                    }
                    // 更新偏好设置中的页码
                    preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, newVal.intValue());
                    // 更新上次翻页时间
                    lastPageChangeTime = System.currentTimeMillis();
                }
            });
        }

        Platform.runLater(() -> {
            Scene scene = (pagination != null && pagination.getScene() != null) ? pagination.getScene() :
                    (entryContainer != null && entryContainer.getScene() != null) ? entryContainer.getScene() : null;

            if (scene != null && scene.getWindow() instanceof Stage) {
                primaryStage = (Stage) scene.getWindow();
                // 新增：加载CSS文件
                scene.getStylesheets().add(getClass().getResource("/com/karaik/scripteditor/css.css").toExternalForm());
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
                } else {
                    if (paginationUIController != null && entries.isEmpty()) {
                        paginationUIController.updatePaginationView();
                    } else if (paginationUIController != null) {
                        restoreLastPage();
                    }
                    initializing = false; // 文件打开或恢复完成后，设置为非初始化状态
                }
            } else {
                System.err.println("Error: Scene or Stage not available during initialization.");
                initializing = false; // 确保即使出错也能退出初始化状态
            }
        });
    }

    // 检查是否可以翻页
    public boolean canChangePage() {
        long currentTime = System.currentTimeMillis();
        return !isRendering.get() && (currentTime - lastPageChangeTime) >= PAGE_CHANGE_COOLDOWN;
    }

    // 设置渲染状态
    public void setRendering(boolean rendering) {
        this.isRendering.set(rendering);
    }

    private void loadPreferencesForStartup() {
        this.itemsPerPage = preferences.getInt(PREF_KEY_ITEMS_PER_PAGE, itemsPerPage);
    }

    private void setupItemsPerPageComboBox() {
        if (itemsPerPageComboBox != null) {
            itemsPerPageComboBox.setItems(FXCollections.observableArrayList(20, 30, 50)); //
            if (!itemsPerPageComboBox.getItems().contains(this.itemsPerPage)) {
                this.itemsPerPage = 20; //
            }
            itemsPerPageComboBox.setValue(this.itemsPerPage);

            itemsPerPageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal != this.itemsPerPage) {
                    this.itemsPerPage = newVal;
                    preferences.putInt(PREF_KEY_ITEMS_PER_PAGE, this.itemsPerPage); // 保存到偏好设置
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
                lastPageChangeTime = System.currentTimeMillis(); // 恢复页码后更新时间戳
            } else {
                // 如果页码超出范围，重置为第一页
                pagination.setCurrentPageIndex(0);
                if (paginationUIController != null) {
                    paginationUIController.createPageForEntries(0);
                }
                lastPageChangeTime = System.currentTimeMillis(); // 恢复页码后更新时间戳
            }
        }
        Platform.runLater(() -> initializing = false); // 恢复完成后，设置为非初始化状态
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

    // 复制处理
    @FXML
    private void handleCopyCurrentPage() {
        if (entries.isEmpty() || pagination == null) {
            new Alert(Alert.AlertType.INFORMATION, "没有内容可以复制。").showAndWait();
            return;
        }

        int currentPageIndex = pagination.getCurrentPageIndex();
        int start = currentPageIndex * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        StringBuilder clipboardContentBuilder = new StringBuilder();
        for (int i = start; i < end; i++) {
            SptEntry entry = entries.get(i);
            clipboardContentBuilder
                    .append("○").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("○ ")
                    .append(entry.getFullOriginalText()).append("\n");
            clipboardContentBuilder
                    .append("●").append(entry.getIndex()).append("|").append(entry.getAddress()).append("|").append(entry.getLength()).append("● ")
                    .append(entry.getFullTranslatedText()).append("\n");
            clipboardContentBuilder.append("\n");
        }

        if (clipboardContentBuilder.length() > 0) {
            ClipboardContent content = new ClipboardContent();
            content.putString(clipboardContentBuilder.toString().trim());
            Clipboard.getSystemClipboard().setContent(content);
        } else {
            new Alert(Alert.AlertType.INFORMATION, "当前页没有可复制的文本内容。").showAndWait();
        }
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
