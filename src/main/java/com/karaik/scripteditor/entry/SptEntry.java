package com.karaik.scripteditor.entry;

import com.karaik.scripteditor.controller.consts.EditorConst; // Make sure this import is correct
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class SptEntry {

    private String index;
    private String address;
    private String length;

    private List<ReadOnlyStringWrapper> originalSegmentsList;
    private ObservableList<StringProperty> translatedSegmentsList;

    public SptEntry(String index, String address, String length, String rawOriginalWithSwapFlags, String rawTranslatedWithSwapFlags) {
        this.index = index;
        this.address = address;
        this.length = length;

        String[] rawOriginals = rawOriginalWithSwapFlags.split(Pattern.quote(EditorConst.SWAP_FLAG), -1);
        String[] rawTranslateds = rawTranslatedWithSwapFlags.split(Pattern.quote(EditorConst.SWAP_FLAG), -1);

        this.originalSegmentsList = new ArrayList<>();
        for (String segment : rawOriginals) {
            if (segment.isEmpty()) continue;
            this.originalSegmentsList.add(new ReadOnlyStringWrapper(segment));
        }

        this.translatedSegmentsList = FXCollections.observableArrayList();
        for (String segment : rawTranslateds) {
            if (segment.isEmpty()) continue;
            this.translatedSegmentsList.add(new SimpleStringProperty(segment));
        }
    }

    public List<ReadOnlyStringWrapper> getOriginalSegments() {
        return originalSegmentsList;
    }

    public ObservableList<StringProperty> getTranslatedSegments() {
        return translatedSegmentsList;
    }

    public String getFullOriginalText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < originalSegmentsList.size(); i++) {
            ReadOnlyStringWrapper wrapper = originalSegmentsList.get(i);
            String segment = wrapper.get();
            builder.append(segment);
            if (i < originalSegmentsList.size() - 1) {
                builder.append(EditorConst.SWAP_FLAG);
            }
        }
        builder.append(EditorConst.SWAP_FLAG);
        return builder.toString();
    }

    public String getFullTranslatedText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < translatedSegmentsList.size(); i++) {
            StringProperty property = translatedSegmentsList.get(i);
            String segment = property.get();
            builder.append(segment);
            if (i < translatedSegmentsList.size() - 1) {
                builder.append(EditorConst.SWAP_FLAG);
            }
        }
        builder.append(EditorConst.SWAP_FLAG);
        return builder.toString();
    }

    public void addTranslatedSegment(String initialText) {
        this.translatedSegmentsList.add(new SimpleStringProperty(initialText));
    }

    public void removeTranslatedSegment(int index) {
        if (index >= 0 && index < this.translatedSegmentsList.size()) {
            this.translatedSegmentsList.remove(index);
        }
    }

    public void removeTranslatedSegment(StringProperty segmentProperty) {
        this.translatedSegmentsList.remove(segmentProperty);
    }
}