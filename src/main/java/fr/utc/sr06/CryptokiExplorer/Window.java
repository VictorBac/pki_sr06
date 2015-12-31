package fr.utc.sr06.CryptokiExplorer;/**
 * Created by raphael on 23/12/15.
 */

import iaik.pkcs.pkcs11.Module;
import iaik.pkcs.pkcs11.Slot;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Window extends Application {

    private VBox root;
    private final int SIZE = 60;

    private Module cryptoModule;
    private String modulePath;

    private TextField moduleField;
    private MessageIndicator messageIndicator;
    private Pane functionZone;
    private ListView<Slot> slotsSidebar;
    private ObservableList<Slot> slots;
    private ListView<UIFunction> functionsSidebar;
    private ObservableList<UIFunction> functions;

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
        subroot.setLeft(getLeftLists());
        subroot.setCenter(getFunctionZone());

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
            unloadCurrentModule();

            cryptoModule = Module.getInstance(path);
            cryptoModule.initialize(null);
            modulePath = path;
            moduleField.setText(path);

            slots.addAll(cryptoModule.getSlotList(Module.SlotRequirement.ALL_SLOTS));
            functions.addAll(new InfoFunction(t_("infoFunction"), cryptoModule), new MechanismsFunction(t_("mechanismsFunction"), cryptoModule));

            Platform.runLater(() -> {
                if (!slots.isEmpty()) {
                    slotsSidebar.getSelectionModel().select(0);
                    functionsSidebar.getSelectionModel().select(0); // select Info
                }
            });
        } catch (IOException e) {
            messageIndicator.error(t_("moduleOpenIOError"), e.getLocalizedMessage());
            e.printStackTrace();
        } catch (TokenException e) {
            messageIndicator.error(t_("moduleOpenInitError"), e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void unloadCurrentModule() {
        // sidebars hold references to the module
        slots.clear();
        functions.clear();

        if (cryptoModule != null) {
            try {
                cryptoModule.finalize(null);
            } catch (TokenException e) {
                e.printStackTrace(); // TODO propager aux appelants ?
            }

            cryptoModule = null;
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

    private Pane getFunctionZone() {
        functionZone = new HBox();
        functionZone.setPrefWidth(SIZE);
        functionZone.prefHeightProperty().bind(root.heightProperty().subtract(100));

        return functionZone;
    }


    private Node getLeftLists() {
        slotsSidebar = new ListView<>();
        slots = FXCollections.observableArrayList();
        slotsSidebar.setItems(slots);
        slotsSidebar.setPlaceholder(new Label(t_("placeholderSlotsSidebar")));
        slotsSidebar.setCellFactory((view) ->
            new ListCell<Slot>() {
                @Override
                protected void updateItem(Slot item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                    } else {
                        // TODO, c'est le bordel
                        Token token = null;
                        try {
                            token = item.getToken();
                        } catch (TokenException e) {
                            e.printStackTrace();
                        }
                        if (token != null) {
                            setText(String.format(t_("tokenPresentCell"), item.getSlotID()));
                        } else {
                            setText(String.format(t_("noTokenCell"), item.getSlotID()));
                        }
                    }
                }
            }
        );

        functionsSidebar = new ListView<>();
        functionsSidebar.prefWidthProperty().bind(root.widthProperty().divide(5));
        functionsSidebar.prefHeightProperty().bind(root.heightProperty().divide(3));
        functionsSidebar.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            newValue.load(getCurrentSlot());
            functionZone.getChildren().setAll(newValue.getUI());

            if (oldValue != null) {
                oldValue.unload();
            }
        });
        functions = FXCollections.observableArrayList();
        functionsSidebar.setItems(functions);
        functionsSidebar.setPlaceholder(new Label(t_("placeholderFunctionsSidebar")));

        VBox box = new VBox();
        box.getChildren().addAll(slotsSidebar, functionsSidebar);
        return box;
    }

    private Slot getCurrentSlot() {
        return slotsSidebar.getSelectionModel().getSelectedItem();
    }

    @Override
    /* Appelé quand l'application se termine (appel de Platform.exit() ou fermeture de la dernière fenêtre */
    public void stop() {
        unloadCurrentModule();
    }

    public static void main(String[] args) {
        Window win = new Window();
        win.launch(args);
    }
}