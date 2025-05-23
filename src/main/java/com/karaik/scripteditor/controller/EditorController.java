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
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

public class EditorController {

    @FXML @Getter private VBox entryContainer;
    @FXML @Getter private Pagination pagination;
    @FXML private TextField pageInputField;
    @FXML private ComboBox<Integer> itemsPerPageComboBox;
    @FXML private CheckBox alwaysOnTopCheckBox;

    @Getter private File currentFile;
    @Getter private List<SptEntry> entries = new ArrayList<>();
    @Getter private boolean modified = false;
    @Getter private Stage primaryStage;

    private FileHandlerController fileHandlerController;
    private PaginationUIController paginationUIController;

    private final Preferences preferences = Preferences.userNodeForPackage(EditorController.class);
    private static final String PREF_KEY_LAST_FILE = "lastOpenedFile";
    private static final String PREF_KEY_ITEMS_PER_PAGE = "itemsPerPage";
    private static final String PREF_KEY_LAST_PAGE_INDEX = "lastPageIndex";
    private static final String PREF_KEY_ALWAYS_ON_TOP = "alwaysOnTop";

    @Getter private int itemsPerPage = 50; // 默认

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
                } else {
                    if (paginationUIController != null && entries.isEmpty()) {
                        paginationUIController.updatePaginationView();
                    } else if (paginationUIController != null) {
                        restoreLastPage();
                    }
                }
            } else {
                System.err.println("Error: Scene or Stage not available during initialization.");
            }
        });
    }

    private void loadPreferencesForStartup() {
        this.itemsPerPage = preferences.getInt(PREF_KEY_ITEMS_PER_PAGE, 50);
    }

    private void setupItemsPerPageComboBox() {
        if (itemsPerPageComboBox != null) {
            itemsPerPageComboBox.setItems(FXCollections.observableArrayList(20, 50, 100, 500));
            if (!itemsPerPageComboBox.getItems().contains(this.itemsPerPage)) {
                this.itemsPerPage = 50;
            }
            itemsPerPageComboBox.setValue(this.itemsPerPage);

            itemsPerPageComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal != this.itemsPerPage) {
                    this.itemsPerPage = newVal;
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
            if (lastPageIndex >= 0 && lastPageIndex < pagination.getPageCount()) {
                pagination.setCurrentPageIndex(lastPageIndex);
                if (paginationUIController != null) {
                    paginationUIController.createPageForEntries(lastPageIndex);
                }
            }
        }
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
            Alert alert = new Alert(Alert.AlertType.ERROR, "敢乱输就敢出bug。");
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