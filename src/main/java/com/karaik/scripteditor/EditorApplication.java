package com.karaik.scripteditor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EditorApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/karaik/scripteditor/EditorView.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.setTitle("虫爱少女汉化文本编辑器");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
