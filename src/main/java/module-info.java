module com.karaik.scripteditor {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.prefs;


    opens com.karaik.scripteditor to javafx.fxml;
    exports com.karaik.scripteditor;
    exports com.karaik.scripteditor.controller;
    opens com.karaik.scripteditor.controller to javafx.fxml;
}