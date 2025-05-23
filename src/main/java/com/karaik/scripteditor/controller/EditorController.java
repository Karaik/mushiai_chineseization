package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.entry.SptEntry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Pagination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter; // Lombok Getter for specific fields if needed

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EditorController {

    @FXML @Getter private VBox entryContainer; // 让子Controller可以通过Getter访问
    @FXML @Getter private Pagination pagination;   // 让子Controller可以通过Getter访问

    @Getter private File currentFile;
    @Getter private List<SptEntry> entries = new ArrayList<>();
    @Getter private boolean modified = false;
    @Getter private Stage primaryStage; // 主舞台

    private FileHandlerController fileHandlerController;
    private PaginationUIController paginationUIController;

    public static final int ITEMS_PER_PAGE = 100;

    @FXML
    public void initialize() {
        this.fileHandlerController = new FileHandlerController(this);
        this.paginationUIController = new PaginationUIController(this);

        Platform.runLater(() -> { // 延迟执行以确保Scene可用
            Scene scene = pagination.getScene();
            if (scene != null && scene.getWindow() instanceof Stage) {
                primaryStage = (Stage) scene.getWindow();
                setupPrimaryStageEventHandlers();
                updateTitle();
            } else {
                System.err.println("错误: 初始化时场景或舞台不可用。");
            }
        });
    }

    private void setupPrimaryStageEventHandlers() {
        primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> { // Ctrl+S 保存
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                fileHandlerController.saveFile();
                event.consume();
            }
        });
        primaryStage.setOnCloseRequest(event -> { // 关闭前确认
            if (modified) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前文件有未保存的更改，确定要退出吗？");
                alert.initOwner(primaryStage);
                Optional<ButtonType> result = alert.showAndWait();
                if (!result.isPresent() || (result.get() != ButtonType.OK && result.get() != ButtonType.YES)) {
                    event.consume(); // 取消关闭
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
}