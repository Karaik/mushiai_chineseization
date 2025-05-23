package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
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

    @Getter private File currentFile;
    @Getter private List<SptEntry> entries = new ArrayList<>();
    @Getter private boolean modified = false;
    @Getter private Stage primaryStage;

    private FileHandlerController fileHandlerController;
    private PaginationUIController paginationUIController;

    private static final String PREF_KEY_LAST_FILE = "lastOpenedFile";
    private final Preferences preferences = Preferences.userNodeForPackage(EditorController.class);

    public static final int ITEMS_PER_PAGE = 50;

    @FXML
    public void initialize() {
        this.fileHandlerController = new FileHandlerController(this);
        this.paginationUIController = new PaginationUIController(this);

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

                if (paginationUIController != null) {
                    paginationUIController.setupPagination(); // Initialize pagination view
                }

                File lastFile = getLastOpenedFile();
                if (lastFile != null && lastFile.exists()) {
                    fileHandlerController.openSpecificFile(lastFile);
                } else {
                    if (paginationUIController != null && entries.isEmpty()) {
                        paginationUIController.updatePaginationView();
                    }
                }
            } else {
                System.err.println("Error: Scene or Stage not available during initialization.");
            }
        });
    }

    private void setupPrimaryStageEventHandlers() {
        primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                fileHandlerController.saveFile();
                event.consume();
            }
        });

        primaryStage.setOnCloseRequest(event -> {
            rememberFile(currentFile); // Remember file before checking modification
            if (modified) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前文件有未保存的更改，确定要退出吗？");
                if (primaryStage != null) alert.initOwner(primaryStage);
                Optional<ButtonType> result = alert.showAndWait();
                if (!result.isPresent() || (result.get() != ButtonType.OK && result.get() != ButtonType.YES)) {
                    event.consume();
                }
            }
        });
    }

    @FXML private void handleOpenFile() { fileHandlerController.openFile(); }
    @FXML private void handleSaveFile() { fileHandlerController.saveFile(); }

    @FXML
    private void handleJumpToPage() { // Jump button action / TextField Enter action
        if (pageInputField == null || pagination == null || pageInputField.getText().isEmpty()) {
            return;
        }
        try {
            int pageTarget = Integer.parseInt(pageInputField.getText()) - 1;
            if (pageTarget >= 0 && pageTarget < pagination.getPageCount()) {
                pagination.setCurrentPageIndex(pageTarget);
            } else {
                new Alert(Alert.AlertType.WARNING, "页码超出范围。").showAndWait();
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "敢乱输就敢出bug。").showAndWait();
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