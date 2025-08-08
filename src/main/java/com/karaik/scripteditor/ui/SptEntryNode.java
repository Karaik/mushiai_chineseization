package com.karaik.scripteditor.ui;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptChecker;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public class SptEntryNode extends VBox {

    public static final int MAX_TEXT_LENGTH = 24;
    private static final double ORIGINAL_TEXT_AREA_PREF_WIDTH = 350;
    private static final double TRANSLATED_TEXT_AREA_PREF_WIDTH = 350;

    private SptEntry entry;
    private Runnable onModified;

    private Label metaLabel;
    private Button copyBtn;
    private VBox originalColContainer;
    private VBox translatedColContainer;
    private Button addBtnInstance;

    private List<Runnable> disposables = new ArrayList<>();
    private ListChangeListener<StringProperty> translatedSegmentsListener;

    private final List<HBox> translatedSegmentRows = new ArrayList<>();

    public SptEntryNode() {
        super(5);
        this.setPadding(new Insets(5));

        // 2. 在构造函数中构建一次UI骨架
        metaLabel = new Label();
        metaLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em;");

        copyBtn = new Button("复制本条");

        HBox metaRow = new HBox(5, metaLabel, copyBtn);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(metaLabel, Priority.ALWAYS);

        originalColContainer = new VBox(3);
        originalColContainer.getChildren().add(new Label("原文:"));

        translatedColContainer = new VBox(3);
        translatedColContainer.getChildren().add(new Label("译文:"));

        addBtnInstance = new Button("(+)");
        HBox addBox = new HBox(addBtnInstance);
        addBox.setAlignment(Pos.CENTER_RIGHT);
        addBox.setPadding(new Insets(3, 0, 0, 0));
        // 将 addBox 添加到 translatedColContainer 的末尾，之后动态行会插在它前面
        translatedColContainer.getChildren().add(addBox);

        HBox body = new HBox(10, originalColContainer, translatedColContainer);
        HBox.setHgrow(originalColContainer, Priority.ALWAYS);
        HBox.setHgrow(translatedColContainer, Priority.ALWAYS);

        this.getChildren().addAll(metaRow, body);
    }

    public void updateData(SptEntry newEntry, Runnable newOnModified) {
        // 清理上一个条目的监听器和绑定
        dispose();

        this.entry = newEntry;
        this.onModified = newOnModified;

        if (this.entry == null) {
            this.setVisible(false); // 如果没有数据，直接隐藏节点
            return;
        }

        this.setVisible(true);

        // 3. 只更新UI组件的内容，而不是重建它们
        // 更新元数据
        metaLabel.setText(this.entry.getIndex() + " | " + this.entry.getAddress() + " | " + this.entry.getLength());
        EventHandler<ActionEvent> copyAction = e -> copyWholeEntryInternal(this.entry);
        copyBtn.setOnAction(copyAction);
        addDisposable(() -> copyBtn.setOnAction(null));

        // 更新原文列
        updateOriginalColumn();

        // 更新译文列
        updateTranslatedColumn();

        // 绑定译文段落列表的增删变化
        ListChangeListener<StringProperty> translatedSegmentsListener = c -> {
            // 当列表变化时，只需重新更新译文列即可
            updateTranslatedColumn();
            if (this.onModified != null) this.onModified.run();
        };
        this.entry.getTranslatedSegments().addListener(translatedSegmentsListener);
        addDisposable(() -> this.entry.getTranslatedSegments().removeListener(translatedSegmentsListener));

        // 绑定添加按钮事件
        EventHandler<ActionEvent> addAction = e -> {
            if (this.entry.getTranslatedSegments().size() < 4) {
                this.entry.addTranslatedSegment("");
            }
        };
        addBtnInstance.setOnAction(addAction);
        addDisposable(() -> addBtnInstance.setOnAction(null));
    }

    private void updateOriginalColumn() {
        // 从索引1开始，保留"原文:"标签
        originalColContainer.getChildren().remove(1, originalColContainer.getChildren().size());

        List<ReadOnlyStringWrapper> originalSegs = entry.getOriginalSegments();
        if (originalSegs.isEmpty() || (originalSegs.size() == 1 && (originalSegs.get(0).get() == null || originalSegs.get(0).get().isEmpty()))) {
            Label empty = new Label("(原文为空)");
            empty.setPadding(new Insets(2));
            originalColContainer.getChildren().add(empty);
        } else {
            originalSegs.forEach(segWrapper -> {
                TextArea ta = new TextArea(segWrapper.get());
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setPrefWidth(ORIGINAL_TEXT_AREA_PREF_WIDTH);
                ta.setPrefHeight(calculateTextAreaHeightBasedOnContent(segWrapper.get(), ta.getFont()));
                originalColContainer.getChildren().add(ta);

                // 监听原文变化（如果可能的话）
                ChangeListener<String> listener = (obs, oldV, newV) -> ta.setText(newV);
                segWrapper.addListener(listener);
                addDisposable(() -> segWrapper.removeListener(listener));
            });
        }
    }

    private void updateTranslatedColumn() {
        // 从索引1开始，保留"译文:"标签，并保留末尾的addBox
        int childCount = translatedColContainer.getChildren().size();
        if (childCount > 2) {
            translatedColContainer.getChildren().remove(1, childCount - 1);
        }

        ObservableList<StringProperty> segs = this.entry.getTranslatedSegments();
        for (StringProperty segProp : segs) {
            HBox row = buildSegRowInternal(segProp);
            // 插入到addBox之前
            translatedColContainer.getChildren().add(translatedColContainer.getChildren().size() - 1, row);
        }

        updateAddBtnState(addBtnInstance, segs);
    }

    private void buildAndConfigureUI() {
        this.getChildren().clear();
        if (this.entry == null) return;

        metaLabel = new Label(this.entry.getIndex() + " | " + this.entry.getAddress() + " | " + this.entry.getLength());
        metaLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em;");

        Button copyBtn = new Button("复制本条");
        EventHandler<ActionEvent> copyAction = e -> copyWholeEntryInternal(this.entry);
        copyBtn.setOnAction(copyAction);
        addDisposable(() -> copyBtn.setOnAction(null));

        HBox metaRow = new HBox(5, metaLabel, copyBtn);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(metaLabel, Priority.ALWAYS);
        this.getChildren().add(metaRow);

        originalColContainer = new VBox(3);
        translatedColContainer = new VBox(3);

        buildOriginalColumnInternal();
        buildTranslatedColumnInternalAndAttachListener();

        HBox body = new HBox(10, originalColContainer, translatedColContainer);
        HBox.setHgrow(originalColContainer, Priority.ALWAYS);
        HBox.setHgrow(translatedColContainer, Priority.ALWAYS);
        this.getChildren().add(body);
    }

    private void buildOriginalColumnInternal() {
        originalColContainer.getChildren().clear();
        originalColContainer.getChildren().add(new Label("原文:"));
        List<ReadOnlyStringWrapper> originalSegs = entry.getOriginalSegments();
        if (originalSegs.isEmpty() || (originalSegs.size() == 1 && (originalSegs.get(0).get() == null || originalSegs.get(0).get().isEmpty()))) {
            Label empty = new Label("(原文为空)");
            empty.setPadding(new Insets(2));
            originalColContainer.getChildren().add(empty);
        } else {
            originalSegs.forEach(segWrapper -> {
                TextArea ta = new TextArea(segWrapper.get());
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setPrefWidth(ORIGINAL_TEXT_AREA_PREF_WIDTH);
                ta.setPrefHeight(calculateTextAreaHeightBasedOnContent(segWrapper.get(), ta.getFont()));
                originalColContainer.getChildren().add(ta);
                ChangeListener<String> originalTextListener = (obs, oldV, newV) -> {
                    ta.setText(newV);
                };
                segWrapper.addListener(originalTextListener);
                addDisposable(() -> segWrapper.removeListener(originalTextListener));
            });
        }
    }

    private void buildTranslatedColumnInternalAndAttachListener() {
        rebuildFullTranslatedColumnUI();
        ObservableList<StringProperty> segs = this.entry.getTranslatedSegments();
        if (segs != null) {
            translatedSegmentsListener = c -> {
                boolean needsRebuild = false;
                while (c.next()) {
                    if (c.wasAdded() || c.wasRemoved() || c.wasReplaced() || c.wasUpdated()) {
                        needsRebuild = true;
                        break;
                    }
                }
                if (needsRebuild) {
                    rebuildFullTranslatedColumnUI();
                    if (this.onModified != null) this.onModified.run();
                }
            };
            segs.addListener(translatedSegmentsListener);
        }
    }

    private void rebuildFullTranslatedColumnUI() {
        translatedColContainer.getChildren().clear();
        translatedColContainer.getChildren().add(new Label("译文:"));
        ObservableList<StringProperty> segs = this.entry.getTranslatedSegments();
        if (segs != null) {
            for (StringProperty segProp : segs) {
                translatedColContainer.getChildren().add(buildSegRowInternal(segProp));
            }
        }
        addBtnInstance = new Button("(+)");
        updateAddBtnState(addBtnInstance, segs);
        EventHandler<ActionEvent> addAction = e -> {
            if (this.entry.getTranslatedSegments().size() < 4) {
                this.entry.addTranslatedSegment("");
            }
        };
        addBtnInstance.setOnAction(addAction);
        addDisposable(() -> { if (addBtnInstance != null) addBtnInstance.setOnAction(null); });
        HBox addBox = new HBox(addBtnInstance);
        addBox.setAlignment(Pos.CENTER_RIGHT);
        addBox.setPadding(new Insets(3, 0, 0, 0));
        translatedColContainer.getChildren().add(addBox);
    }

    private HBox buildSegRowInternal(StringProperty prop) {
        TextArea ta = new TextArea();
        ta.setWrapText(true);
        ta.textProperty().bindBidirectional(prop);
        addDisposable(() -> {
            if (prop != null) {
                try {
                    ta.textProperty().unbindBidirectional(prop);
                } catch (IllegalArgumentException e) { /* ignore */ }
            }
        });
        ta.setPrefWidth(TRANSLATED_TEXT_AREA_PREF_WIDTH);
        ta.setPrefHeight(calculateTextAreaHeightBasedOnContent(prop.get(), ta.getFont()));

        Label counter = new Label();
        counter.setStyle("-fx-font-size: 0.8em; -fx-text-fill: grey;");
        Label illegal = new Label();
        illegal.setStyle("-fx-font-size: 0.8em; -fx-text-fill: red;");

        Runnable refreshUIForRow = () -> {
            String text = prop.get() == null ? "" : prop.get();
            counter.setText(text.length() + "/" + MAX_TEXT_LENGTH);
            StringBuilder badChars = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (!SptChecker.checkChar(c) && badChars.indexOf(String.valueOf(c)) < 0) {
                    badChars.append(c);
                }
            }
            boolean isTooLong = text.length() > MAX_TEXT_LENGTH;
            boolean hasBadChars = badChars.length() > 0;
            ta.getStyleClass().remove("text-area-red-overflow");
            if (isTooLong || hasBadChars) {
                ta.getStyleClass().add("text-area-red-overflow");
            }
            illegal.setText(hasBadChars ? "非法字符: " + badChars : "");
        };

        ChangeListener<String> textChangeListener = (o, ov, nv) -> {
            refreshUIForRow.run();
            if (this.onModified != null) this.onModified.run();
        };
        ta.textProperty().addListener(textChangeListener);
        addDisposable(() -> ta.textProperty().removeListener(textChangeListener));
        refreshUIForRow.run();

        Button rm = new Button("-");
        EventHandler<ActionEvent> removeAction = e -> {
            int idx = this.entry.getTranslatedSegments().indexOf(prop);
            if (idx != -1) {
                this.entry.removeTranslatedSegment(idx);
            }
        };
        rm.setOnAction(removeAction);
        addDisposable(() -> rm.setOnAction(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox infoLine = new HBox(5, counter, spacer, illegal);
        infoLine.setAlignment(Pos.CENTER_LEFT);
        VBox taBox = new VBox(3, ta, infoLine);
        HBox.setHgrow(taBox, Priority.ALWAYS);
        HBox row = new HBox(3, taBox, rm);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static double calculateTextAreaHeightBasedOnContent(String text, Font font) {
        if (text == null || text.isEmpty()) return 30.0;
        Font f = (font != null) ? font : Font.getDefault();
        int lines = text.split("\r\n|\r|\n", -1).length;
        double lineHeight = f.getSize() * 1.4 + 5;
        double calculatedHeight = Math.max(1, lines) * lineHeight;
        return Math.max(30.0, Math.min(calculatedHeight, 150.0));
    }

    private static void updateAddBtnState(Button btn, ObservableList<StringProperty> segs) {
        if (btn == null) return;
        boolean disable = segs == null || segs.size() >= 4;
        btn.setDisable(disable);
        btn.setTooltip(disable ? new Tooltip("最多只能有4个译文段落") : null);
    }

    private static void copyWholeEntryInternal(SptEntry entry) {
        if (entry == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("○").append(entry.getIndex()).append("|")
                .append(entry.getAddress()).append("|")
                .append(entry.getLength()).append("○ ")
                .append(entry.getFullOriginalText()).append("\n")
                .append("●").append(entry.getIndex()).append("|")
                .append(entry.getAddress()).append("|")
                .append(entry.getLength()).append("● ")
                .append(entry.getFullTranslatedText());
        ClipboardContent cc = new ClipboardContent();
        cc.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private void addDisposable(Runnable disposable) {
        this.disposables.add(disposable);
    }

    public void dispose() {
        if (this.entry != null && translatedSegmentsListener != null && this.entry.getTranslatedSegments() != null) {
            try {
                this.entry.getTranslatedSegments().removeListener(translatedSegmentsListener);
            } catch (Exception e) {
                System.err.println("Error removing translatedSegmentsListener during dispose: " + e.getMessage());
            }
            translatedSegmentsListener = null;
        }
        for (Runnable disposable : disposables) {
            try {
                disposable.run();
            } catch (Exception e) {
                System.err.println("Error during SptEntryNode internal dispose task: " + e.getMessage());
            }
        }
        disposables.clear();
    }
}