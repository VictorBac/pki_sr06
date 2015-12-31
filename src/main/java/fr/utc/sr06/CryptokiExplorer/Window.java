package fr.utc.sr06.CryptokiExplorer;/**
 * Created by raphael on 23/12/15.
 */

import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.TokenException;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.layout.*;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

class MyLabel extends Label {

    public MyLabel(String text) {
        super(text);

        setAlignment(Pos.BASELINE_CENTER);
    }
}

public class Window extends Application {

    private VBox root;
    private final int SIZE = 60;

    private Module cryptoModule;
    private String modulePath;

    private TextField moduleField;
    private MessageIndicator messageIndicator;

    @Override
    public void start(Stage stage) {
        initUI(stage);
    }

    private void initUI(Stage stage) {
        root = new VBox();
        BorderPane subroot = new BorderPane();

        Label label1 = new Label("Load module:");

        moduleField = new TextField();
        moduleField.promptTextProperty().set("Type the module path here or click on 'choose module'...");
        moduleField.setOnAction((event) -> loadModule(moduleField.getText()));
        moduleField.prefWidthProperty().bind(root.widthProperty().divide(2));

        Button loadModuleButton = new Button();
        loadModuleButton.setText("Choose a module...");
        loadModuleButton.setOnAction((event) -> chooseAndLoadModule());

        HBox hb = new HBox();
        hb.getStyleClass().add("module-path-bar");
        hb.prefHeightProperty().bind(root.heightProperty().divide(12));
        hb.getChildren().addAll(label1, moduleField, loadModuleButton);
        hb.setAlignment(Pos.CENTER_LEFT);

        messageIndicator = new MessageIndicator();
        subroot.setLeft(getLeftListview());
        subroot.setCenter(getCenterAreaText());

        MenuBar menuBar = createMenus();
        root.getChildren().addAll(menuBar, hb, messageIndicator, subroot);


        Scene scene = new Scene(root, 1000 , 800);
        scene.getStylesheets().add("css/stylesheet.css");

        stage.getIcons().add(new Image("css/key-xxl.png"));

        stage.setTitle("BorderPane");
        stage.setScene(scene);
        stage.show();

        loadModuleButton.requestFocus();
    }

    private EventHandler<ActionEvent> ActionMenuFile() {
        return event -> {
            MenuItem mItem = (MenuItem) event.getSource();
            String side = mItem.getText();
            if ("Choose Module...".equalsIgnoreCase(side)) {
                chooseAndLoadModule();
            } else if ("Charger_Module".equalsIgnoreCase(side)) {
                System.out.println("right");
            } else if ("Charger_Module".equalsIgnoreCase(side)) {
                System.out.println("top");
            } else if ("Charger_Module".equalsIgnoreCase(side)) {
                System.out.println("bottom");
            }
        };
    }

    private void loadModule(String path) {
        try {
            cryptoModule = Module.getInstance(path);
            cryptoModule.initialize(null);
            modulePath = path;
            moduleField.setText(path);
        } catch (IOException e) {
            messageIndicator.error("Unable to load the module: " + e.getLocalizedMessage());
            e.printStackTrace();
        } catch (TokenException e) {
            messageIndicator.error("Unable to initialize the module: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void chooseAndLoadModule() {
        askModule().ifPresent((file) -> loadModule(file.getAbsolutePath()));
    }

    private Optional<File> askModule() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Module Library");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Module Files", "*.so", "*.dll", "*.dylib"));
        return Optional.ofNullable(fileChooser.showOpenDialog(root.getScene().getWindow()));
    }

    private MenuBar createMenus() {
        MenuBar menuBar = new MenuBar();

        Menu menuFile = new Menu("File");

        MenuItem module = new MenuItem("Choose Module...");
        module.setOnAction(ActionMenuFile());
        menuFile.getItems().add(module);


        Menu menuEdit = new Menu("Edit");

        Menu menuView = new Menu("View");

        menuBar.getMenus().addAll(menuFile, menuEdit, menuView);

        return menuBar;
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