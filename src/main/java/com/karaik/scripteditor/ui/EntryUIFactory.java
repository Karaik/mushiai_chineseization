package com.karaik.scripteditor.ui;

import com.karaik.scripteditor.entry.SptEntry;
import com.karaik.scripteditor.util.SptChecker;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class EntryUIFactory {

    // 原文或译文单段最大长度
    public static final int MAX_TEXT_LENGTH = 24;

    // 创建条目的根节点
    public static Node createEntryNode(SptEntry entry, Runnable onModified) {

        // 创建元数据行
        HBox metaRow = new HBox(5);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // 显示 index、address、length
        Label metaLabel = new Label(entry.getIndex() + " | " + entry.getAddress() + " | " + entry.getLength());
        metaLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 0.9em;");

        // 复制整条按钮
        Button copyBtn = new Button("复制本条");
        copyBtn.setOnAction(e -> copyWholeEntry(entry));

        // 元数据行布局
        HBox.setHgrow(metaLabel, Priority.ALWAYS);
        metaRow.getChildren().addAll(metaLabel, copyBtn);

        // 创建正文区
        HBox body = new HBox(10);
        VBox originalCol = buildOriginalColumn(entry);
        VBox translatedCol = buildTranslatedColumn(entry, onModified);

        // 设置列拉伸
        HBox.setHgrow(originalCol, Priority.ALWAYS);
        HBox.setHgrow(translatedCol, Priority.ALWAYS);
        body.getChildren().addAll(originalCol, translatedCol);

        // 组装根节点
        VBox root = new VBox(5, metaRow, body);
        root.setPadding(new Insets(5));
        return root;
    }

    // 生成原文列
    private static VBox buildOriginalColumn(SptEntry entry) {
        VBox col = new VBox(3);
        col.getChildren().add(new Label("原文:"));

        // 判断原文是否为空
        if (entry.getOriginalSegments().isEmpty()
                || (entry.getOriginalSegments().size() == 1 && entry.getOriginalSegments().get(0).get().isEmpty())) {
            Label empty = new Label("(原文为空)");
            empty.setPadding(new Insets(2));
            col.getChildren().add(empty);
        } else {
            // 为每段原文创建 TextArea
            entry.getOriginalSegments().forEach(seg -> {
                TextArea ta = new TextArea(seg.get());
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setPrefHeight(10);
                col.getChildren().add(ta);
            });
        }
        return col;
    }

    // 生成译文列
    private static VBox buildTranslatedColumn(SptEntry entry, Runnable onModified) {
        VBox col = new VBox(3);
        col.getChildren().add(new Label("译文:"));

        ObservableList<StringProperty> segs = entry.getTranslatedSegments();

        // 为已有译文段落生成行
        for (int i = 0; i < segs.size(); i++) {
            col.getChildren().add(buildSegRow(entry, segs.get(i), col, onModified));
        }

        // 创建添加段落按钮
        Button addBtn = new Button("(+)");
        updateAddBtnState(addBtn, segs);
        addBtn.setOnAction(e -> {
            // 添加新段落
            StringProperty prop = entry.addTranslatedSegment("");
            onModified.run();
            Node row = buildSegRow(entry, prop, col, onModified);
            // 在添加按钮之前插入
            col.getChildren().add(col.getChildren().size() - 1, row);
            updateAddBtnState(addBtn, segs);
        });

        // 添加按钮容器
        HBox addBox = new HBox(addBtn);
        addBox.setAlignment(Pos.CENTER_RIGHT);
        addBox.setPadding(new Insets(3, 0, 0, 0));
        col.getChildren().add(addBox);

        return col;
    }

    // 生成单段译文行
    private static HBox buildSegRow(SptEntry entry,
                                    StringProperty prop,
                                    VBox parentCol,
                                    Runnable onModified) {

        // 创建可编辑文本域
        TextArea ta = new TextArea();
        ta.setWrapText(true);
        ta.textProperty().bindBidirectional(prop);
        ta.setPrefHeight(10);

        // 创建字数提示
        Label counter = new Label();
        counter.setStyle("-fx-font-size: 0.8em; -fx-text-fill: grey;");

        // 创建非法字符提示
        Label illegal = new Label();
        illegal.setStyle("-fx-font-size: 0.8em; -fx-text-fill: red;");

        // 更新样式与提示
        Runnable refresh = () -> {
            String text = prop.get() == null ? "" : prop.get();
            counter.setText(text.length() + "/" + MAX_TEXT_LENGTH);

            // 收集非法字符
            StringBuilder bad = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (!SptChecker.checkChar(c) && bad.indexOf(String.valueOf(c)) < 0) {
                    bad.append(c);
                }
            }

            // 整行变红
            boolean tooLong = text.length() > MAX_TEXT_LENGTH;
            boolean hasBad  = bad.length() > 0;
            ta.setStyle((tooLong || hasBad) ? "-fx-text-fill: red;" : "");

            // 更新非法字符提示
            illegal.setText(hasBad ? "非法字符: " + bad : "");
        };

        // 文本变化监听
        ta.textProperty().addListener((o, ov, nv) -> {
            refresh.run();
            onModified.run();
        });

        // 初始化计数与提示
        refresh.run();

        // 删除按钮
        Button rm = new Button("-");
        rm.setOnAction(e -> {
            int idx = entry.getTranslatedSegments().indexOf(prop);
            if (idx != -1) {
                entry.removeTranslatedSegment(idx);
                onModified.run();
                parentCol.getChildren().remove(rm.getParent());
                Node addBox = parentCol.getChildren().get(parentCol.getChildren().size() - 1);
                if (addBox instanceof HBox h && !h.getChildren().isEmpty()) {
                    Node btn = h.getChildren().get(0);
                    if (btn instanceof Button b) updateAddBtnState(b, entry.getTranslatedSegments());
                }
            }
        });

        // 行内放置计数与非法字符提示（非法字符在最右）
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox infoLine = new HBox(5, counter, spacer, illegal);
        infoLine.setAlignment(Pos.CENTER_LEFT);

        // 竖向布局：文本域 + 行内提示
        VBox taBox = new VBox(3, ta, infoLine);
        HBox.setHgrow(taBox, Priority.ALWAYS);

        // 整行布局
        HBox row = new HBox(3, taBox, rm);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // 更新添加按钮的可用状态
    private static void updateAddBtnState(Button addBtn, ObservableList<StringProperty> segs) {
        if (segs.size() >= 4) {
            addBtn.setDisable(true);
            addBtn.setTooltip(new Tooltip("最多只能有4个译文段落"));
        } else {
            addBtn.setDisable(false);
            addBtn.setTooltip(null);
        }
    }

    // 复制整条条目内容到剪贴板
    private static void copyWholeEntry(SptEntry entry) {
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
}
