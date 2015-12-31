package fr.utc.sr06.CryptokiExplorer;/**
 * Created by raphael on 23/12/15.
 */

import javafx.application.Application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import javafx.event.EventHandler;
import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

class MyLabel extends Label {

    public MyLabel(String text) {
        super(text);

        setAlignment(Pos.BASELINE_CENTER);
    }
}

public class Window extends Application {

    private BorderPane root;
    private final int SIZE = 60;

    @Override
    public void start(Stage stage) {


        initUI(stage);
    }

    private void initUI(Stage stage) {

        root = new BorderPane();
        BorderPane subroot = new BorderPane();


        Label label1 = new Label("Module chargé:");
        TextField textField = new TextField ("./");
        textField.prefWidthProperty().bind(root.widthProperty().divide(2));

        HBox hb = new HBox();
        hb.prefHeightProperty().bind(root.heightProperty().divide(12));
        hb.getChildren().addAll(label1, textField);
        hb.setSpacing(10);

        hb.setAlignment(Pos.CENTER_LEFT);

        MenuBar menuBar = new MenuBar();

        Menu menuFile = new Menu("File");

        MenuItem module = new MenuItem("Charger_Module");
        module.setOnAction(ActionMenuFile(textField));
        menuFile.getItems().add(module);


        Menu menuEdit = new Menu("Edit");

        Menu menuView = new Menu("View");

        menuBar.getMenus().addAll(menuFile, menuEdit, menuView);




        root.setTop(menuBar);
        root.setCenter(subroot);
        root.setLeft(getLeftLabel());
        subroot.setTop(hb);

        // root.setTop(getTopChoice());
        root.setBottom(getBottomLabel());
        subroot.setLeft(getLeftListview());
        subroot.setRight(getRightLabel());
        subroot.setCenter(getCenterAreaText());

        Scene scene = new Scene(root, 1000 , 800);
        scene.getStylesheets().add("fr/utc/sr06/CryptokiExplorer/css/stylesheet.css");

        stage.getIcons().add(new Image("fr/utc/sr06/CryptokiExplorer/css/key-xxl.png"));


        stage.setTitle("BorderPane");
        stage.setScene(scene);
        stage.show();


    }

    private EventHandler<ActionEvent> ActionMenuFile(final TextField arg) {
        return new EventHandler<ActionEvent>() {

            public void handle(ActionEvent event) {
                MenuItem mItem = (MenuItem) event.getSource();
                String side = mItem.getText();
                if ("Charger_Module".equalsIgnoreCase(side)) {

                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Open Resource File");
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("Module Files", "*.so"));
                    File selectedFile = fileChooser.showOpenDialog(root.getScene().getWindow());
                    if (selectedFile != null) {
                            arg.setText(selectedFile.getAbsolutePath());
                    }


                } else if ("Charger_Module".equalsIgnoreCase(side)) {
                    System.out.println("right");
                } else if ("Charger_Module".equalsIgnoreCase(side)) {
                    System.out.println("top");
                } else if ("Charger_Module".equalsIgnoreCase(side)) {
                    System.out.println("bottom");
                }
            }
        };
    }


    private Label getTopLabel() {



        Label lbl = new MyLabel("Top");
        lbl.setPrefHeight(SIZE);
        lbl.prefWidthProperty().bind(root.widthProperty());
        lbl.setStyle("-fx-border-style: dotted; -fx-border-width: 0 0 1 0;"
                + "-fx-border-color: gray; -fx-font-weight: bold");

        return lbl;
    }

    private ChoiceBox getTopChoice() {


        ChoiceBox cb = new ChoiceBox();
        cb.setItems(FXCollections.observableArrayList(
                "New Document", "Open ",
                new Separator(), "Save", "Save as")
        );

        return cb;

    }


    private HBox getTopBox() {

        Label label1 = new Label("Module chargé:");
        TextField textField = new TextField ("./");
        textField.prefWidthProperty().bind(root.widthProperty().divide(2));

        HBox hb = new HBox();
        hb.prefHeightProperty().bind(root.heightProperty().divide(12));
        hb.getChildren().addAll(label1, textField);
        hb.setSpacing(10);

        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }
    private Label getBottomLabel() {

        Label lbl = new MyLabel("Bottom");
        lbl.setPrefHeight(SIZE);
        lbl.prefWidthProperty().bind(root.widthProperty());
        //lbl.setStyle("-fx-border-style: dotted; -fx-border-width: 1 0 0 0;"
        //        + "-fx-border-color: gray; -fx-font-weight: bold");

        return lbl;
    }

    private Label getLeftLabel() {

        Label lbl = new MyLabel("Left");
        lbl.setPrefWidth(SIZE);
        lbl.prefHeightProperty().bind(root.widthProperty());
        return lbl;
    }


    private TextArea getCenterAreaText() {

        TextArea lbl = new TextArea("Left");
        lbl.setPrefWidth(SIZE);
        lbl.prefHeightProperty().bind(root.heightProperty().subtract(100));

        return lbl;
    }


    private ListView getLeftListview() {

        ListView<String> list = new ListView<String>();
        list.prefWidthProperty().bind(root.widthProperty().divide(5));
        list.prefHeightProperty().bind(root.heightProperty().divide(3));
        ObservableList<String> items =FXCollections.observableArrayList (
                "Single", "Double", "Suite", "Family App");
        list.setItems(items);

        return list;
    }


    private Label getRightLabel() {

        Label lbl = new MyLabel("Right");
        lbl.setPrefWidth(SIZE);
        lbl.prefHeightProperty().bind(root.heightProperty().subtract(2*SIZE));
       // lbl.setStyle("-fx-border-style: dotted; -fx-border-width: 0 0 0 1;"
        //        + "-fx-border-color: gray; -fx-font-weight: bold");

        return lbl;
    }

    private Label getCenterLabel() {

        Label lbl = new MyLabel("Center");
        lbl.setStyle("-fx-font-weight: bold");
        lbl.prefHeightProperty().bind(root.heightProperty().subtract(2*SIZE));
        lbl.prefWidthProperty().bind(root.widthProperty().subtract(2*SIZE));

        return lbl;
    }

    public static void main(String[] args) {


        launch(args);
    }



}