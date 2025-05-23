package com.karaik.scripteditor.entry;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

@Getter
public class SptEntry {

    private final String index;
    private final String address;
    private final String length;

    private final ReadOnlyStringWrapper original;
    private final StringProperty translated;

    public SptEntry(String index, String address, String length, String original, String translated) {
        this.index = index;
        this.address = address;
        this.length = length;
        this.original = new ReadOnlyStringWrapper(original);
        this.translated = new SimpleStringProperty(translated);
    }

    public String getOriginal() {
        return original.get();
    }

    public ReadOnlyStringWrapper originalProperty() {
        return original;
    }

    public String getTranslated() {
        return translated.get();
    }

    public void setTranslated(String value) {
        translated.set(value);
    }

    public StringProperty translatedProperty() {
        return translated;
    }
}
