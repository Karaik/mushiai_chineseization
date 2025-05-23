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

        Platform.runLater(() -> {
            Scene scene = pagination.getScene();
            if (scene != null && scene.getWindow() instanceof Stage) {
                primaryStage = (Stage) scene.getWindow();
                setupPrimaryStageEventHandlers();
                updateTitle();

                File lastFile = getLastOpenedFile();
                if (lastFile != null && lastFile.exists()) {
                    fileHandlerController.openSpecificFile(lastFile);
                }

            } else {
                System.err.println("错误: 初始化时场景或舞台不可用。");
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
            if (modified) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前文件有未保存的更改，确定要退出吗？");
                alert.initOwner(primaryStage);
                Optional<ButtonType> result = alert.showAndWait();
                if (!result.isPresent() || (result.get() != ButtonType.OK && result.get() != ButtonType.YES)) {
                    event.consume();
                }
            }
        });
    }

    @FXML private void handleOpenFile() { fileHandlerController.openFile(); }
    @FXML private void handleSaveFile() { fileHandlerController.saveFile(); }

    public void setEntries(List<SptEntry> newEntries) {
        this.entries = newEntries != null ? newEntries : new ArrayList<>();
        paginationUIController.updatePaginationView();
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        updateTitle();
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
            primaryStage.setTitle((modified ? "*" : "") + fileName + " - 虫爱少女汉化文本编辑器（仅供内部使用，严禁外传）");
        }
    }

    public void rememberFile(File file) {
        if (file != null) {
            preferences.put(PREF_KEY_LAST_FILE, file.getAbsolutePath());
        }
    }

    public File getLastOpenedFile() {
        String path = preferences.get(PREF_KEY_LAST_FILE, null);
        return path != null ? new File(path) : null;
    }
}
