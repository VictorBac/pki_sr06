package fr.utc.sr06.CryptokiExplorer;/**
 * Created by raphael on 23/12/15.
 */

import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.TokenException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;

import javafx.event.EventHandler;
import javafx.geometry.Pos;

import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.layout.*;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public class Window extends Application {

    private VBox root;
    private final int SIZE = 60;

    private Module cryptoModule;
    private String modulePath;

    private TextField moduleField;
    private MessageIndicator messageIndicator;

    private ResourceBundle translations;

    @Override
    public void start(Stage stage) {
        loadTranslations();
        initUI(stage);
    }

    private void loadTranslations() {
        try {
            translations = ResourceBundle.getBundle("translations/main", Locale.getDefault());
        } catch (MissingResourceException e) {
            translations = ResourceBundle.getBundle("translations/main", Locale.ENGLISH);
        }
    }

    private String t_(String key) {
        return translations.getString(key);
    }

    private void initUI(Stage stage) {
        root = new VBox();
        BorderPane subroot = new BorderPane();

        Label label1 = new Label(t_("modulePathLabel"));

        moduleField = new TextField();
        moduleField.promptTextProperty().set(t_("modulePathPlaceholder"));
        moduleField.setOnAction((event) -> loadModule(moduleField.getText()));
        moduleField.prefWidthProperty().bind(root.widthProperty().divide(2));

        Button loadModuleButton = new Button();
        loadModuleButton.setText(t_("chooseModuleButton"));
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

        stage.setTitle(t_("mainWindowTitle"));
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
            messageIndicator.error(t_("moduleOpenIOError"), e.getLocalizedMessage());
            e.printStackTrace();
        } catch (TokenException e) {
            messageIndicator.error(t_("moduleOpenInitError"), e.getLocalizedMessage());
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

        Menu menuFile = new Menu(t_("fileMenu"));

        MenuItem module = new MenuItem(t_("chooseModuleButton"));
        module.setOnAction((event) -> chooseAndLoadModule());

        MenuItem quit = new MenuItem(t_("quitMenuItem"));
        quit.setOnAction((event) -> Platform.exit());

        menuFile.getItems().addAll(module, quit);

        Menu menuEdit = new Menu("Edit");

        Menu menuView = new Menu("View");

        menuBar.getMenus().addAll(menuFile, menuEdit, menuView);

        return menuBar;
    }

    private TextArea getCenterAreaText() {
        TextArea lbl = new TextArea();
        lbl.setPrefWidth(SIZE);
        lbl.prefHeightProperty().bind(root.heightProperty().subtract(100));

        return lbl;
    }


    private ListView getLeftListview() {

        ListView<String> list = new ListView<>();
        list.prefWidthProperty().bind(root.widthProperty().divide(5));
        list.prefHeightProperty().bind(root.heightProperty().divide(3));
        ObservableList<String> items = FXCollections.observableArrayList (
                "Single", "Double", "Suite", "Family App");
        list.setItems(items);

        return list;
    }

    @Override
    /* Appelé quand l'application se termine (appel de Platform.exit() ou fermeture de la dernière fenêtre */
    public void stop() {
        if (cryptoModule != null) {
            try {
                cryptoModule.finalize(null);
            } catch (TokenException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Window win = new Window();
        win.launch(args);
    }
}