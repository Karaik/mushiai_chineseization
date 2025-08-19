package com.karaik.scripteditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class EditorApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/karaik/scripteditor/EditorView.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/karaik/scripteditor/css.css")).toExternalForm());
        URL url = getClass().getResource("/images/head.png");
        if (url != null) {
            stage.getIcons().add(new Image(url.toExternalForm()));
        }
        stage.setScene(scene);
        stage.setTitle("虫爱少女汉化文本编辑器");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
