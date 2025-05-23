package com.karaik.scripteditor.controller;

import com.karaik.scripteditor.controller.child.FileController;
import com.karaik.scripteditor.controller.child.PaginationController;
import com.karaik.scripteditor.controller.child.EntryUIController;
import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptWriter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox; // VBox 仍然保留，以防万一 FXML 中其他地方有使用，但不再作为 entryContainer
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.collections.ObservableList; // 导入 ObservableList

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EditorController {

    // @FXML private VBox entryContainer; // 不再需要这个 VBox，将其从 FXML 中移除
    @FXML private ListView<SptEntry> entryListView; // 新增 ListView
    @FXML private Pagination pagination;
    @FXML private CheckBox alwaysOnTopCheckBox;
    @FXML private ComboBox<String> itemsPerPageComboBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    private File currentFile;
    // 使用 ObservableList 来方便 ListView 的数据绑定
    private ObservableList<SptEntry> entries = FXCollections.observableArrayList();
    private boolean modified = false;
    private Stage primaryStage;

    private FileController fileController;
    private PaginationController paginationController;
    private EntryUIController entryUIController; // 用于自定义 ListCell

    private int currentItemsPerPage;

    @FXML
    public void initialize() {
        // Step 1: 初始化 ComboBox 的选项
        if (itemsPerPageComboBox != null) {
            itemsPerPageComboBox.setItems(FXCollections.observableArrayList("10", "20", "50", "100", "300", "500", "1000"));
        }

        // Step 2: 在初始化 paginationController 之前，获取 ComboBox 的当前选中值。
        if (itemsPerPageComboBox != null && itemsPerPageComboBox.getValue() != null) {
            try {
                currentItemsPerPage = Integer.parseInt(itemsPerPageComboBox.getValue());
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse initial items per page from FXML. Using default 50. Error: " + e.getMessage());
                currentItemsPerPage = 50;
            }
        } else {
            currentItemsPerPage = 50;
        }

        // Step 3: 初始化子控制器
        fileController = new FileController(this);
        entryUIController = new EntryUIController(this); // 传递 this，因为 ListCell 内部可能需要访问 mainController
        // PaginationController 不再直接操作 entryContainer，而是管理 ListView 的数据源
        paginationController = new PaginationController(this, entryListView, pagination, currentItemsPerPage);

        // 设置 ListView 的 CellFactory
        if (entryListView != null) {
            entryListView.setCellFactory(listView -> entryUIController.createSptEntryListCell());
            // 初始设置 ListView 的数据源为空，待文件加载后再更新
            entryListView.setItems(FXCollections.observableArrayList());
        }

        // Step 4: 为 ComboBox 添加监听器
        if (itemsPerPageComboBox != null) {
            itemsPerPageComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                try {
                    int newItemsPerPage = Integer.parseInt(newValue);
                    if (newItemsPerPage != currentItemsPerPage) {
                        currentItemsPerPage = newItemsPerPage;
                        paginationController.setItemsPerPage(currentItemsPerPage);
                        // 当每页条目数改变时，强制更新分页器和ListView
                        paginationController.updatePagination(entries.size());
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number for items per page: " + newValue);
                }
            });
        }

        // Step 5: 剩余的UI和事件初始化
        Platform.runLater(() -> {
            Scene scene = pagination.getScene(); // 使用 pagination 的 scene 来获取 Stage
            if (scene != null) {
                primaryStage = (Stage) scene.getWindow();
                scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && event.getCode() == KeyCode.S) {
                        handleSaveFile();
                        event.consume();
                    }
                });
                primaryStage.setOnCloseRequest(event -> {
                    if (modified) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "当前文件有未保存的更改，确定要退出吗？");
                        alert.showAndWait().ifPresent(buttonType -> {
                            if (buttonType != ButtonType.OK && buttonType != ButtonType.YES) {
                                event.consume();
                            }
                        });
                    }
                });
                updateTitle();

                if (alwaysOnTopCheckBox != null) {
                    alwaysOnTopCheckBox.setSelected(primaryStage.isAlwaysOnTop());
                }

            } else {
                // 如果在 initialize 阶段 scene 仍为 null，尝试从其他节点获取
                if (entryListView.getScene() != null && entryListView.getScene().getWindow() instanceof Stage) {
                    primaryStage = (Stage) entryListView.getScene().getWindow();
                    updateTitle();
                    if (alwaysOnTopCheckBox != null) {
                        alwaysOnTopCheckBox.setSelected(primaryStage.isAlwaysOnTop());
                    }
                } else {
                    System.err.println("Scene not available at initialize for primary stage setup.");
                }
            }
        });

        // 初始化进度条和标签
        if (progressBar != null) {
            progressBar.setProgress(0); // 初始为0
            progressBar.setVisible(false); // 默认隐藏
        }
        if (progressLabel != null) {
            progressLabel.setText("准备就绪");
        }
    }

    @FXML
    private void handleAlwaysOnTopToggle() {
        if (primaryStage != null) {
            primaryStage.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected());
        }
    }

    @FXML
    private void handleOpenFile() {
        Task<List<SptEntry>> loadFileTask = fileController.createLoadFileTask();

        if (loadFileTask == null) {
            return; // 用户取消选择
        }

        if (progressBar != null) {
            progressBar.progressProperty().bind(loadFileTask.progressProperty());
            progressBar.setVisible(true);
        }
        if (progressLabel != null) {
            progressLabel.textProperty().bind(loadFileTask.messageProperty());
        }

        loadFileTask.setOnSucceeded(event -> {
            // 将加载到的 List<SptEntry> 转换为 ObservableList 并设置给 entries
            entries.setAll(loadFileTask.getValue());
            unmarkModified();
            updateTitle();

            // 解绑进度条和标签
            if (progressBar != null) {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(1.0);
            }
            if (progressLabel != null) {
                progressLabel.textProperty().unbind();
                progressLabel.setText("文件读取完成");
            }

            // 更新 PaginationController，它会自动刷新 ListView
            paginationController.updatePagination(entries.size());

            // 延时隐藏，让用户看到完成状态
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    if (progressBar != null) progressBar.setVisible(false);
                    if (progressLabel != null) progressLabel.setText("准备就绪");
                });
            }).start();
        });

        loadFileTask.setOnFailed(event -> {
            Throwable e = loadFileTask.getException();
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0.0);
                progressBar.setVisible(false);
            }
            if (progressLabel != null) {
                progressLabel.textProperty().unbind();
                progressLabel.setText("文件读取失败: " + e.getMessage());
            }
            new Alert(Alert.AlertType.ERROR, "文件读取失败: " + e.getMessage()).showAndWait();
        });

        loadFileTask.setOnCancelled(event -> {
            if (progressBar != null) {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0.0);
                progressBar.setVisible(false);
            }
            if (progressLabel != null) {
                progressLabel.textProperty().unbind();
                progressLabel.setText("文件读取已取消");
            }
        });

        new Thread(loadFileTask).start();
    }

    @FXML
    private void handleSaveFile() {
        // 保存逻辑保持不变，SptWriter 直接从 entries 列表获取数据
        if (currentFile == null && entries.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "未打开任何文件或无内容，无法保存。").showAndWait();
            return;
        }
        if (currentFile == null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存译文文件");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file == null) {
                return;
            }
            currentFile = file;
        }

        try {
            SptWriter.saveToFile(entries, currentFile);
            modified = false;
            updateTitle();
            new Alert(Alert.AlertType.INFORMATION, "文件已保存！").showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "保存失败: " + e.getMessage()).showAndWait();
        }
    }

    // --- 公开给子控制器调用的方法 ---

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // 返回 ObservableList 以便其他部分可以观察其变化
    public ObservableList<SptEntry> getEntries() {
        return entries;
    }

    // setEntries 更改为直接操作 ObservableList
    public void setEntries(List<SptEntry> newEntries) {
        this.entries.setAll(newEntries); // 使用 setAll 替换原有内容
        paginationController.updatePagination(entries.size()); // 更新分页器
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
        updateTitle();
    }

    public boolean isModified() {
        return modified;
    }

    public void markModified() {
        if (!modified) {
            modified = true;
            updateTitle();
        }
    }

    public void unmarkModified() {
        if (modified) {
            modified = false;
            updateTitle();
        }
    }

    /**
     * 当内容被修改时，通知 ListView 刷新当前可见的 Cell。
     * 由于 ListView 是虚拟化的，它会自行处理，我们只需要确保数据更新了。
     */
    public void refreshCurrentPage() {
        // 当 SptEntry 内部的 ObservableList<StringProperty> translatedSegments 改变时，
        // ListCell 应该能够自动响应。
        // 如果需要强制刷新整个 ListView 的可见部分，可以使用以下方法，但通常不推荐频繁调用。
        // 更好的做法是确保 SptEntry 内部的属性是可观察的。
        // 这里的 `refreshCurrentPage` 是为了响应 SptEntry 内部 segments 改变。
        // 在 EntryUIController 中，添加/移除段落会调用 refreshCurrentPage()。
        // 为了确保 ListView 响应这些内部变化，可以强制刷新。
        if (entryListView != null) {
            Platform.runLater(() -> entryListView.refresh());
        }
    }

    // --- 内部辅助方法 ---

    private void updateTitle() {
        if (primaryStage != null) {
            String baseTitle = "虫爱少女汉化文本编辑器（仅用于汉化组内部使用，禁止外传）";
            String fileName = (currentFile != null) ? currentFile.getName() : "请选择文件";
            String mark = modified ? "*" : "";
            primaryStage.setTitle(mark + fileName + " - " + baseTitle);
        }
    }
}