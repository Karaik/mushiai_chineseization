package com.karaik.scripteditor.entry;

public class SptEntry {
    private final String original;
    private final String translated;

    public SptEntry(String original, String translated) {
        this.original = original;
        this.translated = translated;
    }

    public String getOriginal() {
        return original;
    }

    public String getTranslated() {
        return translated;
    }
}
