package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
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

    private Preferences preferences = Preferences.userNodeForPackage(EditorController.class);
    private static final String PREF_KEY_LAST_FILE = "lastOpenedFile";
    private static final String PREF_KEY_ITEMS_PER_PAGE = "itemsPerPage";
    private static final String PREF_KEY_LAST_PAGE_INDEX = "lastPageIndex";
    private static final String PREF_KEY_ALWAYS_ON_TOP = "alwaysOnTop";

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
        loadPreferencesForStartup();

        if (mainContentScrollPane != null) {
            mainContentScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
                Node target = event.getTarget() instanceof Node ? (Node) event.getTarget() : null;
                Node currentNode = target;
                boolean isInsideTextArea = false;
                while(currentNode != null) {
                    if (currentNode instanceof TextArea) {
                        isInsideTextArea = true;
                        break;
                    }
                    currentNode = currentNode.getParent();
                }
                if (isInsideTextArea) {
                    return;
                }
                double originalDeltaY = event.getDeltaY();
                double adjustedDeltaY = originalDeltaY * MOUSE_WHEEL_SCROLL_MULTIPLIER;
                if (mainContentScrollPane.getContent() != null && mainContentScrollPane.getContent().getBoundsInLocal().getHeight() > 0) {
                    double newVValue = mainContentScrollPane.getVvalue() - (adjustedDeltaY / mainContentScrollPane.getContent().getBoundsInLocal().getHeight());
                    mainContentScrollPane.setVvalue(Math.max(0, Math.min(1, newVValue)));
                }
                event.consume();
            });
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
                        preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, newVal.intValue());
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
                    preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, newVal.intValue());
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
        this.itemsPerPage = preferences.getInt(PREF_KEY_ITEMS_PER_PAGE, itemsPerPage);
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
            int lastPageIndexFromPrefs = preferences.getInt(PREF_KEY_LAST_PAGE_INDEX, 0);
            int pageCount = (int) Math.ceil((double) entries.size() / itemsPerPage);
            int targetPage = 0;

            if (lastPageIndexFromPrefs >= 0 && lastPageIndexFromPrefs < pageCount) {
                targetPage = lastPageIndexFromPrefs;
            }

            if (pagination.getCurrentPageIndex() != targetPage) {
                pagination.setCurrentPageIndex(targetPage);
            } else {
                if (initializing) {
                    preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, targetPage);
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
                    preferences.putInt(PREF_KEY_LAST_PAGE_INDEX, 0);
                }
                if (paginationUIController != null) {
                    paginationUIController.createPageForEntries(0);
                }
            }
            lastPageChangeTime = System.currentTimeMillis();
        }
    }

    private void setupPrimaryStageEventHandlers() {
        if (primaryStage == null || primaryStage.getScene() == null) return;

        // 监听窗口关闭事件
        primaryStage.setOnCloseRequest(event -> {
            if (modified) { // 如果文件有修改
                // 阻止默认的关闭行为，等待用户确认
                event.consume();

                // 弹出确认保存的对话框
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("保存文件");
                alert.setHeaderText("文件已被修改");
                alert.setContentText("文件尚未保存，是否保存后关闭？");

                ButtonType saveButton = new ButtonType("保存");
                ButtonType discardButton = new ButtonType("不保存");
                ButtonType cancelButton = new ButtonType("取消");

                alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

                // 根据主窗口是否设置为保持最前决定是否将弹窗设置为最前
                if (primaryStage.isAlwaysOnTop()) {
                    alert.setOnShown(e -> {
                        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                        if (alertStage != null) {
                            alertStage.setAlwaysOnTop(true); // 弹窗保持最前
                        }
                    });
                } else {
                    alert.setOnShown(e -> {
                        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
                        if (alertStage != null) {
                            alertStage.setAlwaysOnTop(false); // 弹窗不保持最前
                        }
                    });
                }

                // 显示对话框并获取结果
                alert.showAndWait().ifPresent(response -> {
                    if (response == saveButton) {
                        fileHandlerController.saveFile(); // 保存文件
                        primaryStage.close(); // 保存后关闭应用
                    } else if (response == discardButton) {
                        primaryStage.close(); // 直接关闭应用
                    }
                    // 如果是取消，什么都不做，保持应用打开
                });
            } else {
                // 如果没有修改，直接关闭
                primaryStage.close();
            }
        });
    }

    @FXML
    private void handleAlwaysOnTopToggle() {
        if (primaryStage != null && alwaysOnTopCheckBox != null) {
            boolean selected = alwaysOnTopCheckBox.isSelected();
            primaryStage.setAlwaysOnTop(selected);
            preferences.putBoolean(PREF_KEY_ALWAYS_ON_TOP, selected);
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "没有内容可以复制。");
            configureAlertOnTop(alert);
            alert.showAndWait();
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
            if (i < end - 1) {
                clipboardContentBuilder.append("\n");
            }
        }

        if (clipboardContentBuilder.length() > 0) {
            ClipboardContent content = new ClipboardContent();
            content.putString(clipboardContentBuilder.toString().trim());
            Clipboard.getSystemClipboard().setContent(content);
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "当前页没有可复制的文本内容。");
            configureAlertOnTop(alert);
            alert.showAndWait();
        }
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