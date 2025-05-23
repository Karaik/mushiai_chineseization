package com.karaik.scripteditor.entry;

import com.karaik.scripteditor.controller.consts.EditorConst; // Make sure this import is correct
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public class SptEntry {

    private final String index;
    private final String address;
    private final String length;

    private final List<ReadOnlyStringWrapper> originalSegmentsList;
    private final ObservableList<StringProperty> translatedSegmentsList;

    public SptEntry(String index, String address, String length, String rawOriginalWithSwapFlags, String rawTranslatedWithSwapFlags) {
        this.index = index;
        this.address = address;
        this.length = length;

        String[] rawOriginals = rawOriginalWithSwapFlags.split(Pattern.quote(EditorConst.SWAP_FLAG), -1);
        String[] rawTranslateds = rawTranslatedWithSwapFlags.split(Pattern.quote(EditorConst.SWAP_FLAG), -1);

        this.originalSegmentsList = new ArrayList<>();
        for (String segment : rawOriginals) {
            this.originalSegmentsList.add(new ReadOnlyStringWrapper(segment));
        }

        this.translatedSegmentsList = FXCollections.observableArrayList();
        for (String segment : rawTranslateds) {
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
        return originalSegmentsList.stream()
                .map(ReadOnlyStringWrapper::get)
                .collect(Collectors.joining(EditorConst.SWAP_FLAG));
    }

    public String getFullTranslatedText() {
        return translatedSegmentsList.stream()
                .map(StringProperty::get)
                .collect(Collectors.joining(EditorConst.SWAP_FLAG));
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